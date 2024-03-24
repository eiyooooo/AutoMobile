package com.eiyooooo.automobile.app.helper;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.eiyooooo.automobile.app.SingleDeviceFragment;

public class DevicesViewPagerAdapter extends FragmentStateAdapter {
    public DevicesViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return new SingleDeviceFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}