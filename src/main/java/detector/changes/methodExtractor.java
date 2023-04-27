package detector.changes;

import detector.main;
import detector.utils.stringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class methodExtractor {
    static String data_dir;
    static int baseLenth;

    static Logger logger = LogManager.getLogger(methodExtractor.class.getName());

    public static Map<String, Set<String>> extractMethods(String src_dir){
        logger.info("Start to extract all methods from: "+src_dir);
        //初始化用户目录
        if(data_dir == null){
            String relativelyPath=System.getProperty( "user.dir" );
            StringBuilder builder= new StringBuilder(relativelyPath);
            builder.append("\\data\\sources\\");
            data_dir = builder.toString();
        }

        //初始化项目源码目录
        StringBuilder builder = new StringBuilder(data_dir);
        builder.append(src_dir);
        builder.append("\\");
        String srcDirStr = builder.toString();
        baseLenth = builder.length();

        return searchFiles(srcDirStr);
    }

    private static Map<String, Set<String>> searchFiles(String rootDir){
        Map<String, Set<String>> methods = new HashMap<>();

        try (Stream<Path> paths = Files.walk(Paths.get(rootDir))) {
            List<Path> results = paths
                    .filter(f -> f.toString().endsWith(".java"))
                    .collect(Collectors.toList());

//            results.forEach(System.out::println);

            for(Path result:results){
                String fullPath = result.toString();
                String relativePath = fullPath.substring(baseLenth);
                String fileName = result.getFileName().toString();
                fileName = fileName.substring(0,fileName.length()-5);
                if(stringUtils.hasTest(relativePath.split("\\\\"))){
                    continue;
                }

                Set<String> methodNames = getMethodsFromSingleFile(fullPath);
                if(methodNames.isEmpty()){
                    continue;
                }
                String contents = FileUtils.readFileToString(new File(fullPath));
                final CompilationUnit astRoot = parseStringToCompilationUnit(contents);
                String pkg = astRoot.getPackage().getName().toString();

                for(String method:methodNames){
                    if(!methods.containsKey(method)){
                        methods.put(method, new HashSet<>());
                    }
                    methods.get(method).add(pkg+"."+fileName);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Set<String>> finalMethods;
        //滤掉很多方法中都有的函数
        Set<String> methodNames = new HashSet<>(methods.keySet());
        for(String name:methodNames){
            if(methods.get(name).size() > 9){
                methods.remove(name);
            }
        }

        return methods;
    }

    private static Set<String> getMethodsFromSingleFile(String path) throws IOException {
        String contents = FileUtils.readFileToString(new File(path));

        final CompilationUnit astRoot = parseStringToCompilationUnit(contents);

        if(astRoot.types().isEmpty()){
            return new HashSet<>();
        }

        TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);

        //Get all methods from the class
        MethodDeclaration[] methodDeclarations = typeDecl.getMethods();

        Set<String> methods = new HashSet<>();

        for(MethodDeclaration methodDec:methodDeclarations){
            methods.add(methodDec.getName().toString());
        }

        return methods;
    }

    private static CompilationUnit parseStringToCompilationUnit(String unit) {
        @SuppressWarnings("deprecation")
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit.toCharArray());
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null);
    }

    public static void main(String[] args){
        Map<String, Set<String>> test = extractMethods("lucene-9.3.0-src");

        System.out.println("test");
    }
}
