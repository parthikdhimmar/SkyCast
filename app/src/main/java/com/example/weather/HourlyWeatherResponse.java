package com.example.weather;

import java.util.List;

public class HourlyWeatherResponse {
    private Forecast forecast;
    private Location location;

    public Location getLocation() {
        return location;
    }

    public Forecast getForecast() {
        return forecast;
    }


    public static class Location {
        private String localtime;

        public String getLocaltime() {
            return localtime;
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

        public List<HourlyWeatherData> getHour() {
            return hour;
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