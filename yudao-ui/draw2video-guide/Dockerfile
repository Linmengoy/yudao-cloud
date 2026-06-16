FROM docker.m.daocloud.io/library/node:22-alpine AS build

WORKDIR /app

COPY package.json pnpm-lock.yaml ./
RUN npm config set registry https://registry.npmmirror.com \
  && npm install -g pnpm@10 \
  && pnpm config set registry https://registry.npmmirror.com \
  && pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM docker.m.daocloud.io/library/nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html/guide

EXPOSE 80

