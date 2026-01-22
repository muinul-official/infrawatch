package com.example.maintenance_dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    Button buttonLogin;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    TextView textView, errorMessage, forgotPassword;
    TabLayout loginTabs;
    private boolean isAdminLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(com.google.firebase.FirebaseApp.getInstance(), "infrawatch");
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        textView = findViewById(R.id.registerNow);
        errorMessage = findViewById(R.id.error_message);
        forgotPassword = findViewById(R.id.forgot_password);
        loginTabs = findViewById(R.id.loginTabs);

        // Tab selection listener
        loginTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isAdminLogin = (tab.getPosition() == 1); // Position 1 = Admin tab

                // Update button text based on selection
                if (isAdminLogin) {
                    buttonLogin.setText(R.string.admin_sign_in);
                } else {
                    buttonLogin.setText(R.string.sign_in);
                }

                // Hide error message when switching tabs
                errorMessage.setVisibility(View.GONE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        textView.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), Register.class);
            startActivity(intent);
        });

        buttonLogin.setOnClickListener(v -> {
            String email, password;
            email = editTextEmail.getText() != null ? editTextEmail.getText().toString().trim() : "";
            password = editTextPassword.getText() != null ? editTextPassword.getText().toString() : "";

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(Login.this, "Enter email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(Login.this, "Enter password", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        // Authentication successful - now check role in Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) {
                            Log.e("LOGIN_ERROR", "Current user is null after successful auth");
                            Toast.makeText(Login.this, "Authentication error. Please try again.", Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }

                        String userId = user.getUid();
                        Log.d("LOGIN_SUCCESS", "Firebase Auth successful for UID: " + userId);

                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(document -> {
                                    if (document.exists()) {
                                        String role = document.getString("role");
                                        Log.d("LOGIN_SUCCESS", "User role: " + role);

                                        if (isAdminLogin) {
                                            // Trying to login as admin
                                            if ("admin".equalsIgnoreCase(role)) {
                                                Toast.makeText(getApplicationContext(), "Admin Login Successful",
                                                        Toast.LENGTH_SHORT).show();
                                                // Redirect to DASHBOARD (MainActivity)
                                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                // Not an admin, deny access
                                                Toast.makeText(Login.this, "You are not authorized as admin",
                                                        Toast.LENGTH_SHORT).show();
                                                mAuth.signOut();
                                            }
                                        } else {
                                            // Regular user login - ENFORCE EMAIL VERIFICATION
                                            if (!user.isEmailVerified()) {
                                                Toast.makeText(Login.this,
                                                        "Please verify your email before logging in.",
                                                        Toast.LENGTH_LONG).show();
                                                mAuth.signOut();
                                                return;
                                            }

                                            Toast.makeText(getApplicationContext(), "Login Successful",
                                                    Toast.LENGTH_SHORT).show();
                                            // Redirect to User Map Dashboard
                                            Intent intent = new Intent(getApplicationContext(), UserMapActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    } else {
                                        // User document doesn't exist in Firestore
                                        Log.e("LOGIN_ERROR", "User document not found in Firestore");
                                        Toast.makeText(Login.this, "User data not found", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("LOGIN_ERROR", "Error fetching user role from Firestore: " + e.getMessage(),
                                            e);
                                    Toast.makeText(Login.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG)
                                            .show();
                                    mAuth.signOut();
                                });
                    })
                    .addOnFailureListener(e -> {
                        // Authentication failed
                        Log.e("LOGIN_ERROR", "Firebase Auth failed: " + e.getMessage(), e);
                        errorMessage.setVisibility(View.VISIBLE);
                        Toast.makeText(Login.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        forgotPassword.setOnClickListener(v -> {
            // Create a custom view for the dialog
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
            com.google.android.material.textfield.TextInputEditText emailInput = dialogView
                    .findViewById(R.id.emailInput);

            androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                    Login.this)
                    .setTitle("Reset Password")
                    .setMessage("Enter your email address and we'll send you a link to reset your password.")
                    .setView(dialogView)
                    .setPositiveButton("Send Reset Link", null) // Set to null initially
                    .setNegativeButton("Cancel", null)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                android.widget.Button button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(view -> {
                    String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(email)) {
                        emailInput.setError("Please enter your email");
                        emailInput.requestFocus();
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailInput.setError("Please enter a valid email");
                        emailInput.requestFocus();
                        return;
                    }

                    // Disable button to prevent multiple clicks
                    button.setEnabled(false);
                    button.setText("Sending...");

                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                button.setEnabled(true);
                                button.setText("Send Reset Link");

                                if (task.isSuccessful()) {
                                    Toast.makeText(Login.this, "Password reset email sent to " + email,
                                            Toast.LENGTH_LONG).show();
                                    dialog.dismiss();
                                } else {
                                    String errorMsg = task.getException() != null ? task.getException().getMessage()
                                            : "Unknown error";
                                    Toast.makeText(Login.this, "Failed: " + errorMsg,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                });
            });

            dialog.show();
        });
    }
}
