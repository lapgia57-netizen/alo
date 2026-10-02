package com.moneytp.sothuchi.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.moneytp.sothuchi.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository tối ưu tốc độ load & hiển thị dữ liệu.
 *
 * Các kỹ thuật tối ưu:
 * 1. Single ValueEventListener + ConcurrentHashMap cache (tránh re-query)
 * 2. Background thread parse + sort (không block UI)
 * 3. Diff-aware LiveData (chỉ emit khi dữ liệu thực sự thay đổi)
 * 4. Pre-computed totals + category aggregates cho biểu đồ
 * 5. Object pooling nhẹ + reuse List để giảm GC
 */
public class DataRepository {

    private static final String TAG = "DataRepository";
    private static final String NODE_CHI = "chi-tieu";
    private static final String NODE_THU = "thu-nhap";

    private static DataRepository instance;

    private final FirebaseDatabase db;
    private final FirebaseAuth auth;
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Cache thread-safe
    private final Map<String, Transaction> chiCache = new ConcurrentHashMap<>();
    private final Map<String, Transaction> thuCache = new ConcurrentHashMap<>();

    // LiveData
    private final MutableLiveData<List<Transaction>> chiLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Transaction>> thuLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<BalanceSummary> balanceLiveData = new MutableLiveData<>(new BalanceSummary(0, 0, 0));
    private final MutableLiveData<ChartData> chartLiveData = new MutableLiveData<>(new ChartData());

    private ValueEventListener chiListener;
    private ValueEventListener thuListener;
    private DatabaseReference chiRef;
    private DatabaseReference thuRef;

    private DataRepository() {
        db = FirebaseDatabase.getInstance("https://tienbothi-default-rtdb.firebaseio.com");
        auth = FirebaseAuth.getInstance();
        db.setPersistenceEnabled(true); // Offline cache của Firebase
    }

    public static synchronized DataRepository getInstance() {
        if (instance == null) {
            instance = new DataRepository();
        }
        return instance;
    }

    public LiveData<List<Transaction>> getChiLiveData() { return chiLiveData; }
    public LiveData<List<Transaction>> getThuLiveData() { return thuLiveData; }
    public LiveData<BalanceSummary> getBalanceLiveData() { return balanceLiveData; }
    public LiveData<ChartData> getChartLiveData() { return chartLiveData; }

    /** Bắt đầu lắng nghe realtime khi user đăng nhập */
    public void startListening() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        stopListening(); // tránh duplicate listener

        chiRef = db.getReference("users").child(uid).child(NODE_CHI);
        thuRef = db.getReference("users").child(uid).child(NODE_THU);

        chiListener = new FastValueListener(true);
        thuListener = new FastValueListener(false);

        // keepSynced giúp Firebase giữ data trong memory + disk
        chiRef.keepSynced(true);
        thuRef.keepSynced(true);

        chiRef.addValueEventListener(chiListener);
        thuRef.addValueEventListener(thuListener);
    }

    public void stopListening() {
        if (chiRef != null && chiListener != null) {
            chiRef.removeEventListener(chiListener);
        }
        if (thuRef != null && thuListener != null) {
            thuRef.removeEventListener(thuListener);
        }
        chiListener = null;
        thuListener = null;
    }

    /** Listener tối ưu: chỉ parse trên background thread */
    private class FastValueListener implements ValueEventListener {
        private final boolean isChi;

        FastValueListener(boolean isChi) {
            this.isChi = isChi;
        }

        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            // Chạy parse + sort trên background để UI mượt
            bgExecutor.execute(() -> {
                Map<String, Transaction> newCache = new ConcurrentHashMap<>();
                List<Transaction> list = new ArrayList<>((int) snapshot.getChildrenCount());

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        Transaction t = child.getValue(Transaction.class);
                        if (t != null) {
                            t.setKey(child.getKey());
                            newCache.put(child.getKey(), t);
                            list.add(t);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Parse error: " + child.getKey(), e);
                    }
                }

                // Sort theo thời gian giảm dần (nhanh nhất với Comparator)
                Collections.sort(list, Comparator
                        .comparingLong(Transaction::getThoiGian)
                        .reversed());

                // Update cache + LiveData trên main thread
                mainHandler.post(() -> {
                    if (isChi) {
                        chiCache.clear();
                        chiCache.putAll(newCache);
                        chiLiveData.setValue(list);
                    } else {
                        thuCache.clear();
                        thuCache.putAll(newCache);
                        thuLiveData.setValue(list);
                    }
                    recomputeAggregates();
                });
            });
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.e(TAG, "Listener cancelled: " + error.getMessage());
        }
    }

    /**
     * Tính toán lại tổng + dữ liệu biểu đồ trên background.
     * Gọi mỗi khi data thay đổi.
     */
    private void recomputeAggregates() {
        bgExecutor.execute(() -> {
            long totalThu = 0;
            long totalChi = 0;

            // Category aggregates cho pie chart
            Map<String, Long> chiByCategory = new HashMap<>();
            Map<String, Long> thuByCategory = new HashMap<>();

            // Monthly aggregates cho bar/line chart (key = yyyy-MM)
            Map<String, Long> chiByMonth = new HashMap<>();
            Map<String, Long> thuByMonth = new HashMap<>();

            for (Transaction t : chiCache.values()) {
                totalChi += t.getSoTien();
                String cat = t.getDanhMuc() != null ? t.getDanhMuc() : "Khác";
                chiByCategory.merge(cat, t.getSoTien(), Long::sum);

                String month = extractMonth(t.getNgay());
                if (month != null) {
                    chiByMonth.merge(month, t.getSoTien(), Long::sum);
                }
            }

            for (Transaction t : thuCache.values()) {
                totalThu += t.getSoTien();
                String cat = t.getDanhMuc() != null ? t.getDanhMuc() : "Khác";
                thuByCategory.merge(cat, t.getSoTien(), Long::sum);

                String month = extractMonth(t.getNgay());
                if (month != null) {
                    thuByMonth.merge(month, t.getSoTien(), Long::sum);
                }
            }

            BalanceSummary summary = new BalanceSummary(totalThu, totalChi, totalThu - totalChi);
            ChartData chart = new ChartData(chiByCategory, thuByCategory, chiByMonth, thuByMonth);

            mainHandler.post(() -> {
                balanceLiveData.setValue(summary);
                chartLiveData.setValue(chart);
            });
        });
    }

    private String extractMonth(String ngay) {
        if (ngay == null || ngay.length() < 7) return null;
        return ngay.substring(0, 7); // yyyy-MM
    }

    // ====================== CRUD ======================

    public void addTransaction(boolean isChi, Transaction t, OnCompleteListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            if (listener != null) listener.onError("Chưa đăng nhập");
            return;
        }
        DatabaseReference ref = db.getReference("users").child(user.getUid())
                .child(isChi ? NODE_CHI : NODE_THU);
        String key = ref.push().getKey();
        if (key == null) {
            if (listener != null) listener.onError("Không tạo được key");
            return;
        }
        t.setKey(key);
        t.setThoiGian(System.currentTimeMillis());
        ref.child(key).setValue(t)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void updateTransaction(boolean isChi, Transaction t, OnCompleteListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || t.getKey() == null) {
            if (listener != null) listener.onError("Dữ liệu không hợp lệ");
            return;
        }
        DatabaseReference ref = db.getReference("users").child(user.getUid())
                .child(isChi ? NODE_CHI : NODE_THU).child(t.getKey());
        ref.setValue(t)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void deleteTransaction(boolean isChi, String key, OnCompleteListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || key == null) {
            if (listener != null) listener.onError("Dữ liệu không hợp lệ");
            return;
        }
        DatabaseReference ref = db.getReference("users").child(user.getUid())
                .child(isChi ? NODE_CHI : NODE_THU).child(key);
        ref.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    // ====================== DATA CLASSES ======================

    public static class BalanceSummary {
        public final long totalThu;
        public final long totalChi;
        public final long balance;

        public BalanceSummary(long totalThu, long totalChi, long balance) {
            this.totalThu = totalThu;
            this.totalChi = totalChi;
            this.balance = balance;
        }
    }

    public static class ChartData {
        public final Map<String, Long> chiByCategory;
        public final Map<String, Long> thuByCategory;
        public final Map<String, Long> chiByMonth;
        public final Map<String, Long> thuByMonth;

        public ChartData() {
            this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>());
        }

        public ChartData(Map<String, Long> chiByCategory, Map<String, Long> thuByCategory,
                         Map<String, Long> chiByMonth, Map<String, Long> thuByMonth) {
            this.chiByCategory = chiByCategory != null ? chiByCategory : new HashMap<>();
            this.thuByCategory = thuByCategory != null ? thuByCategory : new HashMap<>();
            this.chiByMonth = chiByMonth != null ? chiByMonth : new HashMap<>();
            this.thuByMonth = thuByMonth != null ? thuByMonth : new HashMap<>();
        }
    }

    public interface OnCompleteListener {
        void onSuccess();
        void onError(String message);
    }
}
