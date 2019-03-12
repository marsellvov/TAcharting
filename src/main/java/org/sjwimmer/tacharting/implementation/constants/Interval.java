package org.sjwimmer.tacharting.implementation.constants;

public enum Interval {

    ONE_MINUTE("1min"),
    FIVE_MINUTE("5min"),
    TEN_MINUTE("15min"),
    THIRTY_MINUTE("30min"),
    SIXTY_MINUTE("60min");

    private final String interval;

    Interval(String interval){
        this.interval = interval;
    }

    public String getInterval(){
        return interval;
    }


}
