
/*
const { expect } = require('chai')
require('chai').should()
const path = require('path')

xdescribe("initialize setup", () => {
  const config = require('config')
  const { basePath, versionsPath } = require('../../src/files')

  it("finds the home path", () => {
    config.get("documentRoot").should.be.ok
    process.env.HOME.should.be.ok
    const expected = path.join(process.env.HOME, config.get("documentRoot"))
    expected.should.equal(basePath())
  })
  it("finds the versions path", () => {
    config.get("versionsStore").should.be.ok
    process.env.HOME.should.be.ok
    const expected = path.join(process.env.HOME, config.get("versionsStore"))
    expected.should.equal(versionsPath())
  })

})

*/