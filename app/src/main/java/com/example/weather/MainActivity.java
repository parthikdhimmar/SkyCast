package com.example.weather;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "c420dd7cec41460682551814250803";
    private static final String BASE_URL = "https://api.weatherapi.com/v1/";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean isLocationBasedUpdate = true;

    private String lastQuery = null;

    private Animation animation;

    private LinearLayout optionLayout;

    private boolean wasLastLocationBased = false;

    private SwipeRefreshLayout swipeRefreshLayout;


    // UI elements
    private SearchView searchView;
    private TextView address, updatedAt, status, temp, tempFeelsLike, wind, windDirection,
            currentCloud, precipitation, visibility, uvIndex, humidity, pressure;
    private ProgressBar loader;
    private TextView errorText;
    private View weatherContainer;
    private RecyclerView hourlyWeatherRecyclerView;
    private HourlyWeatherAdapter hourlyWeatherAdapter;
    private List<HourlyWeather> hourlyWeatherList;

    // Forecast views
    private TextView[] weekNameViews = new TextView[3];
    private TextView[] weekStatusViews = new TextView[3];
    private TextView[] weekTempViews = new TextView[3];

    // Location services
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private LottieAnimationView butterflyAnimation;  // Add this line

    private static final int MAP_ACTIVITY_REQUEST_CODE = 100;

    private static final int LOCATION_SETTINGS_REQUEST_CODE = 3;


    private TextToSpeech tts;
    private boolean hasSpoken = false;
    private SharedPreferences voicePrefs;
    private TextView stopVoiceButton;


    // Update initializeTTS to process queued messages
    private boolean isTtsInitialized = false;
    private List<String> ttsMessageQueue = new ArrayList<>();

    private boolean isReturningFromActivity = false;

    private Set<String> spokenMessages = new HashSet<>();

    private TextView alertText;


    private final BroadcastReceiver networkAndLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isConnected = isInternetAvailable();
            boolean isLocationEnabled = isLocationEnabled();

            if (isConnected && isLocationEnabled) {
                hideError();
                showLoading();

                // Retry last fetch if we have a previous query
                if (lastQuery != null) {
                    isLocationBasedUpdate = wasLastLocationBased;
                    fetchWeatherData(lastQuery);
                } else {
                    // Otherwise, fetch based on location
                    startLocationUpdates();
                }
            }
        }
    };
    private boolean isSearching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //moon

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.blue));
        }

        initializeViews();
        setupRecyclerView();
        setupForecastViews();
        setupLocationServices();
        setupSearchView();
        setupWidgetClickListener();
        setupScrollViewListener();

        voicePrefs = getSharedPreferences("VoicePrefs", MODE_PRIVATE);
        stopVoiceButton = findViewById(R.id.stop_voice);
        boolean voiceStopped = voicePrefs.getBoolean("voiceStopped", false);
        stopVoiceButton.setTextColor(getResources().getColor(android.R.color.black));
        stopVoiceButton.setCompoundDrawableTintList(getResources().getColorStateList(
                voiceStopped ? android.R.color.holo_red_light : android.R.color.holo_green_light));
        initializeTTS();
        setupStopVoiceButton();

        optionLayout = findViewById(R.id.option_layout);
        if (getIntent().getBooleanExtra("fromBackNavigation", false)) {
            isReturningFromActivity = true;
        }

        // Request POST_NOTIFICATIONS permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            }
        }

        // Prompt for battery optimization exemption
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                SharedPreferences prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
                if (!prefs.getBoolean("batteryPromptShown", false)) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                    prefs.edit().putBoolean("batteryPromptShown", true).apply();
                }
            }
        }

        // Setup alert toggle
        Switch alertToggle = findViewById(R.id.alert_toggle);
        SharedPreferences prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
        boolean alertsEnabled = prefs.getBoolean("alertsEnabled", false);
        alertToggle.setChecked(alertsEnabled);
        alertToggle.setTranslationX(alertsEnabled ? -convertDpToPx(5) : 0);
        updateToggleColor(alertToggle, alertsEnabled);
        alertToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
                        return;
                    }
                }
                startAlertService(prefs, isChecked);
                alertToggle.setTranslationX(-convertDpToPx(5));
            } else {
                stopService(new Intent(this, WeatherAlertService.class));
                prefs.edit().putBoolean("alertsEnabled", false).apply();
                alertToggle.setTranslationX(0);
            }
            updateToggleColor(alertToggle, isChecked);
            prefs.edit().putBoolean("alertsEnabled", isChecked).apply();
        });

        // Check location permission and start updates if granted and location is enabled
        if (checkLocationPermission()) {
            if (isLocationEnabled()) {
                showLoading();
                startLocationUpdates();
            } else {
                showError("Please turn on your location");
                queueErrorMessage("Please turn on your location");
            }
        } else {
            requestLocationPermission();
        }
    }

    private float convertDpToPx(int dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void initializeViews() {

        searchView = findViewById(R.id.searchView);


        // Add these lines to initialize the option views
        TextView visibleOptionTxt = findViewById(R.id.visible_option_TXT);
        LinearLayout optionLayout = findViewById(R.id.option_layout);

        visibleOptionTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (optionLayout.getVisibility() == View.VISIBLE) {
                    optionLayout.setVisibility(View.GONE);
                } else {
                    optionLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        // Set initial visibility to GONE
        optionLayout.setVisibility(View.GONE);

        address = findViewById(R.id.address);
        updatedAt = findViewById(R.id.updated_at);
        status = findViewById(R.id.status);
        temp = findViewById(R.id.temp);
        tempFeelsLike = findViewById(R.id.temp_feels_like);
        wind = findViewById(R.id.wind);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        windDirection = findViewById(R.id.wind_direction);
        currentCloud = findViewById(R.id.current_cloud);
        precipitation = findViewById(R.id.Precipitation_mm);
        visibility = findViewById(R.id.visibility_km);
        uvIndex = findViewById(R.id.UV_index);
        humidity = findViewById(R.id.humidity);
        pressure = findViewById(R.id.pressure);
        loader = findViewById(R.id.loader);
        alertText = findViewById(R.id.alertText);
        errorText = findViewById(R.id.errorText);
        weatherContainer = findViewById(R.id.weatherContainer);
        LottieAnimationView butterflyAnimation = findViewById(R.id.butterflyAnimation);
        setupButterflyAnimation(butterflyAnimation);
    }

    private void setupRecyclerView() {
        hourlyWeatherRecyclerView = findViewById(R.id.weatherTimingRecyclerView);
        hourlyWeatherRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        hourlyWeatherList = new ArrayList<>();
        hourlyWeatherAdapter = new HourlyWeatherAdapter(hourlyWeatherList);
        hourlyWeatherRecyclerView.setAdapter(hourlyWeatherAdapter);
    }

    private void setupForecastViews() {
        weekNameViews[0] = findViewById(R.id.weekName1);
        weekNameViews[1] = findViewById(R.id.weekName2);
        weekNameViews[2] = findViewById(R.id.weekName3);

        weekStatusViews[0] = findViewById(R.id.week1_Status);
        weekStatusViews[1] = findViewById(R.id.week2_Status);
        weekStatusViews[2] = findViewById(R.id.week3_Status);

        weekTempViews[0] = findViewById(R.id.week1_Temp);
        weekTempViews[1] = findViewById(R.id.week2_Temp);
        weekTempViews[2] = findViewById(R.id.week3_Temp);
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || isSearching) return;

                for (Location location : locationResult.getLocations()) {
                    String query = location.getLatitude() + "," + location.getLongitude();
                    isLocationBasedUpdate = true; // Mark as location-based update
                    fetchWeatherData(query);
                    stopLocationUpdates();
                    break;
                }
            }
        };
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                isSearching = true;
                isLocationBasedUpdate = false;
                stopLocationUpdates();
                fetchWeatherData(query);
                searchView.clearFocus(); // Add this to hide keyboard after submit
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // Add this to ensure the search view expands when getting focus
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                searchView.setIconified(false);
            }
        });


        TextView help_btn = findViewById(R.id.Help_Activity);

        help_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, help_user.class);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                startActivity(intent);
            }
        });

        TextView rating_btn = findViewById(R.id.rating_Activity);

        rating_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, rating_feedback.class);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                startActivity(intent);
            }
        });

        TextView mapOption = findViewById(R.id.map_option);
        mapOption.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                startMapActivity();
            } else {
                requestLocationPermission();
            }
        });


        ImageView weatherIcon = findViewById(R.id.weatherIcon);


        ScaleAnimation zoomInOut = new ScaleAnimation(
                1.0f, 1.2f, // Start and end values for the X axis scaling
                1.0f, 1.2f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f  // Pivot point of Y scaling
        );

        zoomInOut.setDuration(1300); // Duration of one cycle (in ms)
        zoomInOut.setRepeatCount(Animation.INFINITE); // Repeat forever
        zoomInOut.setRepeatMode(Animation.REVERSE); // Reverse back to original size

        weatherIcon.startAnimation(zoomInOut);


        // Update swipeRefreshLayout listener in setupSearchView
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (!isInternetAvailable()) {
                swipeRefreshLayout.setRefreshing(false);
                String error = "Please check your internet connection";
                showError(error);
                if (!spokenMessages.contains(error)) {
                    queueErrorMessage(error);
                    spokenMessages.add(error);
                }
                return;
            }

            if (!isLocationEnabled() && (lastQuery == null || wasLastLocationBased)) {
                swipeRefreshLayout.setRefreshing(false);
                String error = "Please turn on your location";
                showError(error);
                if (!spokenMessages.contains(error)) {
                    queueErrorMessage(error);
                    spokenMessages.add(error);
                }
                return;
            }

            if (lastQuery != null) {
                isLocationBasedUpdate = wasLastLocationBased;
                fetchWeatherData(lastQuery);
            } else {
                startLocationUpdates();
            }

            swipeRefreshLayout.setRefreshing(false);
        });
    }


    private void fetchWeatherData(String query) {
        showLoading();
        lastQuery = query;
        wasLastLocationBased = isLocationBasedUpdate;

        // Save last query to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
        prefs.edit().putString("lastQuery", query).apply();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = service.getCurrentWeather(API_KEY, query);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                    fetchForecastData(query);
                    fetchHourlyWeatherData(query);
                    fetchAstronomyData(query);
                } else {
                    showError("You can search Invalid location");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                showError("Check your internet connection");
            }
        });
    }

    private void fetchForecastData(String query) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        Call<WeatherResponse> call = service.getForecast(API_KEY, query, 3); // 3 days forecast

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateForecastUI(response.body());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e("Forecast", "Error fetching forecast: " + t.getMessage());
            }
        });
    }


    private void initializeTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("MainActivity", "TTS language not supported or missing: " + Locale.US);
                } else {
                    isTtsInitialized = true;
                    Log.d("MainActivity", "TTS initialized successfully");
                    // Process queued messages
                    synchronized (ttsMessageQueue) {
                        for (String error : ttsMessageQueue) {
                            speakErrorMessage(error);
                        }
                        ttsMessageQueue.clear();
                    }
                }
            } else {
                Log.e("MainActivity", "TTS initialization failed, status: " + status);
            }
        });
    }

    // Update setupStopVoiceButton to handle toggle correctly
    private void setupStopVoiceButton() {
        stopVoiceButton.setOnClickListener(v -> {
            boolean voiceStopped = voicePrefs.getBoolean("voiceStopped", false);
            if (!voiceStopped) {
                if (tts != null) {
                    tts.stop();
                }
                stopVoiceButton.setTextColor(getResources().getColor(android.R.color.black));
                stopVoiceButton.setCompoundDrawableTintList(getResources().getColorStateList(android.R.color.holo_red_light));
                voicePrefs.edit().putBoolean("voiceStopped", true).apply();
            } else {
                stopVoiceButton.setTextColor(getResources().getColor(android.R.color.black));
                stopVoiceButton.setCompoundDrawableTintList(getResources().getColorStateList(android.R.color.holo_green_light));
                voicePrefs.edit().putBoolean("voiceStopped", false).apply();
            }
        });
    }


    private void speakErrorMessage(String error) {
        if (voicePrefs.getBoolean("voiceStopped", false)) {
            Log.d("MainActivity", "TTS skipped: voiceStopped is true");
            return;
        }
        if (tts == null) {
            Log.e("MainActivity", "TTS not initialized");
            return;
        }
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 4 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Good Night";
        }
        String message = String.format("%s! %s.", greeting, error);
        int result = tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        if (result == TextToSpeech.ERROR) {
            Log.e("MainActivity", "TTS speak failed for message: " + message);
        } else {
            Log.d("MainActivity", "TTS speaking: " + message);
        }
    }


    private void updateUI(WeatherResponse weatherResponse) {
        hideLoading();
        weatherContainer.setVisibility(View.VISIBLE);

        WeatherResponse.Location location = weatherResponse.getLocation();
        String city = location.getName();
        String region = location.getRegion();
        String country = location.getCountry();
        String searchQuery = searchView.getQuery().toString().trim().toLowerCase();
        String temperature = String.format(Locale.getDefault(), "%.0f°C", weatherResponse.getCurrent().getTempC());

        if (isLocationBasedUpdate) {
            saveWidgetData(city, temperature);
            updateAllWidgets(city, temperature);
        }

        String formattedAddress;
        if (searchQuery.equalsIgnoreCase(country)) {
            formattedAddress = country;
        } else if (searchQuery.equalsIgnoreCase(region)) {
            formattedAddress = region + ", " + country;
        } else if (searchQuery.equalsIgnoreCase(city)) {
            formattedAddress = city;
            if (region != null && !region.isEmpty() && !region.equalsIgnoreCase(city)) {
                formattedAddress += ", " + region;
            }
            formattedAddress += ", " + country;
        } else {
            formattedAddress = city;
            if (region != null && !region.isEmpty() && !region.equalsIgnoreCase(city)) {
                formattedAddress += ", " + region;
            }
            formattedAddress += ", " + country;
        }

        address.setText(formattedAddress);
        TextView currentDateTime = findViewById(R.id.current_date_time);
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, MMM d h:mm a", Locale.getDefault());
            Date localTime = inputFormat.parse(weatherResponse.getLocation().getLocaltime());
            currentDateTime.setText("Local time : " + outputFormat.format(localTime));
            updatedAt.setText("Updated at : " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()));
        } catch (ParseException e) {
            currentDateTime.setText("Local time : " + weatherResponse.getLocation().getLocaltime());
            updatedAt.setText("Updated at : " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()));
        }

        WeatherResponse.Current current = weatherResponse.getCurrent();
        status.setText(current.getCondition().getText());
        temp.setText(String.format(Locale.getDefault(), "%.0f°C", current.getTempC()));
        tempFeelsLike.setText(String.format(Locale.getDefault(), "Feels like : %.0f°C", current.getFeelslikeC()));
        wind.setText(String.format(Locale.getDefault(), "Wind : %.0f kp/h", current.getWindKph()));
        windDirection.setText("" + current.getWindDir());
        currentCloud.setText("" + current.getCloud() + "%");
        precipitation.setText(String.format(Locale.getDefault(), "%.1f mm", current.getPrecipMm()));
        visibility.setText(String.format(Locale.getDefault(), "%.0f km", current.getVisKm()));
        uvIndex.setText(String.format(Locale.getDefault(), "%.1f", current.getUv()));
        humidity.setText(String.format(Locale.getDefault(), "Humidity : %d%%", current.getHumidity()));
        pressure.setText(String.format(Locale.getDefault(), "Pressure : %.0f mb", current.getPressureMb()));

        // Check weather alert conditions
        String alertMessage = null;
        if (current.getTempC() > 35) {
            alertMessage = "🔥 Heat Alert: Stay hydrated!";
        } else if (current.getWindKph() > 40) {
            alertMessage = "💨 Strong Winds: Be cautious!";
        } else if (current.getHumidity() > 85) {
            alertMessage = "💧 High Humidity: It may feel hotter!";
        } else if (current.getPressureMb() < 1000) {
            alertMessage = "🌧️ Low Pressure: Storm may be coming!";
        }

        if (alertMessage != null) {
            alertText.setText(alertMessage);
            alertText.setVisibility(View.VISIBLE);
        } else {
            alertText.setVisibility(View.GONE);
        }

        if (!hasSpoken && !voicePrefs.getBoolean("voiceStopped", false)) {
            String voiceCity = weatherResponse.getLocation().getName();
            String voiceTemp = String.format(Locale.getDefault(), "%.0f degrees Celsius", weatherResponse.getCurrent().getTempC());
            String weather = weatherResponse.getCurrent().getCondition().getText();
            String message = getTimeBasedMessage(voiceCity, voiceTemp, weather);
            if (alertMessage != null) {
                message += " " + alertMessage;
            }
            speakWeather(message);
            hasSpoken = true;
            voicePrefs.edit().putBoolean("hasSpoken", true).apply();
        }

        ImageView weatherIcon = findViewById(R.id.weatherIcon);
        loadWeatherIcon(current.getCondition().getIcon(), weatherIcon);
    }

    private void updateForecastUI(WeatherResponse weatherResponse) {
        List<WeatherResponse.ForecastDay> forecastDays = weatherResponse.getForecast().getForecastday();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());

        for (int i = 0; i < 3 && i < forecastDays.size(); i++) {
            WeatherResponse.ForecastDay day = forecastDays.get(i);

            String dateStr;
            if (i == 0) {
                dateStr = "Today";
            } else if (i == 1) {
                dateStr = "Tomorrow";
            } else {
                try {
                    String forecastDateStr = day.getHour().get(0).getTime().substring(0, 10);
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = inputFormat.parse(forecastDateStr);
                    dateStr = dateFormat.format(date);
                } catch (Exception e) {
                    dateStr = "Day " + (i + 1);
                }
            }

            weekNameViews[i].setText(dateStr);
            weekStatusViews[i].setText(day.getDay().getCondition().getText());
            weekTempViews[i].setText(String.format(Locale.getDefault(), "%.0f°C", day.getDay().getAvgtempC()));
        }
    }

    private void fetchHourlyWeatherData(String query) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        Call<HourlyWeatherResponse> call = service.getHourlyWeather(API_KEY, query, 1);

        call.enqueue(new Callback<HourlyWeatherResponse>() {
            @Override
            public void onResponse(Call<HourlyWeatherResponse> call, Response<HourlyWeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateHourlyWeatherUI(response.body());
                }
            }

            @Override
            public void onFailure(Call<HourlyWeatherResponse> call, Throwable t) {
                Log.e("HourlyWeather", "Error: " + t.getMessage());
            }
        });
    }

    private void updateHourlyWeatherUI(HourlyWeatherResponse response) {
        hourlyWeatherList.clear();

        // Get the location's local hour from the API response
        String localTimeStr = response.getLocation().getLocaltime(); // "2025-04-15 17:17" for Dubai example
        int currentHour;
        try {
            String hourStr = localTimeStr.substring(11, 13); // Extract "17" from "2025-04-15 17:17"
            currentHour = Integer.parseInt(hourStr);
        } catch (Exception e) {
            // Fallback to device time if parsing fails
            Calendar calendar = Calendar.getInstance();
            currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        }

        for (HourlyWeatherResponse.HourlyWeatherData hourlyData :
                response.getForecast().getForecastday().get(0).getHour()) {
            String time = hourlyData.getTime();
            String hour = time.substring(11, 13);
            int hourInt = Integer.parseInt(hour);

            if (hourInt >= currentHour) {
                hourlyWeatherList.add(new HourlyWeather(
                        formatTime(time),
                        hourlyData.getTempC(),
                        hourlyData.getCondition().getIcon()
                ));
            }
        }
        hourlyWeatherAdapter.notifyDataSetChanged();
    }

    private void fetchAstronomyData(String query) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherService service = retrofit.create(WeatherService.class);
        Call<WeatherApiResponse> call = service.getAstronomyData(API_KEY, query);

        call.enqueue(new Callback<WeatherApiResponse>() {
            @Override
            public void onResponse(Call<WeatherApiResponse> call, Response<WeatherApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TextView sunrise = findViewById(R.id.sunrise);
                    TextView sunset = findViewById(R.id.sunset);
                    sunrise.setText("Sunrise : " + response.body().getAstronomy().getAstro().getSunrise());
                    sunset.setText("Sunset : " + response.body().getAstronomy().getAstro().getSunset());
                }
            }

            @Override
            public void onFailure(Call<WeatherApiResponse> call, Throwable t) {
                Log.e("Astronomy", "Error: " + t.getMessage());
            }
        });
    }

    private void loadWeatherIcon(String iconUrl, ImageView imageView) {
        if (iconUrl == null || iconUrl.isEmpty()) return;
        String fullUrl = iconUrl.startsWith("http") ? iconUrl : "https:" + iconUrl;
        Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_weather_placeholder)
                .error(R.drawable.ic_weather_error)
                .into(imageView);
    }

    private String formatTime(String time) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("h a", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(time));
        } catch (ParseException e) {
            return time;
        }
    }

    private void showLoading() {
        if (!swipeRefreshLayout.isRefreshing()) {
            loader.setVisibility(View.VISIBLE);
        }
        errorText.setVisibility(View.GONE);
        weatherContainer.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loader.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showError(String message) {
        loader.setVisibility(View.GONE);
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
        weatherContainer.setVisibility(View.GONE);
        hourlyWeatherList.clear();              // ← clear hourly list
        hourlyWeatherAdapter.notifyDataSetChanged();
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Check if location services are enabled
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    // Add this method to your MainActivity
    private void startMapActivity() {
        Intent intent = new Intent(MainActivity.this, MapActivity.class);
        startActivityForResult(intent, MAP_ACTIVITY_REQUEST_CODE);
    }

    private void startAlertService(SharedPreferences prefs, boolean isChecked) {
        startService(new Intent(this, WeatherAlertService.class));
        prefs.edit().putBoolean("alertsEnabled", isChecked).apply();
    }

    // Add method to update toggle color
    private void updateToggleColor(Switch alertToggle, boolean isChecked) {
        int thumbColor = isChecked ? R.color.toggle_on_color : R.color.toggle_off_color;
        int trackColor = isChecked ? R.color.toggle_on_color : R.color.toggle_off_color;
        alertToggle.getThumbDrawable().setTint(ContextCompat.getColor(this, thumbColor));
        alertToggle.getTrackDrawable().setTint(ContextCompat.getColor(this, trackColor));
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("MainActivity", "Permission result for requestCode: " + requestCode + ", granted: " + (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED));
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationEnabled()) {
                    Log.d("MainActivity", "Location enabled after permission grant, starting updates");
                    showLoading();
                    startLocationUpdates();
                } else {
                    Log.d("MainActivity", "Location disabled after permission grant, prompting user");
                    showError("Please turn on your location");
                    queueErrorMessage("Please turn on your location");
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, LOCATION_SETTINGS_REQUEST_CODE);
                }
            } else {
                Log.d("MainActivity", "Location permission denied");
                showError("Location permission denied");
                queueErrorMessage("Location permission denied");
            }
        } else if (requestCode == 2) {
            Switch alertToggle = findViewById(R.id.alert_toggle);
            SharedPreferences prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                alertToggle.setChecked(true);
                startAlertService(prefs, true);
                updateToggleColor(alertToggle, true);
            } else {
                alertToggle.setChecked(false);
                prefs.edit().putBoolean("alertsEnabled", false).apply();
                updateToggleColor(alertToggle, false);
                Toast.makeText(this, "Notification permission denied. Alerts disabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getTimeBasedMessage(String city, String temp, String weather) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String weatherStatus = weather.contains("sun") ? "sunny" : (weather.contains("cloud") ? "cloudy" : weather);

        if (hour >= 4 && hour < 6) {
            return String.format("Good early morning! In %s, it's %s with %s weather.", city, temp, weatherStatus);
        } else if (hour < 10) {
            return String.format("Good morning! In %s, it's %s and the weather is %s.", city, temp, weatherStatus);
        } else if (hour < 14) {
            return String.format("Hello! In %s, it's %s and the weather is %s today.", city, temp, weatherStatus);
        } else if (hour < 17) {
            return String.format("Good afternoon! In %s, it's %s with %s weather.", city, temp, weatherStatus);
        } else if (hour < 21) {
            return String.format("Good evening! In %s, it's %s and the weather is %s.", city, temp, weatherStatus);
        } else if (hour < 24) {
            return String.format("Good night! In %s, it's %s and the weather is %s tonight.", city, temp, weatherStatus);
        } else {
            return String.format("Late night in %s! It's %s with %s weather.", city, temp, weatherStatus);
        }
    }
    // Add speakWeather method
    private void speakWeather(String message) {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // Update onPause to stop TTS without shutdown
    @Override
    protected void onPause() {
        super.onPause();
        if (tts != null) {
            tts.stop();
        }
        unregisterReceiver(networkAndLocationReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(networkAndLocationReceiver, filter);
        if (tts == null || !isTtsInitialized) {
            initializeTTS();
        } else if (!isReturningFromActivity) {
            synchronized (ttsMessageQueue) {
                for (String error : ttsMessageQueue) {
                    speakErrorMessage(error);
                }
                ttsMessageQueue.clear();
            }
        }
        Log.d("MainActivity", "onResume: Checking location and internet status");
        if (checkLocationPermission() && isLocationEnabled() && !isSearching) {
            Log.d("MainActivity", "onResume: Location enabled, starting updates");
            showLoading();
            startLocationUpdates();
        } else if (!isLocationEnabled()) {
            Log.d("MainActivity", "onResume: Location disabled");
            showError("Please turn on your location");
            queueErrorMessage("Please turn on your location");
        }
        isReturningFromActivity = false; // Reset flag after handling
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (optionLayout != null && optionLayout.getVisibility() == View.VISIBLE) {
                Rect outRect = new Rect();
                optionLayout.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    optionLayout.setVisibility(View.GONE);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    // Add this to handle scroll events
    private void setupScrollViewListener() {
        ScrollView scrollView = findViewById(R.id.scrollView); // Make sure your ScrollView has this ID
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (optionLayout != null && optionLayout.getVisibility() == View.VISIBLE) {
                optionLayout.setVisibility(View.GONE);
            }
        });
    }

    private void checkLocationAndInternetStatus() {
        boolean isInternetConnected = isInternetAvailable();
        boolean isLocationEnabled = isLocationEnabled();

        if (!isInternetConnected) {
            String error = "Please check your internet connection";
            showError(error);
            Log.d("MainActivity", "Internet off, queueing error message");
            if (!spokenMessages.contains(error)) {
                queueErrorMessage(error);
                spokenMessages.add(error);
            }
            return;
        }

        if (!isLocationEnabled) {
            String error = "Please turn on your location";
            showError(error);
            Log.d("MainActivity", "Location off, queueing error message");
            if (!spokenMessages.contains(error)) {
                queueErrorMessage(error);
                spokenMessages.add(error);
            }
            return;
        }

        spokenMessages.clear(); // Clear spoken messages when both are enabled
        hideError();
        showLoading();
        startLocationUpdates();
    }

    // Update queueErrorMessage to respect spokenMessages
    private void queueErrorMessage(String error) {
        if (voicePrefs.getBoolean("voiceStopped", false)) {
            Log.d("MainActivity", "TTS skipped: voiceStopped is true");
            return;
        }
        if (tts == null) {
            Log.e("MainActivity", "TTS not instantiated");
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("MainActivity", "TTS language not supported or missing: " + Locale.US);
                    } else {
                        isTtsInitialized = true;
                        Log.d("MainActivity", "TTS initialized successfully");
                        if (!voicePrefs.getBoolean("voiceStopped", false) && !spokenMessages.contains(error)) {
                            speakErrorMessage(error);
                            spokenMessages.add(error);
                        }
                    }
                } else {
                    Log.e("MainActivity", "TTS initialization failed, status: " + status);
                }
            });
            return;
        }
        if (isTtsInitialized && !spokenMessages.contains(error)) {
            speakErrorMessage(error);
            spokenMessages.add(error);
        } else if (!isTtsInitialized) {
            synchronized (ttsMessageQueue) {
                if (!ttsMessageQueue.contains(error) && !spokenMessages.contains(error)) {
                    ttsMessageQueue.add(error);
                    Log.d("MainActivity", "Queued TTS message: " + error);
                }
            }
        }
    }


    private void hideError() {
        errorText.setVisibility(View.GONE);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onBackPressed() {
        if (isSearching) {
            isSearching = false;
            isLocationBasedUpdate = true;
            searchView.setQuery("", false);
            searchView.clearFocus();

            // Check if location is enabled
            if (isLocationEnabled()) {
                startLocationUpdates();
            } else {
                showError("Please turn on your location");  // ← Show proper message
            }
        } else {
            super.onBackPressed();
        }
    }


    private void setupWidgetClickListener() {
        TextView widgetButton = findViewById(R.id.HomeScreenWidget);
        widgetButton.setOnClickListener(v -> {
            if (address == null || temp == null) {
                Toast.makeText(MainActivity.this, "Please load weather data first", Toast.LENGTH_SHORT).show();
                return;
            }

            // Only allow widget creation if we're showing location-based data
            if (!isLocationBasedUpdate) {
                Toast.makeText(MainActivity.this,
                        "Please use your current location for widget creation",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Get current weather data
            String city = address.getText().toString().split(",")[0].trim();
            String temperature = temp.getText().toString();

            // Save data for widget
            saveWidgetData(city, temperature);

            // Show toast message
            Toast.makeText(MainActivity.this,
                    "Widget is ready to add from widget list",
                    Toast.LENGTH_SHORT).show();
        });
    }


    // In MainActivity.java, update the onActivityResult method:
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                String locationQuery = data.getStringExtra("location_query");
                if (locationQuery != null && !locationQuery.isEmpty()) {
                    searchView.clearFocus();
                    searchView.setIconified(false);
                    searchView.setQuery(locationQuery, false);
                    isSearching = true;
                    isLocationBasedUpdate = false;
                    fetchWeatherData(locationQuery);
                }
            } else if (resultCode == RESULT_CANCELED) {
                if (checkLocationPermission() && isLocationEnabled()) {
                    isSearching = false;
                    isLocationBasedUpdate = true;
                    searchView.setQuery("", false);
                    searchView.clearFocus();
                    startLocationUpdates();
                }
            }
        } else if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            if (isLocationEnabled()) {
                Log.d("MainActivity", "Location enabled after settings, starting updates");
                showLoading();
                startLocationUpdates();
            } else {
                Log.d("MainActivity", "Location still disabled after settings");
                showError("Please turn on your location");
                queueErrorMessage("Please turn on your location");
            }
        }
    }

    private void saveWidgetData(String city, String temperature) {
        SharedPreferences prefs = getSharedPreferences("WeatherPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("widget_city", city);
        editor.putString("widget_temp", temperature);
        editor.apply();
    }

    private void updateAllWidgets(String city, String temperature) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        ComponentName thisWidget = new ComponentName(this, WeatherWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int appWidgetId : appWidgetIds) {
            WeatherWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId, city, temperature);
        }
    }

    private void setupButterflyAnimation(LottieAnimationView butterflyAnimation) {
        // Set initial properties
        butterflyAnimation.setRepeatCount(LottieDrawable.INFINITE);
        butterflyAnimation.setRepeatMode(LottieDrawable.RESTART);
        butterflyAnimation.playAnimation();

        // Click listener for butterfly
        butterflyAnimation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a translation animation
                butterflyAnimation.animate()
                        .translationXBy(300f)  // Move right
                        .translationYBy(-150f) // Move up
                        .setDuration(2000)     // 2 seconds duration
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                // After flying right, fly left and down
                                butterflyAnimation.animate()
                                        .translationXBy(-300f)  // Move back left
                                        .translationYBy(150f)   // Move back down
                                        .setDuration(2000)      // 2 seconds duration
                                        .start();
                            }
                        })
                        .start();
            }
        });


        butterflyAnimation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save original position
                final float originalX = butterflyAnimation.getX();
                final float originalY = butterflyAnimation.getY();

                // Create a translation animation with rotation
                butterflyAnimation.animate()
                        .translationXBy(300f)  // Move right
                        .translationYBy(-150f) // Move up
                        .rotation(20f)         // Tilt right
                        .setDuration(2000)     // 2 seconds duration
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                // After flying right, fly left and down
                                butterflyAnimation.animate()
                                        .translationXBy(-300f)  // Move back left
                                        .translationYBy(150f)   // Move back down
                                        .rotation(-20f)         // Tilt left
                                        .setDuration(2000)      // 2 seconds duration
                                        .withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                // Reset to original position and rotation
                                                butterflyAnimation.setX(originalX);
                                                butterflyAnimation.setY(originalY);
                                                butterflyAnimation.setRotation(0f);
                                            }
                                        })
                                        .start();
                            }
                        })
                        .start();
            }
        });

    }



    interface WeatherService {
        @GET("current.json")
        Call<WeatherResponse> getCurrentWeather(
                @Query("key") String apiKey,
                @Query("q") String query
        );

        @GET("forecast.json")
        Call<WeatherResponse> getForecast(
                @Query("key") String apiKey,
                @Query("q") String query,
                @Query("days") int days
        );

        @GET("forecast.json")
        Call<HourlyWeatherResponse> getHourlyWeather(
                @Query("key") String apiKey,
                @Query("q") String query,
                @Query("days") int days
        );

        @GET("astronomy.json")
        Call<WeatherApiResponse> getAstronomyData(
                @Query("key") String apiKey,
                @Query("q") String query
        );
    }
}