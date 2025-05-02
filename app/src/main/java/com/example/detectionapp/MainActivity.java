package com.example.detectionapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;



public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ImageView logo = findViewById(R.id.appLogo);
        Animation zoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in_out);
        logo.startAnimation(zoomAnim);
        setupPasswordToggle();
    }
    
    private void setupPasswordToggle() {
        EditText passwordEditText = findViewById(R.id.editTextPassword);
        ImageView toggleIcon = findViewById(R.id.passwordToggle);
    
        // Set the initial icon based on the current InputType
        if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            toggleIcon.setImageResource(R.drawable.invisible); // Password is hidden
        } else {
            toggleIcon.setImageResource(R.drawable.visible); // Password is visible
        }
    
        toggleIcon.setOnClickListener(v -> {
            if (passwordEditText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                // Show password
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                toggleIcon.setImageResource(R.drawable.visible);
            } else {
                // Hide password
                passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                toggleIcon.setImageResource(R.drawable.invisible);
            }
            // Move the cursor to the end of the text
            passwordEditText.setSelection(passwordEditText.getText().length());
        });
    }

    public void validateAccount(View v) {
        EditText usernameEditText = findViewById(R.id.editTextEmail);
        EditText passwordEditText = findViewById(R.id.editTextPassword);
        String passwordInput = passwordEditText.getText().toString();
        String emailInput = usernameEditText.getText().toString();

        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Input Error")
                    .setMessage("Please fill in all required fields!")
                    .setPositiveButton("OK", null)
                    .show();
        }
        else {
            if (!emailInput.equals(getString(R.string.email))) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("INVALID EMAIL")
                        .setMessage("Try again please!")
                        .setPositiveButton("OK", null)
                        .show();
                usernameEditText.setError("Incorrect email!");
            } else {
                if (!passwordInput.equals(getString(R.string.password))) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("INVALID PASSWORD")
                            .setMessage("Try again please!")
                            .setPositiveButton("OK", null)
                            .show();
                    passwordEditText.setError("Incorrect password!");
                } else {
                    Intent intent = new Intent(MainActivity.this, NavBotActivity.class);
                    startActivity(intent);
                }
            }
        }
    }
}