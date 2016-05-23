
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

import ch.rgw.lucinda.IndexManager;
import ch.rgw.lucinda.LauncherKt;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by gerry on 20.03.16.
 */

public class TestParse {

    IndexManager indexManager = LauncherKt.getIndexManager();

    @AfterClass
    static public void delete() throws IOException {

    }

    @Test @Ignore
    public void testParse() throws Exception {
        File file = new File("target/test-classes/test.odt");
        //System.out.print(file.getAbsolutePath());
        FileInputStream fis = new FileInputStream(file);
        indexManager.addDocument(fis, new JsonObject().put("uri", file.getAbsolutePath()).put("_id","1"));
        fis.close();

        file = new File("target/test-classes/test.pdf");
        fis = new FileInputStream(file);
        indexManager.addDocument(fis, new JsonObject().put("uri", file.getAbsolutePath()).put("_id","2"));
        fis.close();
        JsonArray res = indexManager.queryDocuments("lorem", 10);
       /*
        for (Object doc : res) {
            System.out.print(Json.encodePrettily(doc));
        }
        */
    }
}
