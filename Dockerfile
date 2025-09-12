FROM eclipse-temurin:24-jdk-alpine as build

WORKDIR /room-mgmt

COPY gradle ./gradle
COPY gradlew ./gradlew
COPY room-management.yaml ./room-management.yaml
COPY settings.gradle.kts ./settings.gradle.kts
COPY src ./src
COPY build.gradle.kts ./build.gradle.kts

RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:24-jre-alpine

RUN addgroup --system spring && adduser --system spring --ingroup spring

WORKDIR /room-mgmt

COPY --from=build /room-mgmt/build/libs/app.jar ./app.jar

RUN chown -R spring:spring /room-mgmt

USER spring

EXPOSE 8080

ENTRYPOINT exec java -XX:+UseZGC -Xmx512M -jar app.jar


