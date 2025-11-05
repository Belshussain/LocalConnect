package com.example.localconnect;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.localconnect.databinding.ActivityDashboardCustomerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Customer dashboard with improved UI and separate profile editing
 */
public class DashboardCustomerActivity extends AppCompatActivity {
    private ActivityDashboardCustomerBinding b;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private DocumentReference meRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        b = ActivityDashboardCustomerBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        String uid = auth.getCurrentUser().getUid();
        meRef = db.collection("users").document(uid);

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

        // Live user info
        meRef.addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                String name = doc.getString("name");
                Double balance = doc.getDouble("balance");

                b.welcome.setText(getString(R.string.welcome_user, name == null ? "" : name));
                b.balance.setText(getString(R.string.balance_fmt, balance == null ? 0.0 : balance));

                // Store data in hidden fields for compatibility
                b.editName.setText(name);
                b.editEmail.setText(doc.getString("email"));
                b.editPhone.setText(doc.getString("phone"));
            }
        });

        // Action buttons
        b.payByPhone.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentActivity.class))
        );

        b.scanQr.setOnClickListener(v ->
                startActivity(new Intent(this, QRScanActivity.class))
        );

        b.viewTransactions.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class))
        );

        // Navigate to Profile Edit Activity
        b.updateProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileEditActivity.class))
        );

        // Logout
        b.logout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}