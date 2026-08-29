package org.passerbya;

import java.util.List;

public class PluginManifest {
    private String id;
    private String version;
    private String name;
    private String description;
    private String type;
    private String executable;
    private List<PluginParameter> parameters;
    private boolean isExecutable;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExecutable() {
        return executable;
    }

    public void setExecutableFlag(String executable) {
        this.executable = executable;
    }

    public List<PluginParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<PluginParameter> parameters) {
        this.parameters = parameters;
    }

    public boolean isExecutable() {
        return isExecutable;
    }

//    public void setExecutableFlag(boolean executable) {
//        isExecutable = executable;
//    }
}