FROM openjdk:12-jdk-slim

WORKDIR /app

COPY . .

RUN ./gradlew build

CMD ["java", "-jar", "build/libs/*.jar"]