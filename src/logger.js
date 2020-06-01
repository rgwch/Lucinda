const winston = require('winston')

/*
  simple logging configuration. To use this preconfigured logger, don't require 'winston',
  but require('logger')
*/

let level
switch(process.env.NODE_ENV){
  case "debug": level="debug"; break;
  case "dockered": level="error"; break;
  case "production": level="warn"; break;
  default: level="info"
}

const logger = winston.createLogger({
  level,

  format: winston.format.combine(
    winston.format.colorize(),
    winston.format.splat(),
    winston.format.simple()
  ),
  transports: [
    new winston.transports.Console()
  ]
})


module.exports = logger
