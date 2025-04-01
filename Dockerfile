FROM openjdk:17-jdk-slim  AS build

WORKDIR /app

COPY . .

RUN ./gradlew build -x test

FROM openjdk:17-jdk-slim

COPY --from=build /app/build/libs/curtaincall-0.0.1-SNAPSHOT.jar /app/curtaincall.jar

CMD ["java", "-Dspring.profiles.active=prod", "-jar", "/app/curtaincall.jar"]