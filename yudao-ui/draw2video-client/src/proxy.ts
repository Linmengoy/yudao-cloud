import { NextResponse, type NextRequest } from "next/server";
import { type AppLocale, defaultLocale, locales } from "./i18n/routing";

function resolveLocale(request: NextRequest): AppLocale {
  const cookieLocale = request.cookies.get("NEXT_LOCALE")?.value;
  if (locales.includes(cookieLocale as AppLocale)) return cookieLocale as AppLocale;

  const headerLocale = request.headers
    .get("accept-language")
    ?.split(",")[0]
    ?.split("-")[0]
    ?.toLowerCase();

  return locales.includes(headerLocale as AppLocale) ? headerLocale as AppLocale : defaultLocale;
}

export function proxy(request: NextRequest) {
  const headers = new Headers(request.headers);
  headers.set("X-NEXT-INTL-LOCALE", resolveLocale(request));

  return NextResponse.next({ request: { headers } });
}

export const config = {
  matcher: ["/((?!api|_next|.*\\..*).*)"],
};
