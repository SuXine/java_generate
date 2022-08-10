package com.libra.plugin.handler;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.libra.plugin.enums.MappingEnums;
import com.libra.plugin.model.bo.Param;
import com.libra.plugin.utils.AnnotationsUtils;
import com.libra.plugin.utils.PsiTypeUtils;
import com.libra.plugin.utils.ParamsTypeRepeatDetector;

import java.util.List;
import java.util.Map;

public class DefaultParamHandler extends ParamHandler {

    @Override
    public void handleResultParam(ParamsTypeRepeatDetector repeatDetector, List<Param> params, PsiType psiType,String paramName) {
        if (PsiTypeUtils.isPrimitive(psiType)) {
            Param param = new Param();
            param.setName(paramName);
            param.setDesc(paramName != null ? "" : "直接返回该类型值，没有KEY");
            param.setType(psiType.getPresentableText());
            params.add(param);
        } else if (PsiTypeUtils.isList(psiType)) {
            Map<PsiTypeParameter, PsiType> map = PsiTypeUtils.initGenericMap(psiType);
            PsiType wrapType = getWrapType(psiType, map);
            PsiClass wrapClass = PsiUtil.resolveClassInType(wrapType);
            if (wrapClass != null) {
                PsiField[] fields = wrapClass.getAllFields();
                for (PsiField field : fields) {
                    Param param = makeParam(field, psiType, 0, repeatDetector, map);
                    if (param != null) {
                        params.add(param);
                    }
                }
            }
        } else {
            PsiClass clazz = PsiUtil.resolveClassInType(psiType);
            if (clazz != null) {
                PsiField[] fields = clazz.getAllFields();
                Map<PsiTypeParameter, PsiType> map = PsiTypeUtils.initGenericMap(psiType);
                for (PsiField field : fields) {
                    Param param = makeParam(field, psiType, 0, repeatDetector, map);
                    if (param != null) {
                        params.add(param);
                    }
                }
            }
        }

    }


    @Override
    public void handleParam(ParamsTypeRepeatDetector repeatDetector, List<Param> params, StringBuilder url, PsiParameter parameter, MappingEnums mapping) {
        // 如果方法没有注解，按照表格方式
        if(mapping == null) {
            handleResultParam(repeatDetector, params, parameter.getType(),parameter.getNameIdentifier().getText());
            return;
        }

        if (AnnotationsUtils.isRequestParam(parameter)) {
            handleRequestParam(url, parameter);
        } else if (AnnotationsUtils.isRequestBody(parameter)) {
            handleResultParam(repeatDetector, params, parameter.getType(),parameter.getNameIdentifier().getText());
        } else {
            if (mapping == MappingEnums.GET) {
                handleRequestParam(url, parameter);
                return;
            }
            handleResultParam(repeatDetector, params, parameter.getType(),parameter.getNameIdentifier().getText());
        }
    }

    public static void handleRequestParam(StringBuilder url, PsiParameter parameter) {
        if (PsiTypeUtils.isPrimitive(parameter.getType())) {
            if (!url.toString().contains("?")) {
                url.append("?");
            }
            if (url.toString().endsWith("?")) {
                url.append(parameter.getName());
                url.append("=xxx");
            } else {
                url.append("&");
                url.append(parameter.getName());
                url.append("=xxx");
            }
        } else {
            PsiType type = parameter.getType();
            PsiClass paramsClass = PsiUtil.resolveClassInClassTypeOnly(type);
            PsiField[] fields = paramsClass.getAllFields();
            for (PsiField field : fields) {
                if (!field.hasModifierProperty("static") &&
                        !field.hasModifierProperty("final")) {
                    if (!url.toString().contains("?")) {
                        url.append("?");
                    }
                    url.append(field.getNameIdentifier().getText());
                    url.append("=xxx&");
                }
            }
        }
    }

}
