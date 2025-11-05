package com.example.localconnect;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * admin dashboard which shows name, email and phone as well as a list of users for editing.
 */

public class DashboardAdminActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private DocumentReference meRef;

    private EditText searchInput, editName, editPhone;
    private Button searchBtn, viewAllBtn, logoutBtn, updateProfileBtn;
    private ListView resultsList;
    private TextView welcomeText, editEmail;

    private ArrayList<String> usersList;
    private ArrayList<String> userIds;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageManager.applySavedLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_admin);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();
        meRef = db.collection("users").document(uid);

        // UI references
        searchInput = findViewById(R.id.searchInput);
        searchBtn = findViewById(R.id.searchBtn);
        viewAllBtn = findViewById(R.id.viewAllBtn);
        logoutBtn = findViewById(R.id.logout);
        resultsList = findViewById(R.id.resultsList);
        welcomeText = findViewById(R.id.welcomeAdmin);

        // Profile fields
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail); // Display only
        editPhone = findViewById(R.id.editPhone);
        updateProfileBtn = findViewById(R.id.updateProfile);

        usersList = new ArrayList<>();
        userIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, usersList);
        resultsList.setAdapter(adapter);

        // Show logged in admin’s email
        if (auth.getCurrentUser() != null && welcomeText != null) {
            String email = auth.getCurrentUser().getEmail();
            welcomeText.setText("Welcome Admin: " + email);
        }

        // Prefill admin profile
        meRef.addSnapshotListener((doc, e) -> {
            if (doc != null && doc.exists()) {
                editName.setText(doc.getString("name"));
                editEmail.setText(doc.getString("email"));
                editPhone.setText(doc.getString("phone"));
            }
        });

        // Update profile
        updateProfileBtn.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newPhone = editPhone.getText().toString().trim();

            if (newName.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPhone.length() != 10 || !newPhone.matches("\\d{10}")) {
                Toast.makeText(this, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show();
                return;
            }

            updateFirestoreProfile(uid, newName, newPhone);
        });

        // Search users
        searchBtn.setOnClickListener(v -> searchUsers());

        // View all users
        viewAllBtn.setOnClickListener(v -> loadAllUsers());

        // User list click listener
        resultsList.setOnItemClickListener((parent, view, position, id) -> {
            String userId = userIds.get(position);
            showUserOptionsDialog(userId, position);
        });

        // Logout
        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void updateFirestoreProfile(String uid, String name, String phone) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);

        meRef.update(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void searchUsers() {
        String queryText = searchInput.getText().toString().trim();
        if (queryText.isEmpty()) {
            Toast.makeText(this, "Enter name or phone to search", Toast.LENGTH_SHORT).show();
            return;
        }

        usersList.clear();
        userIds.clear();

        // Search by name
        db.collection("users").whereEqualTo("name", queryText).get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        addUserResult(doc);
                    }
                    adapter.notifyDataSetChanged();
                });

        // Search by phone
        db.collection("users").whereEqualTo("phone", queryText).get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        addUserResult(doc);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadAllUsers() {
        usersList.clear();
        userIds.clear();

        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                addUserResult(doc);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void addUserResult(QueryDocumentSnapshot doc) {
        String uid = doc.getId();
        String name = doc.getString("name");
        String phone = doc.getString("phone");
        String role = doc.getString("role");
        Double balance = doc.getDouble("balance");

        usersList.add(name + " (" + phone + ") - " + role + " | Balance: R" + balance);
        userIds.add(uid);
    }

    private void showUserOptionsDialog(String uid, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Action");
        String[] options = {"Update Balance", "Delete User", "View Transactions"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Update Balance
                    showBalanceUpdateDialog(uid, position);
                    break;
                case 1: // Delete User
                    deleteUser(uid, position);
                    break;
                case 2: // View Transactions
                    Intent intent = new Intent(this, TransactionHistoryActivity.class);
                    intent.putExtra("userUid", uid);
                    startActivity(intent);
                    break;
            }
        });
        builder.show();
    }

    private void showBalanceUpdateDialog(String uid, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter new balance");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newBalanceStr = input.getText().toString().trim();
            if (newBalanceStr.isEmpty()) {
                Toast.makeText(this, "Balance cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            double newBalance = Double.parseDouble(newBalanceStr);

            db.collection("users").document(uid)
                    .update("balance", newBalance)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Balance updated", Toast.LENGTH_SHORT).show();
                        loadAllUsers();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ADMIN", "Error updating balance", e);
                        Toast.makeText(this, "Failed to update balance", Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void deleteUser(String uid, int position) {
        db.collection("users").document(uid).delete()
                .addOnSuccessListener(aVoid -> {
                    usersList.remove(position);
                    userIds.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("ADMIN", "Error deleting user", e);
                    Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                });
    }
}
