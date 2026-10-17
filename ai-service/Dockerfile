# ========== 阶段1：Maven 构建（多模块，构建 common + ai-service） ==========
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先复制所有模块的 pom.xml，利用 Docker 缓存层加速依赖下载
COPY pom.xml ./
COPY common/pom.xml common/
COPY user-service/pom.xml user-service/
COPY exercise-service/pom.xml exercise-service/
COPY learning-engine-service/pom.xml learning-engine-service/
COPY ai-service/pom.xml ai-service/
COPY api-gateway/pom.xml api-gateway/

# 复制源码（common 是公共依赖，必须一起复制）
COPY common/src common/src
COPY ai-service/src ai-service/src

# 构建 ai-service 及其依赖模块 common，跳过测试
RUN mvn -B clean package -pl ai-service -am -DskipTests

# ========== 阶段2：运行时镜像（仅 JRE，体积小） ==========
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /build/ai-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
