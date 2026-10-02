package com.moneytp.sothuchi.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model giao dịch - giữ nguyên cấu trúc dữ liệu Firebase gốc:
 * danh_muc, mo_ta, ngay, so_tien, ghi_chu, thoi_gian
 */
public class Transaction implements Serializable {
    private String key;
    private String danh_muc;
    private String mo_ta;
    private String ngay;       // YYYY-MM-DD
    private long so_tien;
    private String ghi_chu;
    private long thoi_gian;
    private boolean isExpense; // true = chi-tieu, false = thu-nhap

    public Transaction() {
        // Required empty constructor for Firebase
    }

    public Transaction(String danh_muc, String mo_ta, String ngay, long so_tien, String ghi_chu) {
        this.danh_muc = danh_muc;
        this.mo_ta = mo_ta;
        this.ngay = ngay;
        this.so_tien = so_tien;
        this.ghi_chu = ghi_chu;
        this.thoi_gian = System.currentTimeMillis();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("danh_muc", danh_muc);
        map.put("mo_ta", mo_ta);
        map.put("ngay", ngay);
        map.put("so_tien", so_tien);
        map.put("ghi_chu", ghi_chu);
        map.put("thoi_gian", thoi_gian);
        return map;
    }

    // Getters & Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getDanh_muc() { return danh_muc; }
    public void setDanh_muc(String danh_muc) { this.danh_muc = danh_muc; }

    public String getMo_ta() { return mo_ta; }
    public void setMo_ta(String mo_ta) { this.mo_ta = mo_ta; }

    public String getNgay() { return ngay; }
    public void setNgay(String ngay) { this.ngay = ngay; }

    public long getSo_tien() { return so_tien; }
    public void setSo_tien(long so_tien) { this.so_tien = so_tien; }

    public String getGhi_chu() { return ghi_chu; }
    public void setGhi_chu(String ghi_chu) { this.ghi_chu = ghi_chu; }

    public long getThoi_gian() { return thoi_gian; }
    public void setThoi_gian(long thoi_gian) { this.thoi_gian = thoi_gian; }

    public boolean isExpense() { return isExpense; }
    public void setExpense(boolean expense) { isExpense = expense; }
}
