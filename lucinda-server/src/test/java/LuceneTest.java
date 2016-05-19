import ch.rgw.io.FileTool;
import ch.rgw.lucinda.IndexManager;
import io.vertx.core.json.JsonObject;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by gerry on 09.05.16.
 */
public class LuceneTest {

  static File index=new File("target/lucenetest/index");
  static String id="893f8fbfe7b6483a0fa24c97ee18bca98d431e8e";
  IndexManager indexManager;

  @Before
  public void setUp(){
    if(!index.exists()) {
      index.mkdirs();
    }
    indexManager=new IndexManager(index.getAbsolutePath());
  }

  @After
  public void tearDown(){
    indexManager.shutDown();
    FileTool.deltree(index.getAbsolutePath());
  }

  @Test
  public void indexDirect(){
    Document doc=new Document();
    doc.add(new StringField("AAA","Test Feld aaa", Field.Store.YES));
    doc.add(new TextField("BBB","Test Field bbb", Field.Store.YES));
    doc.add(new StringField("_id",id,Field.Store.YES));
    indexManager.updateDocument(doc);

    assertEquals(1,indexManager.queryDocuments("BBB: Test*",100).size());

    assertNotNull(indexManager.getDocument(id));

  }

  @Test
  public void add() throws IOException{
    FileInputStream fis=new FileInputStream("target/test-classes/user.cfg");
    indexManager.addDocument(fis,insert());
    fis.close();
    assertEquals(1,indexManager.queryDocuments("BBB: Test*",100).size());
    assertNotNull(indexManager.getDocument(id));

  }

  @Test
  public void update() throws IOException{
    FileInputStream fis=new FileInputStream("target/test-classes/user.cfg");
    indexManager.addDocument(fis,insert());
    fis.close();
    assertEquals(1,indexManager.queryDocuments("BBB: Test*",100).size());
    Document doc=indexManager.getDocument(id);
    assertNotNull(doc);
    doc.add(new StringField("CCC", "test ccc", Field.Store.YES));
    indexManager.updateDocument(doc);
    assertEquals(1,indexManager.queryDocuments("CCC: Test*",100).size());
  }

  @Test
  public void delete()throws IOException{
    FileInputStream fis=new FileInputStream("target/test-classes/user.cfg");
    indexManager.addDocument(fis,insert());
    fis.close();
    assertEquals(1,indexManager.queryDocuments("BBB: Test*",100).size());
    Document doc=indexManager.getDocument(id);
    assertNotNull(doc);
    indexManager.removeDocument(id);
    assertEquals(0,indexManager.queryDocuments("BBB: Test*",100).size());
  }

  private JsonObject insert(){
    JsonObject jo=new JsonObject()
        .put("AAA", "Test aaa")
        .put("BBB", "Test bbb")
        .put("_id",id);
    return jo;
  }
}
