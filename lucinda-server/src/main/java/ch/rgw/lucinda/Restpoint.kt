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
 ********************************************************************************/

package ch.rgw.lucinda

import ch.rgw.tools.Configuration
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import java.util.logging.Logger


/**
 * Sommetimes, a REST Api is more convenient, than the EventBus. For example, if multicast is not easily possible, as in
 * VPN or Docker scenarios. REST is always available. Lucinde offers both interfaces.
 * Only the channel for async return messages is always EventBus this time.
 *
 * Created by gerry on 22.05.16.
 */
class Restpoint(val cfg: Configuration) : AbstractVerticle() {
    val log = Logger.getLogger("Restpoint")
    val APIVERSION = "1.0"

    override fun start(future: Future<Void>) {
        super.start()
        val dispatcher = Dispatcher(cfg, vertx);
        val router = Router.router(vertx)

        router.get("/api/${APIVERSION}/ping").handler { ctx ->
            ctx.response().end("pong")
            log.info("we've got a ping!")
        }
        /**
         * find files
         */
        router.post("/api/${APIVERSION}/query").handler { ctx ->
            ctx.request().bodyHandler { buffer ->
                val j = buffer.toString()
                log.info("got REST " + j)
                try {
                    val result = dispatcher.find(JsonObject().put("query", j))
                    if(result.isEmpty){
                        ctx.response().setStatusCode(204).end()
                    }else {
                        val resp=JsonObject().put("status","ok").put("result",result)
                        ctx.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8").end(Json.encode(resp))
                    }
                } catch(ex: Exception) {
                    ctx.response().setStatusCode(400).end(ex.message)
                }
            }

        }

        /**
         * Retrieve a file by _id
         */
        router.get("/api/${APIVERSION}/get/:id").handler { ctx ->
            val id = ctx.request().getParam("id")
            val bytes = Buffer.buffer(dispatcher.get(id))
            if(bytes==null){
                ctx.response().setStatusCode(404).end();
            }else {
                ctx.response().putHeader("content-type", "application/octet-stream").setStatusCode(200).end(bytes)
            }
        }

        /**
         * Index a file without adding it to the store
         */
        router.post("/api/${APIVERSION}/index").handler { ctx ->
            ctx.request().bodyHandler { buffer ->
                try {
                    val j = buffer.toJsonObject()
                    dispatcher.addToIndex(j, object : Handler<AsyncResult<Int>> {
                        override fun handle(result: AsyncResult<Int>) {
                            if (result.succeeded()) {
                                log.info("indexed ${j.getString("title")}")
                                ctx.response().statusCode = 200
                                ctx.response().statusMessage = "content indexed"
                                ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                                ctx.response().end(Json.encode(JsonObject().put("status", "ok").put("_id", j.getString("_id"))))
                            } else {
                                log.warning("failed to import ${j.getString("url")}; ${result.cause().message}")
                                ctx.response().statusCode = 406
                                ctx.response().end(Json.encode(JsonObject().put("status", "fail").put("_id", j.getString("_id")).put("message", result.cause().message)))
                            }
                        }

                    })
                } catch(e: Exception) {
                    e.printStackTrace()
                    log.severe("Exception while handling request " + e.message)
                    ctx.response().setStatusCode(400).end("failed to import")
                }
            }

        }

        /**
         * Add a file t the index and to the store
         */
        router.post("/api/${APIVERSION}/addfile").handler { ctx ->
            ctx.request().bodyHandler { buffer ->
                try {
                    val j = buffer.toJsonObject()
                    dispatcher.indexAndStore(j, object : Handler<AsyncResult<Int>> {
                        override fun handle(result: AsyncResult<Int>) {
                            if (result.succeeded()) {
                                log.info("added ${j.getString("title")}")
                                ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                                ctx.response().statusCode = 201
                                ctx.response().statusMessage = "content indexed and added"
                                ctx.response().end(Json.encode(JsonObject().put("status", "ok").put("_id", j.getString("_id"))))
                            } else {
                                log.warning("failed to import ${j.getString("url")}; ${result.cause().message}")
                                ctx.response().statusCode = 500
                                ctx.response().end(Json.encode(JsonObject().put("status", "fail").put("_id", j.getString("_id")).put("message", result.cause().message)))
                            }

                        }
                    })
                } catch(e: Exception) {
                    e.printStackTrace()
                    log.severe("Exception while handling request " + e.message)
                    ctx.response().setStatusCode(400).end("failed to import")
                }
            }
        }

        router.post("/api/${APIVERSION}/update").handler { ctx ->
            ctx.request().bodyHandler { buffer ->
                try {
                    dispatcher.update(buffer.toJsonObject())
                    ctx.response().statusCode = 202
                    ctx.response().statusMessage = "update ok"
                    ctx.response().end()
                } catch(e: Exception) {
                    log.warning("update failed ${buffer.toString()}; ${e.message}")
                    ctx.response().setStatusCode(417).end()

                }

            }

        }

        val hso=HttpServerOptions().setCompressionSupported(true).setIdleTimeout(0).setTcpKeepAlive(true)
        vertx.createHttpServer(hso)
                .requestHandler { request -> router.accept(request) }
                .listen(cfg.get("rest_port", "2016").toInt()) {
                    result ->
                    if (result.succeeded()) {
                        future.complete()
                    } else {
                        future.fail(result.cause())
                    }
                }
    }


}