import ch.rgw.io.FileTool;
import ch.rgw.lucinda.Restpoint;
import ch.rgw.tools.Configuration;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;


/**
 * Created by gerry on 22.05.16.
 */
@RunWith(VertxUnitRunner.class)
public class RestTest {
    Vertx vertx;
    HttpClient http;


    @Before
    public void setUp(TestContext ctx) {
        vertx = Vertx.vertx();
        HttpClientOptions ho=new HttpClientOptions().setDefaultHost("127.0.0.1").setDefaultPort(2016);
        http=vertx.createHttpClient(ho);
        Configuration cfg = new Configuration();
        vertx.deployVerticle(new Restpoint(cfg), new DeploymentOptions(), ctx.asyncAssertSuccess());
    }

    @After
    public void tearDown(TestContext ctx) {
        http.close();
        vertx.close(ctx.asyncAssertSuccess());
    }

    @Test
    public void testPing(TestContext ctx) {
        Async async=ctx.async();
        http.getNow("/api/1.0/ping", response -> {
            response.bodyHandler(buffer -> {
                String result=buffer.toString();
                Assert.assertEquals("pong",result);
                async.complete();
            });
        });

    }

    @Test
    public void testParse(TestContext ctx) throws Exception {
        File file = new File("target/test-classes/test.odt");
        //System.out.print(file.getAbsolutePath());
        byte[] cnt= FileTool.readFile(file);
        Async async=ctx.async();
        HttpClientRequest hcr=http.post("/api/1.0/addfile", response -> {
            async.complete();
        });
        JsonObject jo=new JsonObject()
                .put("_id","1234567890")
                .put("title","odttest")
                .put("payload",cnt);
        hcr.putHeader("content-type","application/json; charset=utf-8");
        hcr.end(Json.encode(jo));

      /*
        file = new File("target/test-classes/test.pdf");
        fis = new FileInputStream(file);
        indexManager.addDocument(fis, new JsonObject().put("uri", file.getAbsolutePath()).put("_id","2"));
        fis.close();
        JsonArray res = indexManager.queryDocuments("lorem", 10);
        */
       /*
        for (Object doc : res) {
            System.out.print(Json.encodePrettily(doc));
        }
        */
    }

}
