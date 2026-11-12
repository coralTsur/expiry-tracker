FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw package -DskipTests
CMD ["sh", "-c", "java -Dspring.profiles.active=demo -Dserver.port=${PORT:-8080} -jar target/*.jar"]