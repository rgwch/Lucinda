/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const express = require('express')
const log = require('./logger')
const config = require('config')
const { find, toSolr } = require('./solr')
const { basePath } = require('./files')
const { getTextContents } = require('./importer')
const path = require('path')
const API = "/lucinda/3.0"
const rest_api = require('./rest')
const { version } = require('../package.json')
const fs = require('fs').promises

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
server.use(express.urlencoded({ extended: false }));

if (process.env.LUCINDA_SIMPLEWEB == 'enabled') {
  server.use(express.static(path.join(__dirname, "../static")))

  server.set('views', path.join(__dirname, '../views'))
  server.set('view engine', 'pug')

  server.get("/", (req, res) => {
    res.render('index', { results: [], num: 10, offset: 0, sendtext: "Suche", previous: "Zurück", backdisabled: "" })
  })

  server.get("/getmeta/:id", async (req, res) => {
    const meta = await find("id:" + req.params.id)
    if (meta.response.numFound == 0) {
      res.status(404).end()
    }
    const f = meta.response.docs[0]
    const array = Object.keys(f)
      .map(k => { return { "key": k, "value": f[k] } })
      .filter(el => ["title", "concern", "loc"].includes(el.key))
    res.render("metadata", { metadata: array, complete: JSON.stringify(f) })
  })

  server.post("/setmeta", async (req, res) => {
    const modified = req.body
    const newmeta = JSON.parse(modified.complete)
    delete modified.complete
    for (key of Object.keys(modified)) {
      newmeta[key] = modified[key]
    }
    try {
      delete newmeta._version_
      const loc = path.join(basePath(), newmeta.loc)
      const contents = await fs.readFile(loc)
      const text = await getTextContents(contents)
      newmeta.contents = text
      const result = await toSolr(newmeta)
      if (result.status == "error") {
        res.render("error", { errmsg: result.err })
      } else {
        res.redirect("/")
      }
    } catch (err) {
      res.render('error', { errmsg: err })
    }

  })

  server.get("/query", async (req, res) => {
    const rq = req.query.request || "*"
    const num = parseInt(req.query.num) || 20
    let offset = parseInt(req.query.offset || 0)
    if (req.query.hasOwnProperty("forward")) {
      offset += num
    } else if (req.query.hasOwnProperty("backward")) {
      offset = Math.max(0, offset - num)
    } else {
      offset = 0
    }

    const meta = await find({ query: "contents:" + rq, limit: num, offset, sort: "concern asc" })
    if (meta.status && meta.status == "error") {
      res.render('error', { errmsg: meta.err })
    } else {
      const resp = meta.response
      const result = meta.response.docs.map(doc => {
        return {
          "id": doc.id,
          "title": doc.title,
          "concern": doc.concern
        }
      })

      const nextdisabled = (offset > resp.numFound) ? "pure-button-disabled" : ""
      const backdisabled = (offset < num) ? "pure-button-disabled" : ""
      res.render('index', {
        results: result,
        total: resp.numFound,
        term: rq,
        offset,
        num, backdisabled, nextdisabled
      })
    }
  })
}

server.use(API, rest_api)


const port = process.env.LUCINDA_PORT || (config.has("listen-port") ? config.get("listen-port") : 9997)
console.log("Lucinda server up and listening at " + port)
server.listen(port)

module.exports = server