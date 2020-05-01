const fetch = require('node-fetch')
const fs = require('fs').promises
const path=require('path')

get().then(ok => {
    console.log("ok")
}).catch(err=>{
    console.log(err)
})

async function get() {
    const cnt = await fs.readFile(path.join(__dirname,"testocr.pdf"))

    result = await fetch("http://localhost:3030",
        {
            headers: { "content-type": "application/octet-stream", accept: "application/octet-stream" },
            method: "post",
            body: cnt
        })
    const file=await result.buffer()
    await fs.writeFile(path.join(__dirname,"testresult.pdf"), file)
    return "ok"
}
