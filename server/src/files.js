const crypt = require('crypto')
const walker = require('walkdir')
const watcher = require('chokidar')
const config = require('config')
const scfg = config.get("solr")
const app = require('./index')
const log = require('./logger')
const solr = require('solr-client').createClient({
    host: "localhost",
    port: scfg.port,
    core: scfg.core
})

const makeFileID = (app, filepath) => {
    const base = app._basepath
    if (filepath.startsWith("file://")) {
        filepath = filepath.substring(7)
    }
    if (filepath.startsWith(base)) {
        filepath = filepath.substring(base.length)
    }
    const hash = crypt.createHash('md5')
    hash.update(filepath)
    return hash.digest('hex')
}

const files = []
const addFile = (file) => {
    files.push(file)
}

const checkStore = () => {
    return new Promise((resolve, reject) => {
        const base = app._basepath
        const emitter = walker(base)
        emitter.on('file', async (filename, stat) => {
            const q = solr.createQuery().q({ id: makeFileID(app, filename) })
            solr.search(q,(err, obj) => {
                if (err) {
                    log.error(err)
                } else {
                    if (obj.response.numkFound == 0) {
                        addFile(filename)
                    } else {
                        log.debug(obj[0].id)
                    }
                }
            })
        })
        emitter.on('end', () => {
            resolve(true)
        })
        emitter.on('error', () => {
            reject(new Error("can't read path " + base))
        })
        emitter.on('fail', p => {
            log.warn("failed on " + p)
        })
    })
}

const watchDirs = () => {
    let storage = app._basepath
    watcher.watch(storage, {
        ignored: /(^|[\/\\])\../,
        followSymlinks: false
    })
        .on('add', async fp => {
            try {
                await service.create({ contents: "file://" + fp }, { inPlace: true })
                log.info("added " + fp)
            } catch (err) {
                log.error("Error adding " + fp + ", " + err)
            }
        })
        .on('change', async fp => {
            try {
                await service.update({ contents: "file://" + fp })
            } catch (err) {
                log.error("Error updating " + fp + ", " + err)
            }
        })
        .on('unlink', async fp => {
            try {
                await service.remove(api.makeFileID(app, fp))
            } catch (err) {
                log.error("Error removing " + fp + ", " + err)
            }
        })
        .on('error', err => { log.error(err) })

}
module.exports = {
    makeFileID,
    checkStore,
    watchDirs
}
