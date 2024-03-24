package com.eiyooooo.automobile.app;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this);
        super.onCreate();
    }
}