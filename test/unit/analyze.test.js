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
    const check2 = inbox + "/2020_06_14-Testperson-Armeswesen-22-04-1975 Bericht Kantonsspital.pdf"
    const expected = "Testperson_Armeswesen_22.04.1975/2020-06-14_Bericht_Kantonsspital.pdf"
    an(check).should.equal(expected)
    an(check2).should.equal(expected)
  })
  it('parses a filename with default pattern 2', () => {
    const check = inbox + "/Testperson_Armeswesen_22.04.1975_Bericht Kantonsspital.pdf"
    const check2 = inbox + "/Testperson-Armeswesen-22-04-1975-Bericht Kantonsspital.pdf"
    const expected = "Testperson_Armeswesen_22.04.1975/Bericht_Kantonsspital.pdf"
    an(check).should.equal(expected)
    an(check2).should.equal(expected)
  })
  it('parses a filename with default pattern 3', () => {
    const check = inbox + "/2020-06-14_Testperson_Armeswesen_1975.04.22_Bericht Kantonsspital.pdf"
    const check2 = inbox + "/2020_06_14_Testperson-Armeswesen-1975-04-22-Bericht Kantonsspital.pdf"
    const expected = "Testperson_Armeswesen_22.04.1975/2020-06-14_Bericht_Kantonsspital.pdf"
    assert(an(check),"analyze failed")
    assert(an(check2),"analyze failed")
    an(check).should.equal(expected)
    an(check2).should.equal(expected)
  })
  it('parses a filename with default pattern 4', () => {
    const check = inbox + "/Testperson_Armeswesen_1975.04.22_Bericht Kantonsspital.pdf"
    const check2 = inbox + "/Testperson-Armeswesen-1975-04-22-Bericht Kantonsspital.pdf"
    const expected = "Testperson_Armeswesen_22.04.1975/Bericht_Kantonsspital.pdf"
    assert(an(check),"analyze failed")
    assert(an(check2),"analyze failed")
    an(check).should.equal(expected)
    an(check2).should.equal(expected)
  })
  it('parses a filename with non matching pattern', () => {
    const check = inbox + "/Testperson_Armeswesen__Bericht Kantonsspital.pdf"
    const ex = an(check)
    assert(ex == undefined)

  })
})