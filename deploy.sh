#!/usr/bin/env bash

WEBSPACE=/var/www/vhosts/elexis.ch/httpdocs/ungrad

PRODUCTS=${WEBSPACE}/products/lucinda
P2=${WEBSPACE}/p2/lucinda

mkdir -p ${PRODUCTS}/${BUILD_NUMBER}
mkdir -p ${P2}/${BUILD_NUMBER}

cp lucinda-server/target/lucinda-server*.jar ${PRODUCTS}/${BUILD_NUMBER}
cp lucinda-client/target/lucinda-client*.jar ${PRODUCTS}/${BUILD_NUMBER}
cp -R elexis-docmgr-lucinda-p2/target/repository/* ${P2}/${BUILD_NUMBER}

rm ${PRODUCTS}/latest
ln -s ${BUILD_NUMBER} ${WEBSPACE}/products/lucinda/latest
rm ${P2}/latest
ln -s ${BUILD_NUMBER} ${WEBSPACE}/p2/lucinda/latest
