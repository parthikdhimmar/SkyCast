package com.example.weather;


public class HourlyWeatherData {
    private String time; // Time (e.g., "2023-10-05 15:00")
    private double temp_c; // Temperature in Celsius

    public String getTime() {
        return time;
    }

    public double getTempC() {
        return temp_c;
    }
}