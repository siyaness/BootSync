FROM node:22-alpine AS frontend-build
WORKDIR /frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/index.html ./
COPY frontend/components.json ./
COPY frontend/postcss.config.js ./
COPY frontend/tailwind.config.js ./
COPY frontend/tsconfig.json ./
COPY frontend/tsconfig.node.json ./
COPY frontend/vite.config.ts ./
COPY frontend/public public
COPY frontend/src src

ENV VITE_BASE_PATH=/app/
RUN npm run build

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
ENV TZ=Asia/Seoul

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src
COPY --from=frontend-build /frontend/dist frontend/dist

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
ENV TZ=Asia/Seoul
ENV SPRING_PROFILES_ACTIVE=prod

RUN groupadd --system bootsync \
    && useradd --system --gid bootsync --create-home --home-dir /app bootsync \
    && chown bootsync:bootsync /app

WORKDIR /app

COPY --from=build --chown=bootsync:bootsync /workspace/build/libs/*.jar app.jar

USER bootsync

EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
