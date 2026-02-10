package com.example.weather;

import java.util.List;

public class WeatherResponse {
    private Location location;
    private Current current;
    private Forecast forecast;

    // Getters
    public Location getLocation() {
        return location;
    }

    public Current getCurrent() {
        return current;
    }

    public Forecast getForecast() {
        return forecast;
    }

    public static class Location {
        private String name;
        private String region;
        private String country;
        private double lat;
        private double lon;
        private String tz_id;
        private String localtime;
        private long localtime_epoch;

        // Add getters for all fields
        public String getName() {
            return name;
        }

        public String getRegion() {
            return region;
        }

        public String getCountry() {
            return country;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public String getTzId() {
            return tz_id;
        }

        public String getLocaltime() {
            return localtime;
        }

        public long getLocaltimeEpoch() {
            return localtime_epoch;
        }
    }
    public static class Current {
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

    public static class Forecast {
        private List<ForecastDay> forecastday;

        public List<ForecastDay> getForecastday() {
            return forecastday;
        }
    }

    public static class ForecastDay {
        private List<HourlyWeatherData> hour;
        private Astro astro;
        private Day day;

        public List<HourlyWeatherData> getHour() {
            return hour;
        }

        public Astro getAstro() {
            return astro;
        }

        public Day getDay() {
            return day;
        }
    }

    public static class Astro {
        private String sunrise;
        private String sunset;

        public String getSunrise() {
            return sunrise;
        }

        public String getSunset() {
            return sunset;
        }
    }

    public static class Day {
        private double avgtemp_c;
        private Condition condition;

        public double getAvgtempC() {
            return avgtemp_c;
        }

        public Condition getCondition() {
            return condition;
        }
    }

    public static class HourlyWeatherData {
        private String time;
        private double temp_c;
        private Condition condition;

        public String getTime() {
            return time;
        }

        public double getTempC() {
            return temp_c;
        }

        public Condition getCondition() {
            return condition;
        }
    }

    public static class Condition {
        private String text;
        private String icon;
        private int code;

        public String getText() {
            return text;
        }

        public String getIcon() {
            return icon;
        }

        public int getCode() {
            return code;
        }
    }
}
