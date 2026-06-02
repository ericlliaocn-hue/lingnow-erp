FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/framework ./framework
COPY backend/lingnow-common ./lingnow-common
COPY backend/lingnow-core ./lingnow-core
COPY backend/lingnow-biz ./lingnow-biz
COPY backend/lingnow-app ./lingnow-app
COPY backend/lingnow-admin ./lingnow-admin
RUN mvn -pl lingnow-admin -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=build /workspace/lingnow-admin/target/lingnow-admin.jar /app/lingnow-admin.jar
RUN mkdir -p /data/lingnow/files /data/lingnow/logs
EXPOSE 6060
ENTRYPOINT ["java", "-jar", "/app/lingnow-admin.jar"]
