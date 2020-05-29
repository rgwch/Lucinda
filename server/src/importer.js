const { parentPort, workerData } = require('worker_threads')
const cfg = require('config')
const log = require('./logger')
const { spawn } = require('child_process')
const fs = require('fs').promises
const path = require('path')
const { versionsPath } = require('./files')

if (workerData) {
  log.info("received job " + workerData)
  const dest = versionsPath() + path.sep + createVersion(workerData)
  if (workerData.endsWith("pdf")) {
    doOCR(workerData, dest).then(async dest => {
      const stat = await fs.stat(dest)
      if (stat.isFile() && stat.size) {
        const buf = await fs.readFile(workerData)
        await fs.copyFile(dest, workerData)
        await fs.unlink(dest)
        await fs.writeFile(dest, buf)
      }
      parentPort.postMessage(workerData)
    })
  } else {
    parentPort.postMessage(workerData)
  }
}

function createVersion(fn) {
  const dat = new Date()
  const ext = path.extname(fn)
  const base = path.basename(fn, ext)
  const st = base + "-" + dat.getFullYear() + "-" + dat.getMonth() + "-" + dat.getDay() + ext
  return st
}

function doOCR(source, dest) {
  return new Promise((resolve, reject) => {
    const proc = spawn(cfg.get("ocr"), [source, dest])
    proc.stdout.on('data', txt => { log.info("info: " + txt.toString()) })
    proc.stderr.on('data', txt => { log.error("err: " + txt.toString()) })
    proc.on('error', err => {
      log.error("Processing error " + err)
      reject(err)
    })
    proc.on('exit', async (code, signal) => {
      log.info("success")
      resolve(dest)
    })
  })
}

module.exports = doOCR
