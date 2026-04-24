FROM eclipse-temurin:21-jre-alpine
WORKDIR /opt/app

LABEL Author="Europeana Foundation <development@europeana.eu>"

ENV ELASTIC_APM_VERSION 1.52.1
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar /usr/local/elastic-apm-agent.jar

COPY dataset-serving/target/dataset-serving.jar /opt/app/dataset-serving.jar

RUN mkdir -p /opt/app/storage

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "dataset-serving.jar"]
