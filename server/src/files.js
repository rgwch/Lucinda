const crypt = require('crypto')
const walker = require('walkdir')
const watcher = require('chokidar')
const config = require('config')
const log = require('./logger')
const path = require('path')
const fs = require('fs')
const { Worker, parentPort, workerData } = require('worker_threads')
const { find, remove } = require('./solr')


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

const wait = ms => {
  return new Promise(resolve => {
    setTimeout(resolve, ms)
  })
}

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
    timer = setInterval(joblist, 10000)
  }
}


let timer = undefined
let busy = false
let pending = 0
const joblist = () => {
  log.debug("entering joblist; busy=" + busy)
  if (!busy) {
    busy = true
    if (files.length > 0) {
      const job = files.pop()
      log.debug("processing file: " + job)
      const worker = new Worker('./src/importer.js', { workerData: job })
      worker.on('message', async outfile => {
        log.info("imported " + JSON.stringify(outfile))
      })
      worker.on('exit', () => {
        log.info("Worker exited")
        busy = false
      })
      worker.on('error', err => {
        log.error(err + ", " + job)
        busy = false
      })
    } else {
      log.debug("Joblist is empty")
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
      if (!path.basename(filename).startsWith(".")) {
        pending++
        const res = await find("id:" + makeFileID(filename))
        if (res.response.numFound == 0) {
          addFile(filename)
        } else {
          log.debug("checkstore skipping existing file " + res.response.docs[0].loc)
        }
        pending--
      }
    })
    emitter.on('end', async () => {
      while (pending > 0) {
        log.debug("checkstore pending operations: " + pending)
        await wait(500)
      }
      log.debug("Checkstore finished")
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
    followSymlinks: false,
    ignoreInitial: true,
    awaitWriteFinish: true
  })
    .on('add', async fp => {
      addFile(fp)
      log.info("added " + fp)
    })
    .on('change', async fp => {
      addFile(fp)
      log.info("updated " + fp)
    })
    .on('unlink', async fp => {
      try {
        await remove(makeFileID(fp))
        log.info("removed " + fp)
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
