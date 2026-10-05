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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Repository tối ưu tốc độ load & hiển thị dữ liệu.
 *
 * Các kỹ thuật tối ưu:
 * 1. Single ValueEventListener + ConcurrentHashMap cache (tránh re-query)
 * 2. Background thread parse + sort (không block UI)
 * 3. LiveData (chỉ emit khi dữ liệu thay đổi)
 * 4. Pre-computed totals + category aggregates cho biểu đồ
 * 5. Firebase disk persistence + keepSynced
 */
public class DataRepository {

    private static final String TAG = "DataRepository";
    public static final String NODE_CHI = "chi-tieu";
    public static final String NODE_THU = "thu-nhap";

    private static volatile DataRepository instance;
    private static final AtomicBoolean persistenceEnabled = new AtomicBoolean(false);

    private final FirebaseDatabase db;
    private final FirebaseAuth auth;
    private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Map<String, Transaction> chiCache = new ConcurrentHashMap<>();
    private final Map<String, Transaction> thuCache = new ConcurrentHashMap<>();

    private final MutableLiveData<List<Transaction>> chiLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Transaction>> thuLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<BalanceSummary> balanceLiveData = new MutableLiveData<>(new BalanceSummary(0, 0, 0));
    private final MutableLiveData<ChartData> chartLiveData = new MutableLiveData<>(new ChartData());

    private ValueEventListener chiListener;
    private ValueEventListener thuListener;
    private DatabaseReference chiRef;
    private DatabaseReference thuRef;

    private DataRepository() {
        if (persistenceEnabled.compareAndSet(false, true)) {
            try {
                FirebaseDatabase.getInstance("https://tienbothi-default-rtdb.firebaseio.com")
                        .setPersistenceEnabled(true);
            } catch (Exception e) {
                Log.w(TAG, "Persistence already enabled or failed: " + e.getMessage());
            }
        }
        db = FirebaseDatabase.getInstance("https://tienbothi-default-rtdb.firebaseio.com");
        auth = FirebaseAuth.getInstance();
    }

    public static DataRepository getInstance() {
        if (instance == null) {
            synchronized (DataRepository.class) {
                if (instance == null) {
                    instance = new DataRepository();
                }
            }
        }
        return instance;
    }

    public LiveData<List<Transaction>> getChiLiveData() { return chiLiveData; }
    public LiveData<List<Transaction>> getThuLiveData() { return thuLiveData; }
    public LiveData<BalanceSummary> getBalanceLiveData() { return balanceLiveData; }
    public LiveData<ChartData> getChartLiveData() { return chartLiveData; }

    public void startListening() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        stopListening();

        chiRef = db.getReference("users").child(uid).child(NODE_CHI);
        thuRef = db.getReference("users").child(uid).child(NODE_THU);

        chiListener = new FastValueListener(true);
        thuListener = new FastValueListener(false);

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

    public void clearCache() {
        chiCache.clear();
        thuCache.clear();
        mainHandler.post(() -> {
            chiLiveData.setValue(new ArrayList<>());
            thuLiveData.setValue(new ArrayList<>());
            balanceLiveData.setValue(new BalanceSummary(0, 0, 0));
            chartLiveData.setValue(new ChartData());
        });
    }

    private class FastValueListener implements ValueEventListener {
        private final boolean isChi;

        FastValueListener(boolean isChi) {
            this.isChi = isChi;
        }

        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            bgExecutor.execute(() -> {
                Map<String, Transaction> newCache = new ConcurrentHashMap<>();
                List<Transaction> list = new ArrayList<>((int) snapshot.getChildrenCount());

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        Transaction t = child.getValue(Transaction.class);
                        if (t != null) {
                            t.setKey(child.getKey());
                            t.setExpense(isChi);
                            newCache.put(child.getKey(), t);
                            list.add(t);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Parse error: " + child.getKey(), e);
                    }
                }

                Collections.sort(list, Comparator
                        .comparingLong(Transaction::getThoi_gian)
                        .reversed());

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

    private void recomputeAggregates() {
        bgExecutor.execute(() -> {
            long totalThu = 0;
            long totalChi = 0;

            Map<String, Long> chiByCategory = new HashMap<>();
            Map<String, Long> thuByCategory = new HashMap<>();
            Map<String, Long> chiByMonth = new HashMap<>();
            Map<String, Long> thuByMonth = new HashMap<>();

            for (Transaction t : chiCache.values()) {
                long amount = t.getSo_tien();
                totalChi += amount;
                String cat = (t.getDanh_muc() != null && !t.getDanh_muc().isEmpty())
                        ? t.getDanh_muc() : "Khác";
                chiByCategory.merge(cat, amount, Long::sum);
                String month = extractMonth(t.getNgay());
                if (month != null) {
                    chiByMonth.merge(month, amount, Long::sum);
                }
            }

            for (Transaction t : thuCache.values()) {
                long amount = t.getSo_tien();
                totalThu += amount;
                String cat = (t.getDanh_muc() != null && !t.getDanh_muc().isEmpty())
                        ? t.getDanh_muc() : "Khác";
                thuByCategory.merge(cat, amount, Long::sum);
                String month = extractMonth(t.getNgay());
                if (month != null) {
                    thuByMonth.merge(month, amount, Long::sum);
                }
            }

            final long fThu = totalThu;
            final long fChi = totalChi;
            final ChartData chartData = new ChartData(chiByCategory, thuByCategory, chiByMonth, thuByMonth);

            mainHandler.post(() -> {
                balanceLiveData.setValue(new BalanceSummary(fThu, fChi, fThu - fChi));
                chartLiveData.setValue(chartData);
            });
        });
    }

    private static String extractMonth(String ngay) {
        if (ngay != null && ngay.length() >= 7) {
            return ngay.substring(0, 7);
        }
        return null;
    }

    public void addTransaction(boolean isChi, Transaction t, OnCompleteListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || t == null) {
            if (listener != null) listener.onError("Dữ liệu không hợp lệ");
            return;
        }
        if (t.getThoi_gian() <= 0) {
            t.setThoi_gian(System.currentTimeMillis());
        }
        DatabaseReference ref = db.getReference("users").child(user.getUid())
                .child(isChi ? NODE_CHI : NODE_THU).push();
        ref.setValue(t.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    public void updateTransaction(boolean isChi, String key, Transaction t, OnCompleteListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || key == null || t == null) {
            if (listener != null) listener.onError("Dữ liệu không hợp lệ");
            return;
        }
        DatabaseReference ref = db.getReference("users").child(user.getUid())
                .child(isChi ? NODE_CHI : NODE_THU).child(key);
        ref.setValue(t.toMap())
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
