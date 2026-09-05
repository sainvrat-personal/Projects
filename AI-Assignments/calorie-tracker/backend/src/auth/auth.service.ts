import { Injectable, Logger } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { TokenSigninDto } from './dto/token-signin.dto';
import { InjectRepository } from '@nestjs/typeorm';
import { User } from 'src/user/user.entity';
import { Repository } from 'typeorm';
import { ConfigService } from '@nestjs/config';
import { OAuth2Client } from 'google-auth-library';

@Injectable()
export class AuthService {
  private readonly logger = new Logger(AuthService.name);

  private readonly googleClient: OAuth2Client;

  constructor(
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,

    private readonly jwtService: JwtService,

    private readonly configService: ConfigService,
  ) {
    this.googleClient = new OAuth2Client(
      this.configService.get<string>('GOOGLE_CLIENT_ID'),
    );
  }

  async tokenSignin(tokenSigninDto: TokenSigninDto): Promise<User | null> {
    try {
      const ticket = await this.googleClient.verifyIdToken({
        idToken: tokenSigninDto.token,
        audience: this.configService.get<string>('GOOGLE_CLIENT_ID'),
      });

      const payload = ticket.getPayload();
      if (!payload) {
        throw new Error('Invalid token payload');
      }

      const { email, name, picture } = payload;
      if (!email) {
        throw new Error('Missing required user information from token');
      }

      let user = await this.usersRepository.findOne({
        where: { email },
      });

      if (!user) {
        user = new User();
        user.email = email;
      }

      user.name = name;
      user.emailVerified = true;
      user.picture = picture;

      const accessToken = this.jwtService.sign({ email });
      user.accessToken = accessToken;

      return user.save();
    } catch (error) {
      this.logger.error('Error verifying token', error);
      throw new Error('Token verification failed');
    }
  }
}
