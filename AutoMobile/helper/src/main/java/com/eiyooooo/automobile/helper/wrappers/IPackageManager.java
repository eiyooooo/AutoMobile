package com.eiyooooo.automobile.helper.wrappers;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.IInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.eiyooooo.automobile.helper.utils.L;

public class IPackageManager {
    public final IInterface manager;
    private Method getPackageInfoMethod;
    private Method getQueryIntentActivitiesMethod;
    private Method getInstalledPackagesMethod;

    public IPackageManager(IInterface manager) {
        this.manager = manager;
    }

    private Method getGetPackageInfoMethod() throws NoSuchMethodException {
        if (getPackageInfoMethod == null) {
            Class<?> cls = manager.getClass();
            getPackageInfoMethod = cls.getDeclaredMethod("getPackageInfo", String.class, int.class);
            getPackageInfoMethod.setAccessible(true);
        }
        return getPackageInfoMethod;
    }

    private Method getQueryIntentActivitiesMethod() throws NoSuchMethodException {
        if (getQueryIntentActivitiesMethod == null) {
            Class<?> cls = manager.getClass();
            getQueryIntentActivitiesMethod = cls.getMethod("queryIntentActivities");
        }
        return getQueryIntentActivitiesMethod;
    }

    private Method getGetInstalledPackagesMethod() throws NoSuchMethodException {
        if (getInstalledPackagesMethod == null) {
            Class<?> cls = manager.getClass();
            getInstalledPackagesMethod = cls.getMethod("getAllPackages");
        }
        return getInstalledPackagesMethod;
    }

    public PackageInfo getPackageInfo(String packageName, int flag) throws InvocationTargetException, IllegalAccessException {
        try {
            return (PackageInfo) getGetPackageInfoMethod().invoke(this.manager, new Object[]{packageName, flag});
        } catch (NoSuchMethodException e) {
            L.e("Error in getPackageInfo", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<ResolveInfo> queryIntentActivities(Intent intent,
                                                   String resolvedType, int flags, int userId) {
        try {
            return (List<ResolveInfo>) getQueryIntentActivitiesMethod().invoke(this.manager, new Object[]{intent, resolvedType, flags, userId});
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            L.e("Error in queryIntentActivities", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public List<String> getInstalledPackages(int flag) {
        try {
            return (List<String>) getGetInstalledPackagesMethod().invoke(this.manager, new Object[]{flag});
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            L.e("Error in getInstalledPackages", e);
        }
        return null;
    }
}