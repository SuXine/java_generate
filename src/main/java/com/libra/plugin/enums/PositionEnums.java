package com.libra.plugin.enums;

public enum PositionEnums {

    URL("url", "请求链接"),
    BODY("requestBody", "请求体（requestBody）"),
    NULL("", ""),
    ;

    private String key;

    private String desc;

    PositionEnums(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

    public String getKey() {
        return key;
    }

    public String getDesc() {
        return desc;
    }

    public static PositionEnums getByKey(String key) {
        for (PositionEnums appCodeEnums : values()) {
            if (appCodeEnums.getKey().equalsIgnoreCase(key))
                return appCodeEnums;
        }
        return null;
    }
}
