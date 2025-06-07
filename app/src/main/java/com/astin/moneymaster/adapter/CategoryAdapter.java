package com.astin.moneymaster.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.astin.moneymaster.R;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    private List<String> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = -1; // No selection by default

    public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        holder.categoryText.setText(category);

        // Highlight selected item
        if (position == selectedPosition) {
            holder.categoryText.setBackgroundResource(R.drawable.day_item_selected_background);
        } else {
            holder.categoryText.setBackgroundResource(R.drawable.day_item_background);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(previousSelected); // Unhighlight old
            notifyItemChanged(selectedPosition); // Highlight new

            listener.onCategoryClick(category); // Notify fragment/activity
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryText;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryText = itemView.findViewById(R.id.categoryText);
        }
    }

    public void selectCategory(String categoryToSelect) {
        int index = categories.indexOf(categoryToSelect);
        if (index != -1 && index != selectedPosition) {
            int previous = selectedPosition;
            selectedPosition = index;
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
        }
    }
}
