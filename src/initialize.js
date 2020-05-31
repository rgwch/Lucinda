const config = require('config')
const log = require('./logger')
const { checkSchema } = require('./solr')
const { checkStore, watchDirs } = require('./files')


module.exports = function () {
  return checkSchema().then(() => {
    return checkStore()
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


