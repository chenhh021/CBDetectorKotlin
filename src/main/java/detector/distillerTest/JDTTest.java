//package detector.distillerTest;
//
//import org.apache.commons.io.FileUtils;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.jdt.core.dom.*;
//import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
//import org.eclipse.jface.text.BadLocationException;
//import org.eclipse.jface.text.Document;
//import org.eclipse.text.edits.MalformedTreeException;
//import org.eclipse.text.edits.TextEdit;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.Collection;
//
//public class JDTTest {
//    static int total_mutants = 0;
//
//    private static void buildReport(String str) {
//        FileAndReportUtils.buildReport(str);
//    }//
//
//    private static void parse(File file) throws CoreException, IOException, BadLocationException {
//
//        buildReport("generating mutants");
//        ArrayList<String> mutants = createMutants(file);
//        processMutants(file, mutants);
//        System.out.println("done");
//    }
//
//    private static void processMutants(File file, ArrayList<String> mutants)
//            throws BadLocationException, IOException {
//
//        for (int i = 1; i <= mutants.size(); i++) {
//
//            String[] parts = null;
//            // for windows system
//            if (File.separator.equals("\\")) {
//                parts = file.getAbsolutePath()
//                        .split(FileAndReportUtils.getProjectFolderPrefix().toString().replace("\\", "\\\\"));
//            }
//            // for linux systems
//            else {
//                parts = file.getAbsolutePath()
//                        .split(FileAndReportUtils.getProjectFolderPrefix().toString().replace("\\", "/"));
//            }
//
//            int count = total_mutants + i;
//            String currentFileLoc = FileAndReportUtils.getMutantprojloc() + File.separator
//                    + FileAndReportUtils.getProjectName() + "-" + count + parts[1];
//            //         System.out.println(currentFileLoc);
//            FileAndReportUtils.createProjectCopy(total_mutants + i);
//            int startPosition = Integer.parseInt(getDataFromMutantsList(mutants.get(i - 1), 1));
//            int length = Integer.parseInt(getDataFromMutantsList(mutants.get(i - 1), 2));
//            String newSource = getDataFromMutantsList(mutants.get(i - 1), 4);
//            //
//            //         @SuppressWarnings("deprecation")
//            String mutated_contents = generateMutatedCode(startPosition, length, newSource,
//                    FileUtils.readFileToString(new File(file.getAbsolutePath())));
//
//            FileWriter fileOverwrite = new FileWriter(currentFileLoc, false);
//            fileOverwrite.write(mutated_contents);
//            fileOverwrite.close();
//
//        }
//        total_mutants += mutants.size();
//        buildReport("Mutants Size: " + mutants.size());
//        buildReport("Total Mutants: " + total_mutants);
//        //      mt.copyProject(fname, "_mutant",mutated_contents);
//    }
//
//    private static String generateMutatedCode(int startPosition, int length, String newSource, String contents)
//            throws MalformedTreeException, BadLocationException {
//
//        Document document = new Document(contents);
//        CompilationUnit astRoot = parseStringToCompilationUnit(contents);
//        AST ast = astRoot.getAST();
//        ASTRewrite rewriter = ASTRewrite.create(ast);
//        TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
//
//        //search for the node to be replaced
//        NodeFinder myNodeFinder = new NodeFinder(typeDecl, startPosition, length);
//        ASTNode oldNode = myNodeFinder.getCoveredNode();
//
//        //Create a new node using the provided source code
//        TextElement siso = ast.newTextElement();
//        siso.setText(newSource);
//
//        //replace the node
//        rewriter.replace(oldNode, siso, null);
//        TextEdit edits = rewriter.rewriteAST(document, null);
//        edits.apply(document);
//        //
//        //      System.out.println(document.get());
//        return document.get();
//    }
//
//    private static String getDataFromMutantsList(String mutant, int index) {
//        String[] splits = mutant.split("\n");
//        String currentSplit = splits[index];
//        int indexOfLabel = currentSplit.indexOf(": ") + 2;
//        String info = currentSplit.substring(indexOfLabel);
//        return info;
//    }
//
//    private static ArrayList<String> createMutants(File file) throws CoreException, IOException {
//
//        String mutationPlanPath = FileAndReportUtils.getMutantsloc() + File.separator + file.getName()
//                + "_mutants.txt";
//        buildReport("Mutant File Location: " + mutationPlanPath);
//        @SuppressWarnings("deprecation")
//        String contents = FileUtils.readFileToString(new File(file.getAbsolutePath()));
//        //      System.out.println(contents);
//        //Generate mutants
//        final ArrayList<String> mutants = new ArrayList<String>();
//
//        try {
//            final CompilationUnit astRoot = parseStringToCompilationUnit(contents);
//            AST ast = astRoot.getAST();
//            ASTRewrite.create(ast);
//
//            //Each TypeDeclaration also seems to represent a class
//            if (astRoot.types().size() > 0) {
//                TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
//
//                //Get all methods from the class
//                MethodDeclaration[] methodDeclarations = typeDecl.getMethods();
//
//                for (final MethodDeclaration methodDeclaration : methodDeclarations) {
//
//                    Block methodBody = methodDeclaration.getBody();
//                    if (methodBody != null) {
//                        methodBody.accept(new ASTVisitor() {
//
//                            //prefix Expressions
//                            public boolean visit(PostfixExpression node) {
//                                StringBuilder sb = new StringBuilder();
//                                IMethodBinding a = methodDeclaration.resolveBinding();
//                                //                     if(a!=null){
//                                //                        ITypeBinding b = a.getDeclaringClass();
//                                //                        sb.append("Class name: " + b.getName() + "\n");
//                                //                     }
//                                sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                sb.append("Length: " + node.getLength() + "\n");
//                                sb.append("Current source: " + node + "\n");
//                                if (node.getOperator().toString().equals("++")) {
//                                    node.setOperator(PostfixExpression.Operator.toOperator("--"));
//                                } else {
//                                    //                     System.out.println("here");
//                                    node.setOperator(PostfixExpression.Operator.toOperator("++"));
//                                }
//                                sb.append("New source: " + node + "\n");
//                                sb.append("\n");
//                                mutants.add(sb.toString());
//                                return true;
//                            }
//
//                            public boolean visit(InfixExpression node) {
//
//                                //                  StringBuilder sb = new StringBuilder();
//
//                                IMethodBinding a = methodDeclaration.resolveBinding();
//                                //                  if(a!=null){
//                                //                     ITypeBinding b = a.getDeclaringClass();
//                                //                     sb.append("Class name: " + b.getName() + "\n");
//                                //                  }
//                                //minus Operator
//                                String cur = node.toString();
//
//                                if (node.getOperator().toString().equals("-")) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.PLUS);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//
//                                    sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.TIMES);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//
//                                    sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.DIVIDE);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//                                    return true;
//                                }
//
//                                // and -> or operator
//                                else if (node.getOperator().toString()
//                                        .equals(InfixExpression.Operator.AND.toString())) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.OR);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//                                }
//
//                                // or -> and operator
//                                else if (node.getOperator().toString()
//                                        .equals(InfixExpression.Operator.OR.toString())) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.AND);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//                                }
//
//                                // cond and -> cond or operator
//                                else if (node.getOperator().toString()
//                                        .equals(InfixExpression.Operator.CONDITIONAL_AND.toString())) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.CONDITIONAL_OR);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//                                }
//
//                                // cond or -> cond and operator
//                                else if (node.getOperator().toString()
//                                        .equals(InfixExpression.Operator.CONDITIONAL_OR.toString())) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//                                }
//
//                                // > to >=
//                                else if (node.getOperator().toString()
//                                        .equals(InfixExpression.Operator.GREATER.toString())) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.GREATER_EQUALS);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//                                }
//
//                                // < to <=
//                                else if (node.getOperator().toString()
//                                        .equals(InfixExpression.Operator.LESS.toString())) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.LESS_EQUALS);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//                                }
//
//                                //multiply operator
//                                else if (node.getOperator().toString().equals("*")) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.PLUS);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//
//                                    sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.MINUS);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//
//                                    sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.DIVIDE);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//                                    return true;
//                                }
//
//                                //Divide operator
//                                else if (node.getOperator().toString().equals("/")) {
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.PLUS);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//
//                                    sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.MINUS);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//
//                                    sb = new StringBuilder();
//                                    sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
//                                    sb.append("Start Position: " + node.getStartPosition() + "\n");
//                                    sb.append("Length: " + node.getLength() + "\n");
//                                    sb.append("Current source: " + cur + "\n");
//                                    node.setOperator(InfixExpression.Operator.TIMES);
//                                    sb.append("New source: " + node + "\n");
//                                    sb.append("\n");
//                                    mutants.add(sb.toString());
//
//                                    return true;
//                                }
//
//                                return true;
//                            }
//                        });
//                    }
//                }
//            }
//            //}
//
//            //Log mutants to a file
//            PrintWriter writer = new PrintWriter(new File(mutationPlanPath));
//            for (String mutant : mutants) {
//                writer.print(mutant);
//            }
//
//            writer.close();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//        return mutants;
//    }
//
//    private static CompilationUnit parseStringToCompilationUnit(String unit) {
//        @SuppressWarnings("deprecation")
//        ASTParser parser = ASTParser.newParser(AST.JLS3);
//        parser.setKind(ASTParser.K_COMPILATION_UNIT);
//        parser.setSource(unit.toCharArray());
//        parser.setResolveBindings(true);
//        return (CompilationUnit) parser.createAST(null);
//    }
//
//    public static void main(String[] args) throws IOException, CoreException, BadLocationException {
//        FileAndReportUtils.getProjectLocation();
//        Collection<File> files = FileAndReportUtils.getFileList();
//
//        for (File file : files) {
//            buildReport("Parsing File: " + /*file.getAbsolutePath()+" ; " +*/ file.getName());
//            parse(file);
//            FileAndReportUtils.addSep();
//        }
//        //      FileAndReportUtils.printReport();
//        FileAndReportUtils.commitReport();
//    }
//}
