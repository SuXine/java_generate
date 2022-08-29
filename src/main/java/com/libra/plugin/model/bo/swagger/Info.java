package com.libra.plugin.model.bo.swagger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Info {
    String description;
    String version;
    String title;
    Map<String, Object> contact;
    Map<String, Object> license;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Map<String, Object> getContact() {
        return contact;
    }

    public void setContact(Map<String, Object> contact) {
        this.contact = contact;
    }

    public Map<String, Object> getLicense() {
        return license;
    }

    public void setLicense(Map<String, Object> license) {
        this.license = license;
    }

    public Info(String description, String version, String title, String licenseName) {
        this.description = description;
        this.version = version;
        this.title = title;
        this.contact = new HashMap<>();
        this.license = new HashMap<>();
        license.put("name", licenseName);
    }
}
