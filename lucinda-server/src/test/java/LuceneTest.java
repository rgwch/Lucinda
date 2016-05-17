import ch.rgw.io.FileTool;
import ch.rgw.lucinda.IndexManager;
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

/**
 * Created by gerry on 09.05.16.
 */
public class LuceneTest {

  static File index=new File("target/lucenetest/index");
  static String id="893f8fbfe7b6483a0fa24c97ee18bca98d431e8e";
  @Before
  public void setUp(){
    if(!index.exists())
      index.mkdirs();
  }

  @After
  public void tearDown(){
    FileTool.deltree(index.getAbsolutePath());
  }

  @Test
  public void addToIndex(){
    IndexManager indexManager=new IndexManager(index.getAbsolutePath());
    Document doc=new Document();
    doc.add(new StringField("AAA","Test Feld aaa", Field.Store.YES));
    doc.add(new TextField("BBB","Test Field bbb", Field.Store.YES));
    doc.add(new StringField("_id",id,Field.Store.YES));
    indexManager.updateDocument(doc);

    assertEquals(1,indexManager.queryDocuments("BBB: Test*",100).size());

    assertNotNull(indexManager.getDocument(id));

  }
}
