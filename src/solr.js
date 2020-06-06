/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const config = require('config')
const log = require('./logger')
const fetch = require('node-fetch')
const scfg = config.get('solr')

const wait = ms => {
  return new Promise(resolve => {
    setTimeout(resolve, ms)
  })
}

const makeSolrURL = () => {
  if (!config.has("solr")) {
    log.error("FATAL: Solr is not defined in configuration!")
    throw new Error("Solr not defined")
  }
  const solr = config.get("solr")
  return solr.host + ":" + solr.port + "/solr/" + solr.core
}

/**
 * Send a command to the Solr server. Specifies json as acceptable answer format.
 * @param {string} api address to call
 * @param {any} body request body
 * @returns the JSON Result from the server
 * @throws error if the call did not succeed.
 */
const sendCommand = async (api, body) => {
  try {
    const result = await fetch(api, {
      headers: { "content-type": "application/json" }, method: "post", body: JSON.stringify(body)
    })
    if (result.status != 200) {
      // console.log(result.statusText)
      throw ("Error sending Command " + result.status + ", " + result.statusText)
    }
    return await result.json()
  } catch (err) {
    console.log(err)
    return {
      "status": "error",
      err, api, body
    }
  }
}

/**
 * check if the configured core exists. If not, rename the 'gettingstarted' core to the configured name.
 */
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


/**
 * Check if the correct lucinda schema is installed in the Solr core. If not, create or correct it.
 */
const checkSchema = async () => {
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
      log.error("schema not found")
      await createCore(solr)
      await checkSchema()
    } else if (res.status == 503) { // service unavailable
      log.warn("Solr is not yet available. Wait 30 seconds")
      await wait(30000)
      await (createCore(solr))
      await checkSchema()
    } else {
      throw new Error("could not retrieve schema " + res.statusText)
    }
  }
}

/**
 * Add a document to Solr
 * @param {any} contents the JSON Document to index. the property contents should contain the text contents and will
 * not be stored.
 */
const toSolr = async contents => {
  const api = makeSolrURL() + "/update?commit=true&json.command=false"
  const result = await sendCommand(api, contents)
  return result
}

/**
 * Apply a query to Solr
 * @param {string} term 
 */
const find = async (term) => {
  const api = makeSolrURL() + "/query"
  const q = (typeof (term) == 'string') ? { query: term } : term
  const result = await sendCommand(api, q)
  return result
}


/**
 * Remove a document from the Solr index
 * @param {*} id 
 */
const remove = async id => {
  const api = makeSolrURL() + "/update?commit=true"
  const result = await sendCommand(api, {
    delete: {
      query: "id:" + id
    }
  })
  return result
}

module.exports = { checkSchema, toSolr, find, remove, wait }
