import { createParamDecorator, ExecutionContext } from '@nestjs/common';
import { User } from 'src/user/user.entity';

export const CurrentUser = createParamDecorator(
  (data: string, ctx: ExecutionContext): User => {
    const req: { user?: User } = ctx.switchToHttp().getRequest();
    return req?.user as User;
  },
);
