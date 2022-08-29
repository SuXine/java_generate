package com.libra.plugin.model.bo.swagger;

import java.util.*;

public class UrlMap {
    List<String> tags;
    String summary;
    String operationId;
    List<String> produces;
    List<Parameter> parameters;
    Map<String, Response> responses;


    public static class Response {
        String description;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Response(String description) {
            this.description = description;
        }
    }

    public static class Parameter {
        String name;
        String in;
        String description;
        Boolean required;
        String type;
        String format;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIn() {
            return in;
        }

        public void setIn(String in) {
            this.in = in;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public List<String> getProduces() {
        return produces;
    }

    public void setProduces(List<String> produces) {
        this.produces = produces;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public void putResponses(String key,Response response) {
        this.responses.put(key, response);
    }

    public UrlMap() {
        this.tags = new ArrayList<>();
        this.operationId = "";
        this.produces = List.of("*/*");
        this.responses = new HashMap<>();
    }
}
