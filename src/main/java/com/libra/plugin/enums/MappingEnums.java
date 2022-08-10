package com.libra.plugin.enums;

public enum MappingEnums {
    POST("org.springframework.web.bind.annotation.PostMapping", "POST"),
    PUT("org.springframework.web.bind.annotation.PutMapping", "PUT"),
    DELETE("org.springframework.web.bind.annotation.DeleteMapping", "DELETE"),
    GET("org.springframework.web.bind.annotation.GetMapping", "GET");

    private String qualifiedName;

    private String mappingType;

    MappingEnums(String qualifiedName, String mappingType) {
        this.qualifiedName = qualifiedName;
        this.mappingType = mappingType;
    }

    public String getQualifiedName() {
        return this.qualifiedName;
    }

    public String getMappingType() {
        return this.mappingType;
    }

    public static MappingEnums getByQualifiedName(String qualifiedName) {
        for (MappingEnums appCodeEnums : values()) {
            if (appCodeEnums.getQualifiedName().equalsIgnoreCase(qualifiedName))
                return appCodeEnums;
        }
        return null;
    }
}
