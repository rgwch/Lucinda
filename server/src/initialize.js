const config = require('config')

module.exports = app => {
    let base=config.get('documentRoot')  
    if(base.startsWith('~/')){
        base=base.substring(2)
    }
    if(!base.startsWith("/")){
        base=path.join(process.env.HOME,base)
    }
    config.set("_basepath",base)
}