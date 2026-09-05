import { IsInt, IsNotEmpty, IsString, Min } from 'class-validator';

export class CreateCalorieDto {
  @IsNotEmpty()
  @IsString()
  description: string;

  @IsNotEmpty()
  @IsInt()
  @Min(1)
  calories: number;
}
