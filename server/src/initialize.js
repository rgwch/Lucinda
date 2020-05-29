const config = require('config')
const path = require('path')
const app = require('./index')
const fs = require('fs')
const log = require('./logger')
const { checkSchema } = require('./solr')
const { checkStore, watchDirs } = require('./files')

module.exports = () => {
    let base = config.get('documentRoot')
    if (base.startsWith('~/')) {
        base = base.substring(2)
    }
    if (!base.startsWith("/")) {
        base = path.join(process.env.HOME, base)
    }
    app._basepath = base
    fs.mkdir(base, { recursive: true }, err => {
        if (err) {
            if (err.code != "EEXIST") {
                throw (err)
            }
            log.debug(err)
        }
    })
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


