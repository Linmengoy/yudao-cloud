export const locales = ["zh", "en"] as const;
export type AppLocale = (typeof locales)[number];
export const defaultLocale: AppLocale = "zh";
