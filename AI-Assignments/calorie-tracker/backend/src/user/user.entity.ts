import {
  BaseEntity,
  Entity,
  PrimaryGeneratedColumn,
  Column,
  Unique,
  OneToMany,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';
import { UserStatusEnum } from './user.enum';
import { Calorie } from '../calorie/calorie.entity';

@Entity()
@Unique('UQ_user_email', ['email'])
export class User extends BaseEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ type: 'text' })
  email: string;

  @Column({ type: 'text', nullable: true })
  name?: string;

  @Column({ type: 'text', nullable: true })
  phone?: string;

  @Column({ type: 'text', nullable: true })
  picture?: string;

  @Column({ type: 'text', nullable: true })
  accessToken?: string;

  @Column({ default: UserStatusEnum.ACTIVE })
  status: UserStatusEnum;

  @Column({ nullable: true })
  emailVerified?: boolean;

  @OneToMany(() => Calorie, (calorie) => calorie.user)
  calories?: Calorie[];

  @CreateDateColumn()
  createdAt: Date;

  @UpdateDateColumn()
  updatedAt: Date;
}
