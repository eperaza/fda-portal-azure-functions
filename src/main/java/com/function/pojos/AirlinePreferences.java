package com.function.pojos;

public class AirlinePreferences {

    String preference;
    String airlineKey;
    Boolean enabled;
    String description;
    Boolean display;
    Boolean choicePilot;
    Boolean choiceFocal;
    Boolean choiceCheckAirman;
    Boolean choiceMaintenance;
    Boolean choiceEFBAdmin;
    String updatedBy;
    String createTS;

    public AirlinePreferences() {
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public String getAirlineKey() {
        return this.airlineKey;
    }

    public void setAirlineKey(String airlineKey) {
        this.airlineKey = airlineKey;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDisplay() {
        return this.display;
    }

    public void setDisplay(Boolean display) {
        this.display = display;
    }

    public Boolean getChoicePilot() {
        return this.choicePilot;
    }

    public void setChoicePilot(Boolean choicePilot) {
        this.choicePilot = choicePilot;
    }

    public Boolean getChoiceFocal() {
        return this.choiceFocal;
    }

    public void setChoiceFocal(Boolean choiceFocal) {
        this.choiceFocal = choiceFocal;
    }

    public Boolean getChoiceCheckAirman() {
        return this.choiceCheckAirman;
    }

    public void setChoiceCheckAirman(Boolean choiceCheckAirman) {
        this.choiceCheckAirman = choiceCheckAirman;
    }

    public Boolean getChoiceMaintenance() {
        return this.choiceMaintenance;
    }

    public void setChoiceMaintenance(Boolean choiceMaintenance) {
        this.choiceMaintenance = choiceMaintenance;
    }

    public Boolean getChoiceEFBAdmin() {
        return this.choiceEFBAdmin;
    }

    public void setChoiceEFBAdmin(Boolean choiceEFBAdmin) {
        this.choiceEFBAdmin = choiceEFBAdmin;
    }

    public String getUpdatedBy() {
        return this.updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreateTS() {
        return this.createTS;
    }

    public void setCreateTS(String createTS) {
        this.createTS = createTS;
    }

}
