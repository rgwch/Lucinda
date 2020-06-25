require('chai').should()
//const expect = require('chai').expect
const an = require('../../src/analyzer')
const cfg = require('config')
const { basePath } = require('../../src/files')
const { assert } = require('chai')
const inbox = cfg.get("inbox")

describe('analyzer', () => {
  it('parses a filename with default pattern 1', () => {
    const check = inbox + "/2020-06-14_Testperson_Armeswesen_22.04.1975_Bericht Kantonsspital.pdf"
    const expected = "Testperson_Armeswesen_22.04.1975/2020-06-14_Bericht_Kantonsspital.pdf"
    an(check).should.equal(expected)
  })
  it('parses a filename with default pattern 2', () => {
    const check = inbox + "/Testperson_Armeswesen_22.04.1975_Bericht Kantonsspital.pdf"
    const expected = "Testperson_Armeswesen_22.04.1975/Bericht_Kantonsspital.pdf"
    an(check).should.equal(expected)
  })
  it('parses a filename with non matching pattern', () => {
    const check = inbox + "/Testperson_Armeswesen__Bericht Kantonsspital.pdf"
    const ex = an(check)
    assert(ex == undefined)

  })
})