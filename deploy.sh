#!/usr/bin/env bash

PRODUCTS=/var/www/vhosts/elexis.ch/httpdocs/ungrad/products/${BUILD_NUMBER}
P2=/var/www/vhosts/elexis.ch/httpdocs/ungrad/p2/lucinda/${BUILD_NUMBER}

mkdir $PRODUCTS
mkdir $P2

cp lucinda-server/target/lucinda-server*.jar $PRODUCTS
cp lucinda-client/target/lucinda-client*.jar
cp -R elexis-docmgr-lucinda-p2/target/repository/* $P2
