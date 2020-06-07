require('chai').should()

const sample = {
  "Content-Type": "application/pdf",
  "X-Parsed-By": [
    "org.apache.tika.parser.DefaultParser",
    "org.apache.tika.parser.pdf.PDFParser"
  ],
  "access_permission:assemble_document": "true",
  "access_permission:can_modify": "true",
  "access_permission:can_print": "true",
  "access_permission:can_print_degraded": "true",
  "access_permission:extract_content": "true",
  "access_permission:extract_for_accessibility": "true",
  "access_permission:fill_in_form": "true",
  "access_permission:modify_annotations": "true",
  "dc:format": "application/pdf; version\u003d1.3",
  "language": "",
  "pdf:PDFVersion": "1.3",
  "pdf:charsPerPage": [
    "0",
    "0"
  ],
  "pdf:encrypted": "false",
  "pdf:hasXFA": "false",
  "pdf:hasXMP": "false",
  "pdf:unmappedUnicodeCharsPerPage": [
    "0",
    "0"
  ],
  "xmpTPg:NPages": "2"
}

xdescribe("importer", () => {
  const { makeMetadata, createVersion } = require('../../src/importer')
  const { basePath } = require('../../src/files')
  const cfg = require('config')

  it("generates metadata", () => {
    const meta = makeMetadata(sample, {
      "origin": "Spitäler Schaffhausen"
    },
      basePath() + "/t/Testperson_Armeswesen_23.07.1955/2020-05-02_Kurzbericht KSSH.pdf")
    meta.should.be.ok
    meta.concern.should.equal("Testperson_Armeswesen_23.07.1955")
    meta.title.should.equal("2020-05-02_Kurzbericht KSSH")
    meta.loc.should.equal("t/Testperson_Armeswesen_23.07.1955/2020-05-02_Kurzbericht KSSH.pdf")
  })
  it("creates versions of files for storage", () => {
    const fn = basePath() + "/t/Testperson_Armeswesen_23.07.1955/2020-05-02_Kurzbericht KSSH.pdf"
    const v1 = createVersion(fn)
    const date = new Date()
    const vs = date.getFullYear() + "-" + (date.getMonth() + 1) + "-" + date.getDate()
    v1.should.equal("Testperson_Armeswesen_23.07.1955_2020-05-02_Kurzbericht KSSH_" + vs + ".pdf")
  })
})

