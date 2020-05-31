const express = require('express')
const { spawn } = require('child_process')
const fs = require('fs').promises
const path = require('path')
const { version } = require('../package.json')
const config = require('config')
const init = require('./initialize')
const log = require('./logger')

log.info(`Lucinda Server v.${version} initializing at ${new Date().toString()}`)
const app = express()

init().then(() => {
  serve()
}).catch(err => {
  log.error("Could not start: " + err)
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

  app.get("/", (req, res) => {
    res.json({
      "status": `Lucinda Server v.${version} ok`,
      "usage": "POST application/octet-stream with pdf contents and receive application/octet-stream with scan result."
    })
  })

  app.post("/", async (req, res) => {
    const input = path.join(__dirname, "input.pdf")
    const output = path.join(__dirname, "output.pdf")
    const body = req.body
    try {
      await fs.writeFile(input, body)
      console.log("received file")
      const proc = spawn("/usr/bin/ocrmypdf", [input, output])

      proc.on('error', err => {
        console.log("Error: " + err)
        res.sendStatus(500).send(err).end()
      })
      proc.on('exit', async (code, signal) => {
        console.log("success exit")
        const cnt = await fs.readFile(output)
        res.send(cnt).status(200).end()
        console.log("sent back")
      })
    } catch (err) {
      console.log("Exception " + err)
      res.sendStatus(500)
    }
  })
  const port = process.env.LUCINDA_PORT || (config.has("listen-port") ? config.get("listen-port") : 9997)
  console.log("Lucinda server up and listening at " + port)
  app.listen(port)
}


module.exports = app