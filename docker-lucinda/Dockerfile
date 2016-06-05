FROM java:8-jre
MAINTAINER weirich@elexis.ch
RUN adduser --system lucinda
RUN apt-get update && apt-get -y upgrade && apt-get -y install tesseract-ocr && apt-get -y clean
WORKDIR /home/lucinda
RUN wget http://www.elexis.ch/ungrad/products/lucinda/latest/lucinda-server-1.1.1-SNAPSHOT.jar
ADD lucinda.cfg lucinda.cfg
EXPOSE 2016
USER lucinda
ENTRYPOINT ["java","-jar","lucinda-server-1.1.1-SNAPSHOT.jar","--config=lucinda.cfg","-d"]
