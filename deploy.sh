#!/usr/bin/env bash


PRODUCTS=${WEBSPACE}/products/lucinda

mkdir -p ${PRODUCTS}/${BUILD_NUMBER}

cp lucinda-server/target/lucinda-server*.jar ${PRODUCTS}/${BUILD_NUMBER}
cp lucinda-client/target/lucinda-client*.jar ${PRODUCTS}/${BUILD_NUMBER}

rm ${PRODUCTS}/latest
ln -s ${PRODUCTS}/${BUILD_NUMBER} ${PRODUCTS}/latest
