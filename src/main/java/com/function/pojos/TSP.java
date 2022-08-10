package com.function.pojos;

public class TSP {

    protected String InfltDB;
    protected Double QstarMRC;
    protected Double dQstar_dCI;
    protected Double ETHETA101;
    
    public TSP() {
    }

    public String getInfltDB() {
        return this.InfltDB;
    }

    public void setInfltDB(String infltDB) {
        this.InfltDB = infltDB;
    }

    public Double getQstarMRC() {
        return this.QstarMRC;
    }

    public void setQstarMRC(Double qstarMRC) {
        this.QstarMRC = qstarMRC;
    }

    public Double getdQstar_dCI() {
        return this.dQstar_dCI;
    }

    public void setdQstar_dCI(Double dQstar_dCI) {
        this.dQstar_dCI = dQstar_dCI;
    }

    public Double getETHETA101() {
        return this.ETHETA101;
    }

    public void setETHETA101(Double eTHETA101) {
        this.ETHETA101 = eTHETA101;
    }
    
}
