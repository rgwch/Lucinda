const { filename, parentPort } = require('worker_threads')
const cfg = require('config')
const log = require('./logger')
const { spawn } = require('child_process')

log.info("received job " + filename)
const dest = filename + "_ocr_"
doOCR(filename, dest).then(dest => {
  parentPort.postMessage(dest)
})


function doOCR(source, dest) {
  return new Promise((resolve, reject) => {
    const proc = spawn(cfg.get("ocr"), [source, dest])
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
