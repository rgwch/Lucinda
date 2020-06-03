/**************************************************************
 * Copyright (c) 2020 G. Weirich                              *
 * Licensed under the Apache license, version 2.0 see LICENSE *
 **************************************************************/

const winston = require('winston')

/*
  simple logging configuration. To use this preconfigured logger, don't require 'winston',
  but require('logger')
*/

let level
switch(process.env.NODE_ENV){
  case "debug": level="debug"; break;
  case "dockered": level="info"; break;
  case "production": level="warn"; break;
  default: level="debug"
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
