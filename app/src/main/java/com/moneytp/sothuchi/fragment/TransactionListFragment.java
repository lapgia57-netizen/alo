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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.ValueEventListener;
import com.moneytp.sothuchi.AddEditActivity;
import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.adapter.TransactionAdapter;
import com.moneytp.sothuchi.model.Transaction;
import com.moneytp.sothuchi.util.FirebaseHelper;

import java.util.List;

public class TransactionListFragment extends Fragment {

    private static final String ARG_TYPE = "type";

    private String type;
    private FirebaseHelper firebaseHelper;
    private ValueEventListener listener;
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
            type = getArguments().getString(ARG_TYPE, FirebaseHelper.TYPE_CHI);
        }
        firebaseHelper = new FirebaseHelper();
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
                        .setPositiveButton(R.string.xoa, (d, w) -> {
                            firebaseHelper.deleteTransaction(type, item.getKey(),
                                    new FirebaseHelper.SimpleCallback() {
                                        @Override
                                        public void onSuccess() {
                                            // Realtime listener sẽ tự cập nhật
                                        }

                                        @Override
                                        public void onError(String message) {
                                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        })
                        .setNegativeButton(R.string.huy, null)
                        .show();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadData);
        loadData();
    }

    private void loadData() {
        if (listener != null) {
            firebaseHelper.removeListener(type, listener);
        }
        listener = firebaseHelper.listenTransactions(type, new FirebaseHelper.DataCallback() {
            @Override
            public void onData(List<Transaction> list) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                adapter.setData(list);
                tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listener != null) {
            firebaseHelper.removeListener(type, listener);
            listener = null;
        }
    }
}
