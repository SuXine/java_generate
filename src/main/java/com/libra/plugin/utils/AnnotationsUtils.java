package com.libra.plugin.utils;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import com.libra.plugin.model.bo.Param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnnotationsUtils {

    private static final String REQUEST_PARAM_QUALIFIED_NAME = "org.springframework.web.bind.annotation.RequestParam";

    private static final String REQUEST_BODY_QUALIFIED_NAME = "org.springframework.web.bind.annotation.RequestBody";

    public static Boolean isRequestParam(PsiParameter psiParameter) {
        PsiAnnotation[] annotations = psiParameter.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (REQUEST_PARAM_QUALIFIED_NAME.equals(annotation.getQualifiedName())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }


    public static Boolean isRequestBody(PsiParameter psiParameter) {
        PsiAnnotation[] annotations = psiParameter.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (REQUEST_BODY_QUALIFIED_NAME.equals(annotation.getQualifiedName())) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

}


