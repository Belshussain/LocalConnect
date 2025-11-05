
package com.example.localconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.localconnect.databinding.ActivityPaymentBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {
    private ActivityPaymentBinding b;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        b = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        b.payBtn.setOnClickListener(v -> makePayment());
    }

    private void makePayment() {
        String phone = b.phoneInput.getText().toString().trim();
        String amtStr = b.amountInput.getText().toString().trim();

        if (phone.isEmpty() || amtStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        String payerUid = auth.getCurrentUser().getUid();

        db.collection("users").document(payerUid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;

            double balance = doc.getDouble("balance");
            if (balance >= amount) {
                // Deduct from payer
                db.collection("users").document(payerUid).update("balance", balance - amount);

                // Credit to receiver if exists
                db.collection("users").whereEqualTo("phone", phone).limit(1).get().addOnSuccessListener(qs -> {
                    if (!qs.isEmpty()) {
                        String receiverUid = qs.getDocuments().get(0).getId();
                        double receiverBal = qs.getDocuments().get(0).getDouble("balance");

                        db.collection("users").document(receiverUid).update("balance", receiverBal + amount);

                        // Save transaction
                        saveTransaction(payerUid, receiverUid, amount);

                        Toast.makeText(this, getString(R.string.payment_success), Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Receiver not found", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, getString(R.string.insufficient_funds), Toast.LENGTH_SHORT).show();

                //  Redirect to Customer Dashboard
                Intent intent = new Intent(PaymentActivity.this, DashboardCustomerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void saveTransaction(String payerUid, String receiverUid, double amount) {
        String txnId = db.collection("transactions").document().getId();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> txn = new HashMap<>();
        txn.put("id", txnId);
        txn.put("from", payerUid);
        txn.put("to", receiverUid);
        txn.put("amount", amount);
        txn.put("timestamp", timestamp);

        // Global transaction log
        db.collection("transactions").document(txnId).set(txn);

        // Add under payer
        db.collection("users").document(payerUid)
                .collection("transactions").document(txnId).set(txn);

        // Add under receiver
        db.collection("users").document(receiverUid)
                .collection("transactions").document(txnId).set(txn);
    }
}
