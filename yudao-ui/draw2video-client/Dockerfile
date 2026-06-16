FROM docker.m.daocloud.io/library/node:22-alpine AS deps

WORKDIR /app

COPY package.json pnpm-lock.yaml ./
RUN npm config set registry https://registry.npmmirror.com \
  && npm install -g pnpm@10 \
  && pnpm config set registry https://registry.npmmirror.com \
  && pnpm install --frozen-lockfile

FROM docker.m.daocloud.io/library/node:22-alpine AS build

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .
ARG NEXT_PUBLIC_API_BASE_URL=""
ARG NEXT_PUBLIC_APP_API_PREFIX=/app-api
ARG NEXT_PUBLIC_WS_BASE_URL=""
ARG NEXT_PUBLIC_TENANT_ID=1
ARG NEXT_PUBLIC_TERMINAL=20
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV NEXT_PUBLIC_APP_API_PREFIX=${NEXT_PUBLIC_APP_API_PREFIX}
ENV NEXT_PUBLIC_WS_BASE_URL=${NEXT_PUBLIC_WS_BASE_URL}
ENV NEXT_PUBLIC_TENANT_ID=${NEXT_PUBLIC_TENANT_ID}
ENV NEXT_PUBLIC_TERMINAL=${NEXT_PUBLIC_TERMINAL}
RUN npm run build

FROM docker.m.daocloud.io/library/node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static

EXPOSE 3000

CMD ["node", "server.js"]
