FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml ./poc/pom.xml
COPY src ./poc/src
COPY libs/cloud-sdk-1.0.3.jar ./cloud-sdk-1.0.3.jar

RUN mvn install:install-file \
  -Dfile=cloud-sdk-1.0.3.jar \
  -DgroupId=com.dji \
  -DartifactId=cloud-sdk \
  -Dversion=1.0.3 \
  -Dpackaging=jar \
  -DgeneratePom=true

RUN cd poc && mvn clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/poc/target/*.jar app.jar

EXPOSE 6789

ENTRYPOINT ["java", "-jar", "app.jar"]