package detector.changes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class projectChangeHandler {
    public static List<String> getBugNames(String project){
        String basedir = changeHandler.getDataDir();
        File projectDir = new File(basedir + project);
        List<String> bugNames = new ArrayList<>();
        for(File bugDir: Objects.requireNonNull(projectDir.listFiles())){
            if(bugDir.isFile()){
                continue;
            }
            bugNames.add(bugDir.getName());
        }
        return bugNames;
    }
    public static List<changeHandler> getProjectChanges(String project){
        List<String> bugNames = getBugNames(project);
        List<changeHandler> bugChanges = new ArrayList<>();
        for(String name: bugNames){
            bugChanges.add(new changeHandler(project, name));
        }
        return bugChanges;
    }

    public static void main(String[] args){
        List<changeHandler> bugChanges = getProjectChanges("DERBY");
        System.out.println();
    }
}
