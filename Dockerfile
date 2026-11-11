# Build the React frontend first.
FROM node:20-bookworm-slim AS frontend-build
WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# Package the Spring Boot app together with the generated frontend assets.
FROM maven:3.9-eclipse-temurin-21 AS app-build
WORKDIR /workspace

COPY IntegracionLLM/.mvn/ ./IntegracionLLM/.mvn/
COPY IntegracionLLM/mvnw IntegracionLLM/pom.xml ./IntegracionLLM/
COPY IntegracionLLM/src/ ./IntegracionLLM/src/
COPY --from=frontend-build /workspace/frontend/dist/ ./frontend/dist/

RUN chmod +x ./IntegracionLLM/mvnw \
    && ./IntegracionLLM/mvnw -f ./IntegracionLLM/pom.xml -Dexec.skip=true -DskipTests package

# Use a smaller runtime image; API keys are supplied at container startup.
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=app-build /workspace/IntegracionLLM/target/bpmn-prompt-backend-0.0.1-SNAPSHOT.jar ./app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
