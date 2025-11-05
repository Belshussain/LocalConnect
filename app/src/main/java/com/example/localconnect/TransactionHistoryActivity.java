package com.example.localconnect;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TransactionHistoryActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ArrayList<String> txnList;
    private ArrayAdapter<String> adapter;
    private String targetUid; // who transactions to view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ListView listView = findViewById(R.id.transactionsListView);
        txnList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, txnList);
        listView.setAdapter(adapter);

        // Choose which user’s transactions to load
        if (getIntent().hasExtra("userUid")) {
            targetUid = getIntent().getStringExtra("userUid"); // aadmin case
        } else {
            targetUid = auth.getCurrentUser().getUid(); // normal user
        }

        loadTransactions();
    }

    private void loadTransactions() {
        db.collection("users").document(targetUid)
                .collection("transactions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    txnList.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : query) {
                        Double amount = doc.getDouble("amount");
                        String toUid = doc.getString("to");
                        String fromUid = doc.getString("from");
                        Long time = doc.getLong("timestamp");

                        if (amount == null || time == null) {
                            continue;
                        }

                        String date = sdf.format(new Date(time));

                        // If thiss user is the sender then show receiver info
                        if (targetUid.equals(fromUid)) {
                            fetchUserDetails(toUid, userInfo -> {
                                String entry = "Paid R" + amount + " to " + userInfo + " on " + date;
                                txnList.add(entry);
                                adapter.notifyDataSetChanged();
                            });
                        }
                        // If this user is the receiver then show sender info
                        else {
                            fetchUserDetails(fromUid, userInfo -> {
                                String entry = "Received R" + amount + " from " + userInfo + " on " + date;
                                txnList.add(entry);
                                adapter.notifyDataSetChanged();
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("TXN_HISTORY", "Error loading transactions", e));
    }

    // Lookup user name + phone
    private void fetchUserDetails(String uid, OnUserFetched callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        callback.onFetched(name + " (" + phone + ")");
                    } else {
                        callback.onFetched("Unknown User");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TXN_HISTORY", "Failed to fetch user " + uid, e);
                    callback.onFetched("Unknown User");
                });
    }

    // callback interface
    interface OnUserFetched {
        void onFetched(String userInfo);
    }
}
