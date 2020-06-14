require('chai').should()
const { createQuery } = require('../../src/solr')

describe("query builder", () => {
  it("normalizes a query", () => {
    createQuery("Müller +Maier").should.eql({ sort: "concern asc,title asc", query: "contents:(Müller +Maier)" })
    createQuery({ query: "Müller +Maier", sort: "lastname asc" }).should.eql({ sort: "lastname asc", query: "contents:(Müller +Maier)" })
    createQuery("lastname:Müller +firstname:Heinz").should.eql({ query: "lastname:Müller +firstname:Heinz", sort: "concern asc,title asc" })
    createQuery({ query: "lastname:Müller" }).should.eql({ query: "lastname:Müller" })
  })
})