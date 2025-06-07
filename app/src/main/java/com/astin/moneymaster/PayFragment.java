package com.astin.moneymaster;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.astin.moneymaster.adapter.PaymentAdapter;
import com.astin.moneymaster.helper.FirebaseHelper;
import com.astin.moneymaster.model.PaymentItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PayFragment extends Fragment {

    private RecyclerView recyclerView;
    private PaymentAdapter adapter;
    private List<PaymentItem> cat_item_list = new ArrayList<>();
    private FloatingActionButton plusButton;
    private ProgressBar progressBar;
    private ViewPager2 viewPager;
    private FirebaseHelper firebaseHelper;

    public PayFragment() {
        super(R.layout.fragment_pay);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            viewPager = mainActivity.viewPager;
        }

        recyclerView = view.findViewById(R.id.recyclerView);
        plusButton = view.findViewById(R.id.plusButton);
        progressBar = view.findViewById(R.id.progressBar);

        adapter = new PaymentAdapter(getContext(), cat_item_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        firebaseHelper = new FirebaseHelper();

        loadItemsFromDatabase();

        plusButton.setOnClickListener(v -> showAddItemDialog());
    }

    private void loadItemsFromDatabase() {
        progressBar.setVisibility(View.VISIBLE);

        firebaseHelper.loadPaymentItems(cat_item_list, new FirebaseHelper.OnDataLoadedListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataLoaded(List<PaymentItem> items) {
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add New Category");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_category_item, null);
        EditText nameInput = dialogView.findViewById(R.id.editItemName);
        EditText budgetInput = dialogView.findViewById(R.id.editItemBudget);

        builder.setView(dialogView);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String budgetStr = budgetInput.getText().toString().trim();

            if (!name.isEmpty()) {
                name = name.toUpperCase(Locale.getDefault());

                if (budgetStr.isEmpty()) {
                    budgetStr = "0.0";
                }

                PaymentItem item = new PaymentItem(name, budgetStr);

                firebaseHelper.addPaymentItem(getContext(), item);
            } else {
                Toast.makeText(getContext(), "Please enter a category name", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewPager != null) {
            viewPager.setUserInputEnabled(true);
        }
    }
}
