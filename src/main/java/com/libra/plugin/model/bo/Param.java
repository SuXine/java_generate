package com.libra.plugin.model.bo;

import java.util.ArrayList;
import java.util.List;

public class Param {
    private String name;

    private String type;

    private String desc;

    private String required;

    /**
     * 参数位置
     */
    private String position;

    private final List<Param> children = new ArrayList<>();

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Param> addChildren(Param child) {
        this.children.add(child);
        return this.children;
    }

    public List<Param> getChildren() {
        return this.children;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }
}
