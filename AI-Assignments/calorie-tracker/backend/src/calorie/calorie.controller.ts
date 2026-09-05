import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  ParseIntPipe,
  Post,
  Put,
  Query,
  ValidationPipe,
} from '@nestjs/common';
import { CalorieService } from './calorie.service';
import { CreateCalorieDto } from './dto/create-calorie.dto';
import { CurrentUser } from 'src/app/current-user.decorator';
import { User } from 'src/user/user.entity';
import { GetCalorieDto } from './dto/get-calorie.dto';

@Controller('calories')
export class CalorieController {
  constructor(private readonly calorieService: CalorieService) {}

  @Post()
  createCalorie(
    @Body(ValidationPipe) createCalorieDto: CreateCalorieDto,
    @CurrentUser() user: User,
  ) {
    return this.calorieService.createCalorie(createCalorieDto, user);
  }

  @Get()
  getCalories(
    @Query(new ValidationPipe({ transform: true }))
    getCalorieDto: GetCalorieDto,
    @CurrentUser() user: User,
  ) {
    return this.calorieService.getCalories(getCalorieDto, user);
  }

  @Get('by-day')
  getCaloriesByDay(
    @Query(new ValidationPipe({ transform: true }))
    getCalorieDto: GetCalorieDto,
    @CurrentUser() user: User,
  ) {
    return this.calorieService.getCaloriesByDay(getCalorieDto, user);
  }

  @Put(':id')
  updateCalorie(
    @Param('id', ParseIntPipe) id: number,
    @Body() body: Partial<CreateCalorieDto>,
    @CurrentUser() user: User,
  ) {
    return this.calorieService.updateCalorie(id, user, body);
  }

  @Delete(':id')
  deleteCalorie(
    @Param('id', ParseIntPipe) id: number,
    @CurrentUser() user: User,
  ) {
    return this.calorieService.softDeleteCalorie(id, user);
  }

  @Post('test-data')
  generateTestData(@CurrentUser() user: User) {
    return this.calorieService.addTestData(user);
  }
}
