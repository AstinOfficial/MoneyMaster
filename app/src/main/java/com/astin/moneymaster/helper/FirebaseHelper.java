package com.astin.moneymaster.helper;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.astin.moneymaster.model.HistoryEntry;
import com.astin.moneymaster.model.PaymentItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseHelper {

    private final DatabaseReference database;
    private final DatabaseReference historyRef;
    private final DatabaseReference categoryRef;

    public FirebaseHelper() {
        this.database = FirebaseDatabase.getInstance().getReference("Money-Planner/Cat_items");
        this.historyRef = FirebaseDatabase.getInstance().getReference("Money-Planner/History");
        this.categoryRef = FirebaseDatabase.getInstance().getReference("Money-Planner/Cat_items");
    }

    // Existing interfaces
    public interface OnDataLoadedListener {
        void onDataLoaded(List<PaymentItem> items);
        void onError(String error);
    }

    // New interfaces for History operations
    public interface OnHistoryDataListener {
        void onDataLoaded(ArrayList<HistoryEntry> items, double totalSpend);
        void onError(String error);
    }

    // NEW: Separate interface for day loading
    public interface OnDaysLoadedListener {
        void onDaysLoaded(ArrayList<String> days, double totalMonthSpend);
        void onError(String error);
    }

    public interface OnMonthsLoadedListener {
        void onMonthsLoaded(ArrayList<String> months);
        void onError(String error);
    }

    public interface OnCategoriesLoadedListener {
        void onCategoriesLoaded(ArrayList<String> categories, Map<String, Double> categoryTotals, double totalMonthSpend);
        void onError(String error);
    }

    public interface OnCategorySpendingListener {
        void onDataLoaded(Map<String, Double> categoryTotals);
        void onError(String error);
    }

    public interface OnCategoryNamesListener {
        void onCategoriesLoaded(List<String> categories);
        void onError(String error);
    }

    public interface OnBudgetUpdateListener {
        void onSuccess();
        void onError(String error);
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onError(String error);
    }

    // NEW: Interface for payment operations from PaymentAdapter
    public interface OnPaymentListener {
        void onPaymentSuccess(String message);
        void onPaymentError(String error);
    }

    // NEW: Interface for item deletion from PaymentAdapter
    public interface OnItemDeleteListener {
        void onDeleteSuccess(String message);
        void onDeleteError(String error);
    }

    // NEW: Interface for budget reset operations
    public interface OnBudgetResetListener {
        void onResetSuccess();
        void onResetError(String error);
    }

    // NEW: Method to reset all budget balances (moved from MainActivity)
    public void resetAllBudgetBalances(OnBudgetResetListener listener) {
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasErrors = false;
                int totalItems = (int) snapshot.getChildrenCount();
                int[] processedItems = {0}; // Use array to make it effectively final

                if (totalItems == 0) {
                    listener.onResetSuccess();
                    return;
                }

                for (DataSnapshot child : snapshot.getChildren()) {
                    String budgetStr = child.child("budget").getValue(String.class);
                    if (budgetStr != null) {
                        child.getRef().child("budget_balance").setValue(budgetStr)
                                .addOnCompleteListener(task -> {
                                    processedItems[0]++;
                                    if (processedItems[0] == totalItems) {
                                        // All items processed
                                        listener.onResetSuccess();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    listener.onResetError("Failed to reset balance for " + child.getKey() + ": " + e.getMessage());
                                });
                    } else {
                        processedItems[0]++;
                        if (processedItems[0] == totalItems) {
                            listener.onResetSuccess();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onResetError("Database error: " + error.getMessage());
            }
        });
    }

    // Existing methods
    public void loadPaymentItems(List<PaymentItem> itemList, OnDataLoadedListener listener) {
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    PaymentItem item = itemSnap.getValue(PaymentItem.class);
                    if (item != null) {
                        itemList.add(item);
                    }
                }
                listener.onDataLoaded(itemList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Error loading data: " + error.getMessage());
            }
        });
    }

    public void addPaymentItem(Context context, PaymentItem item) {
        if (item != null && item.getName() != null) {
            database.child(item.getName()).setValue(item)
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to add item", Toast.LENGTH_SHORT).show());
        }
    }

    // NEW: Method to handle payment processing (moved from PaymentAdapter)
    public void processPayment(PaymentItem item, double amount, OnPaymentListener listener) {
        if (item == null || item.getName() == null) {
            listener.onPaymentError("Invalid item");
            return;
        }

        double currentBalance = Double.parseDouble(item.getBudget_balance());
        double newBalance = currentBalance - amount;

        // Update local object
        item.setBudget_balance(String.valueOf(newBalance));

        // Update Firebase
        database.child(item.getName()).child("budget_balance")
                .setValue(String.valueOf(newBalance))
                .addOnSuccessListener(aVoid -> {
                    // Record history
                    recordHistory(item, amount);
                    listener.onPaymentSuccess("Paid " + amount + " for " + item.getName());
                })
                .addOnFailureListener(e -> {
                    // Revert local change on failure
                    item.setBudget_balance(String.valueOf(currentBalance));
                    listener.onPaymentError("Failed to update balance: " + e.getMessage());
                });
    }

    // NEW: Method to handle item deletion (moved from PaymentAdapter)
    public void deletePaymentItem(PaymentItem item, OnItemDeleteListener listener) {
        if (item == null || item.getName() == null) {
            listener.onDeleteError("Invalid item");
            return;
        }

        database.child(item.getName()).removeValue()
                .addOnSuccessListener(aVoid ->
                        listener.onDeleteSuccess("Deleted " + item.getName()))
                .addOnFailureListener(e ->
                        listener.onDeleteError("Failed to delete: " + e.getMessage()));
    }

    // New methods for History operations
    public void loadMonths(OnMonthsLoadedListener listener) {
        historyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> monthsyearArray = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String category = child.getKey();
                    if (category != null) {
                        monthsyearArray.add(category);
                    }
                }

                // Custom sorting to handle different date formats properly
                Collections.sort(monthsyearArray, (month1, month2) -> {
                    try {
                        SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                        Date date1 = formatter.parse(month1);
                        Date date2 = formatter.parse(month2);
                        return date2.compareTo(date1); // Latest first
                    } catch (ParseException e) {
                        return month2.compareTo(month1);
                    }
                });

                listener.onMonthsLoaded(monthsyearArray);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to load months: " + error.getMessage());
            }
        });
    }

    public void loadCategoriesForMonth(String monthYear, OnCategoriesLoadedListener listener) {
        historyRef.child(monthYear).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Double> categoryTotals = new HashMap<>();
                ArrayList<String> categoryList = new ArrayList<>();
                double totalMonthSpend = 0;

                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    for (DataSnapshot itemSnapshot : daySnapshot.getChildren()) {
                        String categoryName = itemSnapshot.child("categoryName").getValue(String.class);
                        Double amountPaid = itemSnapshot.child("amountPaid").getValue(Double.class);

                        if (categoryName != null && amountPaid != null) {
                            totalMonthSpend += amountPaid;
                            categoryTotals.put(categoryName,
                                    categoryTotals.getOrDefault(categoryName, 0.0) + amountPaid);
                            if (!categoryList.contains(categoryName)) {
                                categoryList.add(categoryName);
                            }
                        }
                    }
                }

                listener.onCategoriesLoaded(categoryList, categoryTotals, totalMonthSpend);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to load categories: " + error.getMessage());
            }
        });
    }

    // FIXED: Proper implementation for loading days
    public void loadDaysForMonth(String month, OnDaysLoadedListener listener) {
        historyRef.child(month).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<String> dayList = new ArrayList<>();
                double totalMonthSpend = 0;

                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    String day = daySnapshot.getKey();
                    if (day != null) {
                        dayList.add(day);

                        for (DataSnapshot itemSnapshot : daySnapshot.getChildren()) {
                            HistoryEntry item = itemSnapshot.getValue(HistoryEntry.class);
                            if (item != null) {
                                totalMonthSpend += item.getAmountPaid();
                            }
                        }
                    }
                }

                // Sort days in descending order (latest first)
                Collections.sort(dayList, (day1, day2) -> {
                    try {
                        int d1 = Integer.parseInt(day1);
                        int d2 = Integer.parseInt(day2);
                        return Integer.compare(d2, d1);
                    } catch (NumberFormatException e) {
                        return day2.compareTo(day1);
                    }
                });

                listener.onDaysLoaded(dayList, totalMonthSpend);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to load days: " + error.getMessage());
            }
        });
    }

    // OVERLOADED: Keep the old method for backward compatibility
    public void loadDaysForMonth(String month, OnHistoryDataListener listener) {
        loadDaysForMonth(month, new OnDaysLoadedListener() {
            @Override
            public void onDaysLoaded(ArrayList<String> days, double totalMonthSpend) {
                // Convert to empty HistoryEntry list for compatibility
                ArrayList<HistoryEntry> result = new ArrayList<>();
                listener.onDataLoaded(result, totalMonthSpend);
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        });
    }

    public void loadItemsForDay(String month, String day, OnHistoryDataListener listener) {
        historyRef.child(month).child(day).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<HistoryEntry> itemList = new ArrayList<>();
                double totalDaySpend = 0;

                for (DataSnapshot child : snapshot.getChildren()) {
                    HistoryEntry item = child.getValue(HistoryEntry.class);
                    if (item != null) {
                        item.setId(child.getKey());
                        itemList.add(item);
                        totalDaySpend += item.getAmountPaid();
                    }
                }

                listener.onDataLoaded(itemList, totalDaySpend);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to load items: " + error.getMessage());
            }
        });
    }

    public void loadItemsForCategory(String month, String category, OnHistoryDataListener listener) {
        historyRef.child(month).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<HistoryEntry> itemList = new ArrayList<>();
                double totalCategorySpend = 0;

                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    for (DataSnapshot itemSnapshot : daySnapshot.getChildren()) {
                        HistoryEntry item = itemSnapshot.getValue(HistoryEntry.class);
                        if (item != null && item.getCategoryName().equalsIgnoreCase(category)) {
                            item.setId(itemSnapshot.getKey());
                            itemList.add(item);
                            totalCategorySpend += item.getAmountPaid();
                        }
                    }
                }

                listener.onDataLoaded(itemList, totalCategorySpend);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to load category items: " + error.getMessage());
            }
        });
    }

    public void loadCategoryNames(OnCategoryNamesListener listener) {
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> categoryList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String categoryName = child.child("name").getValue(String.class);
                    if (categoryName != null) {
                        categoryList.add(categoryName);
                    }
                }
                listener.onCategoriesLoaded(categoryList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to load categories: " + error.getMessage());
            }
        });
    }

    public void calculateMonthlySpendingByCategory(String monthYear, OnCategorySpendingListener listener) {
        historyRef.child(monthYear).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Double> categoryTotals = new HashMap<>();

                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    for (DataSnapshot itemSnapshot : daySnapshot.getChildren()) {
                        String categoryName = itemSnapshot.child("categoryName").getValue(String.class);
                        Double amountPaid = itemSnapshot.child("amountPaid").getValue(Double.class);

                        if (categoryName != null && amountPaid != null) {
                            categoryTotals.put(categoryName,
                                    categoryTotals.getOrDefault(categoryName, 0.0) + amountPaid);
                        }
                    }
                }

                listener.onDataLoaded(categoryTotals);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Failed to calculate spending: " + error.getMessage());
            }
        });
    }

    public void updateBudgetBalance(String categoryName, double amountChange, OnBudgetUpdateListener listener) {
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    if (name != null && name.equalsIgnoreCase(categoryName)) {
                        String balanceStr = child.child("budget_balance").getValue(String.class);
                        if (balanceStr == null) balanceStr = "0.0";

                        double currentBalance = Double.parseDouble(balanceStr);
                        double newBalance = currentBalance - amountChange;

                        child.getRef().child("budget_balance").setValue(String.valueOf(newBalance))
                                .addOnSuccessListener(unused -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onError("Failed to update budget: " + e.getMessage()));
                        return;
                    }
                }
                listener.onError("Category not found");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Database error: " + error.getMessage());
            }
        });
    }

    public void updateBudgetBalanceWithAdjustment(String categoryName, double oldAmount, double newAmount, OnBudgetUpdateListener listener) {
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    if (name != null && name.equalsIgnoreCase(categoryName)) {
                        String balanceStr = child.child("budget_balance").getValue(String.class);
                        if (balanceStr == null) balanceStr = "0.0";

                        double currentBalance = Double.parseDouble(balanceStr);
                        double adjustedBalance = currentBalance + oldAmount - newAmount;

                        child.getRef().child("budget_balance").setValue(String.valueOf(adjustedBalance))
                                .addOnSuccessListener(unused -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onError("Failed to update budget: " + e.getMessage()));
                        return;
                    }
                }
                listener.onError("Category not found");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Database error: " + error.getMessage());
            }
        });
    }

    public void restoreBudgetBalance(String categoryName, double amountToRestore, OnBudgetUpdateListener listener) {
        categoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    if (name != null && name.equalsIgnoreCase(categoryName)) {
                        String balanceStr = child.child("budget_balance").getValue(String.class);
                        if (balanceStr == null) balanceStr = "0.0";

                        double currentBalance = Double.parseDouble(balanceStr);
                        double newBalance = currentBalance + amountToRestore;

                        child.getRef().child("budget_balance").setValue(String.valueOf(newBalance))
                                .addOnSuccessListener(unused -> listener.onSuccess())
                                .addOnFailureListener(e -> listener.onError("Failed to restore budget: " + e.getMessage()));
                        return;
                    }
                }
                listener.onError("Category not found");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError("Database error: " + error.getMessage());
            }
        });
    }

    public void deleteHistoryEntry(String month, String day, String itemId, OnDeleteListener listener) {
        historyRef.child(month).child(day).child(itemId)
                .removeValue()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError("Error deleting item: " + e.getMessage()));
    }

    public void updateHistoryEntry(String month, String day, String itemId, String categoryName, double amount, String itemName, OnBudgetUpdateListener listener) {
        DatabaseReference ref = historyRef.child(month).child(day).child(itemId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("categoryName", categoryName);
        updates.put("amountPaid", amount);
        updates.put("itemName", itemName);

        ref.updateChildren(updates)
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(e -> listener.onError("Failed to update entry: " + e.getMessage()));
    }

    // Method for recording history with PaymentItem object
    public void recordHistory(PaymentItem cat_item, double amountPaid) {
        if (cat_item == null || cat_item.getName() == null || cat_item.getName().isEmpty()) {
            return; // prevent null pointer issues
        }

        String monthYear = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"));
        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Convert all relevant fields to uppercase
        String cat_name = cat_item.getName().toUpperCase(Locale.getDefault());

        // Prepare the Firebase references
        DatabaseReference monthRef = historyRef.child(monthYear);
        DatabaseReference DaysRef = monthRef.child(day);

        // Add entry
        String entryId = DaysRef.push().getKey();
        if (entryId == null) return;

        // Create a new HistoryEntry object with uppercase values
        HistoryEntry entry = new HistoryEntry(entryId, cat_name, amountPaid, timestamp, null);

        // Save to Firebase
        DaysRef.child(entryId).setValue(entry)
                .addOnSuccessListener(aVoid -> {
                    // Entry saved successfully
                })
                .addOnFailureListener(e -> {
                    throw new RuntimeException("Failed to save entry to Firebase: " + e.getMessage(), e);
                });
    }

    // Method for recording history with string parameters
    public void recordHistory(String cat_item, String amountPaid, String datetime, String itemName) throws ParseException {
        if (cat_item == null || amountPaid == null || datetime == null) {
            return;
        }

        // Convert the category and item name to uppercase
        cat_item = cat_item.toUpperCase(Locale.getDefault());
        itemName = (itemName != null) ? itemName.toUpperCase(Locale.getDefault()) : null; // Make itemName uppercase if it's not null

        // Parse the datetime
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        Date date = inputFormat.parse(datetime);

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String monthYear = monthYearFormat.format(date);

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        String day = dayFormat.format(date);

        // Prepare the Firebase references
        DatabaseReference monthRef = historyRef.child(monthYear);
        DatabaseReference DaysRef = monthRef.child(day);

        // Add entry
        String entryId = DaysRef.push().getKey();
        if (entryId == null) return;

        // Parse amountPaid to double
        double number = Double.parseDouble(amountPaid);

        // Create a new HistoryEntry object with uppercase values
        HistoryEntry entry = new HistoryEntry(entryId, cat_item, number, datetime, itemName);

        // Save to Firebase
        DaysRef.child(entryId).setValue(entry)
                .addOnSuccessListener(aVoid -> {

                })
                .addOnFailureListener(e -> {
                    throw new RuntimeException("Failed to save entry to Firebase: " + e.getMessage(), e);
                });
    }
}