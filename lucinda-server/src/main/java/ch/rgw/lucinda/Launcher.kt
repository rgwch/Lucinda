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
 */

package ch.rgw.lucinda

import ch.rgw.tools.CmdLineParser
import ch.rgw.tools.Configuration
import io.vertx.core.*
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.util.logging.Logger

/**
 * Created by gerry on 20.03.16.
 */

val config = Configuration()
val indexManager: IndexManager  by lazy {
    if (!indexdir.exists()) {
        indexdir.mkdirs()
    }
    IndexManager(indexdir.absolutePath)
}

val baseDir: File by lazy {
    if(System.getenv("LUCINDA_DOCUMENTS")!=null){
        File(System.getenv("LUCINDA_DOCUMENTS"))
    }else {
        File(config.get("fs_basedir", "target/store"))
    }
}

val indexdir: File by lazy {
    File(baseDir, "index")
}


fun main(args: Array<String>) {
    var verticleID: String = ""
    var cmdline = CmdLineParser(switches = "rescan,config")
    if (!cmdline.parse(args)) {
        println(cmdline.errmsg)
        System.exit(-1)
    }

    val vertxOptions = VertxOptions()
            .setMaxEventLoopExecuteTime(5000000000L)
            .setBlockedThreadCheckInterval(2000L)


    val vertx = Vertx.vertx(vertxOptions);
    if (cmdline.parsed.containsKey("config")) {
        config.merge(Configuration(cmdline.get("config")))
    } else {
        config.merge(Configuration("default.cfg", "user.cfg"))
    }

    vertx.deployVerticle(Launcher(config)) {
        if (it.succeeded()) {
            verticleID = it.result()
        } else {
            System.exit(-1);
        }
    }

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            println("Shutdown signal received")
            indexManager.shutDown()
            vertx?.undeploy(verticleID)
            vertx?.close()
        }
    })

}


class Launcher(val cfg: Configuration) : AbstractVerticle() {
    val log = Logger.getLogger("lucinda.launcher")
    var autoscannerID: String = ""
    var restpointID: String = ""

    override fun start() {
        super.start()

        if (cfg.get("rest_use", "no") == "yes") {
            vertx.deployVerticle(Restpoint(cfg), DeploymentOptions().setWorker(true)) { handler ->
                if (handler.succeeded()) {
                    log.info("Restpoint launch succeeded")
                    restpointID = handler.result()
                } else {
                    log.severe("Restpoint launch failed " + handler.cause())
                }

            }
        }
        val watchdirs = cfg.get("fs_watch")
        if (watchdirs != null) {
            vertx.deployVerticle(Autoscanner(), DeploymentOptions().setWorker(true)) { handler2 ->
                if (handler2.succeeded()) {
                    log.info("setup watch hook(s) for ${watchdirs}")
                    autoscannerID = handler2.result()
                    val dirs = JsonArray()
                    watchdirs.split("\\s*,\\s*".toRegex()).forEach {
                        dirs.add(it)
                    }

                    vertx.eventBus().send<Any>(Autoscanner.ADDR_START, JsonObject().put("dirs", dirs)) { answer ->
                        if (answer.failed()) {
                            log.severe("could not start Autoscanner " + answer.cause())
                        }
                    }
                } else {
                    log.severe("Autoscanner launch failed " + handler2.cause())
                }
            }
        }

    }

    override fun stop() {
        super.stop()
        /*
        if (communicatorID.isNotEmpty()) {
            vertx.undeploy(communicatorID)
        }
        */
        if (restpointID.isNotEmpty()) {
            vertx.undeploy(restpointID)
        }
        if (autoscannerID.isNotEmpty()) {
            vertx.eventBus().send<Any>(Autoscanner.ADDR_STOP, "") {
                vertx.undeploy(autoscannerID)
            }
        }
    }

}




