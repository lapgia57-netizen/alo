package com.moneytp.sothuchi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ValueEventListener;
import com.moneytp.sothuchi.fragment.ChartFragment;
import com.moneytp.sothuchi.fragment.TransactionListFragment;
import com.moneytp.sothuchi.model.Transaction;
import com.moneytp.sothuchi.util.FirebaseHelper;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private FirebaseHelper firebaseHelper;
    private TextView tvUserEmail, tvTotalIncome, tvTotalExpense, tvBalance;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FloatingActionButton fabAdd;

    private ValueEventListener chiListener, thuListener;
    private long totalIncome = 0;
    private long totalExpense = 0;
    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    private final String[] TAB_TITLES = {"Chi tiêu", "Thu nhập", "Biểu đồ"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseHelper = new FirebaseHelper();
        if (!firebaseHelper.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindViews();
        setupTabs();
        setupListeners();
        loadBalance();
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
        btnLogout.setOnClickListener(v -> {
            firebaseHelper.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        FirebaseUser user = firebaseHelper.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "User");
        }
    }

    private void setupTabs() {
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return TransactionListFragment.newInstance(FirebaseHelper.TYPE_CHI);
                    case 1:
                        return TransactionListFragment.newInstance(FirebaseHelper.TYPE_THU);
                    default:
                        return new ChartFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        viewPager.setOffscreenPageLimit(2);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(TAB_TITLES[position])).attach();

        // Ẩn FAB khi ở tab Biểu đồ
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                fabAdd.setVisibility(position == 2 ? android.view.View.GONE : android.view.View.VISIBLE);
            }
        });
    }

    private void setupListeners() {
        fabAdd.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            String type = (pos == 1) ? FirebaseHelper.TYPE_THU : FirebaseHelper.TYPE_CHI;
            Intent intent = new Intent(this, AddEditActivity.class);
            intent.putExtra(AddEditActivity.EXTRA_TYPE, type);
            startActivity(intent);
        });
    }

    private void loadBalance() {
        chiListener = firebaseHelper.listenTransactions(FirebaseHelper.TYPE_CHI,
                new FirebaseHelper.DataCallback() {
                    @Override
                    public void onData(List<Transaction> list) {
                        totalExpense = 0;
                        if (list != null) {
                            for (Transaction t : list) totalExpense += t.getSo_tien();
                        }
                        updateBalanceUI();
                    }

                    @Override
                    public void onError(String message) { }
                });

        thuListener = firebaseHelper.listenTransactions(FirebaseHelper.TYPE_THU,
                new FirebaseHelper.DataCallback() {
                    @Override
                    public void onData(List<Transaction> list) {
                        totalIncome = 0;
                        if (list != null) {
                            for (Transaction t : list) totalIncome += t.getSo_tien();
                        }
                        updateBalanceUI();
                    }

                    @Override
                    public void onError(String message) { }
                });
    }

    private void updateBalanceUI() {
        tvTotalIncome.setText("+" + currencyFormat.format(totalIncome) + " đ");
        tvTotalExpense.setText("-" + currencyFormat.format(totalExpense) + " đ");
        long balance = totalIncome - totalExpense;
        String prefix = balance >= 0 ? "+" : "";
        tvBalance.setText(prefix + currencyFormat.format(balance) + " đ");
        tvBalance.setTextColor(getColor(balance >= 0 ? R.color.balance_positive : R.color.balance_negative));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chiListener != null) {
            firebaseHelper.removeListener(FirebaseHelper.TYPE_CHI, chiListener);
        }
        if (thuListener != null) {
            firebaseHelper.removeListener(FirebaseHelper.TYPE_THU, thuListener);
        }
    }
}
