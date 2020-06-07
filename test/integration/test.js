const fetch = require('node-fetch')
const fs = require('fs').promises
const path = require('path')
const importer = require('../../src/importer')
const API = "http://localhost:9997/lucinda/3.0"

const dir = path.join(__dirname, "../")
/*
fs.copyFile(dir + "lorem.pdf.text", dir + "lorem.pdf").then(res => {
  importer.doImport(dir + "lorem.pdf")
})

fs.copyFile(dir + "lorem.pdf.image", dir + "lorem2.pdf").then(res => {
  importer.doImport(dir + "lorem2.pdf")
})
*/
/*
fs.copyFile(dir + "lorem.odt", dir + "loremtest.odt").then(res => {
  importer.doImport(dir + "loremtest.odt")
})
*/

/*
fetch(API+"/").then(resp=>{
  console.log(resp.status)
})
*/
/*
fs.readFile(dir + "lorem.pdf.text").then(buffer => {
  const body = JSON.stringify({
    metadata: {
      filepath: "/docdoc/lorem.pdf"
    },
    payload: buffer.toString('base64')
  })
  return fetch(API + "/add", {
    "method": "post",
    "headers": {
      "Content-Type": "application/json"
    },
    "body": body
  })
}).then(result => {
  if (result.status != 200) {
    console.log("Error " + result.status + ", " + result.statusText)
  }
  return result.body
}).then(body => {
  console.log(body.text)
}).catch(err => {
  console.log(err)
})
*/