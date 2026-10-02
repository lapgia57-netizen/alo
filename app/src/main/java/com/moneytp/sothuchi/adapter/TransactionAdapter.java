package com.moneytp.sothuchi.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.model.Transaction;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Adapter tối ưu với DiffUtil + ListAdapter.
 * Chỉ rebind những item thực sự thay đổi → scroll mượt, không flicker.
 */
public class TransactionAdapter extends ListAdapter<Transaction, TransactionAdapter.VH> {

    public interface OnItemActionListener {
        void onEdit(Transaction item);
        void onDelete(Transaction item);
    }

    private final boolean isChi;
    private final OnItemActionListener listener;
    private final NumberFormat currencyFormat;

    public TransactionAdapter(boolean isChi, OnItemActionListener listener) {
        super(DIFF_CALLBACK);
        this.isChi = isChi;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    private static final DiffUtil.ItemCallback<Transaction> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Transaction>() {
                @Override
                public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                    return oldItem.getKey() != null && oldItem.getKey().equals(newItem.getKey());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Transaction t = getItem(position);
        holder.bind(t);
    }

    class VH extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDesc, tvAmount, tvDate, tvNote;
        ImageButton btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDesc = itemView.findViewById(R.id.tv_desc);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvNote = itemView.findViewById(R.id.tv_note);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(Transaction t) {
            tvCategory.setText(t.getDanhMuc() != null ? t.getDanhMuc() : "—");
            tvDesc.setText(t.getMoTa() != null ? t.getMoTa() : "");
            tvDate.setText(t.getNgay() != null ? t.getNgay() : "");
            String amountStr = currencyFormat.format(t.getSoTien()) + " đ";
            tvAmount.setText(amountStr);
            tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    isChi ? R.color.rose_600 : R.color.emerald_600));

            if (t.getGhiChu() != null && !t.getGhiChu().isEmpty()) {
                tvNote.setVisibility(View.VISIBLE);
                tvNote.setText(t.getGhiChu());
            } else {
                tvNote.setVisibility(View.GONE);
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(t);
            });
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(t);
            });
        }
    }
}
