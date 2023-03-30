package detector.distillerTest;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;

import java.io.IOException;
import java.io.Writer;

public class fuzzySearcherTest {
    private static void WriteIndex() throws IOException {
        ByteBuffersDirectory directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter iw = new IndexWriter(directory, config);

        Document doc = new Document();
        doc.add(new StringField("file", "aaaab", Field.Store.YES));
        iw.addDocument(doc);
        iw.commit();

        IndexReader ir = DirectoryReader.open(directory);
        iw.close();

        IndexSearcher searcher = new IndexSearcher(ir);
        Query query = new FuzzyQuery(new Term("file", "aaaaa"));
        TopDocs hits = searcher.search(query, 2);
        Document foundDoc = searcher.doc(hits.scoreDocs[0].doc);
        System.out.println(hits);
    }

    public static void main(String[] args) throws IOException {
        WriteIndex();

    }
}
