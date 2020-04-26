/*******************************************************************************
 * Copyright (c) 2016-2019 by G. Weirich
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
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.StaticHandler
import java.util.logging.Logger


/**
 * Since v. 2.0.6 Lucinda dropped EventBus Support and relies only on the REST interface for external
 * connections. For internal communication, however, EventBus is still in use.
 */
class Restpoint() : AbstractVerticle() {
    val log = Logger.getLogger("Restpoint")
    val APIVERSION = "2.0"
    val LUCINDAVERSION = "2.1.4"

    override fun start(future: Future<Void>) {
        super.start()
        val dispatcher = Dispatcher(vertx)
        val router = Router.router(vertx)


        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.PUT)
                .allowedHeader("X-sid"))


        router.get("/lucinda/${APIVERSION}/ping").handler { ctx ->
            ctx.response().setStatusCode(200).putHeader("content-type","text/plain")
                    .end("Welcome to Lucinda v "+LUCINDAVERSION)
            log.info("we've got a ping!")
        }

        /**
        * Rescan document store
        */
        router.get("/lucinda/${APIVERSION}/rescan").handler { ctx->
            ctx.response().setStatusCode(200).putHeader("content-type","text/plain; charset=utf-8")
                    .end("Started rescan")
            vertx.eventBus()?.send(Autoscanner.ADDR_RESCAN, "rescan")

        }
        /**
         * find files
         */
        router.post("/lucinda/${APIVERSION}/query").handler { ctx ->
            ctx.request().bodyHandler { buffer ->
                val j = buffer.toString()
                log.info("got REST query" + j)
                try {
                    val result = dispatcher.find(JsonObject().put("query", j))
                    if (result.isEmpty) {
                        ctx.response().setStatusCode(204).end()
                    } else {
                        val resp = JsonObject().put("status", "ok").put("result", result)
                        ctx.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8").end(Json.encode(resp))
                    }
                } catch (ex: Exception) {
                    ctx.response().setStatusCode(400).end(ex.message)
                }
            }

        }

        /**
         * Retrieve a file by _id
         */
        router.get("/lucinda/${APIVERSION}/get/:id").handler { ctx ->
            val id = ctx.request().getParam("id")
            val contents = dispatcher.get(id)
            if (contents != null) {
                val bytes = Buffer.buffer(contents)
                if (bytes == null) {
                    ctx.response().setStatusCode(404).end()
                } else {
                    ctx.response().putHeader("content-type", "application/octet-stream").setStatusCode(200).end(bytes)
                }
            } else {
                ctx.response().setStatusCode(404).end()
            }
        }

        /**
         * Index a file without adding it to the store
         * request body must be a JSON object with a field 'payload' which contains the file to index as base64,
         * and ansy number of keys for metadata.
         * return: StatusCode 200, json: {status: "ok", _id: "some_uuid"}
         */
        router.post("/lucinda/${APIVERSION}/index").handler { ctx ->
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
                } catch (e: Exception) {
                    e.printStackTrace()
                    log.severe("Exception while handling request " + e.message)
                    ctx.response().setStatusCode(400).end("failed to import")
                }
            }

        }

        /**
         * Index a file and add it to the store
         * request body must be a JSON object with a field 'payload' which contains the file to index as base64,
         * and any number of keys for metadata.
         * return: StatusCode 200, json: {status: "ok", _id: "some_uuid"}
         */
        router.post("/lucinda/${APIVERSION}/addfile").handler { ctx ->
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
                } catch (e: Exception) {
                    e.printStackTrace()
                    log.severe("Exception while handling request " + e.message)
                    ctx.response().setStatusCode(400).end("failed to import")
                }
            }
        }

        router.post("/lucinda/${APIVERSION}/update").handler { ctx ->
            ctx.request().bodyHandler { buffer ->
                try {
                    dispatcher.update(buffer.toJsonObject())
                    ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                    ctx.response().statusCode = 202
                    ctx.response().statusMessage = "update ok"
                    ctx.response().end()
                } catch (e: Exception) {
                    log.warning("update failed ${buffer.toString()}; ${e.message}")
                    ctx.response().setStatusCode(417).end()

                }

            }

        }

        router.get("/lucinda/${APIVERSION}/remove/:id").handler { ctx ->
            val id = ctx.request().getParam("id")
            try {
                dispatcher.delete(id)
                ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                ctx.response().statusCode = 200
                ctx.response().statusMessage = "Document removed"
                ctx.response().end(Json.encode(JsonObject().put("status", "ok")))

            } catch (e: Exception) {
                log.warning("remove failed " + e.message)
                ctx.response().statusCode = 500
                ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                ctx.response().end(Json.encode(JsonObject().put("status", "fail")))
            }
        }

        val hso = HttpServerOptions().setCompressionSupported(true).setIdleTimeout(0).setTcpKeepAlive(true)
        vertx.createHttpServer(hso)
                .requestHandler { request -> router.accept(request) }
                .listen(restPort) { result ->
                    if (result.succeeded()) {
                        future.complete()
                    } else {
                        future.fail(result.cause())
                    }
                }
    }


}