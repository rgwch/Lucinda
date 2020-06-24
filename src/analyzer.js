const cfg = require('config')
const fs = require(fs)
const path = require(path)
const inbox = cfg.get("inbox")

/**
 * Analyze, if a filename matches one of the patterns defined in cfg.inbox.
 * if so, try to create a "concern" string, find a directory with that name in
 * lucinda_docs and move the file there.
 * Filename of the resulting file will be tha meaning "title" if given, or
 * else the full filename from filepath.
 * @param {string} filepath 
 */
function analyze(filepath) {
  const ext = path.ext(filepath)
  const basename = path.basename(filepath, ext)
  for (const matcher of inbox) {
    const m = matcher.pattern.match(basename)
    if (m) {
      const meaning=matcher.meaning.split(",")
    }
  }
}

module.exports = analyze