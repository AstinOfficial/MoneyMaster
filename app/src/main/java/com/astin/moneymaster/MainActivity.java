package com.astin.moneymaster;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.astin.moneymaster.adapter.ViewPagerAdapter;
import com.astin.moneymaster.helper.FirebaseHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView noInternetText;
    private ProgressBar progressBar;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FirebaseHelper firebaseHelper;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noInternetText = findViewById(R.id.noInternetText);
        progressBar = findViewById(R.id.progressBar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseHelper = new FirebaseHelper();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // User already signed in, initialize app immediately
            progressBar.setVisibility(View.GONE);
            Log.d("ASTIN", "Already signed in as UID: " + currentUser.getUid());
            initializeAppAfterLogin();
        } else {
            // Show loading and sign in
            progressBar.setVisibility(View.VISIBLE);
            signInAdmin();
        }
    }

    private void signInAdmin() {
        String email = "test44@gmail.com";
        String password = "test44";

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE); // Hide loading regardless

                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            Log.d("ASTIN", "Signed in as UID: " + user.getUid());
                            initializeAppAfterLogin();
                        }
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("ASTIN", "Firebase login failed", task.getException());
                    }
                });
    }

    private void initializeAppAfterLogin() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        String[] tabTitles = {
                "Pay \n(" + getCurrentDate() + ")",
                "History"
        };

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(tabTitles[position])
        ).attach();

        tabLayout.post(() -> {
            TabLayout.Tab payTab = tabLayout.getTabAt(0);
            if (payTab != null && payTab.view != null) {
                payTab.view.setOnLongClickListener(v -> {
                    onPayTabLongClicked();
                    return true;
                });
            }
        });

        monitorNetworkConnectivity();
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void monitorNetworkConnectivity() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> {
                    noInternetText.setVisibility(View.GONE);
                    viewPager.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> {
                    noInternetText.setVisibility(View.VISIBLE);
                    viewPager.setVisibility(View.GONE);
                });
            }
        };

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    private void onPayTabLongClicked() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Budget Balances")
                .setMessage("Are you sure you want to reset all budget balances to their original budget amounts?")
                .setPositiveButton("Yes", (dialog, which) -> resetAllBudgetBalances())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void resetAllBudgetBalances() {
        firebaseHelper.resetAllBudgetBalances(new FirebaseHelper.OnBudgetResetListener() {
            @Override
            public void onResetSuccess() {
                Toast.makeText(MainActivity.this, "All budget balances reset", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResetError(String error) {
                Toast.makeText(MainActivity.this, "Failed to reset balances: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}
