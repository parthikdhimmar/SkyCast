package com.example.weather;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApi {
    @GET("astronomy.json")
    Call<WeatherApiResponse> getAstronomy(
            @Query("key") String apiKey,
            @Query("q") String location
    );
}