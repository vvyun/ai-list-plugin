package com.vyunfei.ailist.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import java.util.Objects;

public class PsiJavaClassUtils {

    public static void addImplementsClause(Project project, PsiClass psiClass, String superclassFQN) {
        PsiJavaCodeReferenceElement interfaceRef = createSuperClassReference(project, superclassFQN, psiClass);
        Objects.requireNonNull(psiClass.getImplementsList()).add(interfaceRef);
    }

    public static void addExtendsClause(Project project, PsiClass psiClass, String superclassFQN) {
        PsiJavaCodeReferenceElement superClassRef = createSuperClassReference(project, superclassFQN, psiClass);
        PsiReferenceList extendsList = psiClass.getExtendsList();
        if (extendsList == null) {
            extendsList = JavaPsiFacade.getElementFactory(project).createReferenceList(new PsiJavaCodeReferenceElement[]{superClassRef});
            psiClass.addAfter(extendsList, psiClass.getLBrace());
        } else {
            extendsList.add(superClassRef);
        }
        JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
        codeStyleManager.optimizeImports(psiClass.getContainingFile());
        CodeStyleManager styleManager = CodeStyleManager.getInstance(project);
        styleManager.reformat(psiClass);
    }

    private static PsiJavaCodeReferenceElement createSuperClassReference(Project project, String superclassFQN, PsiClass psiClass) {
        return JavaPsiFacade.getElementFactory(project).createReferenceElementByFQClassName(superclassFQN, psiClass.getResolveScope());
    }
}
