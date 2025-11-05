// app/src/main/java/com/example/localconnect/models/User.java
package com.example.localconnect.models;

/**
 * User document stored in Firestore under users/{uid}.
 */
public class User {
    public String uid;
    public String name;
    public String email;
    public String phone;
    public String role;    // "customer", "seller", "admin"
    public double balance;

    public User() {} // Need fr Firestore

    public User(String uid, String name, String email, String phone, String role, double balance) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.balance = balance;
    }
}
