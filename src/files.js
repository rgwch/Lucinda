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
const fs = require('fs')
const { Worker, parentPort, workerData } = require('worker_threads')
const { find, remove, wait } = require('./solr')
const analyze = require('./analyzer')
const scaninterval = 2000

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
    fs.mkdirSync(base, { recursive: true })
    fs.accessSync(base, fs.constants.W_OK | fs.constants.R_OK)

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
  return makeHash(filepath)
}

const files = []
const addFile = (filename, metadata = {}) => {
  files.push({ filename, metadata })
  if (!timer) {
    timer = setInterval(joblist, scaninterval)
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
      log.debug("processing file: " + job.filename)
      if (process.env.NODE_ENV == "debug") {
        const worker = require('./importer')
        worker.doImport(job.filename, job.metadata).then(result => {
          log.info("processed " + JSON.stringify(result))
          busy = false
        }).catch(err => {
          log.error(err)
          busy = false
        })
      } else {
        const worker = new Worker('./src/importer.js', { workerData: job })
        worker.on('message', outfile => {
          log.info("processed " + JSON.stringify(outfile))
        })
        worker.on('exit', () => {
          log.info("Worker exited")
          busy = false
        })
        worker.on('error', err => {
          log.error(err + ", " + job)
          busy = false
        })
      }
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
 * Check if a file exists already in the index. If it does, check if the hash of the existing file
 * is different from the hash of the new file. If it doesn't exist or the hash difffers: update the index.
 * @param {string} filename full filepath
 */
const checkExists = async (filename) => {
  const res = await find("id:" + makeFileID(filename))
  if (res && res.response) {
    if (res.response.numFound == 0) {
      addFile(filename, { "Lucinda_from": "watcher" })
      log.info("adding " + filename)
    } else {
      const existing = res.response.docs[0].checksum
      if (existing) {
        fs.readFile(filename, (err, tocheck) => {
          if (err) {
            log.error("Can't checksum " + filename + ", " + err)
            addFile(filename, { "Lucinda_from": "watcher" })

          } else {
            newhash = makeHash(tocheck)
            if (newhash !== existing) {
              addFile(filename, { "Lucinda_from": "watcher" })
              log.info("updating " + filename)
            }
          }
        })
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
    .on('add', fp => {
      if (config.has("inbox")) {
        const inbox = config(get("inbox").autocheck)
        if (path.basename(path.dirname(fp)) == inbox) {
          const placed = analyze(fp)
          if (placed) {

          }

        }
      } else {
        setTimeout(checkExists, 1000, fp)
      }

    })
    .on('change', fp => {
      setTimeout(checkExists, 1000, fp)
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
/**
 * Save the original file in the Versionsstore with a name which contains the current date - used to store different versions of a file.
 * @param {string} fn full filepath
 * @returns the full path of the newly saved file
 */
function createVersion(fn) {
  const dat = new Date()
  const ext = path.extname(fn)
  const base = path.basename(fn, ext)
  const concern = path.basename(path.dirname(fn))
  const st = concern + "_" + base + "_" + dat.getFullYear() + "-" + (dat.getMonth() + 1) + "-" + dat.getDate() + ext
  const store = path.join(versionsPath(), st)
  fs.readFile(fn, (err, buffer) => {
    if (err) {
      log.error(err)
      throw (err)
    }
    fs.writeFile(store, buffer, err => {
      if (err) {
        log.error(err)
      } else {
        fs.unlink(fn, err => {
          if (err) {
            log.error(err)
          }
        })
      }
    })
  })
  return store
}

function makeHash(buffer) {
  return crypt
    .createHash('md5')
    .update(buffer)
    .digest('hex');
}

module.exports = {
  makeFileID,
  checkStore,
  watchDirs,
  basePath,
  versionsPath,
  createVersion,
  addFile,
  makeHash
}
