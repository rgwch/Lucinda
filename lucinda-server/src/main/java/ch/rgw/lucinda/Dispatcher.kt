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
 *********************************************************************************/

package ch.rgw.lucinda

import ch.rgw.crypt.makeHash
import ch.rgw.io.FileTool
import ch.rgw.tools.Configuration
import ch.rgw.tools.StringTool
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.TextField
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.Logger

/**
 * Created by gerry on 22.03.16.
 */
class Dispatcher(val cfg: Configuration, val vertx: Vertx) {
    val log = Logger.getLogger("lucinda")
    val fs = vertx.fileSystem()

    fun makeDirPath(parms: JsonObject): File {
        val fname = parms.getString("filename")
        val concern = parms.getString("concern")
        val key = parms.getBinary("key")
        val dir = cfg.get("fs_import", "target/store") + (if(concern!=null) {File.separator + concern} else "")
        return File(dir, fname)
    }


    /**
     * Add data to the index. This method does not save the file, but only index it. Many file types such as OpenDocument, PDF, Word,
     * Plaintext, HTML and so on are supported.
     * Note: The data itself are <em>not</em> stored. This method only creates an index and metadata. The caller is responsible to store
     * the file contents elsewhere, and to be able to find it back from the metadata supplied.
     * @param parm a JsonObject with the following properties:<ul>
     *     <li>lang - a 2 letter String describing the language to use for the indexer (recognizing of stop words, word stems and so on). This
     *     time, "de", "en", "fr", and "it" are supported. If another String is given, a default Analyzer will be used.
     *     <li>payload - a byte array with the contents of the document. This well be removed after indexing</li>
     *     <li>zero or more key-value pairs as metadata of the object to add</li>
     *     </ul>
     *
     */

    fun addToIndex(parm: JsonObject, handler: Handler<AsyncResult<Int>>) {
        val payload = parm.getBinary("payload")
        requireNotNull(payload)
        val uuid = StringTool.byteArraytoHex(ch.rgw.crypt.makeHash(payload))
        parm.put("uuid", uuid)
        val temp=File.createTempFile("__lucinda__","_addToIndex_")
        temp.deleteOnExit()
        FileTool.writeFile(temp,payload)
        vertx.executeBlocking<Int>(FileImporter(Paths.get(temp.absolutePath), parm), object: Handler<AsyncResult<Int>>{
            override fun handle(result: AsyncResult<Int>) {
                temp.delete()
                handler.handle(result);
            }

        })
    }


    /**
     * Store a ByteArray as a file in the file system and index it
     * @param parms: A JsonObject with following properties: <ul>
     *     <li>concern - a grouping parameter for the storage of the file. Is used as directory name</li>
     *     <li>filename - a name for the file</li>
     *     <li>payload - The byte[] to write in the file</li>
     *     <li>key - if a key is supplied, the file is encrypted with that key</li>
     *      </ul>

     */
    fun indexAndStore(parms: JsonObject, handler: Handler<AsyncResult<Int>>)  {
        requireNotNull(parms.getBinary("payload"))
        requireNotNull(parms.getString("filename"))
        val output = makeDirPath(parms)
        val dir=output.parentFile
        if(!dir.exists()){
            dir.mkdirs();
        }
        parms.put("url", output.absolutePath)
        FileTool.writeFile(output, parms.getBinary("payload"));
        vertx.executeBlocking<Int>(FileImporter(Paths.get(parms.getString("url")), parms), handler)
    }

    /**
     * Retrieve Documents according to a query expression
     */
    fun find(parm: JsonObject): JsonArray {
        val ret =
                indexManager.queryDocuments(parm.getString("query"), parm.getInteger("numhits") ?: 100)
        return ret
    }

    fun get(id: String): ByteArray? {
        val doc: Document? = indexManager.getDocument(id)
        if (doc != null) {
            val file = File(doc.get("url"))
            if (file.exists() && file.canRead()) {
                return FileTool.readFile(file)
            } else {
                throw FileNotFoundException(file.absolutePath)
            }
        }
        return null;
    }

    fun update(o: JsonObject){
        val doc: Document? = indexManager.getDocument(o.getString("_id"))
        if(doc!=null){
            o.map.forEach {
                val f=doc.getField(it.key)
                if(f!=null){
                    doc.removeField(it.key)
                    doc.add(TextField(it.key,it.value.toString(), Field.Store.YES))
                }
            }
            indexManager.updateDocument(doc)
        }
    }
}

