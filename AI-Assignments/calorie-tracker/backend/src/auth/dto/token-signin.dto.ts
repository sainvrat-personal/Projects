import { IsString, IsNotEmpty } from 'class-validator';

export class TokenSigninDto {
  @IsNotEmpty()
  @IsString()
  token: string;
}
