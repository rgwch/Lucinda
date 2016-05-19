/*******************************************************************************
 * Copyright (c) 2016 by G. Weirich
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *
 * Contributors:
 * G. Weirich - initial implementation
 ******************************************************************************/

package ch.rgw.lucinda


import ch.rgw.tools.Configuration
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by gerry on 20.03.16.
 */

class Communicator(val cfg: Configuration) : AbstractVerticle() {

    val log = Logger.getLogger("lucinda.Communicator")
    val eb: EventBus by lazy {
        vertx.eventBus()
    }

    override fun start() {
        super.start()
        val dispatcher = Dispatcher(cfg, vertx)
        eb.consumer<Message<JsonObject>>(baseaddr + ADDR_PING) { msg ->
            log.info("we got a Ping!")
            msg.reply(JsonObject().put("status", "ok").put("pong", ip))
        }
        /*
         * address defaulting to 'ch.rgw.lucinda.import':
          * fields in message.body():
          *
         */
        eb.consumer<Message<JsonObject>>(baseaddr + ADDR_IMPORT) { message ->
            val j = message.body() as JsonObject
            log.info("got message ADDR_IMPORT ${j.getString("title")}")
            try {

                dispatcher.indexAndStore(j, object : Handler<AsyncResult<Int>> {
                    override fun handle(result: AsyncResult<Int>) {
                        if (result.succeeded()) {
                            log.info("imported ${j.getString("url")}")
                            message.reply(JsonObject().put("status", "ok").put("_id", j.getString("_id")))
                        } else {
                            log.warning("failed to import ${j.getString("url")}; ${result.cause().message}")
                            message.reply(JsonObject().put("status", "fail").put("_id", j.getString("_id")).put("message", result.cause().message))
                        }
                    }

                })

                /*
                        }
               */
            } catch(e: Exception) {
                e.printStackTrace()
                log.severe("import failed " + e.message)
                fail("", e)
            }

        }

        eb.consumer<Message<JsonObject>>(baseaddr + ADDR_INDEX) { msg ->
            val j = msg.body() as JsonObject
            log.info("got message ADDR_INDEX " + Json.encodePrettily(j))

            try {
                dispatcher.addToIndex(j, object : Handler<AsyncResult<Int>> {
                    override fun handle(result: AsyncResult<Int>) {
                        if (result.succeeded()) {
                            log.info("indexed ${j.getString("title")}")
                            msg.reply(JsonObject().put("status", "ok").put("_id", j.getString("_id")))
                        } else {
                            log.warning("failed to import ${j.getString("url")}; ${result.cause().message}")
                            msg.reply(JsonObject().put("status", "fail").put("_id", j.getString("_id")).put("message", result.cause().message))
                        }
                    }

                })
                msg.reply(JsonObject().put("status", "ok").put("_id", j.getString("_id")));
            } catch(e: Exception) {
                fail("can't index", e)
            }
        }

        eb.consumer<Message<JsonObject>>(baseaddr + ADDR_GETFILE) { message ->
            val j = message.body() as JsonObject
            log.info("got message ADDR_GETFILE " + Json.encodePrettily(j))

            try {
                val bytes = dispatcher.get(j.getString("_id"))
                if (bytes == null) {
                    fail("could not read ${j.getString("url")}")
                } else {
                    val result = JsonObject().put("status", "ok").put("result", bytes)
                    message.reply(result)
                }

            } catch(e: Exception) {
                fail("could not load file; ${e.message}", e)
            }

        }
        eb.consumer<Message<JsonObject>>(baseaddr + ADDR_FINDFILES) { msg ->
            val j = msg.body() as JsonObject
            log.info("got message ADDR_FINDFILES " + Json.encodePrettily(j))

            try {
                val result = dispatcher.find(j)
                msg.reply(JsonObject().put("status", "ok").put("result", result))
            } catch(e: Exception) {

                fail("", e)
            }
        }
        eb.consumer<Message<JsonObject>>(baseaddr+ADDR_UPDATE) { msg ->
            val j=msg.body() as JsonObject
            log.info("got message ADDR_FINDFILES " + Json.encodePrettily(j))

            try {
                val result = dispatcher.update(j)
                msg.reply(JsonObject().put("status", "ok").put("result", result))
            } catch(e: Exception) {

                fail("", e)
            }
        }

    }

    fun success(msg: Message<Any>, result: JsonObject = JsonObject()) {
        msg.reply(result.put("status", "ok"))
    }

    fun fail(msg: String, ex: Exception? = null) {
        val j = JsonObject().put("message", msg)
        log.warning("failed " + ex)
        if (ex == null) {
            j.put("status", "failed")
        } else {
            j.put("status", "exception").put("message", ex.message)
            log.log(Level.WARNING, ex.message + "\n\n" + ex.stackTrace.toString())
        }
        eb.send(ADDR_ERROR, j)
    }

    companion object {
        val BASEADDR = config.get("msg_prefix", "ch.rgw.lucinda")
        /** reply address for error messages */
        const val ADDR_ERROR = ".error"
        /** Add a file to the storage */
        const val ADDR_IMPORT = ".import"
        /** Index a file in-place (don't add it to the storage) */
        const val ADDR_INDEX = ".index"
        /** Retrieve a file by _id*/
        const val ADDR_GETFILE = ".get"
        /** Get Metadata of files matching a search query */
        const val ADDR_FINDFILES = ".find"
        /** Update Metadata of a file by _id*/
        const val ADDR_UPDATE = ".update"
        /** Connection check */
        const val ADDR_PING = ".ping"

    }

}

