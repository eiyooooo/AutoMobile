package com.eiyooooo.automobile.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.eiyooooo.automobile.app.databinding.FragmentDevicesBinding;
import com.eiyooooo.automobile.app.helper.DevicesViewPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

public class DevicesFragment extends Fragment {

    private FragmentDevicesBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentDevicesBinding.inflate(inflater, container, false);

        FragmentActivity activity = getActivity();
        if (activity == null) return binding.getRoot();
        ViewPager2 subViewPager = binding.devicesViewPager;
        DevicesViewPagerAdapter adapter = new DevicesViewPagerAdapter(activity);
        subViewPager.setAdapter(adapter);

        new TabLayoutMediator(binding.devicesTabLayout, subViewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Device 1");
                            break;
                        case 1:
                            tab.setText("Device 2");
                            break;
                    }
                }
        ).attach();

        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}