FROM amazoncorretto:18 as builder
LABEL maintainer="david.lepe94@gmail.com"
ARG JAR_FILE=target/twitch-chat-analyzer-*.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM amazoncorretto:18
COPY --from=builder dependencies/ ./
COPY --from=builder snapshot-dependencies/ ./
COPY --from=builder spring-boot-loader/ ./
COPY --from=builder application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]