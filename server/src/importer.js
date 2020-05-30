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
const { versionsPath, makeFileID, basePath } = require('./files')
const fetch = require('node-fetch')
const { toSolr } = require('./solr')

const getTikaURL = tika => `${tika.host}:${tika.port}/`
const getMetaURL = () => getTikaURL(cfg.get("tika")) + "meta"
const getContentsURL = () => getTikaURL(cfg.get("tika")) + "tika"
const getSolrURL = solr => `${solr.host}:${solr.port}/`


if (workerData) {
  doImport(workerData).then(result => {
    parentPort.postMessage(result)
  })
}

async function doImport(filename, metadata = {}) {
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
    log.error("Please launch Solr and Tika first.\n==>" + ex)
    return undefined
  }
  log.info("received job " + filename)
  let buffer = await fs.readFile(filename)
  const meta = await getMetadata(buffer)
  if (meta && meta["Content-Type"]) {
    if (meta["Content-Type"] == "application/pdf") {
      const numchar = meta["pdf:charsPerPage"]
      if ((Array.isArray(numchar) && parseInt(numchar[0]) < 100) || (parseInt(numchar) < 100)) {
        const dest = await doOCR(filename)
        if (dest) {
          buffer = await fs.readFile(filename)
        }
      }
    }
  }
  const solrdoc = makeMetadata(meta, metadata, filename)
  solrdoc.contents = await getTextContents(buffer)
  const stored=await toSolr(solrdoc)
  return stored
}

function makeMetadata(computed, received, filename) {
  const meta = Object.assign({}, computed, received)
  meta["Lucinda:ImportedAt"] = new Date().toISOString()
  meta.id = makeFileID(filename)
  if (!meta.concern) {
    const base = path.dirname(filename)
    meta.concern = path.basename(base)
  }
  meta.loc = filename
  const storage = basePath()
  if (meta.loc.startsWith(storage)) {
    meta.loc = meta.loc.substring(storage.length + 1)
  }
  if (!meta.title) {
    const ext = path.extname(meta.loc)
    const base = path.basename(meta.loc, ext)
    meta.title = base
  }
  return meta
}

/**
 * If a file is a PDF with image data only: Try to OCR and convert to PDF with Text overlay.
 * If successful: Replace original file and put original to the attic.
 * @param {*} source 
 */
function doOCR(source) {
  return new Promise((resolve, reject) => {
    const dest = versionsPath() + path.sep + createVersion(source)
    const proc = spawn(cfg.get("ocr"), [source, dest])
    proc.stdout.on('data', txt => { log.info("info: " + txt.toString()) })
    proc.stderr.on('data', txt => { log.error("err: " + txt.toString()) })
    proc.on('error', err => {
      log.error("Processing error " + err)
      reject(err)
    })
    proc.on('exit', async (code, signal) => {
      if (code == 0) {
        log.info("success")
        const stat = await fs.stat(dest)
        if (stat.isFile() && stat.size) {
          const buf = await fs.readFile(source)
          await fs.copyFile(dest, source)
          await fs.unlink(dest)
          await fs.writeFile(dest, buf)
        }
        resolve(dest)
      } else {
        reject("OCR ended with error code " + code)
      }
    })
  })
}

async function getMetadata(buffer) {
  const meta = await fetch(getMetaURL(), {
    headers: {
      accept: "application/json"
    },
    method: "PUT",
    body: buffer
  })
  if (meta.status != 200) {
    throw new Error("Could not retrieve metadata")
  }
  return await meta.json();
}

async function getTextContents(buffer) {
  const contents = await fetch(getContentsURL(), {
    method: "PUT",
    body: buffer
  })
  if (contents.status != 200) {
    throw new Error("Could not retrieve file contents")
  }
  return (await contents.text()).trim()
}

function createVersion(fn) {
  const dat = new Date()
  const ext = path.extname(fn)
  const base = path.basename(fn, ext)
  const concern = path.basename(path.dirname(fn))
  const st = concern + "_" + base + "_" + dat.getFullYear() + "-" + (dat.getMonth() + 1) + "-" + dat.getDate() + ext
  return st
}


module.exports = { doImport, makeMetadata, createVersion }
