package com.moneytp.sothuchi;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.moneytp.sothuchi.model.Transaction;
import com.moneytp.sothuchi.util.FirebaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_KEY = "key";
    public static final String EXTRA_TRANSACTION = "transaction";

    private TextInputEditText etDate, etCategory, etDescription, etAmount, etNote;
    private MaterialButton btnSave, btnCancel;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private FirebaseHelper firebaseHelper;
    private String type;
    private String editKey;
    private boolean isEdit = false;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        firebaseHelper = new FirebaseHelper();
        if (!firebaseHelper.isLoggedIn()) {
            finish();
            return;
        }

        type = getIntent().getStringExtra(EXTRA_TYPE);
        if (type == null) type = FirebaseHelper.TYPE_CHI;

        editKey = getIntent().getStringExtra(EXTRA_KEY);
        isEdit = editKey != null && !editKey.isEmpty();

        bindViews();
        setupDatePicker();

        if (isEdit) {
            tvTitle.setText("Sửa giao dịch");
            Transaction t = (Transaction) getIntent().getSerializableExtra(EXTRA_TRANSACTION);
            if (t != null) populateForm(t);
        } else {
            tvTitle.setText(FirebaseHelper.TYPE_CHI.equals(type) ? "Thêm chi tiêu" : "Thêm thu nhập");
            etDate.setText(dateFormat.format(calendar.getTime()));
        }

        btnSave.setOnClickListener(v -> save());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvTitle);
        etDate = findViewById(R.id.etDate);
        etCategory = findViewById(R.id.etCategory);
        etDescription = findViewById(R.id.etDescription);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupDatePicker() {
        etDate.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        etDate.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });
    }

    private void populateForm(Transaction t) {
        etDate.setText(t.getNgay() != null ? t.getNgay() : "");
        etCategory.setText(t.getDanh_muc() != null ? t.getDanh_muc() : "");
        etDescription.setText(t.getMo_ta() != null ? t.getMo_ta() : "");
        etAmount.setText(String.valueOf(t.getSo_tien()));
        etNote.setText(t.getGhi_chu() != null ? t.getGhi_chu() : "");
        if (t.getNgay() != null) {
            try {
                calendar.setTime(dateFormat.parse(t.getNgay()));
            } catch (Exception ignored) { }
        }
    }

    private void save() {
        String ngay = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String danhMuc = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
        String moTa = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String ghiChu = etNote.getText() != null ? etNote.getText().toString().trim() : null;

        if (TextUtils.isEmpty(danhMuc) || TextUtils.isEmpty(moTa) || TextUtils.isEmpty(ngay)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ngay.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Ngày không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        long soTien;
        try {
            soTien = Long.parseLong(amountStr);
            if (soTien < 0 || soTien > 999999999999L) {
                Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Giới hạn độ dài giống app gốc
        if (danhMuc.length() > 100) danhMuc = danhMuc.substring(0, 100);
        if (moTa.length() > 300) moTa = moTa.substring(0, 300);
        if (ghiChu != null && ghiChu.isEmpty()) ghiChu = null;
        if (ghiChu != null && ghiChu.length() > 80) ghiChu = ghiChu.substring(0, 80);

        Transaction transaction = new Transaction(danhMuc, moTa, ngay, soTien, ghiChu);

        setLoading(true);

        FirebaseHelper.SimpleCallback callback = new FirebaseHelper.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(AddEditActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        };

        if (isEdit) {
            firebaseHelper.updateTransaction(type, editKey, transaction, callback);
        } else {
            firebaseHelper.addTransaction(type, transaction, callback);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
    }
}
