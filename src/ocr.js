/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const { spawn } = require('child_process')
const fs = require('fs')
const path = require('path')
const cfg = require('config')
const log = require('./logger')
const { createVersion } = require('./files')

/**
 * Convert an image file to a pdf (without text). Uses img2pdf for that task.
 * Thus, img2pdf must be installed and on the path available
 * @param {string} input the input file. full filepath
 */
function doConvert(input) {
  return new Promise((resolve, reject) => {
    const dir = path.dirname(input)
    const ext = path.extname(input)
    const base = path.basename(input, ext)
    const dest = path.join(dir, base + ".pdf")
    const outStream = fs.createWriteStream(dest)
    const proc = spawn("img2pdf", [input])
    proc.on('error', err => {
      fs.rmSync(dest, { force: true })
      reject(err)
    })
    proc.on('exit', code => {
      outStream.close()
      const oldfile = createVersion(input)
      log.info("moving " + input + " to " + oldfile)
      resolve(dest)
    })
    proc.stdout.pipe(outStream)
  })
}

/**
 * If a file is a PDF with image data only: Try to OCR and convert to PDF with Text overlay.
 * If successful: Replace original file.
 * Uses ocrmypdf, which must be configured.
 * @param {string} source filepath
 * @returns the filepath of the newly written pdf/a or undefined, if no file was written
 */
function doOCR(source) {
  return new Promise((resolve, reject) => {
    const args = ["-l", cfg.get("preferredLanguage"), "--skip-text", source, source]
    const proc = spawn(cfg.get("ocr"), args)
    proc.stdout.on('data', txt => { log.info("info: " + txt.toString()) })
    // ocrmypdf pipes all messages to stderr!
    proc.stderr.on('data', txt => {
      const text = txt.toString()
      if (text.trim().startsWith("ERROR")) {
        log.error(text.trim())
        reject(text)
      } else {
        // log.info(text.trim())
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
          reject("bad arguments for ocrMyPDF " + args)
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