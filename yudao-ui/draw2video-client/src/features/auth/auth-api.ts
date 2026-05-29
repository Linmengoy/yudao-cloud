import { api } from "@/lib/api-client";
import type {
  EmailCodeLoginReq,
  EmailLoginReq,
  EmailRegisterReq,
  LoginToken,
  PasswordLoginReq,
  ResetPasswordByEmailReq,
  SendEmailCodeReq,
  SendSmsCodeReq,
  SmsLoginReq,
  ValidateEmailCodeReq,
} from "./auth-types";

export function loginByPassword(data: PasswordLoginReq) {
  return api.post<LoginToken>("/member/auth/login", data);
}

export function loginBySms(data: SmsLoginReq) {
  return api.post<LoginToken>("/member/auth/sms-login", data);
}

export function loginByEmail(data: EmailLoginReq) {
  return api.post<LoginToken>("/member/auth/email-login", data);
}

export function loginByEmailCode(data: EmailCodeLoginReq) {
  return api.post<LoginToken>("/member/auth/email-code-login", data);
}

export function sendSmsCode(data: SendSmsCodeReq) {
  return api.post<boolean>("/member/auth/send-sms-code", data);
}

export function sendEmailCode(data: SendEmailCodeReq) {
  return api.post<boolean>("/member/auth/send-email-code", data);
}

export function validateEmailCode(data: ValidateEmailCodeReq) {
  return api.post<boolean>("/member/auth/validate-email-code", data);
}

export function registerByEmail(data: EmailRegisterReq) {
  return api.post<LoginToken>("/member/auth/email-register", data);
}

export function resetPasswordByEmail(data: ResetPasswordByEmailReq) {
  return api.put<boolean>("/member/user/reset-password-by-email", data);
}

export function logout() {
  return api.post<boolean>("/member/auth/logout");
}
