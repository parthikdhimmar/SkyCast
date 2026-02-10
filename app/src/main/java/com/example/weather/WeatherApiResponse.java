package com.example.weather;

public class WeatherApiResponse {
    private Astronomy astronomy;

    public Astronomy getAstronomy() {
        return astronomy;
    }

    public static class Astronomy {
        private Astro astro;

        public Astro getAstro() {
            return astro;
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
}