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
 */

package ch.rgw.lucinda

import ch.rgw.tools.CmdLineParser
import ch.rgw.tools.Configuration
import ch.rgw.tools.net.NetTool
import com.hazelcast.config.Config
import io.vertx.core.*
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by gerry on 20.03.16.
 */

val rootLog = Logger.getLogger("lucinda")
val config = Configuration()


val indexManager: IndexManager  by lazy {
    if (!indexdir.exists()) {
        indexdir.mkdirs()
    }
    IndexManager(indexdir.absolutePath)
}

val indexdir: File by lazy {
    File(config.get("fs_indexdir", "target/store"))
}

val baseaddr: String get() = config.get("msg_prefix", "ch.rgw.lucinda")
var ip: String = ""

fun main(args: Array<String>) {
    var verticleID: String = ""
    var vertx: Vertx? = null
    var cmdline = CmdLineParser(switches = "client,ip,rescan,config,daemon")
    if (!cmdline.parse(args)) {
        println(cmdline.errmsg)
        System.exit(-1)
    }

    val client = cmdline.parsed.containsKey("client")
    if (cmdline.parsed.containsKey("config")) {
        config.merge(Configuration(cmdline.get("config")))
    }

    val net = cmdline.get("ip")
    ip = if (net.isEmpty()) {
        ""
    } else {
        NetTool.getMatchingIP(net) ?: ""
    }

    val hazel = Config()
    val vertxOptions = VertxOptions().setClustered(true)
            .setMaxEventLoopExecuteTime(5000000000L)
            .setBlockedThreadCheckInterval(2000L)

    if (ip.isNotEmpty()) {
        val network = hazel.networkConfig
        network.interfaces.setEnabled(true).addInterface(ip)
        network.publicAddress = ip
        vertxOptions.setClusterHost(ip)
    }
    val mgr = HazelcastClusterManager(hazel)

    Vertx.clusteredVertx(vertxOptions.setClusterManager(mgr)) { result ->
        vertx = result.result()
        if (cmdline.parsed.containsKey("config")) {
            config.merge(Configuration(cmdline.get("config")))
        } else {
            config.merge(Configuration("default.cfg", "user.cfg"))
        }
        if (client == false) {
            vertx!!.deployVerticle(Launcher(config)) {
                if (it.succeeded()) {
                    verticleID = it.result()
                } else {
                    System.exit(-1);
                }
            }
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


    if (cmdline.parsed.containsKey("rescan")) {
        vertx?.eventBus()?.send(baseaddr + Autoscanner.ADDR_RESCAN, "rescan")
    }
    if (!cmdline.parsed.containsKey("daemon")) {
        println("Enter search term for queries or 'exit' to end program")
        val caddr = baseaddr + Communicator.ADDR_FINDFILES
        while (true) {
            val input = readLine()
            when (input) {
                "exit" -> System.exit(0)
                "rescan" -> {
                    vertx?.eventBus()?.send(baseaddr + Autoscanner.ADDR_RESCAN, "rescan")
                }
                else -> {
                    vertx?.eventBus()?.send<Any>(caddr, JsonObject().put("query", input).put("numhits", 1000)) { result ->
                        if (result.succeeded()) {
                            val po: JsonObject = result.result().body() as JsonObject
                            if (po.getString("status") == "ok") {
                                val metadata = po.getJsonArray("result")
                                metadata.forEach {
                                    val url = (it as JsonObject).getString("url")
                                    val title = it.getString("title")
                                    val id = it.getString("_id")
                                    println(if (url.isNullOrBlank()) {
                                        if (title.isNullOrBlank()) {
                                            id
                                        } else {
                                            title
                                        }
                                    } else {
                                        url
                                    })
                                    if (rootLog.level == Level.FINEST) {
                                        println("id: ${it.getString("_id")}, uuid: ${it.getString("uuid")}")
                                    }
                                    if (it.getString("deleted") == "true") {
                                        println("*** deleted ***")
                                    }
                                }
                                println("Query ok - ${metadata.size()} hits.\n")

                            } else {
                                println("error: " + po.getString("message"))
                            }

                        }
                    }
                }
            }
            Thread.sleep(100)
        }
    }
}


class Launcher(val cfg: Configuration) : AbstractVerticle() {
    val log = Logger.getLogger("lucinda.launcher")
    var communicatorID: String = ""
    var autoscannerID: String = ""
    var restpointID:String=""

    override fun start() {
        super.start()
        if(cfg.get("msg_use","no")=="yes") {
            vertx.deployVerticle(Communicator(cfg), DeploymentOptions().setWorker(true)) { handler ->
                if (handler.succeeded()) {
                    log.log(Level.INFO, "Communicator launch successful " + handler.result())
                    communicatorID = handler.result()
                } else {
                    log.log(Level.SEVERE, "Communicator launch failed " + handler.result())
                }
            }
        }
        if(cfg.get("rest_use","no")=="yes"){
            vertx.deployVerticle(Restpoint(cfg), DeploymentOptions().setWorker(true)){ handler ->
                if(handler.succeeded()){
                    log.info("Restpoint launch succeeded")
                    restpointID=handler.result()
                }else{
                    log.severe("Restpoint launch failed "+handler.result())
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

                    vertx.eventBus().send<Any>(baseaddr + Autoscanner.ADDR_START, JsonObject().put("dirs", dirs)) { answer ->
                        if (answer.failed()) {
                            log.severe("could not start Autoscanner " + answer.result())
                        }
                    }
                } else {
                    log.severe("Autoscanner launch failed " + handler2.result())
                }
            }
        }

    }

    override fun stop() {
        super.stop()
        if (communicatorID.isNotEmpty()) {
            vertx.undeploy(communicatorID)
        }
        if(restpointID.isNotEmpty()){
            vertx.undeploy(restpointID)
        }
        if (autoscannerID.isNotEmpty()) {
            vertx.eventBus().send<Any>(baseaddr + Autoscanner.ADDR_STOP, "") {
                vertx.undeploy(autoscannerID)
            }
        }
    }

}




