const winston=require('winston')

/*
  simple logging configuration. To use this preconfigured logger, don't require 'winston',
  but require('logger')
*/

const logger=winston.createLogger({
    level:'debug',

    format: winston.format.combine(
      winston.format.colorize(),
      winston.format.splat(),
      winston.format.simple()
    ),
    transports:[
      new winston.transports.Console()
    ]
  })

  logger.info("Lucinda Server: logger created")
  logger.debug("Debug level active ")
  
module.exports=logger
