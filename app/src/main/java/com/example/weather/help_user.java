package com.example.weather;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class help_user extends AppCompatActivity {

    private TextView[] helpTextViews;
    private LinearLayout[] helpLayouts;
    private int selectedTextViewId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_user);


        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.blue));
        }

        // Initialize arrays
        helpTextViews = new TextView[]{
                findViewById(R.id.Help_Text1),
                findViewById(R.id.Help_Text2),
                findViewById(R.id.Help_Text3),
                findViewById(R.id.Help_Text4),
                findViewById(R.id.Help_Text5),
                findViewById(R.id.Help_Text6),
                findViewById(R.id.Help_Text7),
                findViewById(R.id.Help_Text8),
                findViewById(R.id.Help_Text9),
                findViewById(R.id.Help_Text10),
                findViewById(R.id.Help_Text11)
        };

        helpLayouts = new LinearLayout[]{
                findViewById(R.id.Help_layout_1),
                findViewById(R.id.Help_layout_2),
                findViewById(R.id.Help_layout_3),
                findViewById(R.id.Help_layout_4),
                findViewById(R.id.Help_layout_5),
                findViewById(R.id.Help_layout_6),
                findViewById(R.id.Help_layout_7),
                findViewById(R.id.Help_layout_8),
                findViewById(R.id.Help_layout_9),
                findViewById(R.id.Help_layout_10),
                findViewById(R.id.Help_layout_11)
        };

        // Set click listeners for all TextViews
        for (int i = 0; i < helpTextViews.length; i++) {
            final int index = i;
            helpTextViews[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleLayout(index);
                }
            });

            TextView back_btn = findViewById(R.id.back_btn_Help);

            back_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(help_user.this, MainActivity.class);
                    intent.putExtra("fromBackNavigation", true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    private void toggleLayout(int index) {
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);

        for (int i = 0; i < helpLayouts.length; i++) {
            if (i == index) continue; // skip current one

            if (helpLayouts[i].getVisibility() == View.VISIBLE) {
                helpLayouts[i].startAnimation(fadeOut);
                helpLayouts[i].setVisibility(View.GONE);
            }

            helpTextViews[i].setTextSize(18);
            helpTextViews[i].setTextColor(getResources().getColor(R.color.help_text_default));

            Drawable defaultArrow = getTintedDrawable(R.drawable.vector_bottom_arrow, R.color.help_text_default);
            helpTextViews[i].setCompoundDrawablesWithIntrinsicBounds(null, null, defaultArrow, null);
        }

        if (selectedTextViewId == index) {
            selectedTextViewId = -1;
        } else {
            helpLayouts[index].startAnimation(slideIn);
            helpLayouts[index].setVisibility(View.VISIBLE);

            helpTextViews[index].setTextSize(25);
            helpTextViews[index].setTextColor(getResources().getColor(R.color.help_text_selected));

            Drawable coloredArrow = getTintedDrawable(R.drawable.vector_bottom_arrow, R.color.help_text_selected);
            helpTextViews[index].setCompoundDrawablesWithIntrinsicBounds(null, null, coloredArrow, null);

            selectedTextViewId = index;
        }
    }
    private Drawable getTintedDrawable(int drawableResId, int colorResId) {
        Drawable drawable = getResources().getDrawable(drawableResId, getTheme());
        drawable = drawable.mutate(); // So it doesn’t affect other views using this drawable
        drawable.setTint(getResources().getColor(colorResId, getTheme()));
        return drawable;
    }



}