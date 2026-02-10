package com.example.weather;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class WeatherWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Get saved weather data
        SharedPreferences prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
        String city = prefs.getString("widget_city", "Loading...");
        String temp = prefs.getString("widget_temp", "--°C");

        // Update all widgets
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, city, temp);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                                    int appWidgetId, String cityName, String temperature) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.weather_widget);

        // Set the widget data
        views.setTextViewText(R.id.widget_city, cityName);
        views.setTextViewText(R.id.widget_temp, temperature);

        // Create an intent to launch MainActivity when clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}