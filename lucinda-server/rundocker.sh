#!/usr/bin/env bash

docker rm lucinda
docker run -p 2016:2016 --name lucinda -v $1:/var/lucinda/base -v $2:/var/lucinda/data rgwch/lucinda-server:`cat VERSION`
