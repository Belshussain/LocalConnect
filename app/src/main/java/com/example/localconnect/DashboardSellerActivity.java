package com.example.localconnect;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.localconnect.databinding.ActivityDashboardSellerBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.WriterException;

/**
 * Seller dashboard with improved UI and separate profile editing
 */
public class DashboardSellerActivity extends AppCompatActivity {
    private ActivityDashboardSellerBinding b;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private DocumentReference meRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        b = ActivityDashboardSellerBinding.inflate(getLayoutInflater());
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

        // Live balance + prefill profile
        meRef.addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                String name = doc.getString("name");
                Double balance = doc.getDouble("balance");

                b.title.setText(getString(R.string.seller_dashboard_title, name == null ? "" : name));
                b.balance.setText(getString(R.string.balance_fmt, balance == null ? 0.0 : balance));

                // Store in hidden fields for compatibility
                b.editName.setText(name);
                b.editEmail.setText(doc.getString("email"));
                b.editPhone.setText(doc.getString("phone"));
            }
        });

        // Navigate to Profile Edit Activity
        b.updateProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileEditActivity.class))
        );

        // Generate QR for seller UID
        String payload = "localconnect:pay:" + uid;
        try {
            Bitmap bmp = Utils.generateQr(payload, 800);
            b.qrImage.setImageBitmap(bmp);
            b.qrPayload.setText(payload);
        } catch (WriterException ex) {
            Toast.makeText(this, getString(R.string.qr_generate_failed), Toast.LENGTH_SHORT).show();
        }

        // View transactions
        b.viewTransactions.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class))
        );

        // Logout
        b.logout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}