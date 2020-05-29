const { filename, parentPort } = require('worker_threads')
const cfg = require('config')
const log = require('./logger')
const { spawn } = require('child_process')
const fs=require('fs').promises

if (filename) {
  log.info("received job " + filename)
  const dest = filename + "_ocr_"
  doOCR(filename, dest).then(async dest => {
    const stat=await fs.stat(dest)
    if(stat.isFile() && stat.size){
      const buf=await fs.readFile(filename)
      await fs.copyFile(dest,filename)
      await fs.unlink(dest)
      
    }
    parentPort.postMessage(dest)
  })
}

function createVersion(fn){
  
}

function doOCR(source, dest) {
  return new Promise((resolve, reject) => {
    const proc = spawn(cfg.get("ocr"), [source, dest])
    proc.stdout.on('data',txt=>{log.info("info: "+txt.toString())})
    proc.stderr.on('data',txt=>{log.error("err: "+txt.toString())})
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
