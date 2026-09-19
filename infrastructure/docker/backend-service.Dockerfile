FROM eclipse-temurin:21-jdk AS build

ARG SERVICE_MODULE
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY libs libs
COPY services services

RUN ./mvnw -q -pl "services/${SERVICE_MODULE}" -am -DskipTests package

FROM eclipse-temurin:21-jre

ARG SERVICE_MODULE
WORKDIR /app

COPY --from=build "/workspace/services/${SERVICE_MODULE}/target/${SERVICE_MODULE}-0.1.0-SNAPSHOT-exec.jar" /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
