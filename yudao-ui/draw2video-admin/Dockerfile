FROM docker.m.daocloud.io/library/node:22-alpine AS build

WORKDIR /app

COPY package.json pnpm-lock.yaml ./
RUN npm config set registry https://registry.npmmirror.com \
  && npm install -g pnpm@10 \
  && pnpm config set registry https://registry.npmmirror.com \
  && pnpm install --frozen-lockfile

COPY . .
ARG ADMIN_BUILD_MODE=prod
RUN pnpm "build:${ADMIN_BUILD_MODE}" \
  && rm -rf /app/dist-release \
  && cp -R "/app/dist-${ADMIN_BUILD_MODE}" /app/dist-release

FROM docker.m.daocloud.io/library/nginx:1.27-alpine

ENV ADMIN_GATEWAY_HOST=host.docker.internal
ENV ADMIN_GATEWAY_PORT=48080

COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist-release /usr/share/nginx/html

EXPOSE 80
