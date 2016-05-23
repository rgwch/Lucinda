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
import net.didion.jwnl.data.Exc;
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
    public void testAddFile(TestContext ctx) throws Exception {
        File file = new File("target/test-classes/test.odt");
        //System.out.print(file.getAbsolutePath());
        byte[] cnt= FileTool.readFile(file);
        Async async=ctx.async();
        HttpClientRequest hcr=http.post("/api/1.0/addfile", response -> {
            Assert.assertEquals(201,response.statusCode());
            retrieve(ctx);
            Async async2=ctx.async();
            http.getNow("/api/1.0/get/1234567890", rsp -> {
                rsp.bodyHandler(buffer ->{
                    byte[] check=buffer.getBytes();
                    Assert.assertArrayEquals(check,cnt);
                    async2.complete();
                });
            });
            async.complete();
        });
        JsonObject jo=new JsonObject()
                .put("_id","1234567890")
                .put("title","odttest")
                .put("filename","test.odt")
                .put("payload",cnt);
        hcr.putHeader("content-type","application/json; charset=utf-8");
        hcr.end(Json.encode(jo));
    }

    @Test
    public void testIndexFile(TestContext ctx) throws Exception{
        File file = new File("target/test-classes/test.pdf");
        byte[] cnt= FileTool.readFile(file);
        Async async=ctx.async();
        HttpClientRequest hcr=http.post("/api/1.0/index", response -> {
            Assert.assertEquals(200,response.statusCode());
            retrieve(ctx);
            async.complete();
        });
        JsonObject jo=new JsonObject()
            .put("_id","1234567890")
            .put("title","pdftest")
            .put("filename","test.pdf")
            .put("payload",cnt);
        hcr.putHeader("content-type","application/json; charset=utf-8");
        hcr.end(Json.encode(jo));
    }

    @Test
    public void testUpdate(TestContext ctx){
        byte[] cnt={1,2,3,4,5,6,7,8,9,10};
        JsonObject jo=new JsonObject()
            .put("payload",cnt)
            .put("_id","1234")
            .put("foo","bar");
        Async async=ctx.async();
        http.post("/api/1.0/index", response -> {
            Assert.assertEquals(200,response.statusCode());
            Async async2=ctx.async();
            async.complete();
            jo.put("foo","baz").put("hello","world");
            http.post("/api/1.0/update", rsp -> {
                Assert.assertEquals(202,rsp.statusCode());
                Async async3=ctx.async();
                async2.complete();
                http.post("/api/1.0/query", retr -> {
                    Assert.assertEquals(200,retr.statusCode());
                    rsp.bodyHandler(buffer ->{
                        JsonArray found=buffer.toJsonArray();
                        Assert.assertEquals(1,found.size());
                        JsonObject hit=found.getJsonObject(0);
                        Assert.assertEquals("baz",hit.getString("foo"));
                        async3.complete();
                    });
                }).putHeader("content-type","text/plain").end("hello:world");
            }).putHeader("content-type","application/json").end(jo.encode());
        }).putHeader("content-type","application/json; charset=utf-8").end(jo.encode());
    }

    private void retrieve(TestContext ctx){
        Async async=ctx.async();
        http.post("/api/1.0/query", response -> {
            response.bodyHandler(buffer -> {
               JsonArray results=new JsonArray(buffer.toString());
                Assert.assertEquals(1,results.size() );
                async.complete();
            });
        }).putHeader("content-type","text/plain; charset=utf-8")
            .end("lorem");

    }
}
