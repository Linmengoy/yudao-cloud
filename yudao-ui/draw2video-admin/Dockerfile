# syntax=docker/dockerfile:1.7

FROM docker.m.daocloud.io/library/node:22-alpine AS build

WORKDIR /app

COPY package.json pnpm-lock.yaml ./
RUN --mount=type=cache,id=draw2video-admin-npm,target=/root/.npm \
  --mount=type=cache,id=draw2video-admin-pnpm,target=/root/.local/share/pnpm/store \
  npm config set registry https://registry.npmmirror.com \
  && npm install -g pnpm@10 \
  && pnpm config set registry https://registry.npmmirror.com \
  && pnpm config set store-dir /root/.local/share/pnpm/store \
  && pnpm install --frozen-lockfile

COPY . .
RUN pnpm build:prod

FROM docker.m.daocloud.io/library/nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist-prod /usr/share/nginx/html

EXPOSE 80
