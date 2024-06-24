package com.vyunfei.ailist.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.vyunfei.ailist.gencode.GenImplClassCode;
import com.vyunfei.ailist.notify.Notifications;

public class GenerateAllNoImplAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            String apiPath = project.getBasePath() + "/" + project.getName().replace("-", ".") + ".api/src/main/java/";
            String impPath = project.getBasePath() + "/" + project.getName().replace("-", ".") + "/src/main/java/";
            try {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    GenImplClassCode genImplClassCode = new GenImplClassCode(impPath);
                    genImplClassCode.startGenCode(apiPath, project);
                });
            } catch (Exception ex) {
                Notifications.showException(project, ex);
            }
        }
    }
}
