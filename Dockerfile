FROM ubuntu:20.04
LABEL maintainer="weirich@webelexis.ch"
LABEL version="3.0.0"

WORKDIR /opt

ENV LC_ALL=C.UTF-8
ENV LANG=C.UTF-8
ENV DEBIAN_FRONTEND=noninteractive
ENV NODE_VER=12.18.0
ENV NODE_VARIANT=node-v${NODE_VER}-linux-x64
ENV NODE_ENV=dockered

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
  img2pdf \
  tesseract-ocr tesseract-ocr-deu tesseract-ocr-fra tesseract-ocr-ita \
  zlib1g-dev wget

RUN wget https://bootstrap.pypa.io/get-pip.py && python3 get-pip.py && \
  python3 -m pip install --user ocrmypdf

RUN git clone https://github.com/agl/jbig2enc && \
  cd jbig2enc && \
  ./autogen.sh && ./configure && make && make install

RUN wget https://nodejs.org/dist/v${NODE_VER}/${NODE_VARIANT}.tar.xz && \
  tar -xf ${NODE_VARIANT}.tar.xz && \
  ln -s /opt/${NODE_VARIANT}/bin/node /usr/bin/node && \
  ln -s /opt/${NODE_VARIANT}/bin/npm /usr/bin/npm && \
  ln -s /root/.local/bin/ocrmypdf /usr/bin/ocrmypdf 

COPY package*.json ./
RUN npm ci --only=production
COPY . .

EXPOSE 9997

CMD ["node","src/index.js"]

