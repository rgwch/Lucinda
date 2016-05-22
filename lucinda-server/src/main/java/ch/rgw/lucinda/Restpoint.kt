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
 * Created by gerry on 22.05.16.
 */
class Restpoint(val cfg: Configuration) : AbstractVerticle() {
    val log = Logger.getLogger("Restpoint")

    override fun start(future: Future<Void>) {
        super.start()
        val dispatcher = Dispatcher(cfg, vertx);
        val router = Router.router(vertx)

        router.get("/api/1.0/ping").handler { ctx ->
            ctx.response().end("pong")
            log.info("we've got a ping!")
        }
        router.post("/api/1.0/query").handler { ctx ->
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

        router.get("/api/1.0/get/:id").handler { ctx ->
            val id = ctx.request().getParam("id")
            val bytes = Buffer.buffer(dispatcher.get(id))
            ctx.response().putHeader("content-type", "application/octet-stream")
            ctx.response().end(bytes)
        }

        router.post("/api/1.0/index").handler { ctx ->
            val j = ctx.bodyAsJson
            dispatcher.addToIndex(j, object : Handler<AsyncResult<Int>> {
                override fun handle(result: AsyncResult<Int>) {
                    if (result.succeeded()) {
                        log.info("indexed ${j.getString("title")}")
                        ctx.response().write(Json.encode(JsonObject().put("status", "ok").put("_id", j.getString("_id"))))
                        ctx.response().putHeader("content-type", "application/json; charset=utf-8")
                        ctx.response().statusCode = 201
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

        router.post("/api/1.0/addfile").handler { ctx ->
            val j = ctx.bodyAsJson
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

        vertx.createHttpServer()
                .requestHandler { request -> router.accept(request) }
                .listen(cfg.get("rest_port", "8080").toInt()) {
                    result ->
                    if (result.succeeded()) {
                        future.complete()
                    } else {
                        future.fail(result.cause())
                    }
                }
    }

}