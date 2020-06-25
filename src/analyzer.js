const cfg = require('config')
const fs = require('fs')
const path = require('path')
const inbox = cfg.get("inbox")
const concern_fields = cfg.get("concern_fields")

/**
 * Analyze, if a filename matches one of the patterns defined in cfg.inbox.
 * if so, try to create a "concern" string, find a directory with that name in
 * lucinda_docs and move the file there.
 * Filename of the resulting file will be tha meaning "title" if given, or
 * else the full filename from filepath.
 * @param {string} filepath 
 */
function analyze(filepath) {
  const ext = path.extname(filepath)
  const basename = path.basename(filepath, ext)
  for (const matcher of inbox.filenames) {
    const rx = new RegExp(matcher.pattern)
    const m = rx.exec(basename)
    if (m) {
      const meaning = matcher.meaning.split(",")
      let dirname = ""
      for (let cf = 0; cf < concern_fields.length; cf++) {
        const part = meaning.indexOf(concern_fields[cf])
        if (part != -1) {
          dirname += m[part + 1]
        }
      }
      let filename = ""
      const di = meaning.indexOf("date")
      if (di != -1) {
        filename += m[di + 1]
      }
      const title = meaning.indexOf("title")
      if (title != -1) {
        filename += "_" + m[title + 1]
      }
      if (filename.length == 0) {
        filename = m[0]
      }
      return dirname + path.sep + filename.replace(/\s/g, "_") + ext
    }
  }
}

/**
 * Find a field of the concern description 
 * @param {*} concernfield 
 * @param {*} matchedfields 
 * @param {*} meaning 
 */
function findMatch(concernfield, matchedfields, meaning) {
  const found = meaning.find(el => concernfield == el)
  if (found) {
    return found
  }
}
module.exports = analyze