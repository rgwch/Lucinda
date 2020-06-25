const { Console } = require('console')

const fs = require('fs').promises
const version = "3.0.7"

doReplace("openapi.yaml", /version: "(\d\.\d\.\d)"/, `version: "${version}"`)
doReplace("docker-compose.yaml", /lucinda-server:(\d\.\d\.\d)/, `lucinda-server:${version}`)
doReplace("package.json", /"version": "(\d\.\d\.\d)"/, `"version": "${version}"`)
doReplace("Dockerfile", /version="(\d\.\d\.\d)"/, `version="${version}"`)
doReplace("Dockerfile.opt", /version="(\d\.\d\.\d)"/, `version="${version}"`)


async function doReplace(file, pattern, replacement) {
  try {
    const cnt = await fs.readFile(file, "utf-8")
    await fs.rename(file, file + ".bak")
    await fs.writeFile(file, cnt.replace(pattern, replacement))
    console.log("written " + file)
  } catch (ex) {
    console.log("*** ERROR " + ex)
  }
}