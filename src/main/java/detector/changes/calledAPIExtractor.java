package detector.changes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import ch.uzh.ifi.seal.changedistiller.model.entities.SourceCodeChange;
import detector.utils.stringUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import static detector.changes.methodExtractor.extractMethods;

// 目标：输出更改的test中所有调用的api
public class calledAPIExtractor {
    static private String dataDir_;
    static public String project_;
    static public String bug_;

    static public Map<String, File> oriFiles_;
    static public Map<String, File> desFiles_;
    static public Map<String, Set<String>> javaMethodList_;

    public static void loadFiles(String project, String bug, Map<String, Set<String>> javaMethodList){
        project_ = project;
        bug_ = bug;
        if(oriFiles_ != null) {
            oriFiles_.clear();
        }else{
            oriFiles_ = new HashMap<>();
        }

        if(desFiles_ != null) {
            desFiles_.clear();
        }else{
            desFiles_ = new HashMap<>();
        }

        javaMethodList_ = javaMethodList;

        if(dataDir_ == null){
            String relativelyPath=System.getProperty( "user.dir" );
            StringBuilder builder= new StringBuilder(relativelyPath);
            builder.append("\\data\\patches\\");
            int dataBaseDir = builder.length();
            dataDir_ = builder.toString();
        }

        StringBuilder builder = new StringBuilder(dataDir_);
        builder.append(project);
        builder.append("\\");
        builder.append(bug);
        builder.append("\\");
        int baseDir = builder.length();
        builder.append("from\\");
        File oriDir = new File(builder.toString());
        File[] oriFileNames = oriDir.listFiles();

        for(File orifile:oriFileNames) {
            String fileName = orifile.getName();
            String[] pathInProject = fileName.split("_");
            fileName = pathInProject[pathInProject.length - 1];
            fileName = fileName.substring(0, fileName.length() - 5);
            oriFiles_.put(fileName, orifile);
        }


        builder.delete(baseDir, builder.length());
        builder.append("to\\");
        File desDir = new File(builder.toString());
        File[] desFileNames = desDir.listFiles();
        for(File desfile:desFileNames) {
            String fileName = desfile.getName();
            String[] pathInProject = fileName.split("_");
            fileName = pathInProject[pathInProject.length - 1];
            fileName = fileName.substring(0, fileName.length() - 5);
            desFiles_.put(fileName, desfile);
        }
    }
    private static CompilationUnit parseStringToCompilationUnit(String unit) {
        @SuppressWarnings("deprecation")
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit.toCharArray());
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null);
    }

    public static HashMap<String, Set<String>> getInvocatedMethods(changeHandler patch){
        HashMap<String, Set<String>> fileMethods = patch.fileMethods;
        HashMap<String, Set<String>> fileInvocatedMethods = new HashMap<>();
        fileMethods.forEach((fileName, Methods)->{
            //是测试，且在修改后的文件中存在
            if(stringUtils.hasTest(fileName.split("\\.")) && desFiles_.containsKey(fileName)){
                File file = desFiles_.get(fileName);
                try {
                    fileInvocatedMethods.put(fileName, getInvocatedMethodsOfSingleFile(file, Methods));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        for(String fileFullName: patch.addedFiles){
            String fullPath[] = fileFullName.split("_");
            if(stringUtils.hasTest(fullPath)){
                StringBuilder builder = new StringBuilder(dataDir_);
                builder.append(project_);
                builder.append("\\");
                builder.append(bug_);
                builder.append("\\to\\");
                builder.append(fileFullName);
                File file = new File(builder.toString());
                try {
                Set<String> methods = getAllMethods(file);
                String fileName = fullPath[fullPath.length-1];
                fileName = fileName.substring(0, fileName.length()-5);

                fileInvocatedMethods.put(fileName, getInvocatedMethodsOfSingleFile(file, methods));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return fileInvocatedMethods;
    }

    //获取某个文件的所有方法, 目前并不完善，默认不输入参数(输出的方法均为method(), 而不会是method(int, String)之类的)
    private static Set<String> getAllMethods(File file) throws IOException {
        String contents = FileUtils.readFileToString(file);

        final CompilationUnit astRoot = parseStringToCompilationUnit(contents);

        if(astRoot.types().isEmpty()){
            return new HashSet<>();
        }

        TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);

        //Get all methods from the class
        MethodDeclaration[] methodDeclarations = typeDecl.getMethods();

        Set<String> methods = new HashSet<>();

        for(MethodDeclaration methodDec:methodDeclarations){
            methods.add(methodDec.getName().toString()+"()");
        }

        return methods;
    }

    //从单个文件抽取调用的API， 输入为文件和对应的方法列表
    private static Set<String> getInvocatedMethodsOfSingleFile(File file, Set<String> methods) throws IOException {
        String contents = FileUtils.readFileToString(file);

        final CompilationUnit astRoot = parseStringToCompilationUnit(contents);
        TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
        List<ImportDeclaration> imports = astRoot.imports();
        List<String> importsNames = new ArrayList<>();
        for(ImportDeclaration im:imports){
            importsNames.add(im.getName().toString());
        }
        importsNames.add(astRoot.getPackage().getName().toString());

        //Get all methods from the class
        MethodDeclaration[] methodDeclarations = typeDecl.getMethods();
        Map<String, Integer> methodDic = new HashMap<>();
        for(int i = 0; i < methodDeclarations.length; ++i){
            MethodDeclaration method = methodDeclarations[i];
            methodDic.put(method.getName().toString(), i);
        }

        //每个方法只扫描一边，防止出现引用循环时产生死循环
        Set<String> seen = new HashSet<>();
        Set<String> raw_results = new HashSet<>();

        for(String method:methods){
            //处理方法名，处理前这里的方法名是file.method格式
            method = extractName(method);

            if(!methodDic.containsKey(method)){
                continue;
            }

            MethodDeclaration mDeclaration = methodDeclarations[methodDic.get(method)];
            raw_results.addAll(getInvocatedMethodsOfSingleMethod(mDeclaration));

            seen.add(method);
        }

        Queue<String> invocatedInnerMethods = new ArrayDeque<>();

        for(String result:raw_results){
            if(methodDic.containsKey(result) && !seen.contains(result)){
                invocatedInnerMethods.add(result);
            }
        }

        while(!invocatedInnerMethods.isEmpty()){
            String method = invocatedInnerMethods.remove();
            MethodDeclaration mDeclaration = methodDeclarations[methodDic.get(method)];
            Set<String> newMethods = getInvocatedMethodsOfSingleMethod(mDeclaration);
            raw_results.addAll(newMethods);
            seen.add(method);

            for(String newMethod:newMethods){
                if(methodDic.containsKey(newMethod) && !seen.contains(newMethod)){
                    invocatedInnerMethods.add(newMethod);
                }
            }
        }

        Set<String> results = new HashSet<>();
        for(String raw:raw_results){
            if(!javaMethodList_.containsKey(raw)){
                continue;
            }
            for(String candidate:javaMethodList_.get(raw)){
                if(methodMatch(importsNames, candidate)){
                    results.add(candidate+"."+raw);
                }
            }
        }

        return results;
    }

    private static boolean methodMatch(List<String> imports, String candidate){
        String[] candiSplit = candidate.split("\\.");
        for(String im:imports){
            String[] imSplit = im.split("\\.");
            int i;
            for(i = 0; i < candiSplit.length && i < imSplit.length; ++i){
                if(!candiSplit[i].equals(imSplit[i])){
                    break;
                }
            }
            if(i == imSplit.length||imSplit[i].equals("*")){
                return true;
            }
        }
        return false;
    }
    private static String extractName(String name){
//        System.out.println("Extract Name:");
//        System.out.println(name);
//        System.out.println("\n");
        String[] seperateName = name.split("\\.");
        name = seperateName[seperateName.length-1];
        int bracket_position = name.indexOf("(");
        if(bracket_position > 0) {
            name = name.substring(0, bracket_position);
        }
//        System.out.println("After extract");
//        System.out.println(name);
//        System.out.println("\n");
        return name;
    }

    //把文件分为测试文件和功能文件
    private static HashMap<String, List<File>> splitTest(File dir){
        File[] files = dir.listFiles();
        List<File> tests = new ArrayList<>();
        List<File> srcs = new ArrayList<>();

        HashMap<String, List<File>> methodsNames = new HashMap<>();
        for(File file:files){
            String name = file.getName();
            if(stringUtils.hasTest(name.split("\\."))){
                tests.add(file);
            }else{
                srcs.add(file);
            }
        }

        methodsNames.put("test", tests);
        methodsNames.put("srt", srcs);

        return methodsNames;
    }

    //抽取单个方法的API, 不加入自己防止无限循环
    private static Set<String> getInvocatedMethodsOfSingleMethod(MethodDeclaration method){
        Block body = method.getBody();
        Set<String> invocatedMethods = new HashSet<>();
        if(body == null){
            return invocatedMethods;
        }

        body.accept(new ASTVisitor() {
                              @Override
                              public boolean visit(MethodInvocation node) {
                                  String name = node.getName().toString();
                                  if(!name.equals(method.getName().toString())) {
                                      invocatedMethods.add(node.getName().toString());
                                  }
                                  return true;
                              }
                          }
        );
        return invocatedMethods;
    }

    public static void main(String[] args) throws IOException {

//        String filePath = "D:\\chh\\SE\\projects\\CBDetectorKotlin\\data\\patches\\LUCENE\\0045e0f_Bug_LUCENE-2937\\to\\lucene_src_test_org_apache_lucene_util_TestSmallFloat.java";
//        File target = new File(filePath);
//        List<String> methodlist = new ArrayList<>();
//        methodlist.add("testByteToFloat()");

//        Set<String> invocatedAPIs = getInvocatedMethodsOfSingleFile(target, methodlist);

        String project_name = "LUCENE";
//        String bug_name = "0082b50_Bug_LUCENE-5473";
        String bug_name = "0045e0f_Bug_LUCENE-2937";

        changeHandler test = new changeHandler(project_name, bug_name);
        Map<String, Set<String>> javaMethodList = extractMethods("lucene-9.3.0-src");

        loadFiles(project_name, bug_name, javaMethodList);
        HashMap<String, Set<String>> test_Apis = getInvocatedMethods(test);

//        String contents = FileUtils.readFileToString(new File(filePath));



//        String contents = "package test0001;\n" + "import java.util.*;\n" + "public class Test {\n"
//                + "   public static void main(String[] args) {\n"
//                + "      System.out.println(\"Hello\" + \" world\");\n" + "   }\n" + "}";

//        final CompilationUnit astRoot = parseStringToCompilationUnit(contents);
//        AST ast = astRoot.getAST();
//        TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
//
//        //Get all methods from the class
//        MethodDeclaration[] methodDeclarations = typeDecl.getMethods();
//
//        List<SimpleName> invocatedMethods = new ArrayList<>();
//        List<IMethodBinding> bindings = new ArrayList<>();
//
//        for(MethodDeclaration methodDeclaration:methodDeclarations){
//            methodDeclaration.equals(methodDeclaration)
//            Block methodBody = methodDeclaration.getBody();
//
//            if(methodBody != null){
//                methodBody.accept(new ASTVisitor() {
//                                      @Override
//                                      public boolean visit(MethodInvocation node) {
//                                          invocatedMethods.add(node.getName());
//                                          bindings.add(node.resolveMethodBinding());
//                                          List argu = node.typeArguments();
//                                          return true;
//                                      }
//                                  }
//                );
//            }
//        }



        System.out.println("test");
    }

}

