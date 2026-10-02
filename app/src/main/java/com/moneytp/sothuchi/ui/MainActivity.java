package com.moneytp.sothuchi.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.adapter.TransactionAdapter;
import com.moneytp.sothuchi.model.Transaction;
import com.moneytp.sothuchi.util.DataRepository;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Màn hình chính: hiển thị danh sách thu/chi với RecyclerView + DiffUtil (nhanh nhất).
 * Tab Chi / Thu, tổng số dư realtime, nút thêm, nút biểu đồ phân tích.
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvTotalThu, tvTotalChi, tvBalance;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd, fabChart;

    private DataRepository repo;
    private FirebaseAuth auth;
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // Hai adapter riêng cho Chi và Thu
    private TransactionAdapter chiAdapter;
    private TransactionAdapter thuAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        repo = DataRepository.getInstance();

        bindViews();
        setupAdapters();
        setupTabs();
        setupObservers();
        setupFab();

        repo.startListening();
    }

    private void bindViews() {
        tvTotalThu = findViewById(R.id.tv_total_thu);
        tvTotalChi = findViewById(R.id.tv_total_chi);
        tvBalance = findViewById(R.id.tv_balance);
        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);
        fabAdd = findViewById(R.id.fab_add);
        fabChart = findViewById(R.id.fab_chart);
    }

    private void setupAdapters() {
        chiAdapter = new TransactionAdapter(true, new TransactionAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Transaction item) {
                openEditDialog(true, item);
            }

            @Override
            public void onDelete(Transaction item) {
                confirmDelete(true, item);
            }
        });

        thuAdapter = new TransactionAdapter(false, new TransactionAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Transaction item) {
                openEditDialog(false, item);
            }

            @Override
            public void onDelete(Transaction item) {
                confirmDelete(false, item);
            }
        });
    }

    private void setupTabs() {
        viewPager.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
                RecyclerView rv = new RecyclerView(parent.getContext());
                rv.setLayoutParams(new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.MATCH_PARENT));
                rv.setLayoutManager(new LinearLayoutManager(parent.getContext()));
                rv.setHasFixedSize(true);
                rv.setItemViewCacheSize(20); // cache nhiều item → scroll siêu mượt
                rv.setDrawingCacheEnabled(true);
                rv.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

                if (viewType == 0) {
                    rv.setAdapter(chiAdapter);
                } else {
                    rv.setAdapter(thuAdapter);
                }
                return new RecyclerView.ViewHolder(rv) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                // already set in create
            }

            @Override
            public int getItemCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                return position;
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "CHI TIÊU" : "THU NHẬP");
        }).attach();
    }

    private void setupObservers() {
        repo.getChiLiveData().observe(this, list -> {
            if (list != null) chiAdapter.submitList(list);
        });
        repo.getThuLiveData().observe(this, list -> {
            if (list != null) thuAdapter.submitList(list);
        });
        repo.getBalanceLiveData().observe(this, summary -> {
            if (summary != null) {
                tvTotalThu.setText(currencyFormat.format(summary.totalThu) + " đ");
                tvTotalChi.setText(currencyFormat.format(summary.totalChi) + " đ");
                tvBalance.setText(currencyFormat.format(summary.balance) + " đ");
            }
        });
    }

    private void setupFab() {
        fabAdd.setOnClickListener(v -> {
            boolean isChi = viewPager.getCurrentItem() == 0;
            openEditDialog(isChi, null);
        });

        fabChart.setOnClickListener(v -> {
            startActivity(new Intent(this, ChartActivity.class));
        });
    }

    private void openEditDialog(boolean isChi, Transaction existing) {
        // Đơn giản: mở Activity hoặc Dialog. Ở đây dùng Intent sang AddEditActivity
        Intent i = new Intent(this, AddEditActivity.class);
        i.putExtra("isChi", isChi);
        if (existing != null) {
            i.putExtra("transaction", existing);
        }
        startActivity(i);
    }

    private void confirmDelete(boolean isChi, Transaction item) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa chứng từ")
                .setMessage("Bạn có chắc muốn xóa vĩnh viễn giao dịch này?")
                .setPositiveButton("Xóa", (d, w) -> {
                    repo.deleteTransaction(isChi, item.getKey(), new DataRepository.OnCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "Đã xóa", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(MainActivity.this, "Lỗi: " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            repo.stopListening();
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_chart) {
            startActivity(new Intent(this, ChartActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Không stopListening ở đây vì có thể rotate. Chỉ stop khi logout.
    }
}
