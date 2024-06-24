package com.vyunfei.ailist.gencode;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import java.util.Arrays;

public class GenImplMethodCode {

    public static void implMethod(Project project, PsiClass implClass, PsiMethod... noImplMethods) {
        overrideOrImplMethod(project, implClass, false, noImplMethods);
    }

    public static void overrideOrImplMethod(Project project, PsiClass implClass, PsiMethod... noImplMethods) {
        overrideOrImplMethod(project, implClass, true, noImplMethods);
    }

    public static void overrideOrImplMethod(Project project, PsiClass implClass, boolean override, PsiMethod... noImplMethods) {
        for (PsiMethod method : noImplMethods) {
            PsiMethod implMethod = (PsiMethod) method.copy();
            PsiModifierList modifierList = implMethod.getModifierList();
            // is public
            if (!modifierList.hasModifierProperty(PsiModifier.PUBLIC)) {
                modifierList.setModifierProperty(PsiModifier.PUBLIC, true);
            }
            for (PsiAnnotation annotation : implMethod.getAnnotations()) {
                annotation.delete();
            }
            // add @Override
            if (override) {
                modifierList.addAnnotation("Override");
            }
            // Parameters remove annotation
            PsiParameterList implParameterList = implMethod.getParameterList();
            PsiParameter[] parameters = implParameterList.getParameters();
            for (PsiParameter parameter : parameters) {
                for (PsiAnnotation annotation : parameter.getAnnotations()) {
                    annotation.delete();
                }
            }
            // add bodyCode
            PsiCodeBlock body = implMethod.getBody();
            if (body == null) {
                body = PsiElementFactory.getInstance(project).createCodeBlock();
                if (override) {
                    String code = "xxxApplication." + implMethod.getName() + "(" +
                            (implMethod.getParameterList().getParameters().length == 0 ?
                                    "" :
                                    Arrays.stream(implMethod.getParameterList().getParameters()).map(PsiParameter::getName).reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b)
                            )
                            + ");";
                    if (implMethod.getReturnType() != null && !implMethod.getReturnType().equalsToText("void")) {
                        code = "return " + code;
                    }
                    PsiStatement newStatement = PsiElementFactory.getInstance(project).createStatementFromText(code, implMethod);
                    body.add(newStatement);
                } else {
                    body.add(PsiParserFacade.getInstance(project).createLineCommentFromText(implClass.getLanguage(), " todo"));
                }
                implMethod.add(body);
            }
            implClass.add(implMethod);
        }
    }
}
