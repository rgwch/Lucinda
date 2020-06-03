/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const log = require('./logger')
const cfg = require('config')
const { checkSchema } = require('./solr')
const { checkStore, watchDirs } = require('./files')

log.info("Lucinda Server: logger created")
log.debug("Debug level active ")
log.info("Environment is " + process.env.NODE_ENV)
log.info("preferred language is " + cfg.get("preferredLanguage"))

/**
 * Initialize: 
 * * First check if the solr core 'lucindadata'
 * exists and has the correct schema.
 * If nor, rename the solr core 'gettingstarted'
 * to 'lucindadata' and create the schema.
 * 
 * * Second walk the directory tree of the lucinda filebase
 * * Third, Setup the dir watcher, if configured
 * * Fourth: launch the REST server
 */
checkSchema()
  .then(() => {
    return checkStore()
  })
  .then(() => {
    if (cfg.get("watch") == true) {
      watchDirs()
    }
    require('./server')
  })
  .catch(err => {
    log.error("Could not start " + err)
  })

