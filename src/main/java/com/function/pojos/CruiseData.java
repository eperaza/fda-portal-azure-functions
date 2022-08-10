package com.function.pojos;

public class CruiseData {
    
    protected int altitude;
    protected int staticAirTemperature;
    protected int grossWeight;
    protected int costIndex;
    protected String airline;
    protected String tail;

    public CruiseData() {
    }

    public int getAltitude() {
        return this.altitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    public int getStaticAirTemperature() {
        return this.staticAirTemperature;
    }

    public void setStaticAirTemperature(int staticAirTemperature) {
        this.staticAirTemperature = staticAirTemperature;
    }

    public int getGrossWeight() {
        return this.grossWeight;
    }

    public void setGrossWeight(int grossWeight) {
        this.grossWeight = grossWeight;
    }

    public int getCostIndex() {
        return this.costIndex;
    }

    public void setCostIndex(int costIndex) {
        this.costIndex = costIndex;
    }

    public String getAirline() {
        return this.airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public String getTail() {
        return this.tail;
    }

    public void setTail(String tail) {
        this.tail = tail;
    }

    

}
