package detector;

import detector.changes.changeHandler;
import detector.mapping.fileMapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.Date;

import java.text.SimpleDateFormat;


import static detector.changes.projectChangeHandler.getBugNames;
import static detector.changes.projectChangeHandler.getProjectChanges;

public class main {
    static Logger logger = LogManager.getLogger(main.class.getName());

    public static void main(String args[]) throws IOException {
        logger.info("detection start");
        //初始化基础参数
        String relativelyPath=System.getProperty( "user.dir" );
        StringBuilder builder= new StringBuilder(relativelyPath);
        builder.append("\\output\\");
        //输出路径
        String out_dir = builder.toString();
        //源项目：出补丁的java项目
        String ori = "LUCENE";
        //目标项目：出源代码的c#项目
        String des = "Lucene.Net_4_8_0";


        // set target list
//        List<String> oriProjectsList = new ArrayList<>();
//        oriProjectsList.add("TEST");
//        List<String> desProjectsList = new ArrayList<>();
//        desProjectsList.add("Lucene.Net_4_8_0");

        //中间变量初始化
        Set<String> files = new HashSet<>(); //涉及变化的文件列表
        HashMap<String, List<String>> mapped_files = new HashMap<>(); //文件间映射：java->c#
        HashMap<String, HashSet<String>> bug_repeat_files = new HashMap<>(); //临时统计：在c#项目中能找到的补丁相关重复文件
        int candidates_num = 0; //比较的java补丁数量
        int target_num = 0; //有c#重复文件的补丁数量
        OutputStreamWriter ows; //输出流
        //当前时间戳
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedDate = sdf.format(new Date(timestamp));


        //输出文件命名,用时间戳保证唯一性
        String path = out_dir +ori+"_"+formattedDate+".txt";
        File out_file = new File(path);
        out_file.createNewFile();
        FileOutputStream fileStream = new FileOutputStream(out_file, true);
        ows = new OutputStreamWriter(fileStream);
        ows.write("Mapped buggy files in Lucene.Net\n");

        //Analyse patches of a single project
//        String ori = oriProjectsList.get(i), des = desProjectsList.get(i);
        HashMap<String, String> fileMappings;

        //对项目中每个bug进行分析
        List<String> bugNames = getBugNames(ori);
        fileMapper mapper = new fileMapper(des);
        candidates_num = bugNames.size();


        for(String bugName:bugNames) {
            logger.info("start analyse for bug:"+bugName);
            //根据补丁抽取差异
            logger.info("distill changes");
            changeHandler patch = new changeHandler(ori, bugName);

            //读取变更文件
            files.addAll(patch.fileMethods.keySet());
            logger.info("changes distilling complete");

            //寻找所有变更文件的映射
            logger.info("search mapped files");
            for (String file : files) {
                if(!mapped_files.containsKey(file)) {
                    mapped_files.put(file, mapper.singleFileMapping(file));
                }
            }

            //记录当前补丁变更文件的映射
            HashSet<String> desFiles = new HashSet<>();
            for(String file:patch.fileMethods.keySet()){
                desFiles.addAll(mapped_files.get(file));
//                desFiles.addAll(mapper.singleFileMapping(file));
            }
            bug_repeat_files.put(patch.bug, desFiles);
            logger.info("search mapped files complete");

            //划分非测试代码和测试代码
            List<String> srcFiles = new ArrayList<>();
            List<String> testFiles = new ArrayList<>();
            for(String fileName:desFiles) {
                String[] fullPath = fileName.split("\\.");

                //根据是否为test进行划分
                if (detector.utils.stringUtils.hasTest(fullPath)) {
                    testFiles.add(fileName);
                }else{
                    srcFiles.add(fileName);
                }
            }

            //临时输出
            if(desFiles.isEmpty() || srcFiles.isEmpty() || testFiles.isEmpty() || desFiles.size() > 9){
                continue;
            }

            ++target_num;

            ows.write(bugName);
            ows.write("\n");
            for(String mappedFile:desFiles){
                ows.write(mappedFile);
                ows.write("\n");
            }
            ows.write("\n");
            ows.flush();
        }

        ows.write("Found targets:"+target_num+" from "+candidates_num+" bugs\n");
        ows.close();

        System.out.println("Found targets:"+target_num+" from "+candidates_num+" bugs\n");
    }
}
