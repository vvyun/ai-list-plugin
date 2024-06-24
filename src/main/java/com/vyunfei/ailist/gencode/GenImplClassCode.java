package com.vyunfei.ailist.gencode;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GenImplClassCode {

    public List<String> apiFiles = new ArrayList<>();
    public List<String> controllerFiles = new ArrayList<>();
    public List<String> applicationFiles = new ArrayList<>();
    public List<String> packageTree = new ArrayList<>();
    public static final String JAVA_EX = ".java";

    public GenImplClassCode(String path) {
        addAllJavaFiles(path, controllerFiles, "Controller.java");
        addAllJavaFiles(path, applicationFiles, "Application.java");
        BulidPackageTree(path, packageTree);
    }

    public void startGenCode(String apiPath, Project project) {
        addAllJavaFiles(apiPath, apiFiles, "Api.java");
        for (String filePath : apiFiles) {
            try {
                File file = new File(filePath);
                String apiFileName = file.getName();
                String parentClassName = apiFileName.replace(JAVA_EX, "");
                PsiShortNamesCache cache = PsiShortNamesCache.getInstance(project);
                PsiClass psiClass = cache.getClassesByName(parentClassName, GlobalSearchScope.allScope(project))[0];
                String baseName = getBaseName(apiFileName, parentClassName);
                PsiClass appPsiClass = generateCodeAndSave2File(project, psiClass, baseName, "Application");
                generateCodeAndSave2File(project, psiClass, baseName, "Controller", appPsiClass);
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
                PsiJavaCodeReferenceElement interfaceRef = JavaPsiFacade.getElementFactory(project).createReferenceElementByFQClassName(Objects.requireNonNull(choiceClass.getQualifiedName()), implClass.getResolveScope());
                Objects.requireNonNull(implClass.getImplementsList()).add(interfaceRef);
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

    public static void BulidPackageTree(String path, List<String> apiJavaFileList) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File chFile : files) {
                    BulidPackageTree(chFile.getAbsolutePath(), apiJavaFileList);
                }
            }
            apiJavaFileList.add(path);
        }
    }
}
