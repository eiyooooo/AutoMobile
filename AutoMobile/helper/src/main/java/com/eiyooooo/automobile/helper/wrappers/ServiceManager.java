package com.eiyooooo.automobile.helper.wrappers;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class ServiceManager {
    private static final Method GET_SERVICE_METHOD;

    static {
        try {
            GET_SERVICE_METHOD = Class.forName("android.os.ServiceManager").getDeclaredMethod("getService", String.class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static DisplayManagerRef displayManagerRef;
    private static IPackageManager packageManager;

    private ServiceManager() {
        /* not instantiable */
    }

    private static IInterface getService(String service, String type) {
        try {
            IBinder binder = (IBinder) GET_SERVICE_METHOD.invoke(null, service);
            Method asInterfaceMethod = Class.forName(type + "$Stub").getMethod("asInterface", IBinder.class);
            return (IInterface) asInterfaceMethod.invoke(null, binder);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static DisplayManagerRef getDisplayManager() {
        if (displayManagerRef == null) {
            try {
                Class<?> clazz = Class.forName("android.hardware.display.DisplayManagerGlobal");
                Method getInstanceMethod = clazz.getDeclaredMethod("getInstance");
                Object dmg = getInstanceMethod.invoke(null);
                displayManagerRef = new DisplayManagerRef(dmg);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
                     InvocationTargetException e) {
                throw new AssertionError(e);
            }
        }
        return displayManagerRef;
    }

    public static IPackageManager getPackageManager() {
        if (packageManager == null) {
            packageManager = new IPackageManager(getService("package", "android.content.pm.IPackageManager"));
        }
        return packageManager;
    }
}