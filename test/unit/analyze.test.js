require('chai').should()
const an = require('../../src/analyzer')
const cfg = require('config')
const { basePath } = require('../../src/files')
const inbox = cfg.get("inbox")

describe('analyzer', () => {
  it('parses a filename', () => {
    const check = inbox + "/2020-06-14_Testperson_Armeswesen_22.04.1975_Bericht Kantonsspital"
    const expected = basePath() + "/Testperson_Armeswesen_22.04.1975/2020-06-14_Bericht_Kantonsspital"
    an(check).should.equal(expected)
  })
})