package detector.mapping;

import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.ByteBuffersDirectory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class fileMapper {
//    private List<String> files;
    private HashMap<String, List<String>> files;
    IndexReader ir;

    static Pattern isJavaFile = Pattern.compile(".*\\.java");

    //recurrently search all java files
    private void searchFiles(String rootDir){
        File root = new File(rootDir);

        try (Stream<Path> paths = Files.walk(Paths.get(rootDir))) {
            List<Path> results = paths
                    .filter(f -> f.toString().endsWith(".cs"))
                    .collect(Collectors.toList());

//            result.forEach(System.out::println);

            if(files == null){
                files = new HashMap<>();
            }
            for(Path result:results){
                String name = result.getFileName().toString();
                name = name.substring(0,name.length()-3);
                name = name.toLowerCase();
                if(!files.containsKey(name)){
                    files.put(name, new ArrayList<>());
                }

                files.get(name).add(result.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteIndex() throws IOException {
        ByteBuffersDirectory directory = new ByteBuffersDirectory();
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter iw = new IndexWriter(directory, config);

        for(String name:files.keySet()){
            Document doc = new Document();
            doc.add(new StringField("file", name, Field.Store.YES));
            iw.addDocument(doc);
        }
        iw.commit();
        ir = DirectoryReader.open(directory);
        iw.close();
    }

    public fileMapper(String project) throws IOException {
        String relativelyPath=System.getProperty( "user.dir" );
        StringBuilder builder= new StringBuilder(relativelyPath);
        builder.append("\\data\\sources\\");
        builder.append(project);
        searchFiles(builder.toString());
        WriteIndex();
    }

    public List<String> singleFileMapping(String file) throws IOException {
        IndexSearcher searcher = new IndexSearcher(ir);
        Query query = new FuzzyQuery(new Term("file", file), 2);
        TopDocs hits = searcher.search(query, 3);
        List<String> foundFiles = new ArrayList<>();
        for(ScoreDoc hit:hits.scoreDocs){
            Document doc = searcher.doc(hit.doc);
//            String val = doc.getField("file").stringValue();
            String filename = doc.getField("file").stringValue();
            for(String filePath : files.get(filename)){
                foundFiles.add(filePath);
            }
        }
        return foundFiles;
    }

    public static void main(String[] args) throws IOException {
        fileMapper test_map =new fileMapper("Lucene.Net_4_8_0");
        System.out.println(test_map.files.size());
        Set<String> names = test_map.files.keySet();
        List<String> search_name = test_map.singleFileMapping("indexwriter");
        for(String name:search_name){
            System.out.println(name);
        }
    }
}
