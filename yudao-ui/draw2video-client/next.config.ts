import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      {
        source: "/app-api/:path*",
        destination: "http://yudao-gateway:48080/app-api/:path*",
      },
    ];
  },
};

export default nextConfig;
