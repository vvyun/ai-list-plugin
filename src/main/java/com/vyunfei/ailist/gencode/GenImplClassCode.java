package com.vyunfei.ailist.gencode;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.vyunfei.ailist.psi.PsiJavaClassUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenImplClassCode {

    public List<String> controllerFiles = new ArrayList<>();
    public List<String> applicationFiles = new ArrayList<>();
    public List<String> packageTree = new ArrayList<>();
    public static final String JAVA_EX = ".java";

    public GenImplClassCode(String path) {
        addAllJavaFiles(path, controllerFiles, "Controller.java");
        addAllJavaFiles(path, applicationFiles, "Application.java");
        buildPackageTree(path, packageTree);
    }

    public void startGenCode(List<PsiClass> psiClassList, Project project) {
        for (PsiClass psiClass : psiClassList) {
            try {
                String parentClassName = psiClass.getName();
                if (parentClassName != null) {
                    String baseName = getBaseName(parentClassName);
                    PsiClass appPsiClass = generateCodeAndSave2File(project, psiClass, baseName, "Application");
                    generateCodeAndSave2File(project, psiClass, baseName, "Controller", appPsiClass);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static @NotNull String getBaseName(String parentClassName) {
        String baseName;
        if (parentClassName.startsWith("I")) {
            baseName = parentClassName.substring(1, parentClassName.length() - 3);
        } else {
            baseName = parentClassName.substring(0, parentClassName.length() - 3);
        }
        return baseName;
    }

    private PsiClass generateCodeAndSave2File(Project project, PsiClass choiceClass, String baseName, String type, PsiClass... ref) throws IOException {
        PsiClass implClass;
        String className = baseName + type;
        String fileName = baseName + type + JAVA_EX;
        // find implements class
        boolean isController = type.equals("Controller");
        String fileNameFullPath = (isController ? controllerFiles : applicationFiles).stream()
                .filter(it -> it.endsWith("\\" + fileName)).findFirst().orElse(null);
        if (fileNameFullPath == null) {
            // not implements class
            String matchStr = isController ? "interfaces\\rest" : "\\application";
            // get package path
            String packagePath = packageTree.stream().filter(it -> it.endsWith(matchStr)).findFirst().orElseThrow();
            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(packagePath);
            if (file == null) throw new AssertionError();
            PsiDirectory targetDirectory = PsiManager.getInstance(project).findDirectory(file);
            if (targetDirectory == null) throw new AssertionError();
            implClass = JavaPsiFacade.getElementFactory(project).createClass(className);
            if (isController) {
                try {
                    PsiJavaClassUtils.addImplementsClause(project, implClass, choiceClass.getQualifiedName());
                    PsiJavaClassUtils.addExtendsClause(project, implClass, "BaseApi");
                } catch (Exception ignore) {
                }
            }
            GenImplMethodCode.overrideOrImplMethod(project, implClass, isController, choiceClass.getMethods());

            targetDirectory.add(implClass);
            if (ref != null) {
                PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
                implClass = cache.getClassesByName(className, GlobalSearchScope.allScope(project))[0];
                for (PsiClass fieldClass : ref) {
                    // add field
                    PsiField field = JavaPsiFacade.getElementFactory(project).createFieldFromText(
                            "private " + fieldClass.getName() + " xxxApplication; // todo rename", implClass);
                    implClass.add(field);
                    // add import fieldClass
                    JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
                    codeStyleManager.addImport((PsiJavaFile) implClass.getContainingFile(), fieldClass);
                }
            }
        } else {
            PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
            implClass = cache.getClassesByName(className, GlobalSearchScope.allScope(project))[0];
            PsiMethod[] methods = implClass.getMethods();
            if (methods.length == 0) {
                GenImplMethodCode.overrideOrImplMethod(project, implClass, choiceClass.getMethods());
            } else {
                List<PsiMethod> implMethodsStream = Arrays.stream(methods).toList();
                for (PsiMethod method : choiceClass.getMethods()) {
                    String name = method.getName();
                    int length = method.getParameterList().getParameters().length;
                    if (implMethodsStream.stream().anyMatch(it -> it.getName().equalsIgnoreCase(name) && it.getParameterList().getParameters().length == length)) {
                        // 已实现
                        continue;
                    }
                    GenImplMethodCode.overrideOrImplMethod(project, implClass, isController, method);
                }
            }
        }
        return implClass;
    }


    public static void addAllJavaFiles(String path, List<String> apiJavaFileList, String endName) {
        File file = new File(path);
        if (file.isDirectory()
                && !file.getName().startsWith(".")
                && !Arrays.asList("resources", "target", "logs", "out", "build").contains(file.getName())
        ) {
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

    public static void buildPackageTree(String path, List<String> apiJavaFileList) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File chFile : files) {
                    buildPackageTree(chFile.getAbsolutePath(), apiJavaFileList);
                }
            }
            apiJavaFileList.add(path);
        }
    }
}
