FROM amazoncorretto:18

# Set the system timezone, default to UTC
ARG ARG_TIMEZONE="UTC"
ENV TZ=${ARG_TIMEZONE}
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Expose ports for debugging and app
ARG DEBUG_OPTS
ENV JAVA_OPTS=${DEBUG_OPTS}

COPY target/twitch-chat-analyzer-*.jar app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /app.jar" ]