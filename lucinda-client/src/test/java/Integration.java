import ch.rgw.lucinda.Client;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by gerry on 11.05.16.
 */
@RunWith(VertxUnitRunner.class)
public class Integration {

    @Test
    public void testLucinda(TestContext ctx) {
        Client client = new Client();
        Async async1 = ctx.async();
        client.connect(null, "192.168.16.*", result -> {
            String retval = (String) result.get("status");
            assertEquals("connected", retval);
            Async async2 = ctx.async();
            async1.complete();
            client.query("meier", queryResult -> {
                assertEquals("ok", queryResult.get("status"));
                assertNotNull(queryResult.get("result"));
                ArrayList<Map<String, Object>> values = (ArrayList<Map<String, Object>>) queryResult.get("result");
                assertTrue(values.size() > 0);
                Map<String, Object> first = values.get(0);
                Async async3 = ctx.async();
                async2.complete();
                InputStream is = this.getClass().getResourceAsStream("Integration.class");
                Map<String,Object> meta=new HashMap();
                meta.put("one","two");
                meta.put("three","four");

                client.addFile("ThePom.xml","integration-test","My doctype", meta, readFile(is), result2 -> {
                    assertEquals("ok",result2.get("status"));
                    client.query("one:two", qr2 -> {
                        assertNotNull(queryResult.get("result"));
                        ArrayList<Map<String, Object>> values2 = (ArrayList<Map<String, Object>>) queryResult.get("result");
                        assertTrue(values2.size() > 0);
                        async3.complete();
                    });
                });
            });
        });
    }

    private byte[] readFile(InputStream is){
        byte[] daten = null;
        try {
            daten = new byte[is.available()];
            is.read(daten);
        }catch(Exception ex){
        }
        return daten;
    }
}
