package com.vyunfei.ailist.actions;

import com.intellij.codeInsight.generation.OverrideImplementUtil;
import com.intellij.ide.IdeView;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.vyunfei.ailist.gencode.GenImplMethodCode;
import com.vyunfei.ailist.notify.Notifications;

import java.util.Collection;
import java.util.Objects;

public class GenerateCurFileImplAction extends AnAction {


    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        assert project != null;
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        assert editor != null;
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        assert psiFile != null;
        String fileTypeName = psiFile.getFileType().getName();
        if (fileTypeName.equalsIgnoreCase("java")) {
            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            PsiClass choseClass = psiJavaFile.getClasses()[0];
            if (psiFile.getName().replace(".java", "").equals(choseClass.getName())) {
                if (choseClass.isInterface()) {
                    // 判断是否有实现类
                    overrideImplementClass(project, e, choseClass);
                } else {
                    // auto generate need create code
                    Notifications.showWarning(project, "don`t supportType");
                }
            }
        } else {
            Notifications.showWarning(project, "don`t supportType");
        }
    }

    private static void overrideImplementClass(Project project, AnActionEvent e, PsiClass choiceClass) {
        PsiClass implClass;
        // get impl class
        Collection<PsiClass> inheritors = ClassInheritorsSearch.search(choiceClass, GlobalSearchScope.allScope(project), true).findAll();
        if (inheritors.isEmpty()) {
            IdeView view = e.getData(LangDataKeys.IDE_VIEW);
            assert view != null;
            PsiDirectory targetDirectory = DirectoryChooserUtil.getOrChooseDirectory(view);
            if (targetDirectory == null) {
                Notifications.showWarning(project, "no choose directory");
                return;
            }
            String implClassName = choiceClass.getName() + "Impl";
            implClass = JavaPsiFacade.getElementFactory(project).createClass(implClassName);
            PsiJavaCodeReferenceElement interfaceRef = JavaPsiFacade.getElementFactory(project).createReferenceElementByFQClassName(Objects.requireNonNull(choiceClass.getQualifiedName()), implClass.getResolveScope());
            Objects.requireNonNull(implClass.getImplementsList()).add(interfaceRef);
            WriteCommandAction.runWriteCommandAction(project, () -> {
                GenImplMethodCode.overrideOrImplMethod(project, implClass, choiceClass.getMethods());
                targetDirectory.add(implClass);
            });
        } else {
            implClass = inheritors.stream().toList().get(0);
            // change editor
            OpenFileDescriptor descriptor = new OpenFileDescriptor(project, implClass.getContainingFile().getVirtualFile(), 0);
            descriptor.setScrollType(ScrollType.MAKE_VISIBLE);
            Editor editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
            OverrideImplementUtil.chooseAndImplementMethods(project, editor, implClass);
        }
    }

}
