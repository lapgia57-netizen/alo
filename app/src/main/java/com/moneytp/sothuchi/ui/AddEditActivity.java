package com.moneytp.sothuchi.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.model.Transaction;
import com.moneytp.sothuchi.util.DataRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Thêm / Sửa chứng từ thu chi.
 */
public class AddEditActivity extends AppCompatActivity {

    private EditText etDanhMuc, etMoTa, etNgay, etSoTien, etGhiChu;
    private Button btnSave;
    private boolean isChi;
    private Transaction existing;
    private DataRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        isChi = getIntent().getBooleanExtra("isChi", true);
        existing = (Transaction) getIntent().getSerializableExtra("transaction");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(existing == null
                    ? (isChi ? "Thêm Chi tiêu" : "Thêm Thu nhập")
                    : "Sửa chứng từ");
        }

        repo = DataRepository.getInstance();

        etDanhMuc = findViewById(R.id.et_danh_muc);
        etMoTa = findViewById(R.id.et_mo_ta);
        etNgay = findViewById(R.id.et_ngay);
        etSoTien = findViewById(R.id.et_so_tien);
        etGhiChu = findViewById(R.id.et_ghi_chu);
        btnSave = findViewById(R.id.btn_save);

        if (existing != null) {
            etDanhMuc.setText(existing.getDanhMuc());
            etMoTa.setText(existing.getMoTa());
            etNgay.setText(existing.getNgay());
            etSoTien.setText(String.valueOf(existing.getSoTien()));
            etGhiChu.setText(existing.getGhiChu());
        } else {
            // Mặc định ngày hôm nay
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            etNgay.setText(today);
        }

        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String danhMuc = etDanhMuc.getText().toString().trim();
        String moTa = etMoTa.getText().toString().trim();
        String ngay = etNgay.getText().toString().trim();
        String soTienStr = etSoTien.getText().toString().trim();
        String ghiChu = etGhiChu.getText().toString().trim();

        if (danhMuc.isEmpty() || moTa.isEmpty() || ngay.isEmpty() || soTienStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        long soTien;
        try {
            soTien = Long.parseLong(soTienStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (soTien < 0 || soTien > 999_999_999_999L) {
            Toast.makeText(this, "Số tiền nằm ngoài khoảng cho phép", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ngay.matches("\\d{4}-\\d{2}-\\d{2}")) {
            Toast.makeText(this, "Ngày phải theo định dạng yyyy-MM-dd", Toast.LENGTH_SHORT).show();
            return;
        }

        Transaction t = existing != null ? existing : new Transaction();
        t.setDanhMuc(danhMuc);
        t.setMoTa(moTa);
        t.setNgay(ngay);
        t.setSoTien(soTien);
        t.setGhiChu(ghiChu.isEmpty() ? null : ghiChu);
        if (existing == null) {
            t.setThoiGian(System.currentTimeMillis());
        }

        btnSave.setEnabled(false);

        DataRepository.OnCompleteListener listener = new DataRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(AddEditActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                btnSave.setEnabled(true);
                Toast.makeText(AddEditActivity.this, "Lỗi: " + message, Toast.LENGTH_LONG).show();
            }
        };

        if (existing == null) {
            repo.addTransaction(isChi, t, listener);
        } else {
            repo.updateTransaction(isChi, t, listener);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
