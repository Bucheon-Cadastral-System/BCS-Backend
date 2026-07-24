FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon bootJar
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# 이미지 변환에 사용하는 cwebp 설치
RUN apk add --no-cache libwebp-tools
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]