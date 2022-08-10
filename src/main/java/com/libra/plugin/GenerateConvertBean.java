package com.libra.plugin;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.PsiAnnotationStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.impl.java.stubs.impl.PsiMethodStubImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiImmediateClassType;
import com.intellij.psi.impl.source.PsiMethodImpl;
import com.intellij.psi.stubs.PsiFileStub;
import com.libra.plugin.asserts.Assert;
import com.libra.plugin.exception.GenerateMarkDownException;
import com.libra.plugin.utils.DateUtils;
import com.libra.plugin.utils.PsiTypeUtils;
import org.intellij.plugins.markdown.lang.MarkdownFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.java.JavaUMethod;

import java.util.HashMap;
import java.util.Map;

public class GenerateConvertBean extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // 全局变量（包信息）
        Project project = e.getProject();

        // 获取选择的文件
        PsiFile selectFile = getSelectFile(e);

        // 选择原始类
        TreeClassChooserFactory instance = TreeClassChooserFactory.getInstance(project);
        TreeClassChooser selector = instance.createAllProjectScopeChooser("请选择原始类（转换前）");
        selector.showDialog();

        // 获取目标类
        PsiClass targetClass = getSelectClass(e);
        // 获取原始类
        PsiClass sourceClass = selector.getSelected();

        Assert.isNotNull(sourceClass, "请选择原始类（转换前）");
        Assert.isNotNull(targetClass, "请选择目标类（转换后）");

        Map<String, String> sourceClassFiledMap = getFiledMap(sourceClass);
        Map<String, String> targetClassFiledMap = getFiledMap(targetClass);

        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);

        StringBuilder sb = new StringBuilder();
        sb.append(targetClass.getName() + " result = new " + targetClass.getName() + "();\n");

        targetClassFiledMap.forEach((key, value) -> {
            sb.append("result.set" + higherFirstCapse(key) + "(" + lowerFirstCapse(sourceClass.getName()) + ".get" + higherFirstCapse(key) + "()" + ")" + ";\n");
        });

        sb.append("return result;\n");

        PsiMethod method = elementFactory.createMethodFromText(
                "private " +
                        targetClass.getName() + " " +
                        lowerFirstCapse(sourceClass.getName()) + "Convert" + targetClass.getName() +
                        "(" + sourceClass.getName() + " " + lowerFirstCapse(sourceClass.getName()) + ") " +
                        "{ " + sb.toString() + " }",
                selectFile);


        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            public void run() {
                Editor editor = getEditor(e);

                PsiElement element = selectFile.findElementAt(editor.getCaretModel().getOffset());
                // 获取当前方法的PsiCodeBlock元素
                PsiElement codeBlock = element;
                while (!(codeBlock instanceof PsiMethod)) {
                    codeBlock = codeBlock.getParent();
                    codeBlock.getText();
                }

                selectFile.addAfter(method, codeBlock);
            }
        });


    }

    private Map<String, String> getFiledMap(PsiClass sourceClass) {
        PsiField[] sourceClassFields = sourceClass.getAllFields();
        Map<String, String> sourceClassFieldMap = new HashMap<>();
        for (PsiField sourceClassField : sourceClassFields) {
            if (!sourceClassField.hasModifierProperty("static") &&
                    !sourceClassField.hasModifierProperty("final")) {
                sourceClassFieldMap.put(sourceClassField.getName(), sourceClassField.getType().getCanonicalText());
            }
        }
        return sourceClassFieldMap;
    }

    @NotNull
    private PsiClass getSelectClass(@NotNull AnActionEvent e) {
        PsiElement data = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (!(data instanceof PsiClass)) {
            Messages.showErrorDialog("请选择目标类（转换后）", "生成错误！");
            throw new GenerateMarkDownException("请选择目标类（转换后）");
        }
        return (PsiClass) data;
    }

    @NotNull
    private PsiFile getSelectFile(@NotNull AnActionEvent e) {
        PsiFile data = e.getData(CommonDataKeys.PSI_FILE);
        if (data == null) {
            Messages.showErrorDialog("请选择目标类（转换后）", "生成错误！");
            throw new GenerateMarkDownException("请选择目标类（转换后）");
        }
        return data;
    }

    @NotNull
    private Editor getEditor(@NotNull AnActionEvent e) {
        Editor data = e.getData(CommonDataKeys.EDITOR);
        if (data == null) {
            Messages.showErrorDialog("请选择目标类（转换后）", "生成错误！");
            throw new GenerateMarkDownException("请选择目标类（转换后）");
        }
        return data;
    }


    public String lowerFirstCapse(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    public String higherFirstCapse(String str) {
        char[] chars = str.toCharArray();
        chars[0] -= 32;
        return String.valueOf(chars);
    }

}
