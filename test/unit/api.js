/*
process.env.NODE_ENV = "test"
const chai = require('chai')
chai.should()
const chaiHttp = require('chai-http');
chai.use(chaiHttp)
const API = "/lucinda/3.0"

xdescribe("lucinda API", () => {
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

  it("queries for files", () => {
    chai.request(server)
      .get(API + "/query/*")
      .end((err, res) => {
        res.should.have.status(200)
        res.body.should.be.an("object")
        res.body.should.have.property('numFound')
        res.body.should.have.property('docs')
      })
  })
})
*/