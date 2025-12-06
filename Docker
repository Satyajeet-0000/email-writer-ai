# --- STAGE 1: BUILD ---
# Use a Maven image with a modern JDK (like Eclipse Temurin 17) to build the application
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app
# Copy the entire project into the container
COPY . .
# Run the Maven clean install command to compile and package the application into a JAR
# The -DskipTests flag speeds up the build by skipping tests
RUN mvn clean install -DskipTests

# --- STAGE 2: PACKAGE AND RUN ---
# Use a lightweight JRE-only image (smaller size) to run the application
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
# Copy the built JAR from the 'build' stage into the final image
# You must adjust the file name if your JAR is not named with a wildcard
COPY --from=build /app/target/*.jar app.jar

# Define the command to start the application when the container launches
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
