/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/
const express = require('express')
const router = express.Router()
const { find, remove, toSolr } = require('./solr')
const { getTextContents } = require("./importer")
const fs = require('fs')
const path = require('path')
const { basePath, addFile, checkStore, listfiles } = require('./files')
const { version } = require('../package.json')
const log = require('./logger')


router.get("/", (req, res) => {
  res.json({
    "status": `Lucinda Server v.${version} ok`,
  }).end()
})

router.get("/get/:id", async (req, res) => {
  if (process.env.NODE_ENV === "test") {
    if (req.params.id == "testfile") {
      res.set("content-type", "text/plain")
      res.status(200).send("Testfile")
    } else {
      res.status(404).end()
    }
  } else {
    const meta = await find("id:" + req.params.id)
    log.debug(meta)
    if (meta.response.numFound == 0) {
      res.status(404).end()
    } else if (meta.response.numFound > 1) {
      log.error("Document id not unique ", req.params.id)
      res.status(500).end()
    } else {
      const doc = meta.response.docs[0]
      if (doc.loc) {
        const loc = path.join(basePath(), doc.loc)
        res.sendFile(loc)
      } else {
        res.status(404).end()
      }
    }
  }
})

router.get("/getmeta/:id", async (req, res) => {
  const meta = await find("id:" + req.params.id)
  if (meta.response.numFound == 0) {
    res.status(404).end()
  }
  res.json(meta.response.docs[0])
})

router.get("/query/:expression", async (req, res) => {
  try {
    const meta = await find(req.params.expression, { limit: 100 })
    res.json(meta.response)
  } catch (err) {
    log.error("Query error " + req.params.expression + ": " + err)
    res.status(400).end()
  }
})

router.post("/query", async (req, res) => {
  try {
    const meta = await find(req.body)
    if (meta.status && meta.status == "error") {
      log.error(meta.err)
      res.status(400).end()
    } else {
      meta.response.status = "ok"
      res.json(meta.response)
    }
  } catch (ex) {
    log.error("Query error " + ex)
    res.status(400).end()
  }
})

/**
 * Add a new file to the storage and the index
 */
router.post("/addfile", async (req, res) => {
  const { metadata, payload } = req.body
  if (metadata && metadata.filepath) {
    const fpath = path.join(basePath(), metadata.filepath)
    const dir = path.dirname(fpath)
    fs.mkdir(dir, { recursive: true }, err => {
      if (err) {
        log.warn(err)
      }
      fs.writeFile(fpath, Buffer.from(payload, "base64"), err => {
        if (!err) {
          log.info("written file " + fpath)
          metadata.Lucinda_from = "REST API"
          addFile(fpath, metadata)
        } else {
          log.error("could not write file " + err)
        }
      })
    })
    res.status(202).end()
  } else {
    res.status(400).end()
  }


})

/**
 * retrieve a directory view relative to the base path
 * 
 */
router.get("/listfiles", async (req, res) => {
  const { subdir, options } = req.body
  if (subdir) {
    try {
      result = await listfiles(subdir, options)
      res.json({ status: "ok", result })
    } catch (err) {
      log.error(err)
      res.status(500).end()
    }
  }
})

/**
 * Only index a file.
 */
router.post("/addindex", async (req, res) => {
  try {
    const result = await toSolr(req.body)
    res.status(201).json(result)
  } catch (err) {
    log.error(err)
    res.status(400).end()
  }
})

router.put("/update/:id", async (req, res) => {

  res.status(500).end()
})

router.get("/removeindex/:id", async (req, res) => {
  try {
    const result = await remove(req.params.id)
    log.debug(result)
    if (result.responseHeader.status == 0) {
      res.status(200).end()
    }
  } catch (err) {
    res.status(403)
  }
})

router.get("/rescan", (req, res) => {
  checkStore()
  res.json({ "status": "ok" })
})

/**
 * Fetch the text contents of a file (e.g. PDF)
 */
router.post("/parse", async (req, res) => {
  try {
    const result = await getTextContents(req.body)
    res.send(result);
  } catch (err) {
    log.error(err)
    res.status(400).end()
  }
})
module.exports = router