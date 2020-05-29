const crypt = require('crypto')
const walker = require('walkdir')
const watcher = require('chokidar')
const config = require('config')
const scfg = config.get("solr")
const log = require('./logger')
const path = require('path')
const fs = require('fs')
const { Worker, parentPort, workerData } = require('worker_threads')
const { toSolr } = require('./solr')

const solr = require('solr-client').createClient({
  host: "localhost",
  port: scfg.port,
  core: scfg.core
})

const ensureDir = base => {
  if (base.startsWith('~/')) {
    base = base.substring(2)
  }
  if (!base.startsWith("/")) {
    base = path.join(process.env.HOME, base)
  }
  fs.mkdir(base, { recursive: true }, err => {
    if (err) {
      if (err.code != "EEXIST") {
        throw (err)
      }
      log.debug(err)
    }
  })
  return base
}

const basePath = () => ensureDir(config.get('documentRoot'))
const versionsPath = () => ensureDir(config.get("versionsStore"))


/**
 * Create a unique idenitifier for a file path position 
 * @param {*} filepath 
 */
const makeFileID = (filepath) => {
  const base = basePath()
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
  if (!timer) {
    timer = setInterval(loop(), 10000)
  }
}


let timer
let busy = false
const loop = () => {
  if (!busy) {
    busy = true
    if (files.length > 0) {
      const job = files.pop()
      const worker = new Worker('./src/importer.js', { workerData: job })
      worker.on('message', async outfile => {
        log.info("imported " + outfile)
        const buffer = fs.readFileSync(outfile)
        const result = await toSolr(buffer)
        console.log(result)
      })
      worker.on('exit', () => {
        busy = false
      })
      worker.on('error', err => {
        log.error(err + ", " + job)
      })
    } else {
      busy = false
      if (timer) {
        clearInterval(timer)
        timer = undefined
      }
    }
  }
}

const checkStore = () => {
  return new Promise((resolve, reject) => {
    const base = basePath()
    const emitter = walker(base)
    emitter.on('file', async (filename, stat) => {
      const q = solr.createQuery().q({ id: makeFileID(filename) })
      solr.search(q, (err, obj) => {
        if (err) {
          log.error(err)
        } else {
          if (obj.response.numFound == 0) {
            addFile(filename)
          } else {
            log.debug(obj[0].id)
          }
        }
      })
    })
    emitter.on('end', () => {
      timer = setInterval(loop, 1000)
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

/**
 * Continously watch the store for changes. If such changes happen, update the solr index
 */
const watchDirs = () => {
  let storage = basePath()
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
        await service.remove(api.makeFileID(fp))
      } catch (err) {
        log.error("Error removing " + fp + ", " + err)
      }
    })
    .on('error', err => { log.error(err) })

}
module.exports = {
  makeFileID,
  checkStore,
  watchDirs,
  basePath,
  versionsPath
}
