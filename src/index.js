const express = require('express')
const { spawn } = require('child_process')
const fs = require('fs').promises
const path = require('path')
const { version } = require('../package.json')
const config = require('config')
const init = require('./initialize')
const { basePath } = require('./files')
const { find, remove } = require('./solr')
const log = require('./logger')
const API = "/lucinda/3.0"

log.info(`Lucinda Server v.${version} initializing at ${new Date().toString()}`)
const app = express()

init().then(() => {
  serve()
}).catch(err => {
  log.error("Could not start")
})

function serve() {
  app.use(express.raw({
    inflate: true,
    limit: "50mb",
    type: "application/octet-stream"
  }))
  app.use(express.json({
    inflate: true,
    limit: "50mb",
    type: "*/json"
  }))

  app.get(API + "/", (req, res) => {
    res.json({
      "status": `Lucinda Server v.${version} ok`,
    })
  })

  app.get(API + "/get/:id", async (req, res) => {
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

  app.get(API + "/query/:expression", async (req, res) => {
    try {
      const meta = await find(req.params.expression)
      res.json(meta.response.docs)
    } catch (err) {
      log.error("Query error " + req.params.exoression + ": " + err)
      res.status(400).end()
    }
  })

  app.get(API + "/removeindex/:id", async (req, res) => {
    try{
      const result=await remove(req.params.id)
      log.debug(result)
      if(result.responseHeader.status==0){
        res.status(200).end()
      }
    }catch(err){
      res.status(403)
    }
  })
  const port = process.env.LUCINDA_PORT || (config.has("listen-port") ? config.get("listen-port") : 9997)
  console.log("Lucinda server up and listening at " + port)
  app.listen(port)
}


module.exports = app