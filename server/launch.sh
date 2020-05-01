#! /bin/bash

export PATH=/root/.local/bin:/opt/${NODE_VER}/bin:/opt/${JAVA_HOME}/bin:$PATH
cd /opt/solr$${SOLR_VER}
bin/solr start -e


