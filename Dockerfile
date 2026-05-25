# ============================================================
#  QuickPoll — multi-stage Dockerfile
#
#  Stage 1 (build) — compiles the project inside the image so
#  the host machine does not need Maven or a JDK installed.
#  Stage 2 (runtime) — slim JRE image that only carries the JAR.
#
#  The final image is ~200 MB and starts in ~3 s.
# ============================================================

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Cache Maven dependencies — copy only the POM first, resolve, then copy sources.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Run as non-root for safety
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/target/quickpoll.jar app.jar

EXPOSE 8080

# Java flags tuned for containers — picks up container memory limits
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
