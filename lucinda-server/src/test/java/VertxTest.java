/*******************************************************************************
 * Copyright (c) 2016 by G. Weirich
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * <p>
 * Contributors:
 * G. Weirich - initial implementation
 ******************************************************************************/

import ch.rgw.io.FileTool;
import ch.rgw.lucinda.Dispatcher;
import ch.rgw.lucinda.IndexManager;
import ch.rgw.lucinda.LauncherKt;
import ch.rgw.tools.Configuration;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.lucene.index.IndexWriter;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by gerry on 25.03.16.
 */
@RunWith(VertxUnitRunner.class)
public class VertxTest {

    static Vertx vertx;
    static Configuration cfg;
    static Dispatcher dis;
    static IndexManager indexManager;


    @Before
    public void setUp(){
        cfg = new Configuration("default.cfg", "user.cfg");
        if(new File(cfg.get("fs_basedir","")).exists()) {
            FileTool.deltree(cfg.get("fs_basedir", "target/base"));
        }

        indexManager=LauncherKt.getIndexManager();
        IndexWriter writer=indexManager.getWriter();
        vertx = Vertx.vertx();
        dis = new Dispatcher(cfg, vertx);

    }


    @After
    public void tearDown() throws Exception{
        if(indexManager!=null) {
            indexManager.shutDown();
            indexManager = null;
        }
        if(vertx!=null) {
            vertx.close();
            vertx = null;
        }
    }
    @Test
    public void testFilenameResolution() {
        System.out.println(cfg.get("fs_basedir","base"));
        assertEquals("target/store", cfg.get("fs_basedir", ""));
        assertEquals("target/store/index", cfg.get("fs_indexdir", ""));
        assertEquals("target/store/inbox", cfg.get("fs_import", ""));
        assertEquals("target/store/inbox", cfg.get("fs_watch", ""));
        JsonObject parms = new JsonObject().put("filename", "test").put("concern", "direc");
        File file = dis.makeDirPath(parms);
        assertEquals("target/store/inbox/direc/test", file.getPath());

    }

    @Test @Ignore
    public void testAddAndIndex(TestContext ctx) {
        File file = new File("target/test-classes/test.pdf");
        assertTrue(file.exists() && file.canRead());
        byte[] input = FileTool.readFile(file);
        JsonObject parms = new JsonObject().put("filename", file.getName()).put("payload", input).put("concern", "testperson_armeswesen_24.03.1951").put("lang", "de");
        Async async=ctx.async();
        dis.indexAndStore(parms, handler ->{
            assertTrue(handler.succeeded());
            JsonObject query = new JsonObject().put("query", "+pdf +ipsum").put("numhits", 10);
            JsonArray qr = dis.find(query);
            assertFalse(qr.isEmpty());
            JsonObject ret = qr.getJsonObject(0);
            File out=new File(ret.getString("url"));
            assertTrue(out.exists());
            byte[] check = FileTool.readFile(file);
            assertArrayEquals(check, input);
            async.complete();
        });


    }

    @Test @Ignore
    public void testIndexing(TestContext ctx) throws IOException {
        File file = new File("target/test-classes/test.odt");
        assertTrue(file.exists() && file.canRead());
        byte[] input = FileTool.readFile(file);
        String id=ch.rgw.crypt.UtilKt.makeHash(file.getAbsolutePath());
        JsonObject parms = new JsonObject().put("url", file.getAbsolutePath()).put("payload", input).put("concern", "testperson_armeswesen_24.03.1951").put("_id", id);
        Async async=ctx.async();
        dis.addToIndex(parms, handler -> {
            assertTrue(handler.succeeded());
            JsonObject query = new JsonObject().put("query", "ipsum").put("numhits", 10);
            JsonArray result = dis.find(query);
            assertFalse(result.isEmpty());
            JsonObject ret = result.getJsonObject(0);
            assertEquals(file.getAbsolutePath(), ret.getString("url"));
            async.complete();
        });
    }

}
