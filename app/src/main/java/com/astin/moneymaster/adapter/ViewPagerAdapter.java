package com.astin.moneymaster.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.astin.moneymaster.HistoryFragment;
import com.astin.moneymaster.MainActivity;
import com.astin.moneymaster.PayFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull MainActivity mainActivity) {
        super(mainActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        if (position == 0) {
            fragment = new PayFragment();
        } else {
            fragment = new HistoryFragment();
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2; // Two tabs
    }
}