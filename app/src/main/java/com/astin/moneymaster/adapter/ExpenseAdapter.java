package com.astin.moneymaster.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.astin.moneymaster.model.HistoryEntry;
import com.astin.moneymaster.R;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnItemActionListener {
        void onItemLongClick(HistoryEntry item);
        void onItemEditClick(HistoryEntry item);
    }

    private List<HistoryEntry> items;
    private OnItemActionListener actionListener;

    public ExpenseAdapter(List<HistoryEntry> items, OnItemActionListener listener) {
        this.items = items;
        this.actionListener = listener;
    }


    @Override
    public ExpenseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ExpenseViewHolder holder, int position) {
        HistoryEntry item = items.get(position);

        holder.nameText.setText(item.getCategoryName());
        holder.amountText.setText("₹" + item.getAmountPaid());
        holder.categoryText.setText(item.getDateTime());

        // Show itemName only if it exists
        if (item.getItemName() != null && !item.getItemName().trim().isEmpty()) {
            holder.itemName.setVisibility(View.VISIBLE);
            holder.itemName.setText(item.getItemName());
        } else {
            holder.itemName.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (actionListener != null) actionListener.onItemLongClick(item);
            return true;
        });

        holder.editButton.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onItemEditClick(item);
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, amountText, categoryText,itemName;
        Button editButton;

        public ExpenseViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.expenseName);
            amountText = itemView.findViewById(R.id.expenseAmount);
            categoryText = itemView.findViewById(R.id.expenseCategory);
            editButton = itemView.findViewById(R.id.btnEditExpense);
            itemName = itemView.findViewById(R.id.itemName);
        }

    }
}
