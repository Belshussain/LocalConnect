// app/src/main/java/com/example/localconnect/models/Transaction.java
package com.example.localconnect.models;

/**
 * Transaction document stored in Firestore under transactions/{id}.
 */
public class Transaction {
    public String id;        // Firestore docId
    public String fromUid;
    public String toUid;
    public double amount;
    public String method;    // "phone" or "qr"
    public String status;    // "success" or "failed"
    public Object timestamp; // Server timestamp (is stored as com.google.firebase.Timestamp)

    public Transaction() {}
}
