import { api } from "@/lib/api-client";
import type { ProfileUser, UpdateProfileReq } from "./profile-types";

export function getProfile() {
  return api.get<ProfileUser>("/member/user/get");
}

export function updateProfile(data: UpdateProfileReq) {
  return api.put<boolean>("/member/user/update", data);
}
