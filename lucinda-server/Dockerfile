FROM openjdk:8-jdk as build-stage
LABEL MAINTAINER="weirich@elexis.ch" \
      VERSION=2.1.4 \
      DESCRIPTION="Lucene powered file indexing and storage"

RUN apt-get update && apt-get install -y maven

WORKDIR /build

# To avoid downloading half the internet on each build, we create an intermediate layer with all
# maven dependencies.

COPY pom.xml .
RUN mvn dependency:go-offline


COPY src/ /build/src/


RUN  mvn package \
    && mv target/lucinda-server-2.1.4.jar \
    && chmod +x /lucinda.jar



FROM openjdk:8-jre-slim
LABEL MAINTAINER="weirich@elexis.ch" \
      VERSION=2.1.4\
      DESCRIPTION="Lucene powered file indexing and storage"


VOLUME "/var/lucinda/home"
VOLUME "/var/lucinda/docs"

ENV LUCINDA_HOME /var/lucinda/home
ENV LUCINDA_LANGUAGE de
ENV LUCINDA_DOCUMENTS /var/lucinda/docs

COPY --from=build-stage lucinda.jar .

RUN apt-get update \
    && apt-get install -y tesseract-ocr tesseract-ocr-deu \
    && apt-get clean \
    && chmod -R 777 /var/lucinda/docs

EXPOSE 2016

CMD ["java","-Xms512m","-Xmx8192m","-jar","lucinda.jar"]

