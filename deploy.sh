#!/usr/bin/env bash

WEBSPACE=/var/www/vhosts/elexis.ch/httpdocs/ungrad

PRODUCTS=${WEBSPACE}/products/lucinda

mkdir -p ${PRODUCTS}/${BUILD_NUMBER}

cp lucinda-server/target/lucinda-server*.jar ${PRODUCTS}/${BUILD_NUMBER}
cp lucinda-client/target/lucinda-client*.jar ${PRODUCTS}/${BUILD_NUMBER}

rm ${PRODUCTS}/latest
ln -s ${BUILD_NUMBER} ${WEBSPACE}/products/lucinda/latest
