FROM gradle:7.6-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/storage/movies /app/storage/chunks /app/storage/temp
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080 9001 9002
ENTRYPOINT ["java", "-jar", "app.jar"]