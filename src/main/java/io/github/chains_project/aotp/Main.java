package io.github.chains_project.aotp;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import io.github.chains_project.aotp.oops.klass.ClassEntry;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "aotp", description = "Tool to give insight into AOTCache files.")
public class Main implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the AOT cache file.")
    String filePath;

    @Option(names = "--header", description = "Print the file map header.")
    boolean header;

    @Option(names = "--print-class",
            paramLabel = "CLASS",
            description = "Pretty print the fields of the specified class.")
    String printClassName;

    @Option(names = "--list-classes", description = "List classes found in the RW region.")
    boolean listClasses;

    @Option(names = "--class-size",
            paramLabel = "CLASS",
            description = "Print the size of the specified class.",
            arity = "1..*")
    List<String> classSizeClassNames;

    @Override
    public Integer call() {
        boolean anyFlag = header || listClasses
                || (classSizeClassNames != null && !classSizeClassNames.isEmpty())
                || printClassName != null;
        if (!anyFlag) {
            header = true;
            listClasses = true;
        }

        try {
            if (header) {
                AotpApi.printHeader(filePath, System.out);
                return 0;
            }

            if (listClasses) {
                for (String name : AotpApi.listClassNames(filePath)) {
                    System.out.println(name);
                }
            }

            if (classSizeClassNames != null && !classSizeClassNames.isEmpty()) {
                Map<ClassEntry, Integer> sizes = AotpApi.getClassSizes(filePath, classSizeClassNames);
                for (Map.Entry<ClassEntry, Integer> entry : sizes.entrySet()) {
                    System.out.println(entry.getKey().getName() + ": " + entry.getValue());
                }
            }

            if (printClassName != null) {
                if (!AotpApi.printClass(filePath, printClassName, System.out)) {
                    System.err.println("Class not found: " + printClassName);
                    return 1;
                }
            }

            return 0;
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
