FROM frolvlad/alpine-oraclejre8:cleaned
COPY maven /opt/app/
WORKDIR /opt/app
ENTRYPOINT java $JAVA_OPTIONS -jar ./app.jar
EXPOSE 8080