FROM ubuntu:20.04
LABEL maintainer="weirich@webelexis.ch"
LABEL version="3.0.0"

WORKDIR /opt

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive
ENV NODE_VER=node-v12.16.3-linux-x64
ENV JAVA_VER=jdk-11.0.7+10
ENV JAVA_HOME=/opt/${JAVA_VER}
ENV SOLR_VER=7.7.2
ENV TIKA_VER=1.24.1
COPY server server

RUN apt-get update && apt-get -y upgrade && apt-get install -y git \
    build-essential autoconf automake libtool \
    ghostscript \
    icc-profiles-free \
    liblept5 \
    libleptonica-dev \
    libxml2 \
    pngquant \
    python3-cffi \
    python3-distutils \
    python3-pkg-resources \
    python3-reportlab \
    qpdf \
    tesseract-ocr tesseract-ocr-deu tesseract-ocr-fra tesseract-ocr-ita \
    zlib1g-dev wget

RUN wget https://bootstrap.pypa.io/get-pip.py && python3 get-pip.py && \
    python3 -m pip install --user ocrmypdf

RUN git clone https://github.com/agl/jbig2enc && \
    cd jbig2enc && \
    ./autogen.sh && ./configure && make && make install

RUN wget https://nodejs.org/dist/v12.16.3/${NODE_VER}.tar.xz && \
    tar -xf ${NODE_VER}.tar.xz && \
    ln -s /opt/${NODE_VER}/bin/node /usr/bin/node && \
    ln -s /opt/${NODE_VER}/bin/npm /usr/bin/npm && \
    ln -s /root/.local/bin/ocrmypdf /usr/bin/ocrmypdf 

# Integrate java, solr and tika - postponed
# RUN wget https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.7%2B10/OpenJDK11U-jdk_x64_linux_hotspot_11.0.7_10.tar.gz && \
#    tar -xf OpenJDK11U-jdk_x64_linux_hotspot_11.0.7_10.tar.gz && \
#    ln -s /opt/${JAVA_VER}/bin/java /usr/bin/java

# RUN wget https://archive.apache.org/dist/lucene/solr/${SOLR_VER}/solr-${SOLR_VER}.tgz && \
#    tar -xf solr-${SOLR_VER}.tgz

#RUN wget https://downloads.apache.org/tika/tika-server-${TIKA_VER}.jar && \
#    pip install supervisor

WORKDIR /opt/server
RUN npm install

EXPOSE 9997

#CMD ["supervisord", "--nodaemon"]
CMD ["node","index,js"]

