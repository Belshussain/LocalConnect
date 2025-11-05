// app/src/main/java/com/example/localconnect/RegisterActivity.java
package com.example.localconnect;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.example.localconnect.NetworkUtils;


import com.example.localconnect.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers a user and stores their profile in Firestore with balance default of 25000.
 */
public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding b;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Role selector dropdown
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"customer", "seller"});
        b.roleSpinner.setAdapter(roleAdapter);

        b.registerBtn.setOnClickListener(v -> registerUser());
    }

    /**
     * Handles click on "Login" text to navigate back to LoginActivity
     */
    public void goToLogin(View view) {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close register activity
    }

    private void registerUser() {
        String name = b.name.getText().toString().trim();
        String phone = b.phone.getText().toString().trim();
        String email = b.email.getText().toString().trim();
        String pass = b.password.getText().toString().trim();
        String confirmPass = b.confirmPassword.getText().toString().trim(); // new confirm password field
        String role = b.roleSpinner.getSelectedItem().toString();

        //  network check
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate empty fields
        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate phone number: must be 10 digits
        if (phone.length() != 10 || !phone.matches("\\d{10}")) {
            Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate confirm password
        if (!pass.equals(confirmPass)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        b.registerBtn.setEnabled(false);

        // Check if phone already exists in Firestore
        Query phoneQuery = db.collection("users").whereEqualTo("phone", phone);
        phoneQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                b.registerBtn.setEnabled(true);
                Toast.makeText(this, "Phone number already registered. Please login.", Toast.LENGTH_LONG).show();

                // Redirect to Login with phone's email prefill (if user typed it)
                if (!email.isEmpty()) {
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra("prefill_email", email);
                    startActivity(intent);
                    finish();
                }
            } else {
                // Create Firebase Auth user (checks email)
                auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(authTask -> {
                    b.registerBtn.setEnabled(true);

                    if (!authTask.isSuccessful() || auth.getCurrentUser() == null) {
                        if (authTask.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Email already registered. Please login.", Toast.LENGTH_LONG).show();

                            // Redirect to Login with email pre-filled
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.putExtra("prefill_email", email);
                            startActivity(intent);
                            finish();
                        } else {
                            String error = (authTask.getException() != null) ? authTask.getException().getMessage() : "Unknown error";
                            Toast.makeText(this, "Registration failed: " + error, Toast.LENGTH_LONG).show();
                            Log.e("RegisterActivity", "Registration error", authTask.getException());
                        }
                        return;
                    }

                    String uid = auth.getCurrentUser().getUid();

                    // Save user data in Firestore
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("uid", uid);
                    userMap.put("name", name);
                    userMap.put("phone", phone);
                    userMap.put("email", email);
                    userMap.put("role", role);
                    userMap.put("balance", 25000.0);

                    db.collection("users").document(uid).set(userMap)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e("RegisterActivity", "Firestore error", e);
                            });
                });
            }
        });
    }
}