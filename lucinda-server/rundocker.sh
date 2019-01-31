#!/usr/bin/env bash
# run like: ./rundocker.sh full-path-to-documents

docker rm lucinda
docker run -p 2016:2016 --name lucinda -v lucinda_home:/var/lucinda/home -v $1:/var/lucinda/docs rgwch/lucinda-server:`cat VERSION`
