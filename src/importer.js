/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const { parentPort, workerData, isMainThread } = require('worker_threads')
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

/**
 * If we're called as a workerThread, run an import session. 
 * (We might be called by a test session as well, then we would be in the MainThread.)
 */
if (!isMainThread) {
  doImport(workerData.filename,workerData.metadata).then(result => {
    parentPort.postMessage(result)
  })
}

/**
 * Import a file from the filesystem. Will try to extract metadata and text contents. If the
 * file is an image or a pdf containing no text, trys to OCR the image and write a searchable 
 * PDF/A with text layer from it. 
 * @param {string} filename full filepath
 * @param {any} metadata any predefined metadata
 * @returns filepath of the processed file or undefined, if the file xould not be processed or contained no text.
 * 
 */
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

/*
 * Text if the file is an image file that should be converted to pdf first
 * @param {string} filename 
 */
function shouldConvert(filename) {
  const supported = ["tiff", "tif", "png", "jpg", "jpeg", "gif", "bmp", "eps", "pcx", "pcd", "psd"]
  for (const fmt of supported) {
    if (filename.toLowerCase().endsWith(fmt)) {
      return true
    }
  }
  return false;
}

/**
 * Test if the metadata contain an entry which containts the given property
 * @param {any} metadata 
 * @param {any} propertyname property to check
 * @param {string} property  string wich should be contained in the property value
 * @returns true if the 'propertyname' exists and has at least a part which is equal to 'property'.
 */
function hasMeta(metadata, propertyname, property) {
  if (metadata) {
    if (metadata[propertyname]) {
      if (typeof (metadata[propertyname]) == 'string') {
        return metadata[propertyname].includes(property)
      }
    }
  }
}

/**
 * check the metadata to tell if the file is a PDF without text content.
 * @param {any} meta the metadata as returned my Tika.
 * @returns true if the file is a pdf with less than 100 characters on the (first) page.
 */
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

/**
 * create the Lucinda specific metadata from (1) the data received from Tika, (2) any user-supplied metadata and (3) the filepath
 * The directory in which the file resides, is the "concern" of the file. This helps with structuring the filebase, e.g. to
 * add files with other tools such as scanners or mail apps.
 * If the concern matches the pattern lastname_firstname_birthdate, it's seen as a persons document store and  name and firstname
 * are added as individual fields in the metadata.
 * Title of the document is taken from the existing metadata, or from the filename. dc_title and pdf_docinfo_title are always
 * matched the title.
 * The property 'loc' is set to the filepath inside the docbase. 
 * If a file is stored in /srv/lucindadocs/pats/d/Doe_John_2001_04_05/some_document.pdf, and the Lucinda docbaseis /srv/lucindadocs, 
 * then the 'loc' property is set to /pats/d/Doe_John_2001_04_05/some_document.pdf.
 * 
 * @param {any} computed Metadata from tika
 * @param {any} received Metadata from the caller
 * @param {string} filename full filepath
 */
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

/**
 * Fetch the metadata of a document by calling Tika
 * @param {binary} buffer the binary contents of the file
 * @throws error if the call to Tika didn't succeed
 */
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

/**
 * Fetch the text contetns of a document by calling Tika
 * @param {binary} buffer the binary contents of the file
 * @returns the contents as text (which might be "", if no contetns was found)
 * @throws error, if the call to Tika didn't succeed
 */
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



module.exports = { doImport, makeMetadata}
