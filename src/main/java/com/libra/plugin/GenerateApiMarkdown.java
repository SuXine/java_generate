package com.libra.plugin;

//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.alibaba.fastjson.serializer.SerializerFeature;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.javadoc.PsiDocComment;
import com.libra.plugin.asserts.Assert;
import com.libra.plugin.enums.MappingEnums;
import com.libra.plugin.exception.GenerateMarkDownException;
import com.libra.plugin.handler.DefaultParamHandler;
import com.libra.plugin.model.bo.Param;
import com.libra.plugin.utils.*;

import java.util.*;

import org.apache.commons.lang3.ObjectUtils;
import org.intellij.plugins.markdown.lang.MarkdownFileType;
import org.jetbrains.annotations.NotNull;

public class GenerateApiMarkdown extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent e) {
        StringBuilder mdsb = new StringBuilder();

        final PsiJavaFile javaFile = getSelectFile(e);
        // 获取选中的文件
        PsiClass firstRequestMappingClass = getFirstRequestMappingClass(javaFile);

        PsiMethod method = getSelectMethod(e);
        if (method == null) {
            PsiMethod[] methods = firstRequestMappingClass.getMethods();
            for (PsiMethod m : methods) {
                if (m.hasModifierProperty("public")) {
                    mdsb.append(getMethodMdString(firstRequestMappingClass, m));
                    mdsb.append("\n");
                }
            }
        } else {
            mdsb = getMethodMdString(firstRequestMappingClass, method);
        }


        final String text = mdsb.toString().replace("\r", "");

        WriteCommandAction.runWriteCommandAction(e.getProject(), new Runnable() {
            public void run() {
                PsiFileFactory mdFile = PsiFileFactory.getInstance(javaFile.getProject());
                PsiFile fileFromText = mdFile.createFileFromText("接口文档_" + DateUtils.getNowDateString() + ".md",
                        (FileType) MarkdownFileType.INSTANCE, text);

                PsiDirectory containingDirectory = javaFile.getContainingDirectory();
                containingDirectory.add((PsiElement) fileFromText);
            }
        });
    }

    private static final String REQUEST_MAPPING_QUALIFIED_NAME = "org.springframework.web.bind.annotation.RequestMapping";

    @NotNull
    private StringBuilder getMethodMdString(PsiClass firstRequestMappingClass, PsiMethod method) {
        String title = makeTitle(method);
        String desc = makeDesc(method);

        String mappingType = null;
        List<Param> requestParams = new ArrayList<>();
        List<Param> resultParams = new ArrayList<>();
        StringBuilder url = new StringBuilder();


        PsiAnnotation requestMappingAnnotation = firstRequestMappingClass.getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
        PsiAnnotationMemberValue classUrl = requestMappingAnnotation == null ? null : requestMappingAnnotation.findAttributeValue("value");
        url.append((null != classUrl && ObjectUtils.notEqual(classUrl.getText(), "{}")) ? classUrl.getText().replace("\"", "").trim() : "");


        PsiAnnotation methodTypeAnnotation = method.getAnnotation(MappingEnums.GET.getQualifiedName());
        if (null == methodTypeAnnotation) {
            methodTypeAnnotation = method.getAnnotation(MappingEnums.POST.getQualifiedName());
        }
        if (null == methodTypeAnnotation) {
            methodTypeAnnotation = method.getAnnotation(MappingEnums.DELETE.getQualifiedName());
        }
        if (null == methodTypeAnnotation) {
            methodTypeAnnotation = method.getAnnotation(MappingEnums.PUT.getQualifiedName());
        }

        PsiAnnotationMemberValue methodUrl = methodTypeAnnotation == null ? null : methodTypeAnnotation.findAttributeValue("value");
        url.append((null != methodUrl && ObjectUtils.notEqual(methodUrl.getText(), "{}")) ? methodUrl.getText().replace("\"", "").trim() : "");

        MappingEnums mapping = methodTypeAnnotation == null ? null : MappingEnums.getByQualifiedName(methodTypeAnnotation.getQualifiedName());

        mappingType = mapping == null ? null : mapping.getMappingType();

        DefaultParamHandler defaultParamHandler = new DefaultParamHandler();
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            defaultParamHandler.handleParam(new ParamsTypeRepeatDetector(), requestParams, url, parameter, mapping);
        }

        defaultParamHandler.handleResultParam(new ParamsTypeRepeatDetector(), resultParams, method.getReturnType(), null);

        StringBuilder mdsb = new StringBuilder();
        mdsb.append("# ").append(title).append("\n");
        mdsb.append("> ").append(desc).append("\n");
        mdsb.append("\n");

        if (mappingType != null) {
            mdsb.append("**请求类型**").append("\n");
            mdsb.append("- ").append(mappingType).append("\n");
            mdsb.append("\n");
        }

        if (!url.toString().equals("")) {
            mdsb.append("**请求链接**").append("\n");
            mdsb.append("- ").append("`").append(url).append("`").append("\n");
            mdsb.append("\n");
        }


        if (requestParams.size() > 0) {
            mdsb.append("**请求参数<表格形式>**").append("\n");
            mdsb.append("\n");
            mdsb.append("| 参数名        | 类型    | 说明     |").append("\n");
            mdsb.append("| :------------ | :--- | :------ |").append("\n");
            fileParams(requestParams, mdsb);
            mdsb.append("\n");

            mdsb.append("**请求参数<JSON格式>**").append("\n");
            mdsb.append("\n");
            Object o = makeParamExampleStr(requestParams, checkParamIsList(mapping, parameters));
            ObjectMapper mapper = new ObjectMapper();
            try {
                mdsb.append("```json").append("\n");
                mdsb.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o)).append("\n");
                mdsb.append("```");
                mdsb.append("\n");
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON格式化错误！");
            }

        }


        if (resultParams.size() > 0) {
            mdsb.append("**返回结果<表格形式>**").append("\n");
            mdsb.append("\n");
            mdsb.append("| 参数名        | 类型    | 说明     |").append("\n");
            mdsb.append("| :------------ | :--- | :------ |").append("\n");
            fileParams(resultParams, mdsb);
            mdsb.append("\n");

            mdsb.append("**返回结果<JSON格式>**").append("\n");
            mdsb.append("\n");

            Object o = makeParamExampleStr(resultParams, PsiTypeUtils.isList(method.getReturnType()));
            ObjectMapper mapper = new ObjectMapper();
            try {
                mdsb.append("```json").append("\n");
                mdsb.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o)).append("\n");
                mdsb.append("```");
                mdsb.append("\n");
            } catch (JsonProcessingException e) {
                throw new RuntimeException("JSON格式化错误！");
            }
        }
        return mdsb;
    }

    private Boolean checkParamIsList(MappingEnums mapping, PsiParameter[] parameters) {
        Boolean result = Boolean.FALSE;
        if (parameters.length == 1) {
            return PsiTypeUtils.isList(parameters[0].getType());
        }
        for (PsiParameter parameter : parameters) {
            if (AnnotationsUtils.isRequestBody(parameter)) {
                return PsiTypeUtils.isList(parameter.getType());
            }
        }
        return result;
    }


    private PsiClass getFirstRequestMappingClass(PsiJavaFile javaFile) {
        PsiClass[] classes = javaFile.getClasses();
        for (PsiClass item : classes) {
            if (item.hasModifierProperty("public")) {
                return item;
            }
        }
        return null;
    }


    private PsiMethod getSelectMethod(@NotNull AnActionEvent e) {
        PsiElement element = (PsiElement) e.getData(PlatformDataKeys.PSI_ELEMENT);
        if (!(element instanceof PsiMethod)) {
            return null;
        }
        return (PsiMethod) element;
    }


    @NotNull
    private PsiJavaFile getSelectFile(@NotNull AnActionEvent e) {
        PsiFile file = (PsiFile) e.getData(CommonDataKeys.PSI_FILE);
        if (!(file instanceof PsiJavaFile)) {
            Messages.showErrorDialog("请选择java文件", "生成错误！");
            throw new GenerateMarkDownException("请选择java文件");
        }
        return (PsiJavaFile) file;
    }


    private String makeTitle(PsiMethod method) {
        Assert.isNotNull(method, "请选中方法！");
        PsiDocComment docComment = method.getDocComment();
        String docCommentText = null == docComment ? "" : docComment.getText();
        return DocCommentUtils.splitDocCommentText(docCommentText);
    }


    private String makeDesc(PsiMethod method) {
        Assert.isNotNull(method, "请选中方法！");
        PsiDocComment docComment = method.getDocComment();
        String docCommentText = null == docComment ? "" : docComment.getText();
        docCommentText = docCommentText.replace("*", "");
        docCommentText = docCommentText.replace("/", "");
        docCommentText = docCommentText.replace("\n", " ");
        return docCommentText.trim();
    }

    private void fileParams(List<Param> params, StringBuilder mdsb) {
        params.forEach(item -> {
            mdsb.append("|").append(item.getName()).append("|").append(item.getType()).append("|").append(item.getDesc()).append("|").append("\n");
            if (item.getChildren().size() > 1)
                fileParams(item.getChildren(), mdsb);
        });
    }

    private Object makeParamExampleStr(List<Param> paramList, Boolean isList) {
        Map<String, Object> map = new HashMap<>();
        for (Param param : paramList) {
            if (null != param.getChildren() && param.getChildren().size() > 0) {
                // 如果是集合
                Boolean isArray = Boolean.FALSE;
                for (String s : PsiTypeUtils.COLLECTION_PRESENTABLE_TEXT) {
                    String type = param.getType();
                    if (type.contains("<")) {
                        type = type.substring(0, type.indexOf("<"));
                    }
                    if (type.contains(s)) {
                        isArray = Boolean.TRUE;
                        List<Map<String, Object>> array = new ArrayList<>();
                        array.add((Map<String, Object>) makeParamExampleStr(param.getChildren(), Boolean.FALSE));
                        map.put(param.getName().replace("-", ""), array);
                        break;
                    }
                }
                if (!isArray) {
                    if (param.getName() != null) {
                        map.put(param.getName().replace("-", ""), makeParamExampleStr(param.getChildren(), Boolean.FALSE));
                    }
                }
            } else {
                if (param.getName() != null) {
                    map.put(param.getName().replace("-", ""), getParamExample(param));
                }
            }
        }
        if (isList) {
            List<Map<String, Object>> array = new ArrayList<>();
            array.add(map);
            return array;
        }
        return map;
    }

    private Object getParamExample(Param param) {

        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.BOOLEAN_TEXT)) {
            return Boolean.TRUE;
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.BYTE_TEXT)) {
            return "";
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.CHARACTER_TEXT)) {
            return "";
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.SHORT_TEXT)) {
            return 0;
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.LONG_TEXT)) {
            return 0L;
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.FLOAT_TEXT)) {
            return 0.0F;
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.DOUBLE_TEXT)) {
            return 0.0D;
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.STRING_TEXT)) {
            return "";
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.MAP_TEXT)) {
            return new HashMap<>();
        }
        if (PsiTypeUtils.isType(param.getType(), PsiTypeUtils.DATE_TEXT)) {
            return new Date();
        }

        return "";
    }

}


