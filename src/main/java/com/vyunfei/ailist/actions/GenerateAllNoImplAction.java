package com.vyunfei.ailist.actions;

import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.vyunfei.ailist.gencode.GenImplClassCode;
import com.vyunfei.ailist.notify.Notifications;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GenerateAllNoImplAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        assert project != null;
        DataContext dataContext = e.getDataContext();
        Navigatable[] data = dataContext.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
        final List<PsiClass> psiClassList = new ArrayList<>();
        if (data != null) {
            for (Navigatable datum : data) {
                if (datum instanceof ClassTreeNode classTreeNode) {
                    PsiClass psiClass = classTreeNode.getPsiClass();
                    if (psiClass.isInterface()) {
                        psiClassList.add(psiClass);
                    }
                }
            }
            try {
                String impPath = project.getBasePath() + "/";
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    GenImplClassCode genImplClassCode = new GenImplClassCode(impPath);
                    genImplClassCode.startGenCode(psiClassList, project);
                });
            } catch (Exception ex) {
                Notifications.showException(project, ex);
            }
        }
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Navigatable[] data = dataContext.getData(CommonDataKeys.NAVIGATABLE_ARRAY);
        if (data != null) {
            for (Navigatable datum : data) {
                if (datum instanceof ClassTreeNode classTreeNode) {
                    PsiClass psiClass = classTreeNode.getPsiClass();
                    if (psiClass.isInterface()) {
                        e.getPresentation().setEnabledAndVisible(true);
                        return;
                    }
                }
            }
        }
        e.getPresentation().setEnabledAndVisible(false);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return super.getActionUpdateThread();
    }
}
