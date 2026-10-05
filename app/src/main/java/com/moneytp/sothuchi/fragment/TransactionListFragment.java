package com.moneytp.sothuchi.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.moneytp.sothuchi.AddEditActivity;
import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.adapter.TransactionAdapter;
import com.moneytp.sothuchi.model.Transaction;
import com.moneytp.sothuchi.util.DataRepository;

import java.util.List;

/**
 * Danh sách giao dịch — dùng LiveData từ DataRepository (cache + realtime).
 * Swipe-to-refresh chỉ để feedback UX; data đã realtime nên không cần re-query.
 */
public class TransactionListFragment extends Fragment {

    private static final String ARG_TYPE = "type";

    private String type;
    private boolean isChi;
    private DataRepository repository;
    private TransactionAdapter adapter;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefresh;

    public static TransactionListFragment newInstance(String type) {
        TransactionListFragment f = new TransactionListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getString(ARG_TYPE, DataRepository.NODE_CHI);
        } else {
            type = DataRepository.NODE_CHI;
        }
        isChi = DataRepository.NODE_CHI.equals(type);
        repository = DataRepository.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        adapter = new TransactionAdapter(new TransactionAdapter.OnItemActionListener() {
            @Override
            public void onEdit(Transaction item) {
                Intent intent = new Intent(requireContext(), AddEditActivity.class);
                intent.putExtra(AddEditActivity.EXTRA_TYPE, type);
                intent.putExtra(AddEditActivity.EXTRA_KEY, item.getKey());
                intent.putExtra(AddEditActivity.EXTRA_TRANSACTION, item);
                startActivity(intent);
            }

            @Override
            public void onDelete(Transaction item) {
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.xoa)
                        .setMessage(R.string.xac_nhan_xoa)
                        .setPositiveButton(android.R.string.ok, (d, w) -> {
                            repository.deleteTransaction(isChi, item.getKey(),
                                    new DataRepository.OnCompleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            // LiveData tự cập nhật
                                        }

                                        @Override
                                        public void onError(String message) {
                                            if (isAdded()) {
                                                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        // Swipe refresh: feedback UX, data đã realtime qua LiveData
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> {
            // Force re-emit từ cache (không query lại Firebase)
            swipeRefresh.postDelayed(() -> {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            }, 400);
        });

        // Observe LiveData — cập nhật UI ngay khi cache đổi
        Observer<List<Transaction>> observer = list -> {
            if (list == null) list = java.util.Collections.emptyList();
            adapter.submitList(list);
            boolean empty = list.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (swipeRefresh.isRefreshing()) {
                swipeRefresh.setRefreshing(false);
            }
        };

        if (isChi) {
            repository.getChiLiveData().observe(getViewLifecycleOwner(), observer);
        } else {
            repository.getThuLiveData().observe(getViewLifecycleOwner(), observer);
        }
    }
}
