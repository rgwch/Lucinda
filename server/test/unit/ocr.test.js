const  {expect} = require('chai')
require('chai').should()
const path = require('path')
const fs=require('fs')
const ocr = require('../../src/importer')



xdescribe('ocr', () => {
  before(()=>{
    fs.unlink(path.join(__dirname,"../ocrresult.pdf"),err=>{})
  })
  it("makes a searchable pdf from a scanned pdf", async () => {
    const result = await ocr(path.join(__dirname, "../testocr.pdf"), path.join(__dirname, "../ocrresult.pdf"))
    result.should.be.ok
  })
})