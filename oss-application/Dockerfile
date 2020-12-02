FROM openjdk:8-jre-alpine
MAINTAINER "KeStrong"<kestrong@foxmail.com>
VOLUME /tmp
ENV JAVA_OPTS=""
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
COPY bootJar.sh bootJar.sh
CMD  ["sh", "bootJar.sh"]