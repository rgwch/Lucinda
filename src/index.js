const log = require('./logger')
const init = require('./initialize')


init().then(() => {
  require('./server')
}).catch(err => {
  log.error("Could not start "+err)
})

