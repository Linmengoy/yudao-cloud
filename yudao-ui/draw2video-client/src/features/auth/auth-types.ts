export type AuthMode = "sms" | "email" | "register" | "password" | "forgot";

export interface LoginToken {
  userId: number;
  accessToken: string;
  refreshToken: string;
  expiresTime?: number;
  openid?: string;
}

export interface MemberUser {
  id: number;
  nickname?: string;
  mobile?: string;
  email?: string;
  avatar?: string;
  status?: number;
  sex?: number;
  areaName?: string;
  birthday?: number | string;
  createTime?: number | string;
  loginDate?: number | string;
}

export interface PasswordLoginReq {
  mobile: string;
  password: string;
}

export interface SmsLoginReq {
  mobile: string;
  code: string;
}

export interface EmailLoginReq {
  email: string;
  password: string;
}

export interface EmailCodeLoginReq {
  email: string;
  code: string;
}

export interface SendSmsCodeReq {
  mobile: string;
  scene: string;
}

export interface EmailRegisterReq {
  email: string;
  code: string;
  password: string;
  agreeTerms: boolean;
  inviteCode?: string;
}

export interface SendEmailCodeReq {
  email: string;
  scene: string;
}

export interface ValidateEmailCodeReq {
  email: string;
  scene: string;
  code: string;
}

export interface ResetPasswordByEmailReq {
  email: string;
  code: string;
  password: string;
}
