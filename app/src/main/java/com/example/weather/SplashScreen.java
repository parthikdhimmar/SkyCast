package com.example.weather;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;


public class SplashScreen extends AppCompatActivity {

    private static final int SPLASH_DURATION = 4000; // 4 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.blue));
        }




        // Initialize views
        TextView appName = findViewById(R.id.app_name);
        ImageView cloud1 = findViewById(R.id.cloud1);
        ImageView cloud2 = findViewById(R.id.cloud2);
        ImageView cloud3 = findViewById(R.id.cloud3);
        ImageView cloud4 = findViewById(R.id.cloud4);
        ImageView sun = findViewById(R.id.sun);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideFromRight = AnimationUtils.loadAnimation(this, R.anim.slide_from_right);
        Animation slideFromLeft = AnimationUtils.loadAnimation(this, R.anim.slide_from_left);
        Animation slideFromTop = AnimationUtils.loadAnimation(this, R.anim.slide_from_top);
        Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate);
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);

        // Set animations
        appName.startAnimation(fadeIn);
        cloud1.startAnimation(slideFromRight);
        cloud2.startAnimation(slideFromLeft);
        cloud3.startAnimation(slideFromRight);
        cloud4.startAnimation(slideFromLeft);


        new Handler().postDelayed(() -> {
            sun.setVisibility(View.VISIBLE);
            sun.startAnimation(rotate);
        }, 1000);

        // Start main activity after delay
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DURATION);
    }
}
