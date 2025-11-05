
package com.example.localconnect;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.localconnect.models.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class QRScanActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Start QR Scan
        com.journeyapps.barcodescanner.ScanOptions options = new com.journeyapps.barcodescanner.ScanOptions();
        options.setPrompt("Scan QR Code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        options.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity.class);
        barcodeLauncher.launch(options);
    }

    private final androidx.activity.result.ActivityResultLauncher<com.journeyapps.barcodescanner.ScanOptions> barcodeLauncher =
            registerForActivityResult(new com.journeyapps.barcodescanner.ScanContract(), result -> {
                if (result.getContents() != null) {
                    handleQrResult(result.getContents());
                } else {
                    finish();
                }
            });

    private void handleQrResult(String contents) {
        if (contents.startsWith("localconnect:pay:")) {
            String sellerUid = contents.replace("localconnect:pay:", "");
            showAmountDialog(sellerUid);
        } else {
            Toast.makeText(this, "Invalid QR", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showAmountDialog(String sellerUid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Payment Amount");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("Pay", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(amountStr);
            processPayment(sellerUid, amount);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> finish());
        builder.show();
    }

    private void processPayment(String sellerUid, double amount) {
        String uid = auth.getCurrentUser().getUid();

        // Fetch customer balance
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            double balance = doc.getDouble("balance");

            if (balance >= amount) {
                // Deduct from customer
                db.collection("users").document(uid).update("balance", balance - amount);

                // Add to seller
                db.collection("users").document(sellerUid).get().addOnSuccessListener(sellerDoc -> {
                    if (!sellerDoc.exists()) return;
                    double sellerBal = sellerDoc.getDouble("balance");
                    db.collection("users").document(sellerUid).update("balance", sellerBal + amount);

                    // Save transaction record
                    saveTransaction(uid, sellerUid, amount);

                    Toast.makeText(this, "Paid R" + amount + " successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            } else {
                Toast.makeText(this, getString(R.string.insufficient_funds), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTransaction(String customerUid, String sellerUid, double amount) {
        String txnId = db.collection("transactions").document().getId();
        long timestamp = System.currentTimeMillis();

        Map<String, Object> txn = new HashMap<>();
        txn.put("id", txnId);
        txn.put("from", customerUid);
        txn.put("to", sellerUid);
        txn.put("amount", amount);
        txn.put("timestamp", timestamp);

        // Save for global history
        db.collection("transactions").document(txnId).set(txn);

        // Save under customer
        db.collection("users").document(customerUid)
                .collection("transactions").document(txnId).set(txn);

        // Save under seller
        db.collection("users").document(sellerUid)
                .collection("transactions").document(txnId).set(txn);
    }
}
