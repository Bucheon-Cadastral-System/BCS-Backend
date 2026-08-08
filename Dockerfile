FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon bootJar
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# 파일 이름 인코딩. 알파인은 로케일이 비어 있어 JVM 의 sun.jnu.encoding 이 ASCII 로 잡히고,
# 그러면 한글이 들어간 사진 파일 이름을 경로로 만들 때 InvalidPathException 이 난다
# (file.encoding 은 JDK 18 부터 UTF-8 이 기본이지만 파일 이름 인코딩은 로케일을 따른다).
ENV LANG=C.UTF-8

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]