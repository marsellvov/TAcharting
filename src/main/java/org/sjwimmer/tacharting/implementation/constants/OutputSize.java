package org.sjwimmer.tacharting.implementation.constants;

public enum OutputSize {
    COMPACT("compact"),
    FULL("full");

    private final String os;

    OutputSize(String os){
        this.os = os;
    }

    public String getOutputSize(){
        return os;
    }
}
