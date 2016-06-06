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

import ch.rgw.crypt.makeHash
import ch.rgw.io.FileTool
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.IOException
import java.nio.file.*
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.logging.Logger

/**
 * Observe one ore more directories for file changes. If such changes occur, add, remove or renew concerned
 * files with the lucinda's index.
 * Usage: Send an ADDR_START message with a JsonObject cotaining a JsonArray 'dirs' with the Directories to scan and
 * a Long 'interval' with the number of milliseconds to wait after a change before accepting the next one.
 * Created by gerry on 25.04.16.
 */
class Autoscanner : AbstractVerticle() {

    val eb: EventBus by lazy {
        vertx.eventBus()
    }
    var timer = 0L
    val watcher = FileSystems.getDefault().newWatchService()
    val keys = HashMap<WatchKey, Path>()

    override fun start() {
        super.start()

        eb.consumer<Message<JsonObject>>(baseaddr + ADDR_START) { msg ->
            val j = msg.body() as JsonObject
            log.fine("got start message ${Json.encodePrettily(j)}")
            register(j.getJsonArray("dirs"))

            timer = vertx.setPeriodic(j.getLong("interval") ?: 500L) { h ->
                try {
                    loop()
                } catch(e: Exception) {
                    e.printStackTrace()
                    log.severe("exception in work loop " + e.message)
                }
            }
            msg.reply(JsonObject().put("status", "ok"))
        }
        eb.consumer<Message<JsonObject>>(baseaddr + ADDR_STOP) {
            log.fine("got stop message")
            if (timer > 0L) {
                vertx.cancelTimer(timer)
            }
        }
        eb.consumer<Message<String>>(baseaddr + ADDR_RESCAN) {
            log.info("got rescan message")
            watchedDirs.forEach {
                rescan(it)
            }
        }
    }

    fun loop() {
        // check if there's a key to be signalled
        val key: WatchKey? = watcher.poll()

        if (key != null) {
            val dir = keys[key]
            if (dir == null) {
                log.warning("WatchKey not recognized!!")
                return
            }

            for (event in key.pollEvents()) {
                val kind = event.kind()

                // Context for directory entry event is the file name of entry
                val ev = cast<Path>(event)
                val name = ev.context()
                val child = dir.resolve(name)

                // print out event
                // System.out.format("%s: %s\n", event.kind().name(), child)
                log.fine("Event: ${event.kind().name()}, file: ${child}")
                when (kind) {
                    ENTRY_CREATE -> addFile(child)
                    ENTRY_DELETE -> removeFile(child)
                    ENTRY_MODIFY -> checkFile(child)
                    OVERFLOW -> rescan(dir)
                    else -> log.warning("unknown event kind ${kind.name()}")
                }

                // reset key and remove from set if directory no longer accessible
                val valid = key.reset()
                if (!valid) {
                    keys.remove(key)

                    // all directories are inaccessible
                    if (keys.isEmpty()) {
                        return
                    }
                }
            }
        }
    }

    /**
     * Register the given directories with the WatchService
     * @param dirs: JsonArray of Strings denoting Paths
     */
    @Throws(IOException::class)
    private fun register(dirs: JsonArray) {
        dirs.forEach {
            val dir = Paths.get(it as String)
            watchedDirs.add(dir)
            if (!Files.exists(dir)) {
                Files.createDirectories(dir)
                log.info("created directory ${dir.toString()}")
            }
            rescan(dir)
        }
    }

    fun rescan(dir: Path) {
        vertx.executeBlocking<Int>(object : Handler<Future<Int>> {
            override fun handle(future: Future<Int>) {
                Files.walkFileTree(dir, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
                        try {
                            checkFile(file)
                        }catch(e: Exception){
                            log.severe("Exception while checking ${file.toAbsolutePath()}, ${e.message}")
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        if (!(keys.containsValue(dir))) {
                            val key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                            log.info("added watch key for ${dir.toAbsolutePath().toString()}")
                            keys.put(key, dir)
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
                future.complete()
            }

        }, object : Handler<AsyncResult<Int>> {
            override fun handle(result: AsyncResult<Int>) {
                if (result.succeeded()) {
                    log.info("imported ${dir}")
                } else {
                    log.severe("import ${dir} failed (${result.cause().message})")
                }
            }
        })

    }

    /**
     * Check a file to watch. <br />
     * If it is a normal file: Test if it exists in the index
     * <ul>
     *     <li>if it exists in the index: test if it has the same checksum, and if not: reimport</li>
     *     <li>if it does not yet exist, import it
     * </ul><br />
     * if it is a directory: walk the directory tree for all files and directories
     *
     */
    fun checkFile(file: Path) {
        log.entering("Autoscanner", "checkFile")
        if(!exclude(file)) {
            if (Files.isRegularFile(file) && (!Files.isHidden(file))) {
                val absolute = file.toFile().absolutePath
                log.info("checking ${absolute}")
                val id = makeID(file)
                val doc = indexManager.getDocument(id)
                if (doc == null) {
                    log.fine("did not find ${file}/${id} in index. Adding")
                    addFile(file)
                }
            }
        }
        log.exiting("Autoscanner", "checkFile")

    }

    private fun exclude(file: Path): Boolean {
        if(file.fileName.startsWith(".") || Files.isHidden(file) || (Files.size(file)==0L)){
            return true
        }else{
            return false;
        }

    }

    /**
     * Add an Item. If it is a directory, add it to the watch list. If it is a file, add it to the index
     */
    fun addFile(file: Path) {
        if(!exclude(file)) {
            val filename = file.toFile().absolutePath
            log.info("adding ${filename}")
            if (Files.isDirectory(file, NOFOLLOW_LINKS)) {
                val key = file.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                keys.put(key, file)
            } else {
                val fileMetadata = refiner.preProcess(filename, JsonObject()).put("_id", makeID(file))
                vertx.executeBlocking<Int>(FileImporter(file, fileMetadata), object : Handler<AsyncResult<Int>> {
                    override fun handle(result: AsyncResult<Int>) {
                        if (result.failed()) {
                            val errmsg = "import ${file.toAbsolutePath()} failed." + result.cause().message
                            log.severe(errmsg)
                            vertx.eventBus().publish(Communicator.ADDR_ERROR, JsonObject().put("status", "error").put("message", errmsg))
                        }
                    }
                })
            }
        }
    }

    fun removeFile(file: Path) {
        if(!exclude(file)) {
            val absolute = file.toFile().absolutePath
            log.info("removing ${absolute}")
            val id = makeID(file)
            indexManager.removeDocument(id)
        }
    }


    fun setRefiner(ref: Refiner) {
        refiner = ref
    }

    @SuppressWarnings("unchecked")
    internal fun <T> cast(event: WatchEvent<*>): WatchEvent<T> {
        return event as WatchEvent<T>
    }

    companion object {
        const val ADDR_START = "start"
        const val ADDR_STOP = "stop"
        /** Full rescan or first scan of watch directories */
        const val ADDR_RESCAN = "rescan"
        var refiner: Refiner = DefaultRefiner()
        val log = Logger.getLogger("lucinda.autoscanner")
        val watchedDirs = ArrayList<Path>()

        fun makeID(file: Path): String = makeHash(file.toFile().absolutePath)

    }
}


