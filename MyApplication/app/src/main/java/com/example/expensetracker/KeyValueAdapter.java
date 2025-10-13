package com.example.expensetracker;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KeyValueAdapter extends RecyclerView.Adapter<KeyValueAdapter.VH> {
    private final List<KeyValue> data = new ArrayList<>();
    private final NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());

    public void submit(List<KeyValue> items) {
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView t1, t2;
        VH(@NonNull android.view.View itemView) {
            super(itemView);
            t1 = itemView.findViewById(android.R.id.text1);
            t2 = itemView.findViewById(android.R.id.text2);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        android.view.View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        KeyValue kv = data.get(position);
        h.t1.setText(kv.key);
        h.t2.setText(nf.format(kv.value));
    }

    @Override
    public int getItemCount() { return data.size(); }
}
