package com.vyunfei.ailist.gencode;

import com.intellij.openapi.progress.ProgressIndicator;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class GenerateImplCode {

    public List<String> apiFiles = new ArrayList<>();
    public List<String> controllerFiles = new ArrayList<>();
    public List<String> applicationFiles = new ArrayList<>();
    public List<String> packageTree = new ArrayList<>();
    public static final String JAVA_EX = ".java";

    public GenerateImplCode(String path) {
        addAllJavaFiles(path, controllerFiles, "Controller.java");
        addAllJavaFiles(path, applicationFiles, "Application.java");
        addAllPackage(path, packageTree);
    }

    public void startGenCode(String apiPath, @NotNull ProgressIndicator indicator) {
        addAllJavaFiles(apiPath, apiFiles, "Api.java");
        int i = 10;
        for (String filePath : apiFiles) {
            try {
                File file = new File(filePath);
                String apiFileName = file.getName();
                List<String> codeLines = FileUtils.readLines(file, StandardCharsets.UTF_8.name());
                // xxxXxx.java
                // xxxXxx
                String parentClassName = apiFileName.replace(JAVA_EX, "");
                // xxx
                String baseName = getBaseName(apiFileName, parentClassName);
                String parentPackage = "";
                List<String> imports = new ArrayList<>();
                List<String> function = new ArrayList<>();
                boolean flag = false;
                for (String line : codeLines) {
                    if (line.trim().startsWith("package ")) {
                        parentPackage = line;
                        continue;
                    }
                    if (line.trim().startsWith("import ")) {
                        imports.add(line);
                        continue;
                    }
                    // http method
                    if (line.matches(".*?RequestHeader.*apiReq.*?")) {
                        function.add(line);
                        flag = !line.contains(";");
                        continue;
                    }
                    if (flag) {
                        function.set(function.size() - 1, function.get(function.size() - 1) + "\n" + line);
                        if (line.contains(";")) {
                            flag = false;
                        }
                    }
                }
                // import parentClass;
                String importsParent = parentPackage.replace("package", "import").replace(";", "").trim() + "." + parentClassName + ";";
                imports.add(importsParent);
                generateCodeAndSave2File(imports, function, baseName, parentClassName, CONTROLLER_TEMPLATE, "Controller");
                imports.remove(importsParent);
                generateCodeAndSave2File(imports, function, baseName, parentClassName, APP_TEMPLATE, "Application");
                i = i + 10;
                indicator.setFraction(Math.max(i, 90));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static @NotNull String getBaseName(String parentFileName, String parentClassName) {
        String baseName;
        if (parentFileName.startsWith("I")) {
            baseName = parentClassName.substring(1, parentClassName.length() - 3);
        } else {
            baseName = parentClassName.substring(0, parentClassName.length() - 3);
        }
        return baseName;
    }

    // controller template
    private static final String CONTROLLER_TEMPLATE = """
            ${package}

            ${imports}
            import lombok.RequiredArgsConstructor;
            import lombok.extern.slf4j.Slf4j;
            import club.newepoch.isf.api.BaseApi;

            @Slf4j
            @RestController
            @RequiredArgsConstructor
            public class ${baseName}Controller extends BaseApi implements ${apiClassName} {

                private final ${baseName}Application selfApplication;

                ${functions}
            }
            """;
    // Application template
    private static final String APP_TEMPLATE = """
             ${package}

             ${imports}
             import org.springframework.stereotype.Component;
             import lombok.RequiredArgsConstructor;
             import lombok.extern.slf4j.Slf4j;

             @Slf4j
             @Component
             @RequiredArgsConstructor
             public class ${baseName}Application {

                 private final ${baseName}Service selfService;

                 ${functions}
             }
            """;

    private void generateCodeAndSave2File(List<String> imports, List<String> functions, String baseName, String apiClassName, String template, String type) throws IOException {
        String fileName = baseName + type + JAVA_EX;
        // find implements class
        String fileNameFullPath = (type.equals("Controller") ? controllerFiles : applicationFiles).stream()
                .filter(it -> it.endsWith("\\" + fileName)).findFirst().orElse(null);
        if (fileNameFullPath == null) {
            // not implements class
            String matchStr = type.equals("Controller") ? "interfaces\\rest" : "\\application";
            // get package path
            String packagePath = packageTree.stream().filter(it -> it.endsWith(matchStr)).findFirst().orElseThrow();
            fileNameFullPath = packagePath + "\\" + fileName;
            File file = new File(fileNameFullPath);
            imports.sort(Comparator.comparing(String::new));

            String code = template.replace("${imports}", imports.stream().reduce("", (a, b) -> a + "\n" + b));
            code = code.replace("${baseName}", baseName);
            code = code.replace("${apiClassName}", apiClassName);
            code = code.replace("${package}", getPackage(fileNameFullPath, fileName));
            code = code.replace("${functions}", getFunCode(functions, type));
            System.out.println(code);
            FileUtils.write(file, code, StandardCharsets.UTF_8.name());
        } else {
            // get no implements function
            Map<String, List<String>> funNameMap = functions.stream().collect(Collectors.groupingBy(GenerateImplCode::getFunName));
            File file = new File(fileNameFullPath);
            List<String> codeLines = FileUtils.readLines(file, StandardCharsets.UTF_8.name());
            for (String codeLine : codeLines) {
                if (codeLine.matches(".*?public .*? .*?\\(.*?")) {
                    String funName = getFunName(codeLine);
                    funNameMap.remove(funName);
                }
            }
            if (!funNameMap.isEmpty()) {
                List<String> needGenFun = new ArrayList<>();
                for (List<String> value : funNameMap.values()) {
                    needGenFun.addAll(value);
                }
                String funCodes = getFunCode(needGenFun, type);
                String javaCode = FileUtils.readFileToString(file, StandardCharsets.UTF_8.name());
                int i = javaCode.lastIndexOf("}");
                javaCode = javaCode.substring(0, i) + funCodes + "}";
                FileUtils.write(file, javaCode, StandardCharsets.UTF_8.name());
            }
        }

    }

    private static String getPackage(String path, String fileName) {
        int i = path.indexOf("src\\main\\java\\") + "src\\main\\java\\".length();
        return "package " + path.substring(i, path.length() - fileName.length() - 1).replace("\\", ".") + ";";
    }

    private static final String FUN_TEMPLATE = """
                @Override
                public ${function} {
                    ${generate}
                }
            """;

    private static String getFunCode(List<String> function, String typeName) {
        String refTypeName;
        if (typeName.equals("Controller")) {
            refTypeName = "Application";
        } else if (typeName.equals("Application")) {
            refTypeName = "Service";
        } else {
            return "// todo";
        }
        String funCode = function.stream()
                .map(fun -> {
                    String replace = FUN_TEMPLATE.replace("${function}", fun.substring(0, fun.length() - 1).trim());
                    String s = getFunRefCode(fun, refTypeName);
                    replace = replace.replace("${generate}", s);
                    return replace;
                })
                .reduce("", (a, b) -> a + "\n\n" + b);

        return refTypeName.equals("Service") ?
                funCode.replace("@Override", "// todo")
                        .replaceAll("@.*?\\(.*?\\) ", "")
                        .replaceAll("@.*? ", "") :
                funCode;
    }

    private static String getFunRefCode(String fun, String typeName) {
        String funName = getFunName(fun);
        String funArgsStr = getFunArgsStr(fun);
        if (fun.contains("void")) {
            return "self" + typeName + "." + funName + "(" + funArgsStr + ");";
        }
        return "return self" + typeName + "." + funName + "(" + funArgsStr + ");";
    }

    public static void addAllJavaFiles(String path, List<String> apiJavaFileList, String endName) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File chfile : files) {
                    addAllJavaFiles(chfile.getAbsolutePath(), apiJavaFileList, endName);
                }
            }
        } else if (path.endsWith(endName)) {
            apiJavaFileList.add(path);
        }
    }

    public static void addAllPackage(String path, List<String> apiJavaFileList) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File chFile : files) {
                    addAllPackage(chFile.getAbsolutePath(), apiJavaFileList);
                }
            }
            apiJavaFileList.add(path);
        }
    }


    private static String getFunArgsStr(String fun) {
        int start = fun.indexOf("(");
        int end = fun.lastIndexOf(")");
        String argStr = fun.substring(start + 1, end);
        argStr = argStr.replaceAll("@.*?\\(.*?\\) ", "");
        argStr = argStr.replaceAll("@.*? ", "");
        argStr = argStr.replaceAll("<.*?>", "");
        String[] argArr = argStr.split(",");
        int i = 0;
        for (String s : argArr) {
            argArr[i++] = s.trim().split(" ")[1];
        }
        return Arrays.stream(argArr).reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
    }

    private static String getFunName(String fun) {
        fun = fun.replaceAll("<.*?>", "").trim();
        int i = fun.indexOf("(");
        fun = fun.substring(0, i);
        int i1 = fun.lastIndexOf(" ");
        return fun.substring(i1 + 1).trim();
    }

}
