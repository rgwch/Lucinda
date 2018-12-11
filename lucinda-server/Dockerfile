FROM openjdk:8-jdk-slim

RUN apt-get update && apt-get install -y tesseract-ocr tesseract-ocr-deu tesseract-ocr-eng maven git \
    && useradd -d /home/lucinda lucinda \
    && mkdir /home/lucinda \
    && chown -R lucinda.lucinda /home/lucinda \
    && cd /home/lucinda \
    && git clone https://github.com/rgwch/Lucinda \
    && cd Lucinda/lucinda-server \
    && mvn package \
    && mv target/lucinda-server-2.0.5.jar /home/lucinda/lucinda.jar \
    && chmod +x /home/lucinda/lucinda.jar \
    && apt-get -y remove maven openjdk-8-jdk git \
    && apt-get -y autoremove \
    && rm -rf /root/.m2 \
    && rm -rf /home/lucinda/Lucinda \
    && apt-get clean \
    && mkdir /var/lucinda \
    && chown lucinda.lucinda /var/lucinda

USER lucinda
ADD lucinda.cfg /home/lucinda/lucinda.cfg
WORKDIR /home/lucinda
EXPOSE 2016

CMD ["java","-jar","lucinda.jar","--config=lucinda.cfg"]

