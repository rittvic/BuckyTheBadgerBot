FROM maven:3.8-openjdk-18 as build

WORKDIR /app

ADD pom.xml .

RUN mvn verify clean --fail-never

ADD . .

RUN mvn install

FROM openjdk:18.0.2-jdk

WORKDIR /app

COPY --from=build /app/target/BuckyTheBadgerBot-*.jar /app/app.jar

CMD ["java","-jar","app.jar"]
