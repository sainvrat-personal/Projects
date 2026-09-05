import { Module } from '@nestjs/common';
import { CalorieController } from './calorie.controller';
import { CalorieService } from './calorie.service';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Calorie } from './calorie.entity';

@Module({
  imports: [TypeOrmModule.forFeature([Calorie])],
  controllers: [CalorieController],
  providers: [CalorieService],
})
export class CalorieModule {}
