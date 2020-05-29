/*
const init = require('../../src/initialize')
const config = require('config')
const app=require('../../src/index')
require('chai').should()
const path=require('path')

xdescribe("initialize setup", () => {
    it ("finds the home path",async ()=>{
        await init()
        config.get("documentRoot").should.be.ok
        process.env.HOME.should.be.ok
        const expected=path.join(process.env.HOME,config.get("documentRoot"))
        expected.should.equal(app.get("_basepath"))
    })

})
*/