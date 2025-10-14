package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IncomeAdapter extends RecyclerView.Adapter<IncomeAdapter.VH> {

    public interface OnIncomeClickListener {
        void onEdit(Income income);
        void onDelete(Income income);
        default void onOpen(Income income) { onEdit(income); }
    }

    private final List<Income> items = new ArrayList<>();
    private OnIncomeClickListener listener;

    public void setOnIncomeClickListener(OnIncomeClickListener l) {
        this.listener = l;
    }

    public void setItems(List<Income> newItems) {
        List<Income> old = new ArrayList<>(items);
        items.clear();
        if (newItems != null) items.addAll(newItems);
        DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return old.size(); }
            @Override public int getNewListSize() { return items.size(); }
            @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Income o = old.get(oldItemPosition);
                Income n = items.get(newItemPosition);
                return o.id == n.id && String.valueOf(o.uid).equals(String.valueOf(n.uid));
            }
            @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Income o = old.get(oldItemPosition);
                Income n = items.get(newItemPosition);
                return o.amount == n.amount && safeEq(o.description, n.description) && safeEq(o.sourceType, n.sourceType) && o.date == n.date;
            }
        }).dispatchUpdatesTo(this);
    }

    private static boolean safeEq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    public Income removeAt(int position) {
        if (position < 0 || position >= items.size()) return null;
        Income in = items.remove(position);
        notifyItemRemoved(position);
        return in;
    }

    public void restoreAt(int position, Income in) {
        if (in == null) return;
        if (position < 0 || position > items.size()) position = items.size();
        items.add(position, in);
        notifyItemInserted(position);
    }

    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_income, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Income in = items.get(position);

        h.tvDesc.setText(in.description == null ? "" : in.description);
        h.tvSource.setText(in.sourceType == null ? "PERSONAL" : in.sourceType);
        h.tvAmount.setText(String.format(Locale.getDefault(), "%.2f RON", in.amount));
        h.tvDate.setText(formatDate(in.date));

        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onOpen(in); });
        h.btnEdit.setOnClickListener(v -> { if (listener != null) listener.onEdit(in); });
        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(in); });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDesc, tvSource, tvAmount, tvDate;
        ImageButton btnEdit, btnDelete;
        VH(@NonNull View itemView) {
            super(itemView);
            tvDesc   = itemView.findViewById(R.id.tvDesc);
            tvSource = itemView.findViewById(R.id.tvSource);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate   = itemView.findViewById(R.id.tvDate);
            btnEdit  = itemView.findViewById(R.id.btnEdit);
            btnDelete= itemView.findViewById(R.id.btnDelete);
        }
    }

    private static String formatDate(long millis) {
        if (millis <= 0) return "";
        return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .format(new Date(millis));
    }
}
