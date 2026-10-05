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
import com.google.firebase.auth.FirebaseAuth;
import com.moneytp.sothuchi.model.Transaction;
import com.moneytp.sothuchi.util.DataRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Thêm / Sửa giao dịch — Material TextInput, validation chặt.
 */
public class AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_KEY = "key";
    public static final String EXTRA_TRANSACTION = "transaction";

    private TextInputEditText etDate, etCategory, etDescription, etAmount, etNote;
    private MaterialButton btnSave, btnCancel;
    private ProgressBar progressBar;
    private TextView tvTitle;

    private DataRepository repository;
    private String type;
    private String editKey;
    private boolean isEdit = false;
    private boolean isChi = true;
    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        repository = DataRepository.getInstance();

        type = getIntent().getStringExtra(EXTRA_TYPE);
        if (type == null) type = DataRepository.NODE_CHI;
        isChi = DataRepository.NODE_CHI.equals(type);

        editKey = getIntent().getStringExtra(EXTRA_KEY);
        isEdit = editKey != null && !editKey.isEmpty();

        bindViews();
        setupDatePicker();

        if (isEdit) {
            tvTitle.setText("Sửa giao dịch");
            Transaction t = (Transaction) getIntent().getSerializableExtra(EXTRA_TRANSACTION);
            if (t != null) populateForm(t);
        } else {
            tvTitle.setText(isChi ? "Thêm chi tiêu" : "Thêm thu nhập");
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
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                etDate.setText(dateFormat.format(calendar.getTime()));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
        etDate.setFocusable(false);
        etDate.setClickable(true);
    }

    private void populateForm(Transaction t) {
        if (t.getNgay() != null) etDate.setText(t.getNgay());
        if (t.getDanh_muc() != null) etCategory.setText(t.getDanh_muc());
        if (t.getMo_ta() != null) etDescription.setText(t.getMo_ta());
        etAmount.setText(String.valueOf(t.getSo_tien()));
        if (t.getGhi_chu() != null) etNote.setText(t.getGhi_chu());
    }

    private void save() {
        String danhMuc = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
        String moTa = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String ngay = etDate.getText() != null ? etDate.getText().toString().trim() : "";
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
            soTien = Long.parseLong(amountStr.replaceAll("[^0-9]", ""));
            if (soTien < 0 || soTien > 999_999_999_999L) {
                Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (danhMuc.length() > 100) danhMuc = danhMuc.substring(0, 100);
        if (moTa.length() > 300) moTa = moTa.substring(0, 300);
        if (ghiChu != null && ghiChu.isEmpty()) ghiChu = null;
        if (ghiChu != null && ghiChu.length() > 80) ghiChu = ghiChu.substring(0, 80);

        Transaction transaction = new Transaction(danhMuc, moTa, ngay, soTien, ghiChu);
        transaction.setExpense(isChi);

        setLoading(true);

        DataRepository.OnCompleteListener callback = new DataRepository.OnCompleteListener() {
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
            repository.updateTransaction(isChi, editKey, transaction, callback);
        } else {
            repository.addTransaction(isChi, transaction, callback);
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
    }
}
