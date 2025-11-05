package com.example.localconnect;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Dedicated activity for editing user profile (name and phone)
 */
public class ProfileEditActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private DocumentReference userRef;

    private EditText editName, editPhone;
    private TextView displayEmail;
    private Button saveButton, cancelButton;
    private TextView customerCareLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        String uid = auth.getCurrentUser().getUid();
        userRef = db.collection("users").document(uid);

        // Initialize views
        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        displayEmail = findViewById(R.id.displayEmail);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        customerCareLink = findViewById(R.id.customerCareLink);

        // Load current user data
        loadUserProfile();

        // Save button
        saveButton.setOnClickListener(v -> saveProfile());

        // Cancel button
        cancelButton.setOnClickListener(v -> finish());

        //Customer Care Link Listener
        customerCareLink.setOnClickListener(v -> sendSupportEmail());
    }

    private void loadUserProfile() {
        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                editName.setText(doc.getString("name"));
                editPhone.setText(doc.getString("phone"));
                displayEmail.setText(doc.getString("email"));
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfile() {
        String newName = editName.getText().toString().trim();
        String newPhone = editPhone.getText().toString().trim();

        // Validation
        if (newName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPhone.length() != 10 || !newPhone.matches("\\d{10}")) {
            Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button during save
        saveButton.setEnabled(false);

        // Update Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("phone", newPhone);

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to dashboard
                })
                .addOnFailureListener(e -> {
                    saveButton.setEnabled(true);
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    //Create an Intent to send an email to customer care, pre-filling user data
    private void sendSupportEmail() {
        // Get user data for context
        String name = editName.getText().toString().trim();
        String email = displayEmail.getText().toString();

        // The recipient email address
        String recipient = "admin2@gmail.com";
        String subject = "Support Request: Profile Edit Issue";
        String body = "Dear Support Team,\n\nI am experiencing an issue on the Edit Profile page.\n\nMy details are:\nName: " + name + "\nEmail: " + email + "\n\nIssue Description:\n[Please describe your issue here]\n";

        // Build mailto URI with all parameters
        String uriText = "mailto:" + Uri.encode(recipient) +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body);

        Uri uri = Uri.parse(uriText);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(uri);

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show();
        }
    }
}