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
    val APIVERSION="1.0"

    override fun start(future: Future<Void>) {
        super.start()
        val dispatcher = Dispatcher(cfg, vertx);
        val router = Router.router(vertx)

        router.get("/api/${APIVERSION}/ping").handler { ctx ->
            ctx.response().end("pong")
            log.info("we've got a ping!")
        }
        router.post("/api/${APIVERSION}/query").handler { ctx ->
            val j = ctx.bodyAsString
            log.info("git REST " + j)
            try {
                val result = dispatcher.find(JsonObject().put("query", j))
                ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                ctx.response().end(Json.encode(result))
            } catch(ex: Exception) {
                ctx.response().statusCode = 400;
                ctx.response().end(ex.message)
            }

        }

        router.get("/api/${APIVERSION}/get/:id").handler { ctx ->
            val id = ctx.request().getParam("id")
            val bytes = Buffer.buffer(dispatcher.get(id))
            ctx.response().putHeader("content-type", "application/octet-stream")
            ctx.response().end(bytes)
        }

        router.post("/api/${APIVERSION}/index").handler { ctx ->
            val j = ctx.bodyAsJson
            dispatcher.addToIndex(j, object : Handler<AsyncResult<Int>> {
                override fun handle(result: AsyncResult<Int>) {
                    if (result.succeeded()) {
                        log.info("indexed ${j.getString("title")}")
                        ctx.response().write(Json.encode(JsonObject().put("status", "ok").put("_id", j.getString("_id"))))
                        ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                        ctx.response().statusCode = 200
                        ctx.response().statusMessage = "content indexed"
                        ctx.response().end();
                    } else {
                        log.warning("failed to import ${j.getString("url")}; ${result.cause().message}")
                        ctx.response().write(Json.encode(JsonObject().put("status", "fail").put("_id", j.getString("_id")).put("message", result.cause().message)))
                        ctx.response().statusCode = 500
                        ctx.response().end()
                    }
                }

            })
        }

        router.post("/api/${APIVERSION}/addfile").handler { ctx ->
            if((ctx.body == null) or (ctx.body.length()<10)){
                ctx.response().statusCode=200
                ctx.response().end();
            }else {
                val s = ctx.bodyAsString
                val j = JsonObject(s)
                dispatcher.indexAndStore(j, object : Handler<AsyncResult<Int>> {
                    override fun handle(result: AsyncResult<Int>) {
                        if (result.succeeded()) {
                            log.info("indexed ${j.getString("title")}")
                            ctx.response().write(Json.encode(JsonObject().put("status", "ok").put("_id", j.getString("_id"))))
                            ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                            ctx.response().statusCode = 201
                            ctx.response().statusMessage = "content indexed and added"
                            ctx.response().end();
                        } else {
                            log.warning("failed to import ${j.getString("url")}; ${result.cause().message}")
                            ctx.response().write(Json.encode(JsonObject().put("status", "fail").put("_id", j.getString("_id")).put("message", result.cause().message)))
                            ctx.response().statusCode = 500
                            ctx.response().end()
                        }

                    }
                })
            }
        }

        vertx.createHttpServer()
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