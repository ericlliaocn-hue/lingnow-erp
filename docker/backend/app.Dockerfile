FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/framework ./framework
COPY backend/lingnow-common ./lingnow-common
COPY backend/lingnow-core ./lingnow-core
COPY backend/lingnow-biz ./lingnow-biz
COPY backend/lingnow-app ./lingnow-app
COPY backend/lingnow-admin ./lingnow-admin
RUN mvn -pl lingnow-app -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=build /workspace/lingnow-app/target/lingnow-app.jar /app/lingnow-app.jar
RUN mkdir -p /data/lingnow/files /data/lingnow/logs
EXPOSE 6061
ENTRYPOINT ["java", "-jar", "/app/lingnow-app.jar"]
