FROM openjdk:8-jdk-slim as build-stage
LABEL MAINTAINER="weirich@elexis.ch" \
      VERSION=2.1.2 \
      DESCRIPTION="Lucene powered file indexing and storage"

RUN apt-get update && apt-get install -y maven git \
    && git clone https://github.com/rgwch/Lucinda \
    && cd Lucinda/lucinda-server \
    && mvn package \
    && mv target/lucinda-server-2.1.2.jar /lucinda.jar \
    && chmod +x /lucinda.jar



FROM openjdk:8-jre-slim
LABEL MAINTAINER="weirich@elexis.ch" \
      VERSION=2.1.2 \
      DESCRIPTION="Lucene powered file indexing and storage"


VOLUME "/var/lucinda/home"
VOLUME "/var/lucinda/docs"

ENV LUCINDA_HOME /var/lucinda/home
ENV LUCINDA_LANGUAGE de
ENV LUCINDA_DOCUMENTS /var/lucinda/docs

COPY --from=build-stage lucinda.jar .

RUN apt-get update \
    && apt-get install -y tesseract-ocr tesseract-ocr-deu tesseract-ocr-eng \
    && apt-get clean \
    && chmod -R 777 /var/lucinda/docs

EXPOSE 2016

CMD ["java","-Xms512m","-Xmx8192m","-jar","lucinda.jar"]

