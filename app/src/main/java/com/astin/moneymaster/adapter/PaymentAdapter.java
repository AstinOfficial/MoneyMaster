package com.astin.moneymaster.adapter;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.astin.moneymaster.model.PaymentItem;
import com.astin.moneymaster.R;
import com.astin.moneymaster.helper.FirebaseHelper;

import java.util.List;

public class PaymentAdapter extends RecyclerView.Adapter<PaymentAdapter.ViewHolder> {

    private List<PaymentItem> cat_item_list;
    private Context context;
    private FirebaseHelper firebaseHelper;

    public PaymentAdapter(Context context, List<PaymentItem> cat_item_list) {
        this.context = context;
        this.cat_item_list = cat_item_list;
        this.firebaseHelper = new FirebaseHelper();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemText;
        Button payButton;
        EditText amounttxt;

        public ViewHolder(View itemView) {
            super(itemView);
            itemText = itemView.findViewById(R.id.itemText);
            payButton = itemView.findViewById(R.id.payButton);
            amounttxt = itemView.findViewById(R.id.amounttxt);
        }
    }

    @NonNull
    @Override
    public PaymentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentAdapter.ViewHolder holder, int position) {
        PaymentItem item = cat_item_list.get(position);

        // Show item name and budget
        String displayText = item.getName() + "\n (" + item.getBudget_balance() + " / " + item.getBudget() + ")";
        holder.itemText.setText(displayText);

        @SuppressLint("NotifyDataSetChanged") View.OnClickListener payAction = v -> {
            String amountStr = holder.amounttxt.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);

                    // Use FirebaseHelper to process payment
                    firebaseHelper.processPayment(item, amount, new FirebaseHelper.OnPaymentListener() {
                        @Override
                        public void onPaymentSuccess(String message) {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            holder.amounttxt.setText("");
                            notifyDataSetChanged();
                            launchGooglePay();
                        }

                        @Override
                        public void onPaymentError(String error) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Please enter an amount.. Opening Gpay", Toast.LENGTH_SHORT).show();
                launchGooglePay();
            }
        };

        holder.payButton.setOnClickListener(payAction);

        holder.amounttxt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                holder.payButton.performClick();
                return true;
            }
            return false;
        });

        holder.itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete \"" + item.getName() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Use FirebaseHelper to delete item
                        firebaseHelper.deletePaymentItem(item, new FirebaseHelper.OnItemDeleteListener() {
                            @Override
                            public void onDeleteSuccess(String message) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                // Find the current position of the item and remove it safely
                                int currentPosition = cat_item_list.indexOf(item);
                                if (currentPosition != -1 && currentPosition < cat_item_list.size()) {
                                    cat_item_list.remove(currentPosition);
                                    notifyItemRemoved(currentPosition);
                                    notifyItemRangeChanged(currentPosition, cat_item_list.size());
                                }
                            }

                            @Override
                            public void onDeleteError(String error) {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void launchGooglePay() {
        Intent launchIntent = new Intent();
        launchIntent.setClassName("com.google.android.apps.nbu.paisa.user",
                "com.google.nbu.paisa.flutter.gpay.app.LauncherActivity");
        try {
            context.startActivity(launchIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Google Pay app not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return cat_item_list.size();
    }
}