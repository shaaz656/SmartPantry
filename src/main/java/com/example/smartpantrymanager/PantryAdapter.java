package com.example.smartpantrymanager;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(PantryItem item);
        void onDelete(PantryItem item);
    }

    private List<PantryItem> items = new ArrayList<>();
    private boolean highlightExpiring = true;
    private final Listener listener;

    public PantryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<PantryItem> items, boolean highlightExpiring) {
        this.items = items;
        this.highlightExpiring = highlightExpiring;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final PantryItem item = items.get(position);
        holder.textName.setText(item.getName());

        String detail = item.formatQuantity() + " " + item.getUnit();
        int color = Color.parseColor("#37474F");

        if (!item.getExpiry().isEmpty()) {
            detail += "  |  Expires: " + item.getExpiry();
            if (highlightExpiring) {
                long days = daysUntil(item.getExpiry());
                if (days < 0) {
                    detail += " (EXPIRED)";
                    color = Color.parseColor("#B71C1C");
                } else if (days <= 3) {
                    detail += " (expires soon)";
                    color = Color.parseColor("#B71C1C");
                }
            }
        }
        holder.textDetail.setText(detail);
        holder.textDetail.setTextColor(color);

        holder.itemView.setOnClickListener(v -> listener.onEdit(item));
        holder.buttonDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private long daysUntil(String date) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date);
            return (d.getTime() - System.currentTimeMillis()) / (24L * 60 * 60 * 1000);
        } catch (Exception e) {
            return 9999;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textDetail;
        ImageButton buttonDelete;

        ViewHolder(View v) {
            super(v);
            textName = v.findViewById(R.id.textName);
            textDetail = v.findViewById(R.id.textDetail);
            buttonDelete = v.findViewById(R.id.buttonDelete);
        }
    }
}