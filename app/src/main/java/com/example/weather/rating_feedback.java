package com.example.weather;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

public class rating_feedback extends AppCompatActivity {

    private EditText userIdEditText, feedbackEditText;
    private RatingBar ratingBar;
    private AppCompatButton sendButton;
    private TextView myEmailText;
    private CardView cardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating_feedback);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.blue));
        }

        // Initialize views
        userIdEditText = findViewById(R.id.enter_user_Id);
        feedbackEditText = findViewById(R.id.feedbackEditText);
        ratingBar = findViewById(R.id.ratingBar);
        sendButton = findViewById(R.id.sendButton);
        myEmailText = findViewById(R.id.my_id_txt);
        cardView = findViewById(R.id.cardview_rating);

        // Apply slide-down animation
        Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
        cardView.startAnimation(slideDown);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFeedbackEmail();
            }
        });

        TextView back_btn_rating = findViewById(R.id.back_btn_rating);

        back_btn_rating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(rating_feedback.this, MainActivity.class);
                intent.putExtra("fromBackNavigation", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void sendFeedbackEmail() {
        // Get user inputs
        String userId = userIdEditText.getText().toString().trim();
        String feedback = feedbackEditText.getText().toString().trim();
        float rating = ratingBar.getRating();
        String recipientEmail = myEmailText.getText().toString().trim();

        // Validate inputs
        if (userId.isEmpty() || feedback.isEmpty() || rating == 0) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create email content
        String subject = "App Feedback from User ID: " + userId;
        String message = "User ID: " + userId + "\n\n" +
                "Rating: " + rating + "/5\n\n" +
                "Feedback:\n" + feedback;

        // Create email intent
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipientEmail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));

            // Show a confirmation message to the user
            Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }

}
}


