const config = require('config')
const log = require('./logger')
const fetch = require('node-fetch')

const makeSolrURL = () => {
  if (!config.has("solr")) {
    log.error("FATAL: Solr is not defined in configuration!")
    throw new Error("Solr not defined")
  }
  const solr = config.get("solr")
  return solr.host + ":" + solr.port + "/solr/" + solr.core
}

const makeTikaURL = () => {
  if (!config.has("tika")) {
    log.error("FATAL: Tika is not defined in cinfiguration!")
    throw new Error("Tika not defined")
  }
  const tika = config.get("tika")
  return tika.host + ":" + tika.port + "/tika"
}
const sendCommand = async (api, body) => {
  try {
    const result = await fetch(api, {
      headers: { "content-type": "application/json" }, method: "post", body: JSON.stringify(body)
    })
    if (result.status != 200) {
      console.log(result.statusText)
    }
  } catch (err) {
    console.log(err)
  }
}

const createCore = async () => {
  const solr = config.get('solr')
  const api = solr.host + ":" + solr.port + "/solr/admin/cores?action="
  try {
    let response = await fetch(`${api}STATUS&core=${solr.core}`)
    if (response.status != 200) {
      console.log(response.statusText)
    } else {
      const status = await response.json()
      if (!status.status[solr.core].name) {
        response = await fetch(`${api}RENAME&core=gettingstarted&other=${solr.core}`)
        const result = await response.json()
        console.log(result.msg)
      }
    }
  } catch (err) {
    console.log(err)
  }
}


const checkSchema = async app => {
  const fields = config.get("fields")
  const solr = makeSolrURL()
  const api = solr + "/schema"
  const res = await fetch(api, { method: 'get' })
  if (res.status == 200) {
    const schema = await res.json()
    for (const field of schema.schema.fields) {
      const check = fields.find(n => n.name === field.name)
      if (check) {
        if (!config.util.equalsDeep(check, field)) {
          await sendCommand(api, { "replace-field": field })
        }
      } else {
        // await sendCommand(api, { "delete-field": { name: field.name } })
      }
    }
    for (const field of fields) {
      const check = schema.schema.fields.find(n => n.name === field.name)
      if (!check) {
        await sendCommand(api, { "add-field": field })
      }
    }
    // console.log(schema)
  } else {
    if (res.status == 404) {
      console.log("schema not found")
      await createCore(solr)
      await checkSchema(app)
    } else {
      throw new Error("could not retrieve schema " + res.statusText)
    }
  }
}

const toSolr = async contents => {
  const tika = makeTikaURL()
  const headers = {
    "content-type": "application/octet-stream"
  }
  const result = await fetch(tika, {
    method: "PUT",
    body: contents
  })
  if (result.status == 200) {
    // const meta = await result.text()
    const text=await result.text();
    return text
  } else {
    throw new Error("Tika failed " + result.status + ", " + result.statusText)
  }
}

module.exports = { checkSchema, toSolr }
