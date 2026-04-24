## Dataset-generation api container
#FROM tomcat:10-jre21
#
#LABEL Author="Europeana Foundation <development@europeana.eu>"
#
#WORKDIR /usr/local/tomcat/webapps
#
#ENV ELASTIC_APM_VERSION 1.52.1
#ADD https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/$ELASTIC_APM_VERSION/elastic-apm-agent-$ELASTIC_APM_VERSION.jar /usr/local/elastic-apm-agent.jar
#
#COPY dataset-generation/target/dataset-generation.war /opt/app/dataset-generation.war

## if webapi - ✔ Tomcat runs it automatically
# so we don't need that
#cron jobs are NOT web apps
#Tomcat is unnecessary overhead
#JAR model is simpler
#For dataset-generation job:
#
#✔ use JDK + JAR (no Tomcat)
FROM eclipse-temurin:21-jdk

WORKDIR /opt/app

COPY dataset-generation/target/dataset-generation.jar /opt/app/dataset-generation.jar

CMD ["java", "-jar", "dataset-generation.jar"]

