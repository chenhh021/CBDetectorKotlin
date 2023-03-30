package detector;

import detector.changes.changeHandler;
import detector.mapping.fileMapper;

import java.io.File;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.*;

import static detector.changes.projectChangeHandler.getProjectChanges;

public class main {
    static String out_dir;

    public static void main(String args[]) throws IOException {
        if(out_dir == null){
            String relativelyPath=System.getProperty( "user.dir" );
            StringBuilder builder= new StringBuilder(relativelyPath);
            builder.append("\\out\\");
            out_dir = builder.toString();
        }

        // set target list
//        List<String> oriProjectsList = new ArrayList<>();
//        oriProjectsList.add("TEST");
//        List<String> desProjectsList = new ArrayList<>();
//        desProjectsList.add("Lucene.Net_4_8_0");
        Set<String> files = new HashSet<>();
        HashMap<String, List<String>> mapped_files = new HashMap<>(); //文件间映射：java->c#
        HashMap<String, List<String>> repeat_files = new HashMap<>(); //临时统计：在c#项目中能找到的补丁相关重复文件
        int candidates_num = 0; //比较的java补丁数量
        int target_num = 0; //有c#重复文件的补丁数量
        OutputStreamWriter ows;


        String path = out_dir + "lucene_test.txt";
        File out_file = new File(path);
        out_file.createNewFile();
        FileOutputStream fileStream = new FileOutputStream(out_file);
        ows = new OutputStreamWriter(fileStream);
        ows.write("Mapped buggy files in Lucene.Net\n");

        //Analyse patches of a single project
//        String ori = oriProjectsList.get(i), des = desProjectsList.get(i);
        String ori = "LUCENE", des = "Lucene.Net_4_8_0";
        HashMap<String, String> fileMappings;

        List<changeHandler> patches = getProjectChanges(ori);
        fileMapper mapper = new fileMapper(des);
        candidates_num = patches.size();

        for(changeHandler patch:patches){
            files.addAll(patch.fileMethods.keySet());
        }

        for(String file:files){
            mapped_files.put(file, mapper.singleFileMapping(file));
        }

        //临时输出

        for(changeHandler patch:patches){
            List<String> desFiles = new ArrayList<>();
            for(String file:patch.fileMethods.keySet()){
                desFiles.addAll(mapper.singleFileMapping(file));
            }
            repeat_files.put(patch.bug, desFiles);
            if(!desFiles.isEmpty()){
                ++target_num;
            }
        }

        for(String bugName:repeat_files.keySet()){
            List<String> desFiles = repeat_files.get(bugName);
            if(desFiles.isEmpty()){
                continue;
            }
            ows.write(bugName);
            ows.write("\n");
            for(String mappedFile:desFiles){
                ows.write(mappedFile);
                ows.write("\n");
            }
            ows.write("\n");
        }
        ows.close();

        System.out.println("Found targets:"+target_num+"\n");
    }
}
