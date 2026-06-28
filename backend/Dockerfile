# ---- 构建阶段：用 Maven 编译出可执行 jar（走阿里云镜像加速）----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 国内镜像加速依赖下载
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- 运行阶段：仅含 JRE ----
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
