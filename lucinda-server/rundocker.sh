#!/usr/bin/env bash

docker rm lucinda
docker run -p 2016:2016 --name lucinda -v $1:/home/lucinda/data rgwch/lucinda:2.0.1
