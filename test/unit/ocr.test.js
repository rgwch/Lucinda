const { expect } = require('chai')
require('chai').should()
const path = require('path')
const fs = require('fs')
const { doConvert, doOCR } = require('../../src/ocr')
const { doImport } = require('../../src/importer')


describe('ocr', () => {
  before(() => {
    fs.unlink(path.join(__dirname, "../ocrresult.pdf"), err => { })
    fs.unlink(path.join(__dirname, "../lorempng.pdf"), err => { })
  })
  xit("makes a searchable pdf from a scanned pdf", async () => {
    fs.copyFileSync(path.join(__dirname, "../lorem.pdf.image"), path.join(__dirname, "../ocrresult.pdf"))
    const result = await doOCR(path.join(__dirname, "../ocrresult.pdf"))
    result.should.be.ok
  })
  xit("converts an image file to pdf", async () => {
    const result = await doConvert(path.join(__dirname, "../lorempng.png"))
    result.should.be.ok
  })
  it("converts a png to pdf and ocrs it", async () => {
    const result = await doImport(path.join(__dirname, "../lorempng.png"))
    result.should.be.ok
  })
})