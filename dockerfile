FROM openjdk:11-jre-slim

ENV APP_HOME=/app
ENV MONGODB_URI=${MONGODB_URI}
ENV DB_NAME=${DB_NAME:"warehouse"}
RUN mkdir $APP_HOME
WORKDIR $APP_HOME
COPY build/libs/app.jar $APP_HOME/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]