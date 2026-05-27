export type AuthMode = "sms" | "register" | "password";

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
