package com.astin.moneymaster;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.astin.moneymaster.adapter.CategoryAdapter;
import com.astin.moneymaster.adapter.DayAdapter;
import com.astin.moneymaster.adapter.ExpenseAdapter;
import com.astin.moneymaster.helper.FirebaseHelper;
import com.astin.moneymaster.model.HistoryEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private Spinner monthSpinner;
    private RecyclerView dayRecyclerView, itemsRecyclerView;
    private TextView monthSpendtxt, daySpendtxt;
    private FloatingActionButton plusButton;
    private ProgressBar progressBar;
    private static final String TAG = "RecyclerViewTouch";
    private ToggleButton categoryToggle;
    private boolean isShowingCategories = false;
    private String currentMonth;
    private ViewPager2 viewPager;
    private FirebaseHelper firebaseHelper;

    public HistoryFragment() {
        super(R.layout.fragment_history);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize FirebaseHelper
        firebaseHelper = new FirebaseHelper();

        //test
        MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            viewPager = mainActivity.viewPager;
        }

        monthSpinner = view.findViewById(R.id.monthSpinner);
        dayRecyclerView = view.findViewById(R.id.dayRecyclerView);
        itemsRecyclerView = view.findViewById(R.id.itemsRecyclerView);
        monthSpendtxt = view.findViewById(R.id.monthSpendtxt);
        daySpendtxt = view.findViewById(R.id.daySpendtxt);
        plusButton = view.findViewById(R.id.plusButton);
        progressBar = view.findViewById(R.id.progressBar);
        categoryToggle = view.findViewById(R.id.toggleButton);
        dayRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        dayRecyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setUserInputEnabled(false);
            }
        });

        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        itemsRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        viewPager.setUserInputEnabled(true);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        viewPager.setUserInputEnabled(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        viewPager.setUserInputEnabled(false);
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        viewPager.setUserInputEnabled(true);
                        break;

                    case MotionEvent.ACTION_OUTSIDE:
                        viewPager.setUserInputEnabled(false);
                        break;

                    default:
                        viewPager.setUserInputEnabled(false);
                        break;
                }

                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                Log.d(TAG, "onTouchEvent: " + e.toString());
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                Log.d(TAG, "onRequestDisallowInterceptTouchEvent: " + disallowIntercept);
            }
        });

        loadToSpinner();

        monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedMonth = parent.getItemAtPosition(position).toString();
                currentMonth = selectedMonth; // Save the current month selection

                // Load appropriate view based on toggle state
                if (isShowingCategories) {
                    loadCategoriesForMonth(selectedMonth);
                } else {
                    loadDaysForMonth(selectedMonth);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Set up the toggle button listener
        categoryToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isShowingCategories = isChecked;
            String selectedMonth = monthSpinner.getSelectedItem() != null
                    ? monthSpinner.getSelectedItem().toString()
                    : null;

            if (selectedMonth != null) {
                if (isChecked) {
                    // Load categories view
                    loadCategoriesForMonth(selectedMonth);
                    categoryToggle.setText("Categories View");
                } else {
                    // Load days view
                    loadDaysForMonth(selectedMonth);
                    categoryToggle.setText("Days View");
                }
            }
        });

        plusButton.setOnClickListener(v -> ShowDialogBox());

        monthSpendtxt.setOnClickListener(v -> {
            String selectedMonth = monthSpinner.getSelectedItem() != null
                    ? monthSpinner.getSelectedItem().toString()
                    : null;

            if (selectedMonth != null) {
                calculateMonthlySpendingByCategory(selectedMonth);
            } else {
                Toast.makeText(getContext(), "No month selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewPager.setUserInputEnabled(false);
    }

    // New method to load categories for selected month
    private void loadCategoriesForMonth(String monthYear) {
        firebaseHelper.loadCategoriesForMonth(monthYear, new FirebaseHelper.OnCategoriesLoadedListener() {
            @Override
            public void onCategoriesLoaded(ArrayList<String> categories, Map<String, Double> categoryTotals, double totalMonthSpend) {
                // Sort categories based on spending descending
                Collections.sort(categories, (c1, c2) -> {
                    Double spend1 = categoryTotals.get(c1);
                    Double spend2 = categoryTotals.get(c2);
                    // Handle possible nulls just in case
                    if (spend1 == null) spend1 = 0.0;
                    if (spend2 == null) spend2 = 0.0;
                    return spend2.compareTo(spend1);  // descending order
                });

                // Set up the horizontal RecyclerView with sorted categories
                CategoryAdapter categoryAdapter = new CategoryAdapter(categories, category -> {
                    loadItemsForCategory(monthYear, category);
                });

                dayRecyclerView.setAdapter(categoryAdapter);
                monthSpendtxt.setText("Month Total: ₹" + totalMonthSpend);

                // Auto-select the first category if available
                if (!categories.isEmpty()) {
                    String firstCategory = categories.get(0);
                    categoryAdapter.selectCategory(firstCategory);
                    loadItemsForCategory(monthYear, firstCategory);
                } else {
                    daySpendtxt.setText("Category Total: ₹0");
                }
            }


            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }

    private void ShowDialogBox() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Add Missing Items");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.missingdialogbox, null);
        builder.setView(dialogView);

        // Find views
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinnerCategory);
        EditText editItemBudget = dialogView.findViewById(R.id.editItemBudget);
        EditText editDate = dialogView.findViewById(R.id.addDate);
        EditText editItem = dialogView.findViewById(R.id.edititemname);

        // Load categories from Firebase into spinner
        List<String> categoryList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        firebaseHelper.loadCategoryNames(new FirebaseHelper.OnCategoryNamesListener() {
            @Override
            public void onCategoriesLoaded(List<String> categories) {
                categoryList.clear();
                categoryList.addAll(categories);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });

        // Set up date picker
        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(year, month, dayOfMonth, 0, 0, 0);

                        SimpleDateFormat datetime = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                        String selectedDate = datetime.format(selectedCalendar.getTime());

                        editDate.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            String selectedCategory = spinnerCategory.getSelectedItem() != null ? spinnerCategory.getSelectedItem().toString() : "";
            String itemBudgetStr = editItemBudget.getText().toString().trim();
            String itemNameStr = editItem.getText().toString().trim();
            String date = editDate.getText().toString().trim();

            if (selectedCategory.isEmpty() || itemBudgetStr.isEmpty() || date.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            double itemBudget = Double.parseDouble(itemBudgetStr);

            // 1. Save to history
            try {
                firebaseHelper.recordHistory(selectedCategory, itemBudgetStr, date, itemNameStr);
            } catch (ParseException e) {
                Toast.makeText(getContext(), "Try again later", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Update budget_balance using FirebaseHelper
            firebaseHelper.updateBudgetBalance(selectedCategory, itemBudget, new FirebaseHelper.OnBudgetUpdateListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Saved: " + selectedCategory + ", ₹" + itemBudget + ", " + date, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Failed to update budget balance: " + error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, error);
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void calculateMonthlySpendingByCategory(String monthYear) {
        firebaseHelper.calculateMonthlySpendingByCategory(monthYear, new FirebaseHelper.OnCategorySpendingListener() {
            @Override
            public void onDataLoaded(Map<String, Double> categoryTotals) {
                // Sort entries by value descending
                List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(categoryTotals.entrySet());
                Collections.sort(sortedEntries, (e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

                // Build styled message
                SpannableStringBuilder message = new SpannableStringBuilder("Spending in " + monthYear + ":\n\n");

                for (Map.Entry<String, Double> entry : sortedEntries) {
                    String category = entry.getKey();
                    String amount = String.format("₹%.2f", entry.getValue());

                    int start = message.length();
                    message.append(category);
                    int end = message.length();

                    // Make category name bold
                    message.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, end, 0);

                    message.append(": ").append(amount).append("\n");
                }

                new AlertDialog.Builder(getContext())
                        .setTitle("Monthly Category Spending")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }


    private void loadItemsForCategory(String month, String category) {
        ArrayList<HistoryEntry> itemList = new ArrayList<>();
        ExpenseAdapter expenseAdapter = new ExpenseAdapter(itemList, new ExpenseAdapter.OnItemActionListener() {
            @Override
            public void onItemLongClick(HistoryEntry item) {
                showDeleteConfirmationDialog(month, item.getDateTime(), item);
            }

            @Override
            public void onItemEditClick(HistoryEntry item) {
                showEditDialog(month, item.getDateTime(), item);
            }
        });

        // Set the vertical RecyclerView adapter
        itemsRecyclerView.setAdapter(expenseAdapter);

        firebaseHelper.loadItemsForCategory(month, category, new FirebaseHelper.OnHistoryDataListener() {
            @Override
            public void onDataLoaded(ArrayList<HistoryEntry> items, double totalSpend) {
                itemList.clear();
                itemList.addAll(items);
                daySpendtxt.setText("Category Total: ₹" + totalSpend);
                expenseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }

    private void loadToSpinner() {
        progressBar.setVisibility(View.VISIBLE);

        ArrayList<String> monthsyearArray = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, monthsyearArray);
        adapter.setDropDownViewResource(R.layout.spinner_item_dark);
        monthSpinner.setAdapter(adapter);

        firebaseHelper.loadMonths(new FirebaseHelper.OnMonthsLoadedListener() {
            @Override
            public void onMonthsLoaded(ArrayList<String> months) {
                monthsyearArray.clear();
                monthsyearArray.addAll(months);
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);

                if (!monthsyearArray.isEmpty()) {
                    // Always select the first item (which should be the latest month)
                    monthSpinner.setSelection(0);
                    String selectedMonth = monthsyearArray.get(0);
                    currentMonth = selectedMonth;

                    Log.d("ASTIN", "Latest month selected: " + selectedMonth);

                    // Load appropriate view based on toggle state
                    if (isShowingCategories) {
                        loadCategoriesForMonth(selectedMonth);
                    } else {
                        loadDaysForMonth(selectedMonth);
                    }
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load months", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, error);
            }
        });
    }

    private void loadDaysForMonth(String month) {
        firebaseHelper.loadDaysForMonth(month, new FirebaseHelper.OnDaysLoadedListener() {
            @Override
            public void onDaysLoaded(ArrayList<String> days, double totalMonthSpend) {
                ArrayList<String> dayList = new ArrayList<>(days);
                DayAdapter dayAdapter = new DayAdapter(dayList, day -> {
                    loadItemsForDay(month, day);
                });
                dayRecyclerView.setAdapter(dayAdapter);
                monthSpendtxt.setText("Month Total: ₹" + totalMonthSpend);
                daySpendtxt.setText("Day Total: ₹0");

                // Auto-select first day if available
                if (!dayList.isEmpty()) {
                    String latestDay = dayList.get(0);
                    dayAdapter.selectDay(latestDay);
                    loadItemsForDay(month, latestDay);
                    dayRecyclerView.scrollToPosition(0);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load days", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }



    private void loadItemsForDay(String month, String day) {
        ArrayList<HistoryEntry> itemList = new ArrayList<>();
        ExpenseAdapter adapter = new ExpenseAdapter(itemList, new ExpenseAdapter.OnItemActionListener() {
            @Override
            public void onItemLongClick(HistoryEntry item) {
                showDeleteConfirmationDialog(month, day, item);
            }

            @Override
            public void onItemEditClick(HistoryEntry item) {
                showEditDialog(month, day, item);
            }
        });
        itemsRecyclerView.setAdapter(adapter);

        firebaseHelper.loadItemsForDay(month, day, new FirebaseHelper.OnHistoryDataListener() {
            @Override
            public void onDataLoaded(ArrayList<HistoryEntry> items, double totalSpend) {
                itemList.clear();
                itemList.addAll(items);
                daySpendtxt.setText("Day Total: ₹" + totalSpend);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load items for day", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }

    private void showEditDialog(String month, String dayInput, HistoryEntry item) {
        // Check and extract "dd" if dayInput is in "yyyy-MM-dd HH:mm:ss" format
        String dayOnly = dayInput; // fallback if not in expected format
        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        fullFormat.setLenient(false);
        try {
            Date parsedDate = fullFormat.parse(dayInput);
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
            dayOnly = dayFormat.format(parsedDate);
        } catch (ParseException e) {
            Log.w("ASTIN", "Invalid date format for 'day': " + dayInput);
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_expense, null);
        Spinner editSpinnerCategory = dialogView.findViewById(R.id.EditspinnerCategory);
        EditText editAmount = dialogView.findViewById(R.id.editAmount);
        EditText editItem = dialogView.findViewById(R.id.editItemName);
        EditText editDate = dialogView.findViewById(R.id.editDate);

        String currentDate = dayInput + " " + month;
        editDate.setText(currentDate);

        // Set up date picker
        editDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    getContext(),
                    (view, year, monthNEW, dayOfMonth) -> {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(year, monthNEW, dayOfMonth, 0, 0, 0);

                        SimpleDateFormat datetime = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
                        String selectedDate = datetime.format(selectedCalendar.getTime());

                        editDate.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        List<String> categoryList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categoryList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editSpinnerCategory.setAdapter(adapter);

        firebaseHelper.loadCategoryNames(new FirebaseHelper.OnCategoryNamesListener() {
            @Override
            public void onCategoriesLoaded(List<String> categories) {
                categoryList.clear();
                categoryList.addAll(categories);
                adapter.notifyDataSetChanged();

                int index = categoryList.indexOf(item.getCategoryName());
                if (index >= 0) {
                    editSpinnerCategory.setSelection(index);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });

        editAmount.setText(String.valueOf(item.getAmountPaid()));
        String itemNameStr = item.getItemName();
        if (itemNameStr == null || itemNameStr.trim().isEmpty()) {
            editItem.setHint("Item Name (Optional)");
        } else {
            editItem.setText(itemNameStr);
        }

        String finalDayOnly = dayOnly;
        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Expense")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCategory = editSpinnerCategory.getSelectedItem().toString();
                    String itemName = editItem.getText().toString().trim();
                    String amountStr = editAmount.getText().toString().trim();
                    String date = editDate.getText().toString().trim();

                    if (newCategory.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
                        Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double newAmount;
                    try {
                        newAmount = Double.parseDouble(amountStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "Invalid amount entered", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentDate.equals(date)) {
                        // Update existing entry - FIXED: Handle category change properly
                        String oldCategory = item.getCategoryName();
                        double oldAmount = item.getAmountPaid();

                        // First update the history entry
                        firebaseHelper.updateHistoryEntry(month, finalDayOnly, item.getId(), newCategory, newAmount, itemName,
                                new FirebaseHelper.OnBudgetUpdateListener() {
                                    @Override
                                    public void onSuccess() {
                                        // Check if category changed
                                        if (!oldCategory.equalsIgnoreCase(newCategory)) {
                                            // Category changed: restore old category and deduct from new category
                                            firebaseHelper.restoreBudgetBalance(oldCategory, oldAmount,
                                                    new FirebaseHelper.OnBudgetUpdateListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                            // Now deduct from new category
                                                            firebaseHelper.updateBudgetBalance(newCategory, newAmount,
                                                                    new FirebaseHelper.OnBudgetUpdateListener() {
                                                                        @Override
                                                                        public void onSuccess() {
                                                                            Toast.makeText(getContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                                                                        }

                                                                        @Override
                                                                        public void onError(String error) {
                                                                            Log.e(TAG, "Failed to update new category budget: " + error);
                                                                        }
                                                                    });
                                                        }

                                                        @Override
                                                        public void onError(String error) {
                                                            Log.e(TAG, "Failed to restore old category budget: " + error);
                                                        }
                                                    });
                                        } else {
                                            // Same category: just adjust the amount
                                            firebaseHelper.updateBudgetBalanceWithAdjustment(newCategory, oldAmount, newAmount,
                                                    new FirebaseHelper.OnBudgetUpdateListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                            Toast.makeText(getContext(), "Expense updated", Toast.LENGTH_SHORT).show();
                                                        }

                                                        @Override
                                                        public void onError(String error) {
                                                            Log.e(TAG, "Failed to update budget: " + error);
                                                        }
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(getContext(), "Failed to update expense: " + error, Toast.LENGTH_SHORT).show();
                                        Log.e(TAG, error);

                                    }
                                });
                    } else {
                        // Delete old entry and create new one (date changed)
                        try {
                            firebaseHelper.recordHistory(newCategory, amountStr, date, itemName);

                            firebaseHelper.deleteHistoryEntry(month, finalDayOnly, item.getId(),
                                    new FirebaseHelper.OnDeleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            // Restore budget balance for old entry
                                            firebaseHelper.restoreBudgetBalance(item.getCategoryName(), item.getAmountPaid(),
                                                    new FirebaseHelper.OnBudgetUpdateListener() {
                                                        @Override
                                                        public void onSuccess() {
                                                            // Update budget for new entry
                                                            firebaseHelper.updateBudgetBalance(newCategory, newAmount,
                                                                    new FirebaseHelper.OnBudgetUpdateListener() {
                                                                        @Override
                                                                        public void onSuccess() {
                                                                            Toast.makeText(getContext(), "Expense moved successfully", Toast.LENGTH_SHORT).show();
                                                                        }

                                                                        @Override
                                                                        public void onError(String error) {
                                                                            Log.e(TAG, "Failed to update new budget: " + error);
                                                                        }
                                                                    });
                                                        }

                                                        @Override
                                                        public void onError(String error) {
                                                            Log.e(TAG, "Failed to restore old budget: " + error);
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Toast.makeText(getContext(), "Error moving expense: " + error, Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, error);
                                        }
                                    });
                        } catch (ParseException e) {
                            Toast.makeText(getActivity(), "Try again later", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Parse error: " + e.getMessage());
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmationDialog(String month, String day, HistoryEntry item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    deleteExpenseFromFirebase(month, day, item);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteExpenseFromFirebase(String month, String dayInput, HistoryEntry item) {
        String dayOnly = dayInput; // default fallback

        // Check if dayInput is in "yyyy-MM-dd HH:mm:ss" format
        SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        fullFormat.setLenient(false);

        try {
            Date parsedDate = fullFormat.parse(dayInput);
            // If parsed successfully, extract day (dd)
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
            dayOnly = dayFormat.format(parsedDate);
        } catch (ParseException e) {
            Log.w("ASTIN", "Invalid date format for 'day': " + dayInput);
            // Keep dayOnly as-is (use the input directly)
        }

        firebaseHelper.deleteHistoryEntry(month, dayOnly, item.getId(), new FirebaseHelper.OnDeleteListener() {
            @Override
            public void onSuccess() {
                // Restore budget balance
                firebaseHelper.restoreBudgetBalance(item.getCategoryName(), item.getAmountPaid(),
                        new FirebaseHelper.OnBudgetUpdateListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "Failed to restore budget: " + error);
                                // Still show success for main deletion
                                Toast.makeText(getContext(), "Expense deleted", Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error deleting item: " + error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
            }
        });
    }
}