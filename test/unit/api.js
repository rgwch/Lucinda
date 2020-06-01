process.env.NODE_ENV = "test"
const chai = require('chai')
const server=require('../../src/server')
chai.should()
const chaiHttp = require('chai-http');
chai.use(chaiHttp)
const API="/lucinda/3.0"

describe("lucinda API", () => {
  it("responses to ping", done =>{
    chai.request(server)
    .get(API+"/")
    .end((err,res)=>{
      res.should.have.status(200)
      res.body.should.be.an('object')
      res.body.status.endsWith("ok").should.be.true
      done()
    })
  })
})