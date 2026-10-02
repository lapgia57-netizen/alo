package com.moneytp.sothuchi.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.model.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Transaction item);
        void onDelete(Transaction item);
    }

    private final List<Transaction> items = new ArrayList<>();
    private final OnItemActionListener listener;
    private final NumberFormat currencyFormat;

    public TransactionAdapter(OnItemActionListener listener) {
        this.listener = listener;
        this.currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    public void setData(List<Transaction> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction item = items.get(position);
        holder.tvCategory.setText(item.getDanh_muc() != null ? item.getDanh_muc() : "");
        holder.tvDescription.setText(item.getMo_ta() != null ? item.getMo_ta() : "");
        holder.tvDate.setText(item.getNgay() != null ? item.getNgay() : "");

        String prefix = item.isExpense() ? "-" : "+";
        String amountStr = prefix + currencyFormat.format(item.getSo_tien()) + " đ";
        holder.tvAmount.setText(amountStr);

        int color = ContextCompat.getColor(holder.itemView.getContext(),
                item.isExpense() ? R.color.expense : R.color.income);
        holder.tvAmount.setTextColor(color);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDescription, tvDate, tvAmount;
        ImageButton btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
