const { listfiles } = require("../../src/files")

describe("files", () => {
    it("lists the contents of a subdir of filebase", async () => {
        const r1 = await listfiles("s/subd")
        r1.length.should.equal(2)
        const r2 = await listfiles("s/subd", { withSubdirs: true })
        r2.length.should.equal(3)
        const r3 = await listfiles("s/subd", { withPattern: "\\.pdf$" })
        r3.length.should.equal(1)
    })
})