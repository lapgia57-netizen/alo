package com.moneytp.sothuchi.util;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

/**
 * Helper đọc/ghi dữ liệu Firebase Realtime Database.
 * Giữ nguyên path và cấu trúc dữ liệu của app gốc:
 *   users/{uid}/chi-tieu/{pushId}
 *   users/{uid}/thu-nhap/{pushId}
 */
public class FirebaseHelper {

    public static final String TYPE_CHI = "chi-tieu";
    public static final String TYPE_THU = "thu-nhap";

    private final FirebaseAuth auth;
    private final DatabaseReference dbRoot;

    public interface DataCallback {
        void onData(List<Transaction> list);
        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        // Dùng cùng URL với DataRepository để truy vấn chính xác cùng Realtime Database
        dbRoot = FirebaseDatabase.getInstance(
                "https://tienbothi-default-rtdb.firebaseio.com").getReference();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    private DatabaseReference getUserTypeRef(String type) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) throw new IllegalStateException("Chưa đăng nhập");
        if (!TYPE_CHI.equals(type) && !TYPE_THU.equals(type)) {
            throw new IllegalArgumentException("Loại dữ liệu không hợp lệ");
        }
        return dbRoot.child("users").child(user.getUid()).child(type);
    }

    /** Đảm bảo có profile user (giống ensureUserProfile gốc) */
    public void ensureUserProfile(SimpleCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onError("Chưa đăng nhập");
            return;
        }
        DatabaseReference userRef = dbRoot.child("users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> profile = new HashMap<>();
                    String email = user.getEmail() != null ? user.getEmail() : "Anonymous";
                    profile.put("displayName", email.length() > 49 ? email.substring(0, 49) : email);
                    profile.put("createdAt", System.currentTimeMillis());
                    userRef.setValue(profile).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) callback.onSuccess();
                        else callback.onError(task.getException() != null ?
                                task.getException().getMessage() : "Lỗi tạo hồ sơ");
                    });
                } else {
                    callback.onSuccess();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /** Lắng nghe realtime list giao dịch */
    public ValueEventListener listenTransactions(String type, DataCallback callback) {
        DatabaseReference ref = getUserTypeRef(type);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Transaction> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Transaction t = child.getValue(Transaction.class);
                    if (t != null) {
                        t.setKey(child.getKey());
                        t.setExpense(TYPE_CHI.equals(type));
                        list.add(t);
                    }
                }
                // Sắp xếp mới nhất trước (theo thoi_gian hoặc ngay)
                Collections.sort(list, (a, b) -> {
                    long ta = a.getThoi_gian() > 0 ? a.getThoi_gian() : 0;
                    long tb = b.getThoi_gian() > 0 ? b.getThoi_gian() : 0;
                    return Long.compare(tb, ta);
                });
                callback.onData(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };
        ref.addValueEventListener(listener);
        return listener;
    }

    public void removeListener(String type, ValueEventListener listener) {
        if (listener != null) {
            getUserTypeRef(type).removeEventListener(listener);
        }
    }

    /** Thêm giao dịch mới (push) */
    public void addTransaction(String type, Transaction transaction, SimpleCallback callback) {
        DatabaseReference ref = getUserTypeRef(type);
        ref.push().setValue(transaction.toMap())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) callback.onSuccess();
                    else callback.onError(task.getException() != null ?
                            task.getException().getMessage() : "Lỗi lưu dữ liệu");
                });
    }

    /** Cập nhật giao dịch */
    public void updateTransaction(String type, String key, Transaction transaction, SimpleCallback callback) {
        DatabaseReference ref = getUserTypeRef(type).child(key);
        ref.updateChildren(transaction.toMap())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) callback.onSuccess();
                    else callback.onError(task.getException() != null ?
                            task.getException().getMessage() : "Lỗi cập nhật");
                });
    }

    /** Xóa giao dịch */
    public void deleteTransaction(String type, String key, SimpleCallback callback) {
        DatabaseReference ref = getUserTypeRef(type).child(key);
        ref.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) callback.onSuccess();
                    else callback.onError(task.getException() != null ?
                            task.getException().getMessage() : "Lỗi xóa");
                });
    }

    public void signOut() {
        auth.signOut();
    }
}
