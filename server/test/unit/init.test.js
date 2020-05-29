const init = require('../../src/initialize')
const config = require('config')
const app=require('../../src/index')
require('chai').should()
const path=require('path')

describe("initialize setup", () => {
    it ("finds the home path",()=>{
        init()
        config.get("documentRoot").should.be.ok
        process.env.HOME.should.be.ok
        const expected=path.join(process.env.HOME,config.get("documentRoot"))
        expected.should.equal(app.get("_basepath"))
    })

})