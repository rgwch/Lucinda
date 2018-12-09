import ch.rgw.io.FileTool;
import ch.rgw.lucinda.*;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Created by gerry on 16.05.16.
 */
/*
public class FileImporterTest {
    IndexManager indexManager = LauncherKt.getIndexManager();


    @Test @Ignore
    public void testImagePdfFromFile(){
        File file=new File("target/test-classes/testimg.pdf");
        JsonObject metadata= new DefaultRefiner().preProcess(file.getAbsolutePath(),new JsonObject());
        FileImporter fi=new FileImporter(Paths.get(file.getAbsolutePath()),metadata);
        Assert.assertTrue(fi.process().isEmpty());
    }

    @Test @Ignore
    public void testImagePdfFromBytes() throws IOException{
        Map<String,Object> cnt= FileTool.readFileWithChecksum(new File("target/test-classes/testimg.pdf"));
        File temp=File.createTempFile("__lucinda__","__testing");
        temp.deleteOnExit();
        FileTool.writeFile(temp,(byte[])cnt.get("contents"));
        JsonObject metadata=new JsonObject().put("_id",cnt.get("checksumString"))
                .put("uuid",cnt.get("checksumString")).put("url",temp.getAbsolutePath())
                .put("payload",cnt.get("contents"));
        FileImporter fi=new FileImporter(Paths.get(temp.getAbsolutePath()),metadata);
        Assert.assertTrue(fi.process().isEmpty());

    }
}
*/