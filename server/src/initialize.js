const config = require('config')
const path = require('path')
const app = require('./index')
const fs = require('fs')
const log = require('./logger')
const { checkSchema } = require('./solr')
const { checkStore, watchDirs } = require('./files')


module.exports = function () {
  return checkSchema(app).then(() => {
    return checkStore(app)
  }).then(ok => {
    if (config.get("watch") == true) {
      watchDirs()
    }
    return true
  }).catch(err => {
    log.error("Error in itializer: " + err)
    throw (err)
  })
}


