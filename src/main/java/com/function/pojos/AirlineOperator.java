package com.function.pojos;

public class AirlineOperator {
    protected String AirlineID;
    protected String IcaoCode;
    protected String IataCode;
    protected String BoeingCode;
    protected String Name;

    public AirlineOperator() {
    }

    public String getAirlineID() {
        return this.AirlineID;
    }

    public void setAirlineID(String airlineID) {
        AirlineID = airlineID;
    }

    public String getIcaoCode() {
        return this.IcaoCode;
    }

    public void setIcaoCode(String icaoCode) {
        IcaoCode = icaoCode;
    }

    public String getIataCode() {
        return this.IataCode;
    }

    public void setIataCode(String iataCode) {
        IataCode = iataCode;
    }

    public String getBoeingCode() {
        return this.BoeingCode;
    }

    public void setBoeingCode(String boeingCode) {
        BoeingCode = boeingCode;
    }

    public String getName() {
        return this.Name;
    }

    public void setName(String name) {
        Name = name;
    }
    
    
}
