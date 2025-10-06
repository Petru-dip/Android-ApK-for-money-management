package com.example.expensetracker;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.VH> {
    public interface OnExpenseClickListener { void onClick(Expense e); }
    private List<Expense> data;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private OnExpenseClickListener listener;
    public ExpenseAdapter(List<Expense> data) { this.data = data; }
    public void setOnExpenseClickListener(OnExpenseClickListener l) { this.listener = l; }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Expense e = data.get(pos);
        h.tvDesc.setText(e.description);
        h.tvAmount.setText(String.format(Locale.getDefault(), "%.2f RON", e.amount));
        h.tvDate.setText(sdf.format(e.date));
        h.tvCategory.setText((e.category == null ? "" : e.category) + " • " + e.categoryType);
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(e); });
    }
    @Override public int getItemCount() { return data == null ? 0 : data.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView tvDesc, tvAmount, tvDate, tvCategory;
        VH(@NonNull View itemView) {
            super(itemView);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}
