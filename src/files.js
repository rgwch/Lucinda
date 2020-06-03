/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/



const crypt = require('crypto')
const walker = require('walkdir')
const watcher = require('chokidar')
const config = require('config')
const log = require('./logger')
const path = require('path')
const fs = require('fs').promises
const { Worker, parentPort, workerData } = require('worker_threads')
const { find, remove, wait } = require('./solr')

/**
 * Make sure the configured directory exists and is writeable.
 * @param {string} base - the directory to create. Will be cretaed recursively.
 * @throws error if directory can not be created or is not reeadable and writeable
 */
const ensureDir = base => {
  if (base.startsWith('~/')) {
    base = base.substring(2)
  }
  if (!base.startsWith("/")) {
    base = path.join(process.env.HOME, base)
  }
  try {
    await fs.mkdir(base, { recursive: true })
    await fs.access(base, fs.constants.W_OK | fs.constants.R_OK)

  } catch (err) {
    if (err.code != "EEXIST") {
      log.error(err)
      throw (err)
    }
    log.debug(err)
  }
  return base
}

const basePath = () => ensureDir(config.get('documentRoot'))
const versionsPath = () => ensureDir(config.get("versionsStore"))


/**
 * Create a unique idenitifier for a file path position 
 * @param {string} filepath 
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
    timer = setInterval(joblist, 500)
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

/**
 * Walk the file store and check for every file, if it exists already in the solr index. If not, add it.
 */
const checkStore = () => {
  return new Promise((resolve, reject) => {
    const base = basePath()
    const emitter = walker(base)
    emitter.on('file', async (filename, stat) => {
      if (!path.basename(filename).startsWith(".")) {
        pending++
        const res = await find("id:" + makeFileID(filename))
        if (res && res.response) {
          if (res.response.numFound == 0) {
            addFile(filename)
          } else {
            log.debug("checkstore skipping existing file " + res.response.docs[0].loc)
          }
        } else {
          log.error("empty response from query id ")
        }
        pending--
      }
    })
    emitter.on('end', async () => {
      while (pending > 0) {
        log.debug("checkstore pending operations: " + pending)
        await wait(1000)
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
