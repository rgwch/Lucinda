import ch.rgw.lucinda.*;
import ch.rgw.io.FileTool;
import ch.rgw.tools.Configuration;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Created by gerry on 28.03.16.
 */
@RunWith(VertxUnitRunner.class)
public class IntegrationTest {
    final static String BASEDIR="target/store";
    final static String INBOX="target/store/inbox";
    Vertx vertx;
    Autoscanner autoScanner=new Autoscanner();
    Refiner origRefiner=new DefaultRefiner();

    @BeforeClass
    public static void cleanup(){
        FileTool.deltree(BASEDIR);
    }
    //Metastore metaStore;
    @Test @Ignore
    public void testIntegration(@NotNull TestContext ctx) {
        LauncherKt.getConfig().merge(new Configuration("default.cfg","user.cfg"));

        Async async1 = ctx.async();
        final Async asyncmsg=ctx.async();
        Vertx.clusteredVertx(new VertxOptions().setClustered(true), result -> {
            if (result.succeeded()) {
                vertx = result.result();
                autoScanner.setRefiner(new Refiner(){

                    @NotNull
                    @Override
                    public JsonObject preProcess(@NotNull String url, JsonObject meta) {
                        System.out.println("Refiner: got file "+url);
                        asyncmsg.complete();
                        return origRefiner.preProcess(url,meta);
                    }

                    @NotNull
                    @Override
                    public JsonObject postProcess(@NotNull String text, @NotNull JsonObject metadata) {
                        return null;
                    }
                });
                Async async2=ctx.async();
                vertx.deployVerticle(autoScanner, new DeploymentOptions().setWorker(true), launch -> {
                    if(launch.succeeded()){

                        Async async3=ctx.async();
                        JsonArray dirs=new JsonArray();
                        dirs.add(INBOX);

                        vertx.eventBus().send(LauncherKt.getBaseaddr()+Autoscanner.ADDR_START,new JsonObject().put("dirs",dirs), msg ->{
                            if(msg.succeeded()){
                                check_autoscan(ctx);
                            }else{
                                fail("could not start Autoscanner");
                            }
                            async3.complete();
                        });

                    }else{
                        fail("could not deploy AutoScanner");
                    }
                    async2.complete();
                });

                level2(ctx);
                async1.complete();
            } else {
                fail("could not launch Vertx cluster");
            }
        });
    }

    void level2(TestContext ctx) {
    }

    void level3(TestContext ctx) {
        final JsonObject qbe = new JsonObject().put("query", "dolore").put("numhits", 10);
        Async async = ctx.async();
        vertx.eventBus().send(LauncherKt.getBaseaddr()+".find", qbe, retrieve -> {
            if (retrieve.succeeded()) {
                JsonObject res = (JsonObject) retrieve.result().body();
                assertEquals(1,res.getJsonArray("result").size());
                File file2 = new File("target/test-classes/test.odt");
                byte[] input2 = FileTool.readFile(file2);
                JsonObject parms2 = new JsonObject().put("source", file2.getAbsolutePath())
                        .put("payload", input2).put("concern", "testperson_armeswesen_24.03.1951").put("lang", "de")
                        .put("filename", file2.getName());
                Async async2=ctx.async();
                vertx.eventBus().send(LauncherKt.getBaseaddr()+".import", parms2, i2result -> {
                    if (i2result.succeeded()) {
                        Async async6 = ctx.async();
                        vertx.eventBus().send(LauncherKt.getBaseaddr()+".find", qbe, retr2 -> {
                            if (retr2.succeeded()) {
                                JsonObject endresult = (JsonObject) retr2.result().body();
                                JsonArray found = endresult.getJsonArray("result");
                                //System.out.println(Json.encodePrettily(found));
                                assertEquals(2,found.size());
                                check_get(ctx,found.getJsonObject(0));
                            } else {
                                fail("retrieve 2 failed");
                            }
                            async6.complete();
                        });

                    } else {
                        fail("insert 2 failed");
                    }
                    async2.complete();
                });

            }else{
                fail("retrieve failed");
            }
            async.complete();
        });
    }

    void check_autoscan(TestContext ctx){
        Async async=ctx.async();
        try {
            FileTool.writeRandomFile(new File(INBOX, "random.file"), 500L);
        }catch(Exception ex){
            fail("could not write File for Autoscan: "+INBOX+"/random.file");
        }
        async.complete();
    }

    void check_get(TestContext ctx, JsonObject obj){
        Async async=ctx.async();
        final long launch=System.currentTimeMillis();
        vertx.eventBus().send(LauncherKt.getBaseaddr()+".get",new JsonObject().put("_id", obj.getString("_id")), got ->{
            assertTrue(got.succeeded());
            JsonObject result=(JsonObject)got.result().body();
            byte[] contents=result.getBinary("result");
            assertNotNull(contents);
            long time=System.currentTimeMillis()-launch;
            //System.out.println("time to retrieve: "+time+" millis");
            async.complete();
        });
    }

    void checkPing(TestContext ctx){
        Async async=ctx.async();
        vertx.eventBus().send(LauncherKt.getBaseaddr()+".ping",new JsonObject(),pong -> {
            assertTrue(pong.succeeded());
            async.complete();
        });
    }
}
