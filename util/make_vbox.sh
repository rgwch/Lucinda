# This is only a help to remember, what we need. It's not a working script

apt-get update && apt-get install -y git \
  build-essential autoconf automake libtool \
  ghostscript \
  icc-profiles-free \
  liblept5 \
  libleptonica-dev \
  libxml2 \
  libxml2-dev \
  pngquant \
  python3-cffi \
  python3-distutils \
  python3-pkg-resources \
  python3-reportlab \
  qpdf \
  img2pdf \
  tesseract-ocr tesseract-ocr-deu tesseract-ocr-fra tesseract-ocr-ita \
  zlib1g-dev wget \
  unzip \
  openjdk-11-jre-headless

wget https://nodejs.org/dist/v${NODE_VER}/${NODE_VARIANT}.tar.xz 
tar -xf ${NODE_VARIANT}.tar.xz 
ln -s /opt/${NODE_VARIANT} /opt/node
ln -s /root/.local/bin/ocrmypdf /usr/bin/ocrmypdf 
export PATH=/opt/node/bin:$PATH

wget -4 https://downloads.apache.org/tika/tika-server-1.24.1-bin.zip
wget -4 https://www.apache.org/dyn/closer.lua/lucene/solr/8.5.2/solr-8.5.2.tgz
npm install -g forever
npm install -g forever-service

forever-service install -s /home/lucinda/lucinda/src/index.js -e "NODE_CONFIG_DIR=/home/lucinda/lucinda/config LUCINDA_SIMPLEWEB=enabled NODE_ENV=vbox" lucinda
systemctl start lucinda
tail -f /var/log/lucinda.log
