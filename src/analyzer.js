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
      for (const cf of concern_fields) {
        const part = findMatch(cf, m, meaning)
      }
    }
  }
}

function findMatch(concernfield, matchedfields, meaning) {
  if (meaning.find(concernfield)) {

  }
}
module.exports = analyze