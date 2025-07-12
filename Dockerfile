FROM gradle:latest AS builder

WORKDIR /app

# Mount cache for Konan dependencies
RUN --mount=type=cache,target=/home/gradle/.konan \
    --mount=type=cache,target=/home/gradle/.gradle \
    gradle --version

COPY build.gradle.kts gradle.properties ./
COPY gradle/ gradle/

RUN --mount=type=cache,target=/home/gradle/.konan \
    --mount=type=cache,target=/home/gradle/.gradle \
    gradle dependencies

COPY . .
RUN --mount=type=cache,target=/home/gradle/.konan \
    --mount=type=cache,target=/home/gradle/.gradle \
    gradle linkReleaseExecutableNative

FROM ubuntu:22.04

RUN apt-get update && apt-get install -y \
    libc6 \
    libgcc-s1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/build/bin/native/releaseExecutable/rinha-backend-2025.kexe ./app

EXPOSE 8080

CMD ["./app"]