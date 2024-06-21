package com.vyunfei.ailist.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.vyunfei.ailist.gencode.GenerateImplCode;
import com.vyunfei.ailist.notify.Notifications;
import org.jetbrains.annotations.NotNull;

public class GenerateAllNoImplAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project != null) {
            String apiPath = project.getBasePath() + "/" + project.getName().replace("-", ".") + ".api/src/main/java/";
            String impPath = project.getBasePath() + "/" + project.getName().replace("-", ".") + "/src/main/java/";
            try {
                Task.Backgroundable task = new Task.Backgroundable(project, "GenControllerAndApplicationCode", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        progressIndicator.setText("Processing..."); // 设置进度条的描述文本
                        GenerateImplCode generateImplCode = new GenerateImplCode(impPath);
                        progressIndicator.setFraction(0); // 更新进度百分比
                        generateImplCode.startGenCode(apiPath, progressIndicator);
                        progressIndicator.setFraction(100); // 更新进度百分比
                    }

                    @Override
                    public void onFinished() {
                        // 长时间操作成功完成后的回调
                        Notifications.showInfo(project, "Operation Finished Successfully");
                    }

                    @Override
                    public void onThrowable(@NotNull Throwable error) {
                        // 处理异常情况
                        Notifications.showException(project, error);
                    }
                };
                ProgressManager.getInstance().run(task);
            } catch (Exception ex) {
                Notifications.showException(project, ex);
            }
        }
    }
}
