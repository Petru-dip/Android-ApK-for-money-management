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
public class IncomeAdapter extends RecyclerView.Adapter<IncomeAdapter.VH> {
    public interface OnIncomeClickListener { void onClick(Income e); }
    private List<Income> data;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private OnIncomeClickListener listener;
    public IncomeAdapter(List<Income> data) { this.data = data; }
    public void setOnIncomeClickListener(OnIncomeClickListener l) { this.listener = l; }
    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_income, parent, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        Income e = data.get(pos);
        h.tvDesc.setText(e.description);
        h.tvAmount.setText(String.format(Locale.getDefault(), "%.2f RON", e.amount));
        h.tvDate.setText(sdf.format(e.date));
        h.tvSource.setText(e.sourceType);
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(e); });
    }
    @Override public int getItemCount() { return data == null ? 0 : data.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView tvDesc, tvAmount, tvDate, tvSource;
        VH(@NonNull View itemView) {
            super(itemView);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSource = itemView.findViewById(R.id.tvSource);
        }
    }
}
