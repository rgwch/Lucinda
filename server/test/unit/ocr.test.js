const  {expect} = require('chai')
require('chai').should()
const path = require('path')
const ocr = require('../../src/importer')

describe('ocr', () => {
  it("makes a searchable pdf from a scanned pdf", async () => {
    const result = await ocr(path.join(__dirname, "../textocr.pdf"), path.join(__dirname, "../ocrresult.pdf"))
    result.should.be.ok
  })
})