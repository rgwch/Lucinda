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
import ch.rgw.tools.TimeTool
import com.rometools.utils.Strings
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.de.GermanAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.fr.FrenchAnalyzer
import org.apache.lucene.analysis.it.ItalianAnalyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.SearcherManager
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.store.FSDirectory
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import java.io.InputStream
import java.nio.file.FileSystems
import java.util.logging.Level
import java.util.logging.Logger

/**
 * This class maintains the global index for the lucinda
 * Created by gerry on 20.03.16.
 */
class IndexManager(directory: String) {

    //val dir: Path =
    val log = Logger.getLogger("lucinda.indexManager")

    val analyzer = when (config.get("default_language", "de")) {
        "de" -> GermanAnalyzer()
        "fr" -> FrenchAnalyzer()
        "it" -> ItalianAnalyzer()
        "en" -> EnglishAnalyzer()
        else -> StandardAnalyzer()
    }
    val parser= QueryParser("text", analyzer)
    val idParser=QueryParser("_id", KeywordAnalyzer())
    val insertLock=true


    val writer: IndexWriter by lazy {
        log.info("opening index in create or append mode")
        val conf = IndexWriterConfig(analyzer).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND)
        conf.maxBufferedDeleteTerms = 1
        val index = FSDirectory.open(FileSystems.getDefault().getPath(directory))
        IndexWriter(index, conf)
    }
    val searcherManager : SearcherManager by lazy{
        SearcherManager(writer, true, true, null)
    }


    fun shutDown() {
        try {
            if (writer.isOpen) {
                writer.commit()
                writer.deleteUnusedFiles()
                writer.close()
            }
        }catch(ex: Exception){
            ex.printStackTrace()
            log.severe("could not shut down index writer properly "+ex.message)
        }
    }

    /**
     * Index a document. The Document itself is not stored. The analyzer recognizes and understands many formats such as odt, pdf, doc, html
     * @param is InputStream with the data
     * @param attributes concern of the document; will be added to the index
     * @param guid GUID of the document. When searching, this GUID will be the result
     * @param language language for the analyzer. "de", "fr", "it" and "en" are supported.
     * @return The plain text content as found by the parser
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    fun addDocument(istream: InputStream, attributes: JsonObject): Document {

        val metadata = Metadata()
        val handler = BodyContentHandler()
        val context = ParseContext()
        val parser = AutoDetectParser()

        try {
            parser.parse(istream, handler, metadata, context)
        }catch(ex: Exception){
            // e.g. org.apache.tika.sax.WriteLimitReachedException
            // In that case, Data up to the limit (100K) will be available and can be read.
            // so, just write a log entry and continue
            log.warning(ex.message)
        }

        val text = handler.toString()
        val doc = Document()

        for (k in metadata.names()) {
            val key = k.toLowerCase()
            val value = metadata.get(k)
            if (Strings.isBlank(value)) {
                continue
            }
            if (key == "keywords") {
                for (keyword in value.split(",?(\\s+)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    doc.add(TextField(key, keyword, Field.Store.YES))
                }
            } else {
                doc.add(TextField(key, value, Field.Store.YES))
            }
        }
        val ftDate=FieldType()
        ftDate.setStored(true)
        ftDate.setTokenized(false)
        attributes.fieldNames().forEach {
            doc.add(when(it){
              "_id","uuid", "birthdate" -> StringField(it, attributes.getString(it), Field.Store.YES)
              else -> TextField(it, attributes.getString(it),Field.Store.YES)
            })
        }
        doc.removeFields("parseDate")
        doc.add(StringField("parseDate", TimeTool().toString(TimeTool.DATE_COMPACT),Field.Store.YES))
        doc.add(TextField("text", if(text.isEmpty()) "unparseable" else text, Field.Store.NO))
        updateDocument(doc)
        return doc
    }

    /*
    fun insertDocument(doc: Document){
        synchronized(insertLock) {

            writer.addDocument(doc)
            writer.flush()
            writer.commit()
            searcherManager.maybeRefreshBlocking()
        }
    }
*/

    fun updateDocument(doc: Document){
        require(doc.get("_id")!=null)
        synchronized(insertLock) {
            val term = Term("_id", doc.get("_id"))
            writer.updateDocument(term, doc)
            searcherManager.maybeRefreshBlocking()
        }
    }

    /**
     * Query the index for a document. The QueryParser understands a variety of formats and returns at most 'numHits' documents matching that query.

     * @param queryExpression the term(s) to find. can be plaintext ("Meier"), Wildcard ("M??er"), Regexp ("/M[ae][iy]er/"), similarity ("Meier~")
     * *                        and combinations thereof
     * @param numHits   Number of hits to return at most
     * @return A JsonArray with Metadata of the document(s). Can be empty but is never null.
     * @throws ParseException
     * @throws IOException
     */
    fun queryDocuments(queryExpression: String, numHits: Int=1000): JsonArray {
        log.level= Level.FINEST
        require(queryExpression.isNotBlank())
        log.finer("querying for ${queryExpression}")
        val query = parser.parse(queryExpression)
        searcherManager.maybeRefreshBlocking()
        val searcher=searcherManager.acquire()
        val collector = TopScoreDocCollector.create(numHits)
        searcher.search(query, collector)
        val hits = collector.topDocs()
        val score = hits.scoreDocs
        val ret = JsonArray()
        for (sd in score) {
            val hit = searcher.doc(sd.doc)
            val jo = JsonObject()
            hit.getFields().forEach { field ->
                jo.put(field.name(), field.stringValue())
            }
            ret.add(jo)
        }
        searcherManager.release(searcher)
        return ret

    }

    fun getDocument(id: String): Document? {
        require(id.isNotBlank())
        try {
            val query=idParser.parse("_id: ${id}")
            searcherManager.maybeRefreshBlocking()
            val searcher = searcherManager.acquire()
            val result = searcher.search(query,10)
            if (result.totalHits > 1) {
                log.severe("Lucene index corrupt: Duplicate _id: ${id}")
            }
            if (result.totalHits == 0) {
                return null
            }
            val ret= searcher.doc(result.scoreDocs[0].doc)
            searcherManager.release(searcher)
            return ret
        } catch(ex: Exception) {
            log.warning("could not open lucene index " + ex.message)
            return null
        }
    }

    /**
     *  Retrieve plain metadata from a document as a JsonObject (removing the dependency to lucene)
     */
    fun getMetadata(id: String): JsonObject? {
        require(id.isNotBlank())
        val doc = getDocument(id)
        if (doc == null) {
            return null
        }
        val ret = JsonObject()
        doc.fields.forEach {
            ret.put(it.name(), it.stringValue())
        }
        return ret;
    }


    fun removeDocument(id: String) {
        require(id.isNotBlank())
        val query=idParser.parse("_id: ${id}")
        writer.deleteDocuments(query)
        writer.flush()
        writer.commit()
        searcherManager.maybeRefreshBlocking()
    }


    fun escape(orig: String)=QueryParser.escape(orig)
}

class ID(val source: String){
    val asString:String by lazy{
        val chopped=source.replace("[:\\\\\\/\\.0-9_]".toRegex(),"")
        makeHash(chopped)
    }
}
