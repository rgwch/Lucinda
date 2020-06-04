/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const express = require('express')
const log = require('./logger')
const { version } = require('../package.json')
const config = require('config')
const { find } = require('./solr')
const fs = require('fs')
const path = require('path')
const { basePath, addFile } = require('./files')
const API = "/lucinda/3.0"

log.info(`Lucinda Server v.${version} initializing at ${new Date().toString()}`)
const server = express()

server.use(express.raw({
  inflate: true,
  limit: "50mb",
  type: "application/octet-stream"
}))

server.use(express.json({
  inflate: true,
  limit: "50mb",
  type: "*/json"
}))

server.set('views', path.join(__dirname, '../views'))
server.set('view engine', 'pug')

server.get("/", (req, res) => {
  res.render('index',{results:["a","b"]})
})

server.get("/query", (req, res) => {
  
  res.render('index',{results: res})
})
server.get(API + "/", (req, res) => {
  res.json({
    "status": `Lucinda Server v.${version} ok`,
  }).end()
})

server.get(API + "/get/:id", async (req, res) => {
  const meta = await find("id:" + req.params.id)
  log.debug(meta)
  if (meta.response.numFound == 0) {
    res.status(404).end()
  } else if (meta.response.numFound > 1) {
    log.error("Document id not unique ", req.params.id)
    res.status(500).end()
  } else {
    const doc = meta.response.docs[0]
    const loc = path.join(basePath(), doc.loc)
    res.sendFile(loc)
  }
})

server.get(API + "/query/:expression", async (req, res) => {
  try {
    const meta = await find(req.params.expression)
    res.json(meta.response)
  } catch (err) {
    log.error("Query error " + req.params.exoression + ": " + err)
    res.status(400).end()
  }
})

/**
 * Add a new file to the storage and the index
 */
server.post(API + "/add", async (req, res) => {
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

server.put(API + "/update/{id}", async (req, res) => {

})

server.get(API + "/removeindex/:id", async (req, res) => {
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

const port = process.env.LUCINDA_PORT || (config.has("listen-port") ? config.get("listen-port") : 9997)
console.log("Lucinda server up and listening at " + port)
server.listen(port)

module.exports = server