#!/usr/bin/env bash

cd lucinda-server
mvn clean package -Dmaven.test.skip=true
