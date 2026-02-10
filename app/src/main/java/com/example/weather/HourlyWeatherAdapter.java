package com.example.weather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class HourlyWeatherAdapter extends RecyclerView.Adapter<HourlyWeatherAdapter.HourlyWeatherViewHolder> {

    private List<HourlyWeather> hourlyWeatherList;

    public HourlyWeatherAdapter(List<HourlyWeather> hourlyWeatherList) {
        this.hourlyWeatherList = hourlyWeatherList;
    }

    @NonNull
    @Override
    public HourlyWeatherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weather_timing, parent, false);
        return new HourlyWeatherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HourlyWeatherViewHolder holder, int position) {
        HourlyWeather hourlyWeather = hourlyWeatherList.get(position);
        holder.timeTextView.setText(hourlyWeather.getTime());
        holder.temperatureTextView.setText(String.format("%d°C", (int) hourlyWeather.getTemperature()));

        // Load weather icon
        if (hourlyWeather.getIconUrl() != null) {
            String fullUrl = hourlyWeather.getIconUrl().startsWith("http") ?
                    hourlyWeather.getIconUrl() : "https:" + hourlyWeather.getIconUrl();

            Glide.with(holder.itemView.getContext())
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_weather_placeholder)
                    .error(R.drawable.ic_weather_error)
                    .into(holder.weatherIcon);
        }
        ScaleAnimation zoomInOut = new ScaleAnimation(
                1.0f, 1.2f,
                1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        zoomInOut.setDuration(700);
        zoomInOut.setRepeatCount(Animation.INFINITE);
        zoomInOut.setRepeatMode(Animation.REVERSE);
        holder.weatherIcon.startAnimation(zoomInOut);
    }


    @Override
    public int getItemCount() {
        return hourlyWeatherList.size();
    }

    public static class HourlyWeatherViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView temperatureTextView;
        ImageView weatherIcon;

        public HourlyWeatherViewHolder(@NonNull View itemView) {
            super(itemView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            temperatureTextView = itemView.findViewById(R.id.temperatureTextView);
            weatherIcon = itemView.findViewById(R.id.weatherIcon);
        }
    }
}