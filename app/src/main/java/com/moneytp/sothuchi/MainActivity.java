package com.moneytp.sothuchi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.moneytp.sothuchi.fragment.ChartFragment;
import com.moneytp.sothuchi.fragment.TransactionListFragment;
import com.moneytp.sothuchi.util.DataRepository;
import com.moneytp.sothuchi.util.FirebaseHelper;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Màn hình chính — Material Design, TabLayout + ViewPager2.
 * Dữ liệu lấy qua DataRepository (cache + LiveData) để UI phản hồi nhanh.
 */
public class MainActivity extends AppCompatActivity {

    private DataRepository repository;
    private TextView tvUserEmail, tvTotalIncome, tvTotalExpense, tvBalance;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;

    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final String[] TAB_TITLES = {"Chi tiêu", "Thu nhập", "Biểu đồ"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        repository = DataRepository.getInstance();
        repository.startListening();

        bindViews();
        setupTabs();
        setupListeners();
        observeBalance();
    }

    private void bindViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvBalance = findViewById(R.id.tvBalance);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        fabAdd = findViewById(R.id.fabAdd);

        ImageButton btnLogout = findViewById(R.id.btnLogout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            tvUserEmail.setText(user.getEmail());
        }

        btnLogout.setOnClickListener(v -> {
            repository.stopListening();
            repository.clearCache();
            new FirebaseHelper().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void setupTabs() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 1:
                        return TransactionListFragment.newInstance(DataRepository.NODE_THU);
                    case 2:
                        return new ChartFragment();
                    default:
                        return TransactionListFragment.newInstance(DataRepository.NODE_CHI);
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });
        viewPager.setOffscreenPageLimit(2);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            // Tab 0 = chi, Tab 1 = thu; Tab 2 (biểu đồ) mặc định chi
            String type = (pos == 1) ? DataRepository.NODE_THU : DataRepository.NODE_CHI;
            Intent intent = new Intent(this, AddEditActivity.class);
            intent.putExtra(AddEditActivity.EXTRA_TYPE, type);
            startActivity(intent);
        });
    }

    private void observeBalance() {
        repository.getBalanceLiveData().observe(this, new Observer<DataRepository.BalanceSummary>() {
            @Override
            public void onChanged(DataRepository.BalanceSummary summary) {
                if (summary == null) return;
                tvTotalIncome.setText("+" + currencyFormat.format(summary.totalThu) + " đ");
                tvTotalExpense.setText("-" + currencyFormat.format(summary.totalChi) + " đ");
                String prefix = summary.balance >= 0 ? "+" : "";
                tvBalance.setText(prefix + currencyFormat.format(summary.balance) + " đ");
                tvBalance.setTextColor(getColor(
                        summary.balance >= 0 ? R.color.balance_positive : R.color.balance_negative));
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Không stopListening ở đây để Fragment vẫn nhận data khi rotate.
        // Chỉ stop khi logout.
        super.onDestroy();
    }
}
