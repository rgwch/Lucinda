FROM ubuntu:20.04 as base
FROM base as builder 
LABEL maintainer="weirich@webelexis.ch"
LABEL version="3.0.0"

WORKDIR /opt

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive

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
    export PATH=$HOME/.local/bin:$PATH && \
    python3 -m pip install --user ocrmypdf

RUN git clone https://github.com/agl/jbig2enc && \
    cd jbig2enc && \
    ./autogen.sh && ./configure && make && make install

RUN wget https://nodejs.org/dist/v12.16.3/node-v12.16.3-linux-x64.tar.xz && \
    tar -xf node-v12.16.3-linux-x64.tar.xz

COPY server server

WORKDIR /opt/server

RUN npm install



