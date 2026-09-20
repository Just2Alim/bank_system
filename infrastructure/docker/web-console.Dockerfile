FROM node:22-alpine

WORKDIR /app
RUN corepack enable
COPY apps/web-console/package.json apps/web-console/pnpm-lock.yaml apps/web-console/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY apps/web-console/ ./

EXPOSE 5173
CMD ["pnpm", "dev", "--host", "0.0.0.0"]
