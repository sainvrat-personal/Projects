import { Body, Controller, Post, ValidationPipe } from '@nestjs/common';
import { AuthService } from './auth.service';
import { TokenSigninDto } from './dto/token-signin.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('/token-signin')
  tokenSignin(@Body(ValidationPipe) tokenSigninDto: TokenSigninDto) {
    return this.authService.tokenSignin(tokenSigninDto);
  }
}
