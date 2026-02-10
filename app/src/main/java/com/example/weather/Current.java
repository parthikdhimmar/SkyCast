package com.example.weather;

public class Current {
    private String last_updated;
    private double temp_c;
    private double feelslike_c;
    private String wind_dir;
    private int cloud;
    private double precip_mm;
    private double vis_km;
    private double uv;
    private Condition condition;
    private double wind_kph;
    private int humidity;
    private double pressure_mb;

    // Getters
    public String getLastUpdated() {
        return last_updated;
    }

    public double getTempC() {
        return temp_c;
    }

    public double getFeelslikeC() {
        return feelslike_c;
    }

    public String getWindDir() {
        return wind_dir;
    }

    public int getCloud() {
        return cloud;
    }

    public double getPrecipMm() {
        return precip_mm;
    }

    public double getVisKm() {
        return vis_km;
    }

    public double getUv() {
        return uv;
    }

    public Condition getCondition() {
        return condition;
    }

    public double getWindKph() {
        return wind_kph;
    }

    public int getHumidity() {
        return humidity;
    }

    public double getPressureMb() {
        return pressure_mb;
    }

    // Setters
    public void setLastUpdated(String last_updated) {
        this.last_updated = last_updated;
    }

    public void setTempC(double temp_c) {
        this.temp_c = temp_c;
    }

    public void setFeelslikeC(double feelslike_c) {
        this.feelslike_c = feelslike_c;
    }

    public void setWindDir(String wind_dir) {
        this.wind_dir = wind_dir;
    }

    public void setCloud(int cloud) {
        this.cloud = cloud;
    }

    public void setPrecipMm(double precip_mm) {
        this.precip_mm = precip_mm;
    }

    public void setVisKm(double vis_km) {
        this.vis_km = vis_km;
    }

    public void setUv(double uv) {
        this.uv = uv;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public void setWindKph(double wind_kph) {
        this.wind_kph = wind_kph;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public void setPressureMb(double pressure_mb) {
        this.pressure_mb = pressure_mb;
    }
}
