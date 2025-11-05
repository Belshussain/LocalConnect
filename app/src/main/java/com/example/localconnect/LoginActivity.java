// app/src/main/java/com/example/localconnect/LoginActivity.java
package com.example.localconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.localconnect.NetworkUtils;


import com.example.localconnect.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;
    private FirebaseAuth auth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        auth = FirebaseAuth.getInstance();

        // Prefill email if coming from RegisterActivity
        String prefillEmail = getIntent().getStringExtra("prefill_email");
        if (prefillEmail != null) {
            b.email.setText(prefillEmail);
        }

        // Language selector
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"English (en)", "Zulu (zu)", "Xhosa (xh)"});
        b.langSpinner.setAdapter(adapter);
        b.langApply.setOnClickListener(v -> {
            int pos = b.langSpinner.getSelectedItemPosition();
            String code = pos == 1 ? "zu" : pos == 2 ? "xh" : "en";
            LanguageManager.setLocale(this, code, true);
            recreate();
        });

        // Password visibility toggle
        b.passwordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Hide password
                b.password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                b.passwordToggle.setImageResource(android.R.drawable.ic_menu_view);
                isPasswordVisible = false;
            } else {
                // Show password
                b.password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                b.passwordToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                isPasswordVisible = true;
            }
            // Move cursor to end of text
            b.password.setSelection(b.password.getText().length());
        });

        // Login button
        b.loginBtn.setOnClickListener(v -> {
            String email = b.email.getText().toString().trim();
            String pass = b.password.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            // Check email format locally
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
                return;
            }

            // network check
            if (!NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show();
                return;
            }

            b.loginBtn.setEnabled(false);

            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                b.loginBtn.setEnabled(true);
                if (task.isSuccessful()) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this,
                            "Wrong email or password entered. Please try again or create an account.",
                            Toast.LENGTH_LONG).show();
                    Log.e("LoginActivity", "Login failed", task.getException());
                }
            });
        });

        // Register redirect
        b.goRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));

        // Forgot password
        b.forgotPassword.setOnClickListener(v -> {
            String email = b.email.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_email_for_reset), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
                return;
            }
            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, getString(R.string.reset_email_sent), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to send reset email. Please try again.", Toast.LENGTH_LONG).show();
                    Log.e("ForgotPassword", "Reset error", task.getException());
                }
            });
        });
    }
}