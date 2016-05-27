#!/usr/bin/env bash

PRODUCTS=/var/www/vhosts/elexis.ch/httpdocs/ungrad/products/lucinda/${BUILD_NUMBER}
P2=/var/www/vhosts/elexis.ch/httpdocs/ungrad/p2/lucinda/${BUILD_NUMBER}

mkdir -p $PRODUCTS
mkdir -p $P2

cp lucinda-server/target/lucinda-server*.jar $PRODUCTS
cp lucinda-client/target/lucinda-client*.jar $PRODUCTS
cp -R elexis-docmgr-lucinda-p2/target/repository/* $P2
