import { NextRequest } from "next/server";

const GATEWAY_HOST = process.env.NEXT_PUBLIC_GATEWAY_HOST || "111.228.39.103";
const GATEWAY_PORT = process.env.NEXT_PUBLIC_GATEWAY_PORT || "48080";
const GATEWAY_BASE_URL = `http://${GATEWAY_HOST}:${GATEWAY_PORT}`;

type RouteContext = {
  params: Promise<{
    path: string[];
  }>;
};

function resolveClientIp(headers: Headers) {
  const forwardedFor = headers.get("x-forwarded-for");
  if (forwardedFor) {
    return forwardedFor.split(",")[0]?.trim();
  }
  return headers.get("x-real-ip")?.trim();
}

async function proxy(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  const url = new URL(request.url);
  const targetUrl = new URL(`/app-api/${path.join("/")}${url.search}`, GATEWAY_BASE_URL);
  const headers = new Headers(request.headers);
  const clientIp = resolveClientIp(headers);

  headers.delete("host");
  headers.delete("connection");
  headers.delete("keep-alive");
  headers.delete("proxy-authenticate");
  headers.delete("proxy-authorization");
  headers.delete("transfer-encoding");
  headers.delete("upgrade");

  if (clientIp) {
    headers.set("x-real-ip", clientIp);
  }

  const body = request.method === "GET" || request.method === "HEAD" ? undefined : await request.arrayBuffer();
  const response = await fetch(targetUrl, {
    method: request.method,
    headers,
    body,
    cache: "no-store",
  });

  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: response.headers,
  });
}

export async function GET(request: NextRequest, context: RouteContext) {
  return proxy(request, context);
}

export async function POST(request: NextRequest, context: RouteContext) {
  return proxy(request, context);
}

export async function PUT(request: NextRequest, context: RouteContext) {
  return proxy(request, context);
}

export async function PATCH(request: NextRequest, context: RouteContext) {
  return proxy(request, context);
}

export async function DELETE(request: NextRequest, context: RouteContext) {
  return proxy(request, context);
}
