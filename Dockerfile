FROM maven:3.8.4-openjdk-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/storage ./storage
RUN mkdir -p /app/storage/movies /app/storage/chunks /app/storage/temp
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*
EXPOSE 8080 9001
ENTRYPOINT ["java", "-jar", "app.jar"]