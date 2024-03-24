package com.eiyooooo.automobile.app.helper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.eiyooooo.automobile.app.DevicesFragment;
import com.eiyooooo.automobile.app.HomeFragment;
import com.eiyooooo.automobile.app.LogFragment;
import com.eiyooooo.automobile.app.SettingsFragment;

public class MainViewPagerAdapter extends FragmentStateAdapter {
    public MainViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new DevicesFragment();
            case 2:
                return new LogFragment();
            case 3:
                return new SettingsFragment();
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}