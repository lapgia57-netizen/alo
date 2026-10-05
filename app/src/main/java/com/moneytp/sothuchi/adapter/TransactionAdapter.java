package com.moneytp.sothuchi.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.moneytp.sothuchi.R;
import com.moneytp.sothuchi.model.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Adapter với DiffUtil — chỉ rebind item thay đổi, UI mượt khi data realtime update.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(Transaction item);
        void onDelete(Transaction item);
    }

    private final List<Transaction> items = new ArrayList<>();
    private final OnItemActionListener listener;
    private final NumberFormat currencyFormat =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public TransactionAdapter(OnItemActionListener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void submitList(List<Transaction> newList) {
        List<Transaction> safeNew = newList != null ? newList : new ArrayList<>();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return safeNew.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldKey = items.get(oldItemPosition).getKey();
                String newKey = safeNew.get(newItemPosition).getKey();
                return Objects.equals(oldKey, newKey);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Transaction a = items.get(oldItemPosition);
                Transaction b = safeNew.get(newItemPosition);
                return a.getSo_tien() == b.getSo_tien()
                        && Objects.equals(a.getDanh_muc(), b.getDanh_muc())
                        && Objects.equals(a.getMo_ta(), b.getMo_ta())
                        && Objects.equals(a.getNgay(), b.getNgay())
                        && Objects.equals(a.getGhi_chu(), b.getGhi_chu())
                        && a.isExpense() == b.isExpense();
            }
        });
        items.clear();
        items.addAll(safeNew);
        diff.dispatchUpdatesTo(this);
    }

    @Override
    public long getItemId(int position) {
        String key = items.get(position).getKey();
        return key != null ? key.hashCode() : position;
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
