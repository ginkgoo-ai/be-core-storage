FROM maven:3.9-amazoncorretto-23 AS builder
ARG GITHUB_USER
ARG GITHUB_TOKEN

WORKDIR /app
COPY pom.xml ./
COPY settings.xml ./
COPY be-core-storage-app ./be-core-storage-app
COPY be-core-storage-sdk ./be-core-storage-sdk

RUN mvn clean install -U -s settings.xml && \
    mvn package -pl be-core-storage-app -Dmaven.test.skip=true -s settings.xml

FROM openjdk:23-jdk-slim
WORKDIR /app

RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/be-core-storage-app/target/*.jar app.jar
COPY integration/grafana-opentelemetry-java-v2.12.0.jar ./grafana-opentelemetry-java-v2.12.0.jar

CMD ["java", "-Xms128m", "-Xmx1024m", "-javaagent:grafana-opentelemetry-java-v2.12.0.jar", "-jar", "app.jar"]
