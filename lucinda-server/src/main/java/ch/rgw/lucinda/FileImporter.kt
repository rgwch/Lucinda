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

import ch.rgw.crypt.makeHash
import ch.rgw.io.FileTool
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * This is a Handler<Future> which is called from the indexing process to import a new file.
 * Created by gerry on 05.05.16.
 */

class FileImporter(val file: Path, val fileMetadata: JsonObject) : Handler<Future<Int>> {
    val temppath: String by lazy {
        val checkDir = File(config.get("fs_basedir", "target/store"), "tempfiles")
        if (checkDir.exists()) {
            if (checkDir.isFile) {
                log.severe("temporary directory exists but is a file")
            }
        } else {
            if (!checkDir.mkdirs()) {
                log.severe("Can't create tempdir")
            }
        }
        checkDir.absolutePath
    }
    val failures: String by lazy {
        val checkDir = File(config.get("fs_basedir", System.getenv("java.io.tmpdir")), "failures")
        if (checkDir.exists()) {
            if (checkDir.isFile) {
                log.severe("failure directory exists but is a file")
            }
        } else {
            if (!checkDir.mkdirs()) {
                log.severe("Can't create failure directory")
            }
        }
        checkDir.absolutePath
    }


    /*
     * Handle a file to import. Sometimes, a file can't be parsed, because it was not yet fully written at the
     * time, this method is called. So We'll retry in such cases after a while
     */
    override fun handle(future: Future<Int>) {
        var retryCount = 0
        var errmsg = ""
        while (retryCount < 2) {
            log.fine("handle: ${file.fileName}")
            errmsg = process();
            if (errmsg.isEmpty()) {
                future.complete()
                return
            } else {
                log.info("retrying ${file.toAbsolutePath()}")
                try {
                    Thread.sleep(2000L)
                } catch(ex: InterruptedException) {
                    /* never mind */
                }
                retryCount++
            }
        }
        log.warning("${file.toAbsolutePath()} failed.")
        future.fail("Failed: ${file.toFile().absolutePath}; ${errmsg}")
    }

    fun process(): String {
        val filename = file.toFile().absolutePath
        try {
            val payload = if (fileMetadata.containsKey("payload")) {
                fileMetadata.getBinary("payload")
            } else {
                val plc = FileTool.readFileWithChecksum(File(fileMetadata.getString("url")))
                fileMetadata.put("uuid", plc.get("checksumString"))
                plc.get("contents") as ByteArray
            }

            log.info("FileImporter: Importing ${filename}")
            fileMetadata.remove("payload")
            if (fileMetadata.getString("_id").isNullOrBlank()) {
                fileMetadata.put("_id", Autoscanner.makeID(file))
            }
            val doc = indexManager.addDocument(ByteArrayInputStream(payload), fileMetadata);
            val text = doc.getField("text").stringValue()
            if ( (text.length < 15) and (doc.get("content-type").equals("application/pdf"))) {
                if (text == "unparseable") {
                    log.info("unparseable text")
                    return "";
                } else {
                    var failed = false;
                    // if we don't get much text out of a pdf, it's probably a scan containing only one or more images.
                    val basename = temppath + "/" + makeHash(filename)
                    log.info("Seems to be a PDF with only image(s). Trying OCR as ${basename}")
                    try {
                        // This will throw an exception if the pdf is invalid.
                        val document = PDDocument.load(filename);
                        val list = document.getDocumentCatalog().getAllPages();
                        var numImages = 0;
                        list.forEach { page ->
                            val pdResources = (page as? PDPage)?.getResources();
                            val pageImages = pdResources?.xObjects
                            val imageIter = pageImages?.keys?.iterator();
                            imageIter?.forEach {
                                val pdxObjectImage = pageImages?.get(it);
                                if (pdxObjectImage is PDXObjectImage) {
                                    val imgName = basename + "_" + (++numImages).toString()
                                    pdxObjectImage.write2file(imgName);
                                    val sourcename = imgName + "." + pdxObjectImage.suffix
                                    val result = doOCR(sourcename, imgName)
                                    FileTool.deleteFile(sourcename)
                                    if (result) {
                                        val plaintext = File(imgName + ".txt")
                                        if (plaintext.exists() and plaintext.canRead() and (plaintext.length() > 10L)) {
                                            val newtext = FileTool.readTextFile(plaintext)
                                            doc.add(TextField("text", newtext, Field.Store.NO))
                                            indexManager.updateDocument(doc)
                                            plaintext.delete()
                                        } else {
                                            log.warning("no text content found in ${filename}")
                                        }
                                    } else {
                                        failed = true
                                    }
                                }
                            }
                        }
                        document.close()
                        if (failed) {
                            FileTool.copyFile(file.toFile(), File(failures, file.fileName.toString()), FileTool.BACKUP_IF_EXISTS)
                            return ("import failed.")
                        } else {
                            return "";
                        }

                    } catch(ex: Exception) {
                        ex.printStackTrace()
                        log.severe("Fatal error in pdf file ${filename}: ${ex.message}")
                        return ("Exception thrown: ${ex.message}")
                    }
                }
            } else {
                // no pdf or text too short
                return ""
            }


        } catch(ex: Exception) {
            ex.printStackTrace()
            log.severe("fatal error reading ${filename}")
            return ("read error ${ex.message}")
        }
    }

    /**
     * Try to OCR an image document. If tesseract doesn't return after TIMEOUT seconds,
     * stopit
     */

    val TIMEOUT = 300L;

    fun doOCR(source: String, dest: String): Boolean {
        val process = Runtime.getRuntime().exec(listOf("tesseract", source, dest).toTypedArray())
        if (process == null) {
            log.severe("Could not launch tesseract!")
            throw IllegalArgumentException("tesseract setting wrong")
        }
        val errmsg = StringBuilder()
        val err = StreamBuffer(process.errorStream, errmsg);
        val output = StringBuilder()
        val out = StreamBuffer(process.inputStream, output);
        err.start()
        out.start()
        if (process.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
            val exitValue = process.exitValue()
            log.info("tesseract ${source} ended, " + output.toString())
            return exitValue == 0
        } else {
            log.severe("Tesseract process hangs")
            process.destroy()
            return false
        }
    }

    companion object {
        val log = Logger.getLogger("lucinda.fileImporter")

    }

}