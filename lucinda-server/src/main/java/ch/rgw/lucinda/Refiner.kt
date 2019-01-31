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

import ch.rgw.tools.crypt.makeHash
import ch.rgw.io.FileTool
import ch.rgw.tools.BinConverter
import ch.rgw.tools.TimeTool
import io.vertx.core.json.JsonObject
import org.apache.pdfbox.pdfparser.PDFParser
import org.apache.pdfbox.pdfparser.PDFStreamParser
import java.io.ByteArrayInputStream
import java.io.File

/**
 * Created by gerry on 26.04.16.
 */
interface Refiner{
    fun preProcess(url: String, metadata: JsonObject) : JsonObject
    fun postProcess(text: String, metadata: JsonObject) : JsonObject
}
class DefaultRefiner : Refiner {

    override fun preProcess(url: String, metadata:JsonObject): JsonObject {
        metadata.put("url","${url}")
        val file=File(url)
        if(file.exists() && file.canRead()){
            val contents= FileTool.readFileWithChecksum(file)
            metadata.put("payload",contents["contents"])
            metadata.put("uuid",contents["checksumString"])
            metadata.put("title",FileTool.getNakedFilename(url))
            metadata.put("lucinda_doctype","Inbox")
            val pathname=file.parentFile.name
            val parts=pathname.split("_".toRegex())
            if(parts.size==3){
                metadata.put("concern",pathname)
                metadata.put("lastname",parts[0])
                metadata.put("firstname",parts[1])
                val bdate= TimeTool(parts[2])
                metadata.put("birthdate",bdate.toString(TimeTool.DATE_COMPACT))
            }

        }else{
            throw Exception("${url} not found or not readable")
        }
        return metadata
    }

    override fun postProcess(text: String, metadata: JsonObject) : JsonObject{
        val ret=metadata
        return ret
    }
}

class NoopRefiner : Refiner{
    override fun postProcess(text: String, metadata: JsonObject): JsonObject {
        return metadata
    }

    override fun preProcess(url: String, metadata: JsonObject): JsonObject {
        return JsonObject()
    }

}