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


import static detector.changes.calledAPIExtractor.*;
import static detector.changes.methodExtractor.extractMethods;
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
        //原项目：出补丁的java项目
        String ori = "LUCENE";
        //原项目的源代码目录名称
        String oriSrc = "lucene-9.3.0-src";
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
        int abnormal_num = 0; //临时，对应测试文件，抽出的函数调用数量为0的补丁数量
        int after_filtering_num = 0; //临时，筛选后仍能抽出方法调用的补丁数量
        OutputStreamWriter ows; //输出流
        //当前时间戳
        long timestamp = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String formattedDate = sdf.format(new Date(timestamp));
        //java项目的方法列表
        Map<String, Set<String>> javaMethodList = extractMethods(oriSrc);


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
            Set<String> bugFiles = patch.fileMethods.keySet();
            //加入到总集里，类似缓存，加快后续查询
            files.addAll(bugFiles);
            logger.info("changes distilling complete");

            //划分java补丁（更改前的，变更前就有的）中的非测试文件和测试文件
            List<String> srcFiles = new ArrayList<>();  //java非测试文件
            List<String> testFiles = new ArrayList<>(); //java测试文件
            for(String fileName:bugFiles) {
                String[] fullPath = fileName.split("\\.");

                //根据是否为test进行划分
                if (detector.utils.stringUtils.hasTest(fullPath)) {
                    testFiles.add(fileName);
                }else{
                    srcFiles.add(fileName);
                }
            }

            for(String fileName:patch.addedFiles){
                String[] fullPath = fileName.split("_");
                if (detector.utils.stringUtils.hasTest(fullPath)) {
                    testFiles.add(fileName); //注意这里文件名的格式不一样，例：lucene_core_src_test_org_apache_lucene_store_TestLockFactory
                }
            }


            //对补丁进行粗筛, 去掉文件太多，以及只有测试文件或没有测试文件的情况
            if(srcFiles.isEmpty() || testFiles.isEmpty() || bugFiles.size() > 9){
                continue;
            }

            //寻找所有变更文件的映射(处理前将所有文件名变为小写)
            logger.info("search mapped files");
            for (String file : bugFiles) {
                file = file.toLowerCase();
                if(!mapped_files.containsKey(file)) {
                    mapped_files.put(file, mapper.singleFileMapping(file));
                }
            }

            //记录当前补丁变更文件的映射
            HashSet<String> desFiles = new HashSet<>();
            for(String file:patch.fileMethods.keySet()){
                file = file.toLowerCase();
                desFiles.addAll(mapped_files.get(file));
//                desFiles.addAll(mapper.singleFileMapping(file));
            }
            bug_repeat_files.put(patch.bug, desFiles);
            logger.info("search mapped files complete");

            //c#项目没有找到对应文件
            if(desFiles.isEmpty()){
                continue;
            }

            ++target_num;

            //抽取test中的函数调用
            loadFiles(ori, bugName, javaMethodList);
            HashMap<String, Set<String>> test_Apis = getInvocatedMethods(patch); //所有函数调用
            HashMap<String, Set<String>> test_Apis_inner = new HashMap<>(); //调用的内部方法（如Lucene的src文件夹下的所有方法）
            boolean flag = false, flag2 = false;
            for(String name:test_Apis.keySet()){
                Set<String> invocatedMethods = test_Apis.get(name);
                if(invocatedMethods.isEmpty()){
                    flag = true;
                }

                //根据是否包含内部方法筛选
                Set<String> filteredMethods = new HashSet<>();
                for(String invocatedMethod:invocatedMethods){
                    if(javaMethodList.containsKey(invocatedMethod)){
                        Set<String> innerFiles = javaMethodList.get(invocatedMethod);
                        for(String file:innerFiles)
                        filteredMethods.add(file+":"+invocatedMethod);
                    }
                }
                test_Apis_inner.put(name, filteredMethods);

                if(!filteredMethods.isEmpty()){
                    flag2 = true;
                }
            }

            if(flag){
                ++abnormal_num;
            }

            if(flag2){
                ++after_filtering_num;
            }

            ows.write(bugName);
            ows.write("\n\n");
            ows.write("mapped files:\n");
            for(String mappedFile:desFiles){
                ows.write(mappedFile);
                ows.write("\n");
            }
            ows.write("\ntest-invocated methods under file:\n");
            for(String testFile:test_Apis.keySet()){
                ows.write(testFile);
                ows.write(":\n");
//                for(String api:test_Apis_inner.get(testFile)){ //筛选内部方法后
                for(String api:test_Apis.get(testFile)){  //未筛选
                    ows.write(api);
                    ows.write("\n");
                }
                ows.write("\n");
            }
            ows.write("\n");
            ows.flush();
        }

        ows.write("Found targets:"+target_num+" from "+candidates_num+" bugs\n");
        ows.write(abnormal_num+" fails to extract invocated methods\n");
        ows.write(after_filtering_num+" has invocated inner methods\n");
        ows.close();

        System.out.println("Found targets:"+target_num+" from "+candidates_num+" bugs\n");
    }
}
