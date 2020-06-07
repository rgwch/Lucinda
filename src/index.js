/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const log = require('./logger')
const cfg = require('config')
const { checkSchema } = require('./solr')
const { checkStore, watchDirs } = require('./files')
const fetch = require('node-fetch')

log.info("Lucinda Server: logger created")
log.debug("Debug level active ")
log.info("Environment is " + process.env.NODE_ENV)
log.info("LUCINDA_DOCBASE "+process.env_LUCINDA_DOCBASE)
log.info("LUCINDA_PORT "+process.env.LUCINDA_PORT)
log.info("LUCINDA_ATTIC "+process.env.LUCINDA_ATTIC)
log.info("LUCINDA_SIMPLEWEB "+process.env.LUCINDA_SIMPLEWEB)
log.info("SOLR_DATA "+process.env.SOLR_DATA)
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

try {
  const tika = cfg.get("tika")
  const solr = cfg.get("solr")

  fetch(`${tika.host}:${tika.port}`).then(hasTika => {
    if (hasTika.status !== 200) {
      throw new Error("Could not access Tika")
    }
  }).catch(err => {
    log.error("Please launch Solr and Tika first.\n==>" + err)
    log.error("Caught " + err)

  })
  fetch(`${solr.host}:${solr.port}`).then(hasSolr => {
    if (hasSolr.status !== 200) {
      throw new Error("Could not access Solr")
    }
  }).catch(err => {
    log.error("Please launch Solr and Tika first.\n==>" + err)
  })
} catch (ex) {
  log.error("Solr and Tika must be defined in the config")
  process.exit(1)
}
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

