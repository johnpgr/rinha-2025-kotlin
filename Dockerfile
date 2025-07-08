FROM gradle:latest AS builder

WORKDIR /app
COPY build.gradle.kts gradle.properties ./
COPY gradle/ gradle/

RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.konan \
    gradle dependencies

RUN apt-get update && apt-get install -y \
    libsqlite3-dev \
  && rm -rf /var/lib/apt/lists/*

COPY . .
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/root/.konan \
    gradle linkReleaseExecutableNative

FROM ubuntu:22.04

RUN apt-get update && apt-get install -y \
    libc6 \
    libgcc-s1 \
    libsqlite3-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/build/bin/native/releaseExecutable/app.kexe ./app

EXPOSE 8080

CMD ["./app"]