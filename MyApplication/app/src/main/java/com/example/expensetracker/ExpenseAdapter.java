package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.annotation.SuppressLint;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.VH> {

    public interface Listener {
        void onRowClick(Expense expense);
        void onEdit(Expense expense);
        void onDelete(Expense expense);
        void onRowLongClick(Expense expense);
    }

    private final List<Expense> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener l) { this.listener = l; }

    @SuppressLint("NotifyDataSetChanged")
    public void submitList(List<Expense> newList) {
        items.clear();
        if (newList != null) items.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Expense it = items.get(position);
        h.bind(it);
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onRowClick(it); });
        h.itemView.setOnLongClickListener(v -> { if (listener != null) listener.onRowLongClick(it); return true; });
        h.btnEdit.setOnClickListener(v -> { if (listener != null) listener.onEdit(it); });
        h.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDelete(it); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDesc, tvCategory, tvAmount, tvDate;
        final ImageButton btnEdit, btnDelete;
        final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        VH(@NonNull View itemView) {
            super(itemView);
            tvDesc     = itemView.findViewById(R.id.tvDesc);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvAmount   = itemView.findViewById(R.id.tvAmount);
            tvDate     = itemView.findViewById(R.id.tvDate);
            btnEdit    = itemView.findViewById(R.id.btnEdit);
            btnDelete  = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Expense e) {
            if (e == null) return;
            tvDesc.setText(e.description != null && !e.description.isEmpty() ? e.description : "Cheltuială");
            tvCategory.setText(e.category != null ? e.category : "");
            tvAmount.setText(String.format(Locale.getDefault(), "%.2f RON", e.amount));
            tvDate.setText(df.format(new java.util.Date(e.date)));
        }
    }
}
