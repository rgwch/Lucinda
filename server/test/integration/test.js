const fetch = require('node-fetch')
const fs = require('fs').promises
const path = require('path')
const importer = require('../../src/importer')


const dir = path.join(__dirname, "../")
/*
fs.copyFile(dir + "lorem.pdf.text", dir + "lorem.pdf").then(res => {
  importer.doImport(dir + "lorem.pdf")
})
*/
fs.copyFile(dir + "lorem.pdf.image", dir + "lorem2.pdf").then(res => {
  importer.doImport(dir + "lorem2.pdf")
})
