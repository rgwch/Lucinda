package ch.rgw.lucinda

import ch.rgw.tools.Configuration
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.Router
import java.util.logging.Logger

/**
 * Created by gerry on 22.05.16.
 */
class Restpoint(val cfg: Configuration): AbstractVerticle(){
    val log= Logger.getLogger("Restpoint")

    override fun start(future: Future<Void>){
        val router=Router.router(vertx)

        router.get("/api/1.0/ping").handler { ctx ->
            ctx.response().end("pong")
            log.info("we've got a ping!")
        }
        router.post("/api/query").handler { ctx ->
            val cmd=ctx.getBodyAsString("utf-8")

        }

        router.get("/api/query/:id").handler {

        }

        vertx.createHttpServer()
        .requestHandler{ request -> router.accept(request)}
        .listen(cfg.get("rest_port","8080").toInt()){
            result ->
            if(result.succeeded()){
                future.complete()
            }else{
                future.fail(result.cause())
            }
        }
    }

}