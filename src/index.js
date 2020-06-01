const log = require('./logger')
const init = require('./initialize')
const cfg = require('config')

log.info("Lucinda Server: logger created")
log.debug("Debug level active ")
log.info("Environment is " + process.env.NODE_ENV)
log.info("preferred language is " + cfg.get("preferredLanguage"))

init()
.then(() => {
  require('./server')
}).catch(err => {
  log.error("Could not start " + err)
})

