/**************************************************************
 * Copyright (c) 2020-2023 G. Weirich                         *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const { parentPort, workerData, isMainThread } = require('worker_threads')
const cfg = require('config')
const { DateTime } = require('luxon')

const log = require('./logger')
const { doOCR, doConvert } = require('./ocr')
const fs = require('fs').promises
const path = require('path')
const { makeHash, makeFileID, basePath } = require('./files')
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
  doImport(workerData.filename, workerData.metadata).then(result => {
    if (result) {
      parentPort.postMessage(result)
    }
  })
}

/**
 * Import a file from the filesystem. Will try to extract metadata and text contents. If the
 * file is an image or a pdf containing no text, trys to OCR the image and write a searchable 
 * PDF/A with text layer from it. 
 * @param {string} filename full filepath
 * @param {any} metadata any predefined metadata
 * @returns filepath of the processed file or undefined, if the file could not be processed or contained no text.
 * 
 */
async function doImport(filename, metadata = {}) {

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
  log.debug("loaded file " + filename)
  try {
    const meta = await getMetadata(buffer)
    log.debug("Metadata: " + JSON.stringify(meta))
    if (shouldOCR(meta)) {
      try {
        const dest = await doOCR(filename)
        buffer = await fs.readFile(dest)
      } catch (ocrError) {
        log.error("could not OCR " + filename + "; " + ocrError)
      }
    }

    const solrdoc = makeMetadata(meta, metadata, filename)

    solrdoc.contents = await getTextContents(buffer)
    solrdoc.checksum = makeHash(buffer)

    const stored = await toSolr(solrdoc)
    if (stored.status && stored.status == "error") {
      log.error("SOLR error storing " + filename + ": " + JSON.stringify(stored.err))
      return undefined
    }
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
    propertyname = propertyname.replace(/:/, "_")
    if (metadata[propertyname]) {
      if (typeof (metadata[propertyname]) == 'string') {
        return metadata[propertyname].includes(property)
      }
    }
  }
}

/**
 * check the metadata to tell if the file is a PDF without text content. If a creatorTool ocrmypdf
 * exists, will asume that the file was already ocr'd.
 * @param {any} meta the metadata as returned my Tika.
 * @returns true if the file is a pdf with less than 100 characters on any page. 
 * false if the file is not a pdf or has more than 100 characters on any page or is marked as scanned
 * already by ocrMyPDF
 */
function shouldOCR(meta) {
  if (!hasMeta(meta, "Content-Type", "pdf")) {
    return false
  }
  if (hasMeta(meta, "xmp:CreatorTool", "ocrmypdf")) {
    return false
  }
  const numchar = meta["pdf:charsPerPage"] || meta["pdf_charsPerPage"]
  if (numchar) {
    if (Array.isArray(numchar)) {
      const max = numchar.reduce((acc, curr) => { if (curr > acc) acc = curr })
      if (max > 100) {
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
 * Check if the title is one of the not very useful boilerplate titles some programs insert automatically.
 * @param {string} title 
 */
function isNonsenseTitle(title) {
  const nonsense = ["untitled", "pdfpreview", "polypoint", "rptcumd", "fast report document"]
  if (!title) {
    return true
  }
  for (const quatsch of nonsense) {
    if (title.trim().toLowerCase() == quatsch) {
      return true
    }
  }
  return false
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
  // log.debug("Creating Metadata for %s", filename)
  const meta = Object.assign({}, computed, received)
  meta["Lucinda:ImportedAt"] = new Date().toISOString()
  meta.id = makeFileID(filename)
  if (!meta.concern) {
    concernRe = (cfg.has("concernRegexp") ? RegExp(cfg.get("concernRegexp")) : /([^\/]+)_([^\/]+)_(\d\d\.\d\d\.\d\d\d\d)/)
    const cFields = cfg.has("concern_fields") ? cfg.get("concern_fields") : ["lastname", "firstname", "birthdate"]
    const pers = filename.match(concernRe)
    if (pers) {
      meta.concern = pers[0]
      for (let i = 0; i < cFields.length; i++) {
        if (pers[i + 1].match(/\d\d\.\d\d\.\d\d\d\d/)) {
          const bd = DateTime.fromFormat(pers[i + 1], "dd.LL.yyyy")
          meta[cFields[i]] = bd.toFormat("yyyyLLdd")
        } else {
          meta[cFields[i]] = pers[i + 1]
        }
      }
    } else {
      const base = path.dirname(filename)
      meta.concern = path.basename(base)
    }
  }

  meta.loc = filename
  if (!meta.date) {
    const leadingDate = path.basename(filename).match(/^(\d\d\d\d)[\.\-_](\d\d)[\.\-_](\d\d)[\-_\s].+/)
    if (leadingDate) {
      const cDate = DateTime.fromObject({ year: leadingDate[1], month: leadingDate[2], day: leadingDate[3] })
      meta.date = cDate.toFormat("yyyy-LL-dd")
    } else {
      meta.date = meta["creation-Date"] ||
        meta["Creation-Date"] ||
        meta["meta_creation-date"] ||
        meta["meta:creation-date"] ||
        meta["last-modified"] ||
        meta["Last-Modified"] ||
        meta["last-save-date"]
    }
    meta["Creation-Date"] = meta.date
  }
  const storage = basePath()
  if (meta.loc.startsWith(storage)) {
    meta.loc = meta.loc.substring(storage.length + 1)
  }
  if (isNonsenseTitle(meta.title)) {
    const ext = path.extname(meta.loc)
    const base = path.basename(meta.loc, ext)
    meta.title = base
  }
  if (meta["dc:title"]) {
    meta["dc:title"] = meta.title
  }
  if (meta["pdf_docinfo:title"]) {
    meta["pdf_docinfo:title"] = meta.title
  }
  meta.dc_title = meta.title
  meta.pdf_docinfo_title = meta.title
  meta.lucinda_doctype = cfg.has("lucinda-doctype") ? cfg.get("lucinda-doctype") : "Inbox"
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
 * Fetch the text contents of a document by calling Tika
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
  log.debug("found " + cnt.trim().length + " characters")
  return cnt.trim()
}



module.exports = { doImport, makeMetadata, getTextContents }
