package com.astin.moneymaster.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.astin.moneymaster.R;

import java.util.List;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(String day);
    }

    private List<String> days;
    private OnDayClickListener listener;
    private int selectedPosition = -1; // No selection by default

    public DayAdapter(List<String> days, OnDayClickListener listener) {
        this.days = days;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        String day = days.get(position);
        holder.dayText.setText(day);

        // Highlight selected item
        if (position == selectedPosition) {
            holder.dayText.setBackgroundResource(R.drawable.day_item_selected_background);
        } else {
            holder.dayText.setBackgroundResource(R.drawable.day_item_background);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(previousSelected); // Unhighlight old
            notifyItemChanged(selectedPosition); // Highlight new

            listener.onDayClick(day); // Notify fragment
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayText;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
        }
    }

    public void selectDay(String dayToSelect) {
        int index = days.indexOf(dayToSelect);
        if (index != -1 && index != selectedPosition) {
            int previous = selectedPosition;
            selectedPosition = index;
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
        }
    }

}
