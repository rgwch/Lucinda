const { spawn } = require('child_process')
const fs=require('fs')
const path=require('path')
const cfg=require('config')
const log=require('./logger')

function doConvert(input){
  return new Promise((resolve,reject)=>{
    const dir=path.dirname(input)
    const ext=path.extname(input)
    const base=path.basename(input,ext)
    const dest=path.join(dir,base+".pdf")
    const outStream=fs.createWriteStream(dest)
    const proc=spawn("img2pdf",[input])
    proc.on('error',err=>{
      reject(err)
    })
    proc.on('exit',code=>{
      outStream.close()
      resolve(dest)
    })
    proc.stdout.pipe(outStream)
  })  
}

/**
 * If a file is a PDF with image data only: Try to OCR and convert to PDF with Text overlay.
 * If successful: Replace original file and put original to the attic.
 * @param {*} source 
 * @returns the filepath of the newly written pdf/a or undefined, if no file was written
 */
function doOCR(source) {
  return new Promise((resolve, reject) => {
    const proc = spawn(cfg.get("ocr"), ["-l", cfg.get("preferredLanguage"), "--skip-text", source, source])
    proc.stdout.on('data', txt => { log.info("info: " + txt.toString()) })
    // ocrmypdf pipes all messages to stderr!
    proc.stderr.on('data', txt => {
      const text = txt.toString()
      if (text.trim().startsWith("ERROR")) {
        log.error(text.trim())
        reject(text)
      } else {
        log.info(text.trim())
      }
    })
    proc.on('error', err => {
      log.error("Processing error " + err)
      reject(err)
    })
    proc.on('exit', async (code, signal) => {
      switch (code) {
        case 0:
          log.info("success")
          resolve(source)
          break;
        case 1:
          log.error("bad args")
          reject("bad arguments for ocrMyPDF")
          break;
        case 2:
          log.warn("illegal input file")
          resolve(undefined)
          break;
        case 3:
          log.error("dependency missing")
          reject("An ocrmypdf dependency is missing")
          break;
        case 4:
          log.warn("Output is no valid pdf")
          resolve(undefined)
          break;
        case 5:
          log.error("Insufficient rights")
          reject("can't write output file. Missing rights")
          break;
        case 6:
          log.warn("The input file was already processed")
          resolve(undefined)
          break;
        case 7:
          log.error("Error in child process")
          reject("unknown error in child process")
          break;
        case 8:
          log.warn("Can't read encrypted input file")
          resolve(undefined)
          break;
        case 9:
          log.error("invalid custom config for tesseract")
          reject("tesseract config")
          break;
        case 10:
          log.warn("could not create pdf/a")
          resolve(source)
          break;
        case 15:
          log.warn("unknown error")
          resolve(undefined)
          break;
        case 130:
          log.error("interrupted")
          reject("We were interrupted by user")
          break;
        default:
          log.error("bad error code " + code)
          reject("bad error code")

      }

    })
  })
}


module.exports = { doOCR, doConvert }