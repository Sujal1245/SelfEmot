package com.Sujal_Industries.SelfEmot;

public class Result {
    private String name;
    private String confidence;

    public Result(String name, String confidence) {
        this.name = name;
        this.confidence = confidence;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }
}
