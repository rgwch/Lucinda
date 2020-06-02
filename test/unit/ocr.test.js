const { expect } = require('chai')
require('chai').should()
const path = require('path')
const fs = require('fs')
const { doConvert, doOCR } = require('../../src/ocr')


describe('ocr', () => {
  before(() => {
    fs.unlink(path.join(__dirname, "../ocrresult.pdf"), err => { })
    fs.unlink(path.join(__dirname, "../lorempng.pdf"), err => { })
  })
  it("makes a searchable pdf from a scanned pdf", async () => {
    fs.copyFileSync(path.join(__dirname, "../lorem.pdf.image"), path.join(__dirname, "../ocrresult.pdf"))
    const result = await doOCR(path.join(__dirname, "../ocrresult.pdf"))
    result.should.be.ok
  })
  it("converts an image file to pdf", async () => {
    const result = await doConvert(path.join(__dirname, "../lorempng.png"))
    result.should.be.ok
  })
})