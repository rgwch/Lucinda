#!/usr/bin/env bash

WEBSPACE=/var/www/vhosts/elexis.ch/httpdocs/ungrad/

PRODUCTS=${WEBSPACE}products/lucinda/${BUILD_NUMBER}
P2=${WEBSPACE}p2/lucinda/${BUILD_NUMBER}

mkdir -p $PRODUCTS
mkdir -p $P2

cp lucinda-server/target/lucinda-server*.jar $PRODUCTS
cp lucinda-client/target/lucinda-client*.jar $PRODUCTS
cp -R elexis-docmgr-lucinda-p2/target/repository/* $P2

rm ${PRODUCTS}/latest
ln -s ${BUILD_NUMBER} ${WEBSPACE}products/lucinda/latest
rm ${P2}/latest
ln -s ${BUILD_NUMBER} ${WEBSPACE}p2/lucinda/latest