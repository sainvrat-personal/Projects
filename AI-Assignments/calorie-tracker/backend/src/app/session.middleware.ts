import { Injectable, Logger, NestMiddleware } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectRepository } from '@nestjs/typeorm';
import { NextFunction, Request, Response } from 'express';
import { User } from 'src/user/user.entity';
import { Repository } from 'typeorm';

@Injectable()
export class SessionMiddleware implements NestMiddleware {
  private readonly logger = new Logger(SessionMiddleware.name);

  constructor(
    @InjectRepository(User)
    private readonly usersRepository: Repository<User>,

    private readonly jwtService: JwtService,
  ) {}

  async use(req: Request, res: Response, next: NextFunction) {
    try {
      const authorization = req.headers.authorization;
      if (!authorization) {
        this.logger.warn('Authorization header is missing');
        req['user'] = null;
        return next();
      }

      const token = authorization.startsWith('Bearer ')
        ? authorization.slice(7)
        : authorization;
      if (!token) {
        this.logger.warn('Token is missing in the request');
        req['user'] = null;
        return next();
      }

      const { email } = this.jwtService.verify<User>(token);
      const existingUser = await this.usersRepository.findOne({
        where: { email },
      });
      if (!existingUser) {
        this.logger.warn('Invalid user');
        req['user'] = null;
        return next();
      }

      req['user'] = existingUser;
      next();
      this.logger.log(`User ${existingUser.email} authenticated successfully`);
    } catch (error) {
      this.logger.error('Error during user authentication', error);
      res.status(401).json({ message: 'Unauthorized' });
    }
  }
}
