FROM maven:3.9-amazoncorretto-23 AS builder

WORKDIR /app
COPY pom.xml ./
COPY settings.xml ./
COPY be-core-storage-app ./be-core-storage-app
COPY be-core-storage-sdk ./be-core-storage-sdk

RUN mvn clean install -X -U -s settings.xml && \
    mvn package -pl be-core-storage-app -Dmaven.test.skip=true -s settings.xml

FROM openjdk:23-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
COPY integration/grafana-opentelemetry-java-v2.12.0.jar ./grafana-opentelemetry-java-v2.12.0.jar

CMD ["java", "-Xms128m", "-Xmx1024m", "-javaagent:grafana-opentelemetry-java-v2.12.0.jar", "-jar", "app.jar"]
