
process.env.NODE_ENV = "test"
const chai = require('chai')
chai.should()
const chaiHttp = require('chai-http');
chai.use(chaiHttp)
const fs = require('fs')
const path = require('path')
const API = "/lucinda/3.0"

describe("lucinda API", () => {
  const server = require('../../src/server')

  it("responses to ping", done => {
    chai.request(server)
      .get(API + "/")
      .end((err, res) => {
        res.should.have.status(200)
        res.body.should.be.an('object')
        res.body.status.endsWith("ok").should.be.true
        done()
      })
  })

  it("gets a file", done => {
    chai.request(server)
      .get(API + "/get/testfile")
      .end((err, res) => {
        res.should.have.status(200)
        res.should.be.text
        res.text.should.equal("Testfile")
        done()
      })
  })
  it("errors on an inexistent file", done => {
    chai.request(server)
      .get(API + "/get/missing")
      .end((err, res) => {
        res.should.have.status(404)
        done()
      })
  })
  xit("queries for files", () => {
    chai.request(server)
      .get(API + "/query/*")
      .end((err, res) => {
        res.should.have.status(200)
        res.body.should.be.an("object")
        res.body.should.have.property('numFound')
        res.body.should.have.property('docs')
      })
  })
  xit("stores a file", () => {
    const file = fs.readFileSync(path.join(__dirname, "../lorem.pdf.text"))
    chai.request(server)
      .post(API + "/add")
      .set('content-type', 'application/json')
      .send({
        metadata: {
          filepath: "/testperson/lorem.pdf",
        },
        payload: file.toString()
      })

      .end((err, res) => {
        res.should.have.status(202)
      })


  })
})
