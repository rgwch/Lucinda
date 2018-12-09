import ch.rgw.lucinda.*;
import ch.rgw.crypt.UtilKt;
import ch.rgw.io.FileTool;
import io.vertx.core.json.JsonObject;
import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by gerry on 27.04.16.
 */

/*

public class RefinerTest {

    @Test @Ignore
    public void testRefiner() throws Exception {
        testFile("target/store/meier_hans_21.04.1970/testrnd.file");
        testFile("target/store/meier_hans_21.4.1970/testrnd.file");
        testFile("target/store/meier_hans_21.4.70/testrnd.file");
    }


    void testFile(String testfile) throws Exception{
        Refiner refiner=new DefaultRefiner();
        byte[] arr=UtilKt.randomArray(1000);
        File file=new File(testfile);
        file.getParentFile().mkdirs();
        FileTool.writeFile(file, arr);
        JsonObject result=refiner.preProcess(testfile, new JsonObject());
        assertArrayEquals(arr,result.getBinary("payload"));
        assertEquals("meier",result.getString("lastname"));
        assertEquals("hans",result.getString("firstname"));
        assertEquals("19700421",result.getString("birthdate"));
        file.delete();
        file.getParentFile().delete();
    }


}
*/