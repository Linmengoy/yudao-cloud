import type { NextConfig } from "next";

const GATEWAY_HOST = process.env.NEXT_PUBLIC_GATEWAY_HOST || "111.228.39.103";
const GATEWAY_PORT = process.env.NEXT_PUBLIC_GATEWAY_PORT || "48080";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      {
        source: "/app-api/:path*",
        destination: `http://${GATEWAY_HOST}:${GATEWAY_PORT}/app-api/:path*`,
      },
    ];
  },
};

export default nextConfig;
