package com.eiyooooo.automobile.app;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.eiyooooo.automobile.app.helper.MainViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.eiyooooo.automobile.app.databinding.ActivityMainBinding;
import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewPager2 viewPager = binding.contentMain.mainViewPager;
        MainViewPagerAdapter adapter = new MainViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(4);

        BottomNavigationView bottomNavigationView = getBottomNavigationView(viewPager);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
            }
        });
    }

    @NotNull
    private BottomNavigationView getBottomNavigationView(ViewPager2 viewPager) {
        BottomNavigationView bottomNavigationView = binding.contentMain.bottomNavigation;

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.nav_devices) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.nav_log) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.nav_settings) {
                viewPager.setCurrentItem(3);
                return true;
            }
            return false;
        });
        return bottomNavigationView;
    }
}