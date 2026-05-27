import type { MemberUser } from "@/features/auth/auth-types";

export type ProfileUser = MemberUser;

export interface UpdateProfileReq {
  nickname?: string;
  avatar?: string;
}
