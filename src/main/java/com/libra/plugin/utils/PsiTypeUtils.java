package com.libra.plugin.utils;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;

import java.util.*;

import org.apache.commons.lang.ObjectUtils;

public class PsiTypeUtils {


    public static final List<String> BOOLEAN_TEXT = Arrays.asList("Boolean", "java.lang.Boolean", "boolean");
    public static final List<String> BYTE_TEXT = Arrays.asList("Byte", "java.lang.Byte", "byte");
    public static final List<String> CHARACTER_TEXT = Arrays.asList("Character", "java.lang.Character", "char");
    public static final List<String> SHORT_TEXT = Arrays.asList("Short", "java.lang.Short", "short");
    public static final List<String> LONG_TEXT = Arrays.asList("Long", "java.lang.Long", "long");
    public static final List<String> FLOAT_TEXT = Arrays.asList("Float", "java.lang.Float", "float");
    public static final List<String> DOUBLE_TEXT = Arrays.asList("Double", "java.lang.Double", "double");
    public static final List<String> STRING_TEXT = Arrays.asList("String", "java.lang.String");
    public static final List<String> INTEGER_TEXT = Arrays.asList("Integer", "java.lang.Integer","int");
    public static final List<String> MAP_TEXT = Arrays.asList("Map", "java.util.Map", "java.util.TreeMap", "java.util.HashMap");
    public static final List<String> DATE_TEXT = Arrays.asList("Date", "java.util.Date", "java.sql.Timestamp", "java.sql.Date");
    public static final List<String> ENUMS_TEXT = Arrays.asList("java.lang.Enum");

    public static final List<String> PRIMITIVE_PRESENTABLE_TEXT = new ArrayList<>();

    static {
        PRIMITIVE_PRESENTABLE_TEXT.addAll(BOOLEAN_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(BYTE_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(CHARACTER_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(SHORT_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(LONG_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(FLOAT_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(DOUBLE_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(STRING_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(MAP_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(DATE_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(ENUMS_TEXT);
        PRIMITIVE_PRESENTABLE_TEXT.addAll(INTEGER_TEXT);
    }

    public static final List<String> COLLECTION_PRESENTABLE_TEXT = Arrays.asList("List", "java.util.List", "java.util.ArrayList", "java.util.concurrent.CopyOnWriteArrayList", "java.util.Vector", "java.util.Set", "java.util.HashSet", "java.util.concurrent.CopyOnWriteArraySet", "java.util.TreeSet");


    private PsiTypeUtils() {
        throw new RuntimeException("工具类不能初始化！");
    }


    public static Boolean isPrimitive(PsiType type) {
        if (type == null) {
            throw new RuntimeException("type 类型不能为空！");
        }

        // 如果是枚举直接展示
        PsiType[] superTypes = type.getSuperTypes();
        for (PsiType superType : superTypes) {
            if (superType.getCanonicalText().startsWith("java.lang.Enum")) {
                return Boolean.TRUE;
            }
        }

        for (String s : PRIMITIVE_PRESENTABLE_TEXT) {
            if (type.getCanonicalText().startsWith(s)) {
                return Boolean.TRUE;
            }
        }

        return Boolean.valueOf(PRIMITIVE_PRESENTABLE_TEXT.contains(type.getCanonicalText()));
    }

    public static Boolean isType(String type,List<String> typeList) {
        if (type == null) {
            throw new RuntimeException("type 类型不能为空！");
        }
        for (String s : typeList) {
            if (type.startsWith(s)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.valueOf(typeList.contains(type));
    }


    public static Boolean isResolveGenericsObject(PsiType type) {
        if (type == null) {
            throw new RuntimeException("type 类型不能为空！");
        }


        for (String s : COLLECTION_PRESENTABLE_TEXT) {
            String canonicalText = type.getCanonicalText();
            if (canonicalText.contains("<")) {
                canonicalText=canonicalText.substring(0, canonicalText.indexOf("<"));
            }
            if (canonicalText.contains(s)) {
                return Boolean.TRUE;
            }
        }

        if (type instanceof PsiClassType) {
            PsiClassType psiClassType = (PsiClassType) type;
            PsiClassType.ClassResolveResult classResolveResult = psiClassType.resolveGenerics();
            Map<PsiTypeParameter, PsiType> map = psiClassType.resolveGenerics().getSubstitutor().getSubstitutionMap();
            return Boolean.valueOf((map != null && map.size() > 0));
        }
        return Boolean.FALSE;
    }


    public static Boolean isList(PsiType type) {
        if (type == null) {
            throw new RuntimeException("type 类型不能为空！");
        }

        for (String s : COLLECTION_PRESENTABLE_TEXT) {
            String canonicalText = type.getCanonicalText();
            if (s.startsWith(canonicalText)) {
                return Boolean.TRUE;
            }
//            if (canonicalText.contains("<")) {
//                canonicalText=canonicalText.substring(0, canonicalText.indexOf("<"));
//            }
//            if (canonicalText.contains(s)) {
//            }
        }

        return Boolean.FALSE;
    }

    public static Map<PsiTypeParameter, PsiType> initGenericMap(PsiType psiType) {
        Map<PsiTypeParameter, PsiType> map = new HashMap<>();

        if (psiType == null) {
            return map;
        }

        if (!(psiType instanceof PsiClassType)) {
            return map;
        }

        if (isPrimitive(psiType).booleanValue()) {
            return map;
        }

        PsiClassType psiClassType = (PsiClassType) psiType;

        map.putAll(psiClassType.resolveGenerics().getSubstitutor().getSubstitutionMap());

        PsiClass resolve = psiClassType.resolve();
        if (resolve != null) {
            PsiTypeParameter[] typeParameters = psiClassType.resolve().getTypeParameters();
            for (PsiTypeParameter typeParameter : typeParameters) {
                PsiType result = map.get(typeParameter);
                if (ObjectUtils.equals(result.getCanonicalText(), "java.lang.Object")) {
                    break;
                }
                map.putAll(initGenericMap(result));
            }
        }


        PsiType[] superTypes = psiClassType.getSuperTypes();
        for (PsiType superType : superTypes) {
            if (ObjectUtils.equals(superType.getCanonicalText(), "java.lang.Object")) {
                break;
            }
            if (superType.getCanonicalText().startsWith("java.util.Collection")) {
                break;
            }
            if (superType.getCanonicalText().startsWith("java.lang.Comparable")) {
                break;
            }
            if (superType.getCanonicalText().startsWith("java.lang.Enum")) {
                break;
            }
            if (superType instanceof PsiClassType) {
                PsiClassType supperPsiClassType = (PsiClassType) superType;
                map.putAll(supperPsiClassType.resolveGenerics().getSubstitutor().getSubstitutionMap());
            }
            map.putAll(initGenericMap(superType));
        }

        if (psiClassType.resolve() instanceof PsiTypeParameter && map.containsKey(psiClassType.resolve())) {
            PsiType result = map.get(psiClassType.resolve());
            if (isList(result).booleanValue()) {
                map.putAll(initGenericMap(result));
            }
        }



        return map;
    }
}


