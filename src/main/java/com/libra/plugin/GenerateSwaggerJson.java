package com.libra.plugin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.libra.plugin.asserts.Assert;
import com.libra.plugin.enums.MappingEnums;
import com.libra.plugin.exception.GenerateMarkDownException;
import com.libra.plugin.handler.DefaultParamHandler;
import com.libra.plugin.model.bo.Param;
import com.libra.plugin.model.bo.swagger.Info;
import com.libra.plugin.model.bo.swagger.UrlMap;
import com.libra.plugin.utils.*;
import org.apache.commons.lang3.ObjectUtils;
import org.intellij.plugins.markdown.lang.MarkdownFileType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class GenerateSwaggerJson extends AnAction {
    public void actionPerformed(@NotNull AnActionEvent e) {

        final PsiJavaFile javaFile = getSelectFile(e);
        // 获取选中的文件
        PsiClass firstRequestMappingClass = getFirstRequestMappingClass(javaFile);

        PsiMethod method = getSelectMethod(e);
        if (method == null) {
            throw new GenerateMarkDownException("请选择方法！");
        }
        String value = getMethodMdString(firstRequestMappingClass, method);

        final String text = value.replace("\r", "");

        WriteCommandAction.runWriteCommandAction(e.getProject(), new Runnable() {
            public void run() {
                PsiFileFactory mdFile = PsiFileFactory.getInstance(javaFile.getProject());
                PsiFile fileFromText = mdFile.createFileFromText("swagger_json_" + DateUtils.getNowDateString() + ".json",
                        (FileType) MarkdownFileType.INSTANCE, text);

                PsiDirectory containingDirectory = javaFile.getContainingDirectory();
                containingDirectory.add((PsiElement) fileFromText);
            }
        });
    }


    @NotNull
    private String getMethodMdString(PsiClass firstRequestMappingClass, PsiMethod method) {

        Map<String, Object> jsonMap = new HashMap<>();
        // swagger 版本
        jsonMap.put("swagger", "2.0");

        String desc = makeDesc(method);
        String title = makeTitle(method);

        Info info = new Info(desc, "1.0.0", title, title);

        jsonMap.put("info", info);
        // 主机地址
        jsonMap.put("host", "127.0.0.1:8086");
        // 前缀
        jsonMap.put("basePath", "/wb");
        // 分类
        jsonMap.put("tags", new ArrayList<>());

        List<Param> requestParams = new ArrayList<>();

        // 获取URL 和 请求方式
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

        // 构建接口对象
        Map<String, Object> paths = new HashMap<>();


        Map<String, Object> urlMap = new HashMap<>();

        DefaultParamHandler defaultParamHandler = new DefaultParamHandler();
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            defaultParamHandler.handleParam(new ParamsTypeRepeatDetector(), requestParams, url, parameter, null);
        }

        UrlMap urlMapObject = new UrlMap();
        urlMapObject.setSummary(desc);
        urlMapObject.setParameters(requestParams.stream().map(item -> {
            UrlMap.Parameter parameter = new UrlMap.Parameter();
            parameter.setName(item.getName());
            parameter.setType(item.getType());
            parameter.setIn("query");
            parameter.setRequired(Boolean.FALSE);
            parameter.setDescription(item.getDesc());
            parameter.setFormat("");
            return parameter;
        }).collect(Collectors.toList()));
        urlMapObject.putResponses("200", new UrlMap.Response("OK"));
        urlMapObject.putResponses("401", new UrlMap.Response("未登录，没有权限"));

        urlMap.put(mapping.getMappingType().toLowerCase(), urlMapObject);
        paths.put(url.toString(), urlMap);
        jsonMap.put("paths",paths);
        // -----------------------------------------------------------------------------


        List<Param> resultParams = new ArrayList<>();

//        defaultParamHandler.handleResultParam(new ParamsTypeRepeatDetector(), resultParams, method.getReturnType(), null);


//        StringBuilder mdsb = new StringBuilder();
//        mdsb.append("# ").append(title).append("\n");
//        mdsb.append("> ").append(desc).append("\n");
//        mdsb.append("\n");
//
//        if (mappingType != null) {
//            mdsb.append("**请求类型**").append("\n");
//            mdsb.append("- ").append(mappingType).append("\n");
//            mdsb.append("\n");
//        }
//
//        if (!url.toString().equals("")) {
//            mdsb.append("**请求链接**").append("\n");
//            mdsb.append("- ").append("`").append(url).append("`").append("\n");
//            mdsb.append("\n");
//        }
//
//
//        if (requestParams.size() > 0) {
//            mdsb.append("**请求参数<表格形式>**").append("\n");
//            mdsb.append("\n");
//            mdsb.append("| 参数名        | 类型    | 说明     |").append("\n");
//            mdsb.append("| :------------ | :--- | :------ |").append("\n");
//            fileParams(requestParams, mdsb);
//            mdsb.append("\n");
//
//            mdsb.append("**请求参数<JSON格式>**").append("\n");
//            mdsb.append("\n");
//            Object o = makeParamExampleStr(requestParams, checkParamIsList(mapping, parameters));
//            ObjectMapper mapper = new ObjectMapper();
//            try {
//                mdsb.append("```json").append("\n");
//                mdsb.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o)).append("\n");
//                mdsb.append("```");
//                mdsb.append("\n");
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException("JSON格式化错误！");
//            }
//
//        }
//
//
//        if (resultParams.size() > 0) {
//            mdsb.append("**返回结果<表格形式>**").append("\n");
//            mdsb.append("\n");
//            mdsb.append("| 参数名        | 类型    | 说明     |").append("\n");
//            mdsb.append("| :------------ | :--- | :------ |").append("\n");
//            fileParams(resultParams, mdsb);
//            mdsb.append("\n");
//
//            mdsb.append("**返回结果<JSON格式>**").append("\n");
//            mdsb.append("\n");
//
//            Object o = makeParamExampleStr(resultParams, PsiTypeUtils.isList(method.getReturnType()));
//            ObjectMapper mapper = new ObjectMapper();
//            try {
//                mdsb.append("```json").append("\n");
//                mdsb.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o)).append("\n");
//                mdsb.append("```");
//                mdsb.append("\n");
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException("JSON格式化错误！");
//            }
//        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON格式化错误！");
        }
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


