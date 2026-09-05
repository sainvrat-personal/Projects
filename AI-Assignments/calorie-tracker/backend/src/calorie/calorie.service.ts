import { Injectable } from '@nestjs/common';
import { CreateCalorieDto } from './dto/create-calorie.dto';
import { Repository } from 'typeorm';
import { Calorie } from './calorie.entity';
import { InjectRepository } from '@nestjs/typeorm';
import { User } from 'src/user/user.entity';
import { GetCalorieDto } from './dto/get-calorie.dto';

@Injectable()
export class CalorieService {
  constructor(
    @InjectRepository(Calorie)
    private readonly calorieRepository: Repository<Calorie>,
  ) {}

  createCalorie(createCalorieDto: CreateCalorieDto, user: User) {
    const calorie = this.calorieRepository.create({
      ...createCalorieDto,
      user,
    });
    return this.calorieRepository.save(calorie);
  }

  getCalories(getCalorieDto: GetCalorieDto, user: User) {
    const { startDate, endDate, skip, limit } = getCalorieDto;

    const query = this.calorieRepository
      .createQueryBuilder('calorie')
      .where('calorie.userId = :userId', { userId: user.id })
      .andWhere('calorie.deleted = :deleted', { deleted: false });

    if (startDate) {
      query.andWhere('calorie.createdAt >= :startDate', { startDate });
    }
    if (endDate) {
      query.andWhere('calorie.createdAt <= :endDate', { endDate });
    }
    if (skip) {
      query.skip(skip);
    }
    if (limit) {
      query.take(limit);
    }
    query.orderBy('calorie.createdAt', 'DESC');

    return query.getMany();
  }

  async updateCalorie(id: number, user: User, update: Partial<Calorie>) {
    const calorie = await this.calorieRepository.findOne({
      where: { id, userId: user.id, deleted: false },
    });
    if (!calorie) throw new Error('Calorie entry not found');
    Object.assign(calorie, update);
    return this.calorieRepository.save(calorie);
  }

  async softDeleteCalorie(id: number, user: User) {
    const calorie = await this.calorieRepository.findOne({
      where: { id, userId: user.id, deleted: false },
    });
    if (!calorie) throw new Error('Calorie entry not found');
    calorie.deleted = true;
    return this.calorieRepository.save(calorie);
  }

  async getCaloriesByDay(getCalorieDto: GetCalorieDto, user: User) {
    const { startDate, endDate, skip = 0, limit = 10 } = getCalorieDto;

    const query = this.calorieRepository
      .createQueryBuilder('calorie')
      .select([
        `strftime('%Y-%m-%d', calorie.createdAt) as date`,
        'SUM(calorie.calories) as totalCalories',
      ])
      .where('calorie.userId = :userId', { userId: user.id })
      .andWhere('calorie.deleted = :deleted', { deleted: false });

    if (startDate) {
      query.andWhere('calorie.createdAt >= :startDate', { startDate });
    }
    if (endDate) {
      query.andWhere('calorie.createdAt <= :endDate', { endDate });
    }

    query.groupBy('date').orderBy('date', 'DESC').skip(skip).take(limit);

    const raw: { date: string; totalCalories: string | number }[] =
      await query.getRawMany();

    return raw.map((row): { date: string; totalCalories: number } => ({
      date: String(row.date),
      totalCalories: Number(row.totalCalories),
    }));
  }

    async addTestData(user: User) {
    const FOOD_NAMES = [
      'Apple', 'Banana', 'Pizza', 'Burger', 'Salad', 'Chicken', 'Rice', 'Eggs', 'Pasta', 'Steak',
      'Fish', 'Soup', 'Sandwich', 'Fries', 'Ice Cream', 'Yogurt', 'Cereal', 'Juice', 'Milk', 'Coffee',
      'Chicken Soup', 'Omelette', 'Tofu', 'Beans', 'Bread', 'Muffin', 'Sushi', 'Dumplings', 'Curry', 'Noodles',
    ];
    function randomInt(min: number, max: number) {
      return Math.floor(Math.random() * (max - min + 1)) + min;
    }
    function getDateNDaysAgo(n: number) {
      const d = new Date();
      d.setDate(d.getDate() - n);
      d.setHours(randomInt(6, 8), randomInt(0, 59), randomInt(0, 59), 0);
      return d;
    }
    let totalInserted = 0;
    for (let daysAgo = 0; daysAgo < 30; daysAgo++) {
      let dailyTotal = 0;
      const dailyTarget = randomInt(1600, 2500);
      let mealCount = 0;
      const currentDate = getDateNDaysAgo(daysAgo);
      while (dailyTotal < dailyTarget) {
        const calorie = this.calorieRepository.create();
        calorie.description = FOOD_NAMES[randomInt(0, FOOD_NAMES.length - 1)];
        const maxCal = Math.min(500, dailyTarget - dailyTotal);
        if (maxCal < 50) break;
        calorie.calories = randomInt(50, maxCal);
        calorie.user = user;
        calorie.createdAt = new Date(currentDate.getTime() + mealCount * 3 * 60 * 60 * 1000);
        calorie.updatedAt = calorie.createdAt;
        await this.calorieRepository.save(calorie);
        dailyTotal += calorie.calories;
        mealCount++;
        totalInserted++;
      }
    }
    return { success: true, message: `Inserted ${totalInserted} records for 30 days for userId ${user.id}.` };
  }
}
