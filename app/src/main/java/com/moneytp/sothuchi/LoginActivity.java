package com.moneytp.sothuchi;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.moneytp.sothuchi.util.FirebaseHelper;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        // Nếu đã đăng nhập thì vào Main
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            goToMain();
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> doAuth(true));
        btnRegister.setOnClickListener(v -> doAuth(false));
    }

    private void doAuth(boolean isLogin) {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (isLogin) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            afterAuthSuccess();
                        } else {
                            String msg = task.getException() != null ?
                                    task.getException().getMessage() : "Đăng nhập thất bại";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            afterAuthSuccess();
                        } else {
                            String msg = task.getException() != null ?
                                    task.getException().getMessage() : "Đăng ký thất bại";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void afterAuthSuccess() {
        firebaseHelper.ensureUserProfile(new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                goToMain();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();
                goToMain(); // Vẫn cho vào app
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
    }
}
