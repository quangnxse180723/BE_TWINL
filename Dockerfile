# Giai đoạn 1: Build file .jar
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build project bỏ qua bước chạy Test để tiết kiệm thời gian
RUN mvn clean package -DskipTests

# Giai đoạn 2: Chạy ứng dụng
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy file .jar từ Giai đoạn 1 sang Giai đoạn 2
COPY --from=build /app/target/*.jar app.jar
# Expose port mặc định của Spring Boot
EXPOSE 8080
# Lệnh khởi chạy
ENTRYPOINT ["java", "-Dspring.profiles.active=supabase", "-jar", "app.jar"]
