FROM gradle:8.5-jdk17 AS build

WORKDIR /app
COPY . /app/
RUN gradle build --no-daemon

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /app/server/build/libs/*.jar /app/app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]