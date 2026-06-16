import { cookies, headers } from "next/headers";
import { getRequestConfig } from "next-intl/server";
import { type AppLocale, defaultLocale, locales } from "./routing";

function resolveLocale(value?: string | null): AppLocale {
  const normalized = value?.split(",")[0]?.split("-")[0]?.toLowerCase();
  return locales.includes(normalized as AppLocale) ? normalized as AppLocale : defaultLocale;
}

export default getRequestConfig(async () => {
  const cookieLocale = (await cookies()).get("NEXT_LOCALE")?.value;
  const headerLocale = (await headers()).get("accept-language");
  const locale = resolveLocale(cookieLocale ?? headerLocale);

  return {
    locale,
    messages: (await import(`../messages/${locale}.json`)).default,
  };
});
