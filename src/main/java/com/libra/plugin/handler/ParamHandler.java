package com.libra.plugin.handler;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.libra.plugin.enums.MappingEnums;
import com.libra.plugin.enums.PositionEnums;
import com.libra.plugin.model.bo.Param;
import com.libra.plugin.utils.DocCommentUtils;
import com.libra.plugin.utils.PsiTypeUtils;
import com.libra.plugin.utils.ParamsTypeRepeatDetector;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class ParamHandler {

    public abstract void handleResultParam(ParamsTypeRepeatDetector repeatDetector, List<Param> params, PsiType psiType, PsiParameter parameter, PositionEnums positionEnums);

    public abstract void handleParam(ParamsTypeRepeatDetector repeatDetector, List<Param> params, StringBuilder url, PsiParameter parameter, MappingEnums mapping);

    protected static Param makeParam(PsiField field, PsiType psiType, Integer depth, ParamsTypeRepeatDetector repeatDetector, Map<PsiTypeParameter, PsiType> map, PositionEnums positionEnums) {
        if (field.hasModifierProperty("static") || field
                .hasModifierProperty("final")) {
            return null;
        }

        String namePrefix = "";
        for (int i = 0; i < depth; i++) {
            namePrefix = namePrefix + "--";
        }

        String name;
        String desc;
        String required;
        PsiAnnotation annotation = field.getAnnotation("io.swagger.annotations.ApiParam");
        if (null != annotation) {
            PsiAnnotationMemberValue nameValue = annotation.findAttributeValue("name");
            name = (null != nameValue && ObjectUtils.notEqual(nameValue.getText(), "\"\"")) ? nameValue.getText().replace("\"", "").trim() : field.getName();
            PsiAnnotationMemberValue valueValue = annotation.findAttributeValue("value");
            desc = (null != valueValue && ObjectUtils.notEqual(valueValue.getText(), "{}")) ? valueValue.getText().replace("\"", "").trim() : "";
            PsiAnnotationMemberValue requiredValue = annotation.findAttributeValue("required");
            required = (null != requiredValue && ObjectUtils.notEqual(requiredValue.getText(), "{}")) ? requiredValue.getText().replace("\"", "").trim() : field.getName();
        } else {
            name = field.getName();
            desc = (field.getDocComment() != null) ? DocCommentUtils.splitDocCommentText(field.getDocComment().getText()) : "";
            required = Boolean.FALSE.toString();
        }

        Param param = new Param();

        // 如果是直接展示类型
        if (PsiTypeUtils.isPrimitive(field.getType())) {
            param.setName(namePrefix + name);
            param.setDesc(desc);
            param.setRequired(required);
            param.setPosition(positionEnums.getDesc());
            param.setType(field.getType().getPresentableText());
        }
        // 如果是被包装类型
        else if (field.getType() instanceof PsiClassType && ((PsiClassType) field
                .getType()).resolve() instanceof PsiTypeParameter && map
                .containsKey(((PsiClassType) field.getType()).resolve())) {
            // 获取包装类型
            PsiType wrapType = getWrapType(field, psiType, map);
            // 校验该类型出现次数是否 大于设定值 如果大于设置为重复对象
            if (repeatDetector.check(wrapType.getCanonicalText())) {
                param.setName(namePrefix + name);
                param.setDesc(desc);
                param.setType(field.getType().getPresentableText());
                param.setRequired(required);
                param.setPosition(positionEnums.getDesc());

                PsiClass wrapClass = PsiUtil.resolveClassInType(wrapType);



                if (wrapClass != null) {
                    PsiField[] wrapFields = wrapClass.getAllFields();
                    for (PsiField wrapField : wrapFields) {
                        // 填充子字段
                        map.putAll(PsiTypeUtils.initGenericMap(wrapField.getType()));
                        Param childParam = makeParam(wrapField, wrapType, depth + 1, repeatDetector, map, positionEnums);
                        if (childParam != null) {
                            param.addChildren(childParam);
                        }
                    }
                }
            } else {
                param.setName(namePrefix + name);
                param.setDesc("重复对象");
                param.setRequired(required);
                param.setPosition(positionEnums.getDesc());
                param.setType(field.getType().getPresentableText());
            }
        }
        // 如果是包装类型
        else if (PsiTypeUtils.isResolveGenericsObject(field.getType())) {
            // 获取包装类型
            PsiType wrapType = getWrapType(field, psiType, map);

            // 校验该类型出现次数是否 大于设定值 如果大于设置为重复对象
            if (repeatDetector.check(wrapType.getCanonicalText())) {
                param.setName(namePrefix + name);
                param.setDesc(desc);
                param.setRequired(required);
                param.setPosition(positionEnums.getDesc());
                param.setType(field.getType().getPresentableText());

                PsiClass wrapClass = PsiUtil.resolveClassInType(wrapType);

                if (wrapClass != null) {
                    PsiField[] wrapFields = wrapClass.getAllFields();
                    for (PsiField wrapField : wrapFields) {
                        // 填充子字段
                        map.putAll(PsiTypeUtils.initGenericMap(wrapField.getType()));
                        Param childParam = makeParam(wrapField, wrapType, depth + 1, repeatDetector, map, positionEnums);
                        if (childParam != null) {
                            param.addChildren(childParam);
                        }
                    }
                }

            } else {
                param.setName(namePrefix + name);
                param.setDesc("重复对象");
                param.setRequired(required);
                param.setPosition(positionEnums.getDesc());
                param.setType(field.getType().getPresentableText());
            }

        } else {
            PsiClass clazz = PsiUtil.resolveClassInType(field.getType());
            param.setName(namePrefix + name);
            param.setDesc(desc);
            param.setRequired(required);
            param.setPosition(positionEnums.getDesc());
            param.setType(field.getType().getPresentableText());
            if (clazz != null) {
                PsiField[] wrapFields = clazz.getAllFields();
                for (PsiField wrapField : wrapFields) {
                    map.putAll(PsiTypeUtils.initGenericMap(wrapField.getType()));
                    Param childParam = makeParam(wrapField, field.getType(), depth + 1, repeatDetector, map, positionEnums);
                    if (childParam != null) {
                        param.addChildren(childParam);
                    }
                }
            }
        }

        return param;
    }

    /**
     * 获取被包装的对象类型，如果psiType获取不到 根据 field.getType()获取
     *
     * @param field
     * @param psiType
     * @param map
     * @return
     */
    @Nullable
    protected static PsiType getWrapType(PsiField field, PsiType psiType, Map<PsiTypeParameter, PsiType> map) {
        PsiType wrapType = getWrapType(psiType, map);

        PsiClass wrapClass = PsiUtil.resolveClassInType(wrapType);

        if (wrapType != null && wrapClass != null && wrapClass.getAllFields().length > 0) {
            return wrapType;
        }

        map.putAll(PsiTypeUtils.initGenericMap(field.getType()));
        wrapType = getWrapType(field.getType(), map);

        PsiType result = wrapType;
        while (PsiTypeUtils.isResolveGenericsObject(result)) {
            result = getWrapType(field,result,map);
        }

        return result;
    }

    protected static PsiType getWrapType(PsiType psiType, Map<PsiTypeParameter, PsiType> map) {
        PsiClassType psiClassType = (PsiClassType) psiType;

        PsiType result = null;
        if (psiClassType.resolve() instanceof PsiTypeParameter && map.containsKey(psiClassType.resolve())) {
            result = map.get(psiClassType.resolve());
        }

        if (result == null) {
            if (psiClassType.resolve() != null) {

                PsiTypeParameter[] typeParameters = psiClassType.resolve().getTypeParameters();
                if (typeParameters.length > 0) {
                    result = map.get(typeParameters[0]);
                }
            }
        }

        if (result == null) {
            PsiType[] superTypes = psiClassType.getSuperTypes();
            for (PsiType superType : superTypes) {
                result = getWrapType(superType, map);
                if (superType == null) {
                    break;
                }
            }
        }

        if (result != null) {
            if (PsiTypeUtils.isList(result)) {
                map.putAll(PsiTypeUtils.initGenericMap(result));
                return getWrapType(result, map);
            }
            return result;
        }


        return null;
    }

}
