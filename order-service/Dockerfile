FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM curlimages/curl:8.16.0 AS otel-agent
ARG OTEL_JAVA_AGENT_VERSION=2.30.0
RUN curl --fail --silent --show-error --location \
    --output /tmp/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_JAVA_AGENT_VERSION}/opentelemetry-javaagent.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=otel-agent /tmp/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar
COPY --from=build /build/target/app.jar app.jar
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
