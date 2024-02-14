FROM ghcr.io/graalvm/graalvm-community:21 AS build

USER root

RUN microdnf install findutils

WORKDIR /code

COPY settings.gradle .
COPY build.gradle .
COPY src src
COPY gradlew gradlew
COPY gradle gradle

RUN ./gradlew nativeCompile -x test

FROM debian:12.4-slim

WORKDIR /work/

COPY --from=build /code/build/native/nativeCompile/* /work

RUN chmod 775 /work

EXPOSE 8080

CMD ["./backend", "-Xmx170M", "-Xms50M", "-Xss156k"]