const { parentPort, workerData } = require('worker_threads')
const cfg = require('config')
const { DateTime } = require('luxon')
if (!cfg.has('solr')) {
  throw new Error("Solr is not defined in the config")
}
if (!cfg.has('tika')) {
  throw new Error("Tika is not defined in the config")
}
const log = require('./logger')
const { doOCR, doConvert } = require('./ocr')
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
  if (shouldConvert(filename)) {
    try {
      const dest = await doConvert(filename)
      if (dest) {
        log.warn("created converted file " + dest)
        filename = dest
      }
    } catch (err) {
      log.error("could not convert " + filename + ": " + err)
    }
  }

  let buffer = await fs.readFile(filename)
  log.debug("loaded file")
  try {
    const meta = await getMetadata(buffer)
    log.debug("Metadata: " + JSON.stringify(meta))
    if (shouldOCR(meta)) {
      const dest = await doOCR(filename)
      buffer = await fs.readFile(dest)
    }

    const solrdoc = makeMetadata(meta, metadata, filename)
    solrdoc.contents = await getTextContents(buffer)

    const stored = await toSolr(solrdoc)
    return stored
  } catch (err) {
    log.error("could not import " + filename + ", reason: " + err)
    return undefined
  }

}

function shouldConvert(filename) {
  const supported = ["tiff", "tif", "png", "jpg", "jpeg", "gif", "bmp", "eps", "pcx", "pcd", "psd"]
  for (const fmt of supported) {
    if (filename.toLowerCase().endsWith(fmt)) {
      return true
    }
  }
  return false;
}

function hasMeta(metadata, propertyname, property) {
  if (metadata) {
    if (metadata[propertyname]) {
      if (typeof (metadata[propertyname]) == 'string') {
        return metadata[propertyname].includes(property)
      }
    }
  }
}
function shouldOCR(meta) {
  if (!hasMeta(meta,"Content-Type", "pdf")) {
    return false
  }
  if (hasMeta(meta, "xmp_CreatorTool", "ocrmypdf")) {
    return false
  }
  const numchar = meta["pdf:charsPerPage"]
  if (numchar) {
    if (Array.isArray(numchar)) {
      if (parseInt(numchar[0]) > 100) {
        return false;
      }
    } else {
      if (parseInt(numchar) > 100) {
        return false
      }
    }
  }
  return true
}
function makeMetadata(computed, received, filename) {
  log.debug("Creating Metadata for %s", filename)
  const meta = Object.assign({}, computed, received)
  meta["Lucinda:ImportedAt"] = new Date().toISOString()
  meta.id = makeFileID(filename)
  if (!meta.concern) {
    const base = path.dirname(filename)
    meta.concern = path.basename(base)
  }
  const pers = meta.concern.match(/(\w+)_(\w+)_(\d\d\.\d\d\.\d\d\d\d)/)
  if (pers) {
    meta.lastname = pers[1]
    meta.firstname = pers[2]
    const bd = DateTime.fromFormat(pers[3], "dd.LL.yyyy")
    meta.birthdate = bd.toISODate()
  }

  meta.loc = filename
  const storage = basePath()
  if (meta.loc.startsWith(storage)) {
    meta.loc = meta.loc.substring(storage.length + 1)
  }
  if (!meta.title || meta.title.toLowerCase() == "untitled") {
    const ext = path.extname(meta.loc)
    const base = path.basename(meta.loc, ext)
    meta.title = base
  }
  meta.dc_title = meta.title
  meta.pdf_docinfo_title = meta.title

  return meta
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
    throw new Error("Could not retrieve metadata " + meta.status + ", " + meta.statusText)
  }
  return await meta.json();
}

async function getTextContents(buffer) {
  log.debug("Getting Text content ", buffer.length)
  const contents = await fetch(getContentsURL(), {
    method: "PUT",
    body: buffer
  })
  if (contents.status != 200) {
    throw new Error("Could not retrieve file contents")
  }
  const cnt = await contents.text()
  log.debug("found " + cnt)
  return cnt.trim()
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
