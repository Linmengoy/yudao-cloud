export const getCaptchaResponse = (res: any) => {
  return res?.repCode ? res : res?.data?.repCode ? res.data : res
}
