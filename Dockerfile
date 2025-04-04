### 1. Gradle 빌드
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Gradle Wrapper관련 파일 복사
COPY gradlew .
COPY gradle/ ./gradle/
COPY build.gradle .
COPY settings.gradle .

# 소스 복사
COPY src/ ./src/

# gradlew 실행 권한 부여
RUN chmod +x gradlew

# Gradle 빌드 수행
RUN ./gradlew clean build

### 2. 최종 실행 환경
FROM eclipse-temurin:21-jdk

# 빌드된 JAR 파일을 복사
# 버전이 바뀌어도 build/libs/ 내부의 하나의 JAR만 복사하려면, 아래처럼 와일드카드를 사용
COPY --from=builder /app/build/libs/*.jar ravo-manager.jar

# 실행 포트
EXPOSE 8080

# 컨테이너 실행 시 Spring Boot JAR 파일 실행
ENTRYPOINT ["java", "-jar", "/ravo-manager.jar"]
