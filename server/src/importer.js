const { parentPort, workerData } = require('worker_threads')
const cfg = require('config')
if (!cfg.has('solr')) {
  throw new Error("Solr is not defined in the config")
}
if (!cfg.has('tika')) {
  throw new Error("Tika is not defined in the config")
}

const log = require('./logger')
const { spawn } = require('child_process')
const fs = require('fs').promises
const path = require('path')
const { versionsPath } = require('./files')
const fetch = require('node-fetch')

const getTikaURL = tika => `${tika.host}:${tika.port}/`
const getMetaURL = () => getTikaURL(cfg.get("tika")) + "meta"
const getContentsURL = () => getTikaURL(cfg.get("tika")) + "tika"
const getSolrURL = solr => `${solr.host}:${solr.port}/`


if (workerData) {
  doImport(workerData).then(result => {
    parentPort.postMessage(result)
  })
}

async function doImport(filename) {
  try {
    const hasTika = await fetch(getTikaURL(cfg.get("tika")))
    if (hasTika.status !== 200) {
      throw new Error("Could not access Tika")
    }
    const hasSolr = await fetch(getSolrURL(cfg.get("solr")))
    if (hasSolr.status !== 200) {
      throw new Error("Could not access Solr")
    }
  } catch (ex) {
    log.error(ex)
    throw (ex)
  }
  log.info("received job " + filename)
  const dest = versionsPath() + path.sep + createVersion(filename)
  if (filename.endsWith("pdf")) {
    doOCR(filename, dest).then(async dest => {
      const stat = await fs.stat(dest)
      if (stat.isFile() && stat.size) {
        const buf = await fs.readFile(filename)
        await fs.copyFile(dest, filename)
        await fs.unlink(dest)
        await fs.writeFile(dest, buf)
      }
    })
  }
}



function createVersion(fn) {
  const dat = new Date()
  const ext = path.extname(fn)
  const base = path.basename(fn, ext)
  const st = base + "-" + dat.getFullYear() + "-" + dat.getMonth() + "-" + dat.getDay() + ext
  return st
}

function doOCR(source, dest) {
  return new Promise((resolve, reject) => {
    const proc = spawn(cfg.get("ocr"), [source, dest])
    proc.stdout.on('data', txt => { log.info("info: " + txt.toString()) })
    proc.stderr.on('data', txt => { log.error("err: " + txt.toString()) })
    proc.on('error', err => {
      log.error("Processing error " + err)
      reject(err)
    })
    proc.on('exit', async (code, signal) => {
      log.info("success")
      resolve(dest)
    })
  })
}

module.exports = { doImport }
