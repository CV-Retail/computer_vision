# Stage 1: Build
FROM node:20-alpine AS build
ARG BUILD_TARGET=all
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Runtime
FROM nginxinc/nginx-unprivileged:alpine
# Defaults for the reverse proxy; override them with environment variables (see nginx/default.conf.template).
ENV BACKEND_HOST=core-backend \
    BACKEND_PORT=8080 \
    DNS_RESOLVER=127.0.0.11 \
    CLIENT_MAX_BODY_SIZE=100m
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
# 127.0.0.1 and not localhost: localhost resolves to ::1 first and nginx listens on IPv4 only.
HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/ || exit 1
