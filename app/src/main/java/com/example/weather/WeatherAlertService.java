package com.example.weather;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherAlertService extends Service {

    private static final String API_KEY = "c420dd7cec41460682551814250803";
    private static final String BASE_URL = "https://api.weatherapi.com/v1/";
    private static final String CHANNEL_ID = "WeatherAlerts";
    private static final int FOREGROUND_ID = 200;
    private static final int NOTIFICATION_ID = 100;

    private ScheduledExecutorService scheduler;

    private final HashMap<String, Long> lastNotificationTimes = new HashMap<>();
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(false);
    private static final long COOLDOWN_PERIOD_MS = 15 * 60 * 1000; // 15 minutes

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Weather Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes);

            notificationManager.createNotificationChannel(channel);
        }
        startForeground(FOREGROUND_ID, createForegroundNotification());
        startWeatherChecks();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdown();
        }
        stopForeground(true);
    }



    private Notification createForegroundNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.vector_notification)
                .setContentTitle("SkyCast Weather Alerts")
                .setContentText("Monitoring weather for alerts")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }




    private void startWeatherChecks() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (isTaskRunning.get()) {
                Log.d("WeatherAlertService", "Previous task still running, skipping");
                return;
            }
            isTaskRunning.set(true);
            try {
                SharedPreferences prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
                String query = prefs.getString("lastQuery", null);
                if (query == null) {
                    Log.d("WeatherAlertService", "No query found, skipping");
                    return;
                }

                Log.d("WeatherAlertService", "Fetching weather for query: " + query);

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                MainActivity.WeatherService service = retrofit.create(MainActivity.WeatherService.class);
                Call<WeatherResponse> call = service.getCurrentWeather(API_KEY, query);

                Response<WeatherResponse> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse.Current current = response.body().getCurrent();
                    String alertMessage = null;
                    String alertType = null;

                    if (current.getTempC() > 35) {
                        alertMessage = "🔥 Heat Alert: Stay hydrated!";
                        alertType = "temperature";
                    } else if (current.getWindKph() > 40) {
                        alertMessage = "💨 Strong Winds: Be cautious!";
                        alertType = "wind";
                    } else if (current.getHumidity() > 85) {
                        alertMessage = "💧 High Humidity: It may feel hotter!";
                        alertType = "humidity";
                    } else if (current.getPressureMb() < 1000) {
                        alertMessage = "🌧️ Low Pressure: Storm may be coming!";
                        alertType = "pressure";
                    }

                    if (alertMessage != null && canSendNotification(alertType)) {
                        Log.d("WeatherAlertService", "Sending notification: " + alertMessage);
                        sendAlertNotification(alertMessage, response.body().getLocation().getName());
                        lastNotificationTimes.put(alertType, System.currentTimeMillis());
                    } else {
                        Log.d("WeatherAlertService", "No alert conditions met or cooldown active");
                    }
                } else {
                    Log.e("WeatherAlertService", "API response unsuccessful: " + response.code());
                }
            } catch (Exception e) {
                Log.e("WeatherAlertService", "Error fetching weather: " + e.getMessage());
            } finally {
                isTaskRunning.set(false);
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    private boolean canSendNotification(String alertType) {
        Long lastTime = lastNotificationTimes.get(alertType);
        if (lastTime == null) {
            return true;
        }
        return (System.currentTimeMillis() - lastTime) >= COOLDOWN_PERIOD_MS;
    }

    private void sendAlertNotification(String message, String city) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.vector_notification)
                .setContentTitle("Weather Alert for " + city)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

}