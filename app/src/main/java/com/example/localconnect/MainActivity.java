// app/src/main/java/com/example/localconnect/MainActivity.java
package com.example.localconnect;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * When app launch:
 * - If logged in, fetch user role and direct to  correct dashboard.
 * - else open Login.
 */
public class MainActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                auth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
            String role = doc.getString("role");
            if ("seller".equals(role)) {
                startActivity(new Intent(this, DashboardSellerActivity.class));
            } else if ("admin".equals(role)) {
                startActivity(new Intent(this, DashboardAdminActivity.class));
            } else {
                startActivity(new Intent(this, DashboardCustomerActivity.class));
            }
            finish();
        }).addOnFailureListener(e -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
