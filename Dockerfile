FROM tomcat:10-jre21

LABEL Author="Europeana Foundation <development@europeana.eu>"

WORKDIR /usr/local/tomcat/webapps

ENV ELASTIC_APM_VERSION 1.52.1
ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar /usr/local/elastic-apm-agent.jar

COPY dataset-serving/target/dataset-api ./ROOT/

#RUN mkdir -p /opt/app/storage/XML

EXPOSE 8080
