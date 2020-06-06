/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const express = require('express')
const log = require('./logger')
const config = require('config')
const { find } = require('./solr')
const path = require('path')
const API = "/lucinda/3.0"
const rest_api = require('./rest')
const { version } = require('../package.json')

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
server.use(express.static(path.join(__dirname, "../static")))

server.set('views', path.join(__dirname, '../views'))
server.set('view engine', 'pug')

server.get("/", (req, res) => {
  res.render('index', { results: [], num:10, offset: 0, sendtext: "Suche" })
})

server.get("/query", async (req, res) => {
  const rq = req.query.request
  const num=req.query.num || 10
  const offset=parseInt(req.query.offset || 0)
  const meta = await find({ query: "contents:" + rq, limit: num, offset, sort: "concern asc" })
  if (meta.status && meta.status == "error") {
    res.render('error', {errmsg: meta.err})
  } else {
    const resp = meta.response
    const result = meta.response.docs.map(doc => {
      return {
        "id": doc.id,
        "title": doc.title,
        "concern": doc.concern
      }
    })
    const next=parseInt(offset)+parseInt(num)
    res.render('index', { results: result, term: rq, num, offset:next,
      sendtext: "nächste "+num
    })
  }
})

server.use(API,rest_api)


const port = process.env.LUCINDA_PORT || (config.has("listen-port") ? config.get("listen-port") : 9997)
console.log("Lucinda server up and listening at " + port)
server.listen(port)

module.exports = server