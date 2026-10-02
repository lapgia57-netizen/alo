package com.moneytp.sothuchi.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.moneytp.sothuchi.R;

/**
 * Màn hình đăng nhập / đăng ký email-password (giống bản web).
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private ProgressBar progress;
    private TextView tvStatus;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        // Nếu đã login thì vào luôn
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            goToMain();
            return;
        }

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        progress = findViewById(R.id.progress);
        tvStatus = findViewById(R.id.tv_status);

        btnLogin.setOnClickListener(v -> doAuth(true));
        btnRegister.setOnClickListener(v -> doAuth(false));
    }

    private void doAuth(boolean isLogin) {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu tối thiểu 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (isLogin) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            goToMain();
                        } else {
                            showError(task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Đăng nhập thất bại");
                        }
                    });
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            tvStatus.setText("Đăng ký thành công! Đang vào ứng dụng...");
                            tvStatus.setVisibility(View.VISIBLE);
                            goToMain();
                        } else {
                            showError(task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Đăng ký thất bại");
                        }
                    });
        }
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnRegister.setEnabled(!loading);
    }

    private void showError(String msg) {
        tvStatus.setText(msg);
        tvStatus.setVisibility(View.VISIBLE);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
