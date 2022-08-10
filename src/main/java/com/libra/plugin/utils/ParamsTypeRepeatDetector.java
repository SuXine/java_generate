package com.libra.plugin.utils;

import java.util.HashMap;
import java.util.Map;

public class ParamsTypeRepeatDetector {

    private Map<String, Integer> paramTypeNumber = new HashMap<>();

    private static final Integer number = 3;

    /**
     * 检验 paramType 的出现次数 如果小于number返回TURE
     * @param paramType
     * @return
     */
    public Boolean check(String paramType) {
        Integer typeNumber = paramTypeNumber.get(paramType);
        if (typeNumber == null || typeNumber <= number) {
            paramTypeNumber.put(paramType, typeNumber == null ? 1 : typeNumber + 1);
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
