package com.moneytp.sothuchi.model;

import java.io.Serializable;

/**
 * Model đại diện cho một chứng từ thu/chi.
 * Tối ưu: immutable-ish, equals/hashCode cho DiffUtil nhanh.
 */
public class Transaction implements Serializable {
    private String key;
    private String danhMuc;
    private String moTa;
    private String ngay;          // yyyy-MM-dd
    private long soTien;
    private String ghiChu;
    private long thoiGian;        // timestamp ms

    public Transaction() {
        // Required for Firebase
    }

    public Transaction(String key, String danhMuc, String moTa, String ngay, long soTien, String ghiChu, long thoiGian) {
        this.key = key;
        this.danhMuc = danhMuc;
        this.moTa = moTa;
        this.ngay = ngay;
        this.soTien = soTien;
        this.ghiChu = ghiChu;
        this.thoiGian = thoiGian;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getDanhMuc() { return danhMuc; }
    public void setDanhMuc(String danhMuc) { this.danhMuc = danhMuc; }

    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }

    public String getNgay() { return ngay; }
    public void setNgay(String ngay) { this.ngay = ngay; }

    public long getSoTien() { return soTien; }
    public void setSoTien(long soTien) { this.soTien = soTien; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public long getThoiGian() { return thoiGian; }
    public void setThoiGian(long thoiGian) { this.thoiGian = thoiGian; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return soTien == that.soTien &&
                thoiGian == that.thoiGian &&
                java.util.Objects.equals(key, that.key) &&
                java.util.Objects.equals(danhMuc, that.danhMuc) &&
                java.util.Objects.equals(moTa, that.moTa) &&
                java.util.Objects.equals(ngay, that.ngay) &&
                java.util.Objects.equals(ghiChu, that.ghiChu);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, danhMuc, moTa, ngay, soTien, ghiChu, thoiGian);
    }
}
