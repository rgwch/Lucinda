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
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.util.logging.Logger

/**
 * Created by gerry on 20.03.16.
 */

private val lucindacfg = Configuration()

val indexManager: IndexManager  by lazy {
    if (!indexDir.exists()) {
        indexDir.mkdirs()
    }
    IndexManager(indexDir.absolutePath)
}

val restPort = 2016
/**
 * get document directory
 */
val docDir: File by lazy {
    if (System.getenv("LUCINDA_DOCUMENTS") != null) {
        File(System.getenv("LUCINDA_DOCUMENTS"))
    } else {
        File(lucindacfg.get("docdir", "target/store"))
    }
}

val baseDir: File by lazy {
    if (System.getenv("LUCINDA_HOME") != null) {
        File(System.getenv("LUCINDA_HOME"))
    } else {
        File(lucindacfg.get("basedir", "target/store"))
    }
}

/**
 * get index directory
 */
val indexDir: File by lazy {
    File(baseDir, "index")
}

/**
 * get tempfile directory
 */
val tmpDir: File by lazy {
    File(baseDir, "tempfiles")
}

/**
 * get dir for failed imports
 */
val failuresDir: File by lazy {
    File(baseDir, "failures")
}

val default_language: String by lazy {
    System.getenv("LUCINDA_LANGUAGE ") ?: lucindacfg.get("default_language") ?: "de"
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
        lucindacfg.merge(Configuration(cmdline.get("config")))
    } else {
        lucindacfg.merge(Configuration("default.cfg", "user.cfg"))
    }

    vertx.deployVerticle(Launcher()) {
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


/**
 * Launch Autoscanner and Restpoint
 */
class Launcher() : AbstractVerticle() {
    val log = Logger.getLogger("lucinda.launcher")
    var autoscannerID: String = ""
    var restpointID: String = ""

    override fun start() {
        super.start()

        vertx.deployVerticle(Restpoint(), DeploymentOptions().setWorker(true)) { handler ->
            if (handler.succeeded()) {
                log.info("Restpoint launch succeeded")
                restpointID = handler.result()
            } else {
                log.severe("Restpoint launch failed " + handler.cause())
            }

        }

        vertx.deployVerticle(Autoscanner(), DeploymentOptions().setWorker(true)) { handler2 ->
            if (handler2.succeeded()) {
                log.info("setup watch hook(s) for ${docDir.absolutePath}")
                autoscannerID = handler2.result()
                val dirs = JsonArray()
                dirs.add(docDir.absolutePath)

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


    override fun stop() {
        super.stop()
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




