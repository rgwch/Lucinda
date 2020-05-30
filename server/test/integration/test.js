const fetch = require('node-fetch')
const fs = require('fs').promises
const path = require('path')
const importer = require('../../src/importer')


const dir = path.join(__dirname, "../")
fs.copyFile(dir + "pdf_image_test_orig.pdf", dir + "pdf_image_test.pdf").then(res => {
  importer.doImport(dir + "pdf_image_test.pdf")
})
