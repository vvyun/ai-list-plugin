package com.vyunfei.ailist.actions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.ide.IdeView;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.vyunfei.ailist.notify.Notifications;
import org.apache.commons.lang.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

public class Json2BeanAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        try {
            Project project = e.getProject();
            assert project != null;
            IdeView view = e.getData(LangDataKeys.IDE_VIEW);
            assert view != null;
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            assert editor != null;
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            assert psiFile != null;
            String json = editor.getDocument().getText();
            PsiDirectory targetDirectory = DirectoryChooserUtil.getOrChooseDirectory(view);
            if (targetDirectory == null) {
                Notifications.showWarning(project, "no choose directory");
                return;
            }
            json2GenCode(project, psiFile.getName(), json, targetDirectory);
        } catch (Exception ex) {
            Notifications.showError(e.getProject(), ex.getLocalizedMessage());
        }
    }

    private static void json2GenCode(Project project, String fileName, String json, PsiDirectory directory) {
        JsonElement element = JsonParser.parseString(json);
        PsiClass genClass = JavaPsiFacade.getElementFactory(project).createClass(fileName.split("\\.")[0]);
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            for (String key : jsonObject.keySet()) {
                JsonElement chEle = jsonObject.get(key);
                String type = "String";
                String keyClassName = String.valueOf(key.charAt(0)).toUpperCase() + key.substring(1);
                if (chEle.isJsonPrimitive()) {
                    String str = chEle.getAsString();
                    if (NumberUtils.isNumber(str)) {
                        type = "BigDecimal";
                    }
                } else if (chEle.isJsonArray()) {
                    type = "List<" + keyClassName + ">";
                    json2GenCode(project, keyClassName, chEle.getAsJsonArray().get(0).toString(), directory);
                } else if (chEle.isJsonObject()) {
                    type = keyClassName;
                    json2GenCode(project, keyClassName, chEle.toString(), directory);
                }
                PsiField field = JavaPsiFacade.getElementFactory(project).createFieldFromText("private " + type + " " + key + ";", genClass);
                genClass.add(field);
                System.out.println(chEle);
            }
        } else {
            Notifications.showError(project, "file is not json type");
        }
        PsiModifierList modifierList = genClass.getModifierList();
        if (modifierList != null) {
            modifierList.addAnnotation("Date");
            modifierList.addAnnotation("ToString");
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            directory.add(genClass);
        });
    }
}
