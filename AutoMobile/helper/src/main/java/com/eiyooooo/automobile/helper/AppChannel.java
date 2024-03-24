package com.eiyooooo.automobile.helper;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.TaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.*;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Surface;
import com.eiyooooo.automobile.helper.utils.L;
import com.eiyooooo.automobile.helper.utils.Workarounds;
import com.eiyooooo.automobile.helper.wrappers.IPackageManager;
import com.eiyooooo.automobile.helper.wrappers.ServiceManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppChannel {
    IPackageManager pm;
    DisplayMetrics displayMetrics;
    Configuration configuration;
    Context context;
    boolean hasRealContext = false;

    public AppChannel() {
        L.d("Construct AppChannel without a context");
        displayMetrics = new DisplayMetrics();
        displayMetrics.setToDefaults();
        configuration = new Configuration();
        configuration.setToDefaults();
        pm = ServiceManager.getPackageManager();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    L.d("Runnable run");

                    // Looper.prepare() cannot be removed
                    Looper.prepare();

                    FileOutputStream fileOutputStream = new FileOutputStream("/data/local/tmp/automobile/helper_cache", false);

                    // Redirect output, because fillAppInfo will have a bunch of errors
                    PrintStream console = System.out;
                    System.setErr(new PrintStream(fileOutputStream, false));
                    System.setOut(new PrintStream(fileOutputStream, false));
                    context = Workarounds.fillAppInfo();
                    ContextWrapperWrapper wrapper = new ContextWrapperWrapper(context);
                    // Recover output
                    System.setOut(console);
                    System.setErr(console);

                    DisplayManager dm = (DisplayManager) wrapper.getSystemService(Context.DISPLAY_SERVICE);
                    dm.registerDisplayListener(new DisplayManager.DisplayListener() {
                        @Override
                        public void onDisplayAdded(int displayId) {
                            L.d("onDisplayAdded invoked displayId:" + displayId);
                        }

                        @Override
                        public void onDisplayRemoved(int displayId) {
                            L.d("onDisplayRemoved invoked displayId:" + displayId);

                        }

                        @Override
                        public void onDisplayChanged(int displayId) {
                            L.d("onDisplayChanged invoked displayId:" + displayId);
                        }
                    }, null);

                    L.d("Context: " + context.toString());

                    L.d("PHONE_INFO->" + getPhoneInfo());

                    // Looper.loop() cannot be removed
                    Looper.loop();
                } catch (Exception e) {
                    L.w("AppChannel Runnable run error:", e);
                }
            }
        }).start();
    }

    public AppChannel(Context context) {
        L.d("Construct AppChannel with a context");
        displayMetrics = new DisplayMetrics();
        displayMetrics.setToDefaults();
        configuration = new Configuration();
        configuration.setToDefaults();
        pm = ServiceManager.getPackageManager();
        this.context = context;
        hasRealContext = true;
        try {
            L.d("PHONE_INFO->" + getPhoneInfo());
        } catch (Exception e) {
            L.w("getInfo error:", e);
        }
    }

    public static JSONObject getPhoneInfo() throws Exception {
        JSONObject info = new JSONObject();
        info.put("Build.MANUFACTURER", Build.MANUFACTURER);
        info.put("Build.MODEL", Build.MODEL);
        info.put("Build.DEVICE", Build.DEVICE);
        info.put("Build.SERIAL", Build.SERIAL);
        info.put("Build.VERSION.RELEASE", Build.VERSION.RELEASE);
        info.put("Build.VERSION.SDK_INT", Build.VERSION.SDK_INT);
        info.put("Build.BOARD", Build.BOARD);
        info.put("Build.PRODUCT", Build.PRODUCT);
        info.put("Build.USER", Build.USER);
        info.put("Build.BRAND", Build.BRAND);
        info.put("Build.HARDWARE", Build.HARDWARE);
        info.put("Build.SUPPORTED_ABIS", Arrays.toString(Build.SUPPORTED_ABIS));
        return info;
    }


    static class ContextWrapperWrapper extends ContextWrapper {
        public ContextWrapperWrapper(Context base) {
            super(base);
        }

        @Override
        public String getPackageName() {
            // `Workarounds.getContext().getPackageName()` always returns `android`,
            // but `createVirtualDisplay` will validate the package name againest current
            // uid.
            // For ADB shell, the uid is 2000 (shell) and the only avaiable package name is
            // `com.android.shell`
            return "com.android.shell";
        }
    }

    public static Bitmap getLocalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            L.e("getLocalBitmap", e);
            return null;
        }
    }

    public String getAppInfos(List<String> packages) {
        StringBuilder builder = new StringBuilder();
        for (String packageName : packages) {
            PackageInfo packageInfo = null;

            try {
                packageInfo = getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            } catch (InvocationTargetException | IllegalAccessException e) {
                L.e("getPackageInfo error:", e);
            }
            if (packageInfo == null) continue;

            phasePackageInfo(builder, packageInfo);
        }
        return builder.toString().trim();
    }

    @SuppressLint("QueryPermissionsNeeded")
    public List<String> getAppPackages() {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            List<String> packages = new ArrayList<>();
            List<PackageInfo> infos = pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
            for (PackageInfo info : infos) {
                packages.add(info.packageName);
            }
            return packages;
        } else {
            return pm.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        }
    }

    public PackageInfo getPackageInfo(String packageName) throws InvocationTargetException, IllegalAccessException {
        return getPackageInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
    }

    public PackageInfo getPackageInfo(String packageName, int flag) throws InvocationTargetException, IllegalAccessException {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = null;
            try {
                info = pm.getPackageInfo(packageName, flag);
            } catch (PackageManager.NameNotFoundException e) {
                L.e("getPackageInfo", e);
            }
            return info;
        } else {
            return pm.getPackageInfo(packageName, flag);
        }
    }

    public String getAllAppInfo(int appType) throws InvocationTargetException, IllegalAccessException {
        List<String> packages = getAppPackages();
        if (packages == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String packageName : packages) {
            PackageInfo packageInfo = getPackageInfo(packageName);
            if (packageInfo == null) continue;
            int resultTag = packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM;
            if (appType != 0) {
                if (appType == 1) {
                    if (resultTag > 0) {
                        // system app
                        continue;
                    }
                } else {
                    if (resultTag == 0) {
                        // user app
                        continue;
                    }
                }
            }
            phasePackageInfo(builder, packageInfo);
        }
        return builder.toString().trim();
    }

    private void phasePackageInfo(StringBuilder builder, PackageInfo packageInfo) {
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        builder.append(applicationInfo.packageName);
        builder.append("\r").append(getLabel(applicationInfo));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.append("\r").append(packageInfo.applicationInfo.minSdkVersion);
        } else {
            builder.append("\r").append("null");
        }
        builder.append("\r").append(applicationInfo.targetSdkVersion);
        builder.append("\r").append(packageInfo.versionName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.append("\r").append(packageInfo.getLongVersionCode());
        } else {
            builder.append("\r").append(packageInfo.versionCode);
        }
        builder.append("\r").append(applicationInfo.enabled);
        try {
            // Hidden apps will not be obtained
            getPackageInfo(packageInfo.packageName, PackageManager.GET_DISABLED_COMPONENTS);
            builder.append("\r").append(false);
        } catch (InvocationTargetException e) {
            L.d(packageInfo.packageName + "is hidden");
            builder.append("\r").append(true);
        } catch (IllegalAccessException e) {
            builder.append("\r").append("unknown");
        }
        builder.append("\r").append(applicationInfo.uid);
        builder.append("\r").append(applicationInfo.sourceDir);
        builder.append("\n");
    }

    public String getLabel(ApplicationInfo info) {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            return (String) info.loadLabel(pm);
        }
        int res = info.labelRes;
        if (info.nonLocalizedLabel != null) {
            return (String) info.nonLocalizedLabel;
        }
        if (res != 0) {
            AssetManager assetManager = getAssetManagerFromPath(info.sourceDir);
            Resources resources = new Resources(assetManager, displayMetrics, configuration);
            return (String) resources.getText(res);
        }
        return null;
    }

    AssetManager getAssetManagerFromPath(String path) {
        AssetManager assetManager = null;
        try {
            assetManager = AssetManager.class.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            L.e("getAssetManagerFromPath", e);
        }
        try {
            assert assetManager != null;
            assetManager.getClass().getMethod("addAssetPath", String.class).invoke(assetManager, path);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            L.e("getAssetManagerFromPath", e);
        }
        return assetManager;
    }

    public String openApp(String packageName, String activity, int displayId) {
        if (!hasRealContext) {
            String cmd;
            if (displayId != 0) cmd = "am start --display " + displayId + " -n " + packageName + "/" + activity;
            else cmd = "am start -n " + packageName + "/" + activity;
            L.d("start activity cmd: " + cmd);
            try {
                execReadOutput(cmd);
            } catch (Exception e) {
                L.e("openApp", e);
                return e.toString();
            }
            return null;
        }
        try {
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // no animation
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            ActivityOptions options = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && displayId != 0) {
                options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId);
            }
            ComponentName cName = new ComponentName(packageName, activity);
            intent.setComponent(cName);
            if (options != null) {
                context.startActivity(intent, options.toBundle());
            } else {
                context.startActivity(intent);
            }
        } catch (Exception e) {
            L.e("openApp", e);
        }
        return null;
    }

    @SuppressLint("QueryPermissionsNeeded")
    public String getAppActivities(String data) {
        StringBuilder builder = new StringBuilder();
        List<String> packages = getAppPackages();
        for (String packageName : packages) {
            PackageInfo info = null;
            try {
                info = getPackageInfo(packageName);
            } catch (InvocationTargetException | IllegalAccessException e) {
                L.e("getPackageInfo error:", e);
            }
            if (info != null && info.packageName.equals(data)) {
                try {
                    PackageInfo packageInfo = getPackageInfo(data, PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_ACTIVITIES);
                    if (packageInfo.activities == null) {
                        return "";
                    }
                    for (ActivityInfo activityInfo : packageInfo.activities) {
                        builder.append(activityInfo.name).append("\n");
                    }
                } catch (InvocationTargetException | IllegalAccessException e) {
                    L.e("getPackageInfo error:", e);
                }
                return builder.toString();
            }
        }
        return builder.toString();
    }

    public String getAppPermissions(String data) {
        StringBuilder builder = new StringBuilder();
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            try {
                PackageInfo packageInfo = getPackageInfo(data, PackageManager.GET_UNINSTALLED_PACKAGES | PackageManager.GET_PERMISSIONS);
                String[] usesPermissionsArray = packageInfo.requestedPermissions;
                for (String usesPermissionName : usesPermissionsArray) {
                    // get the name of each permission, such as: android.permission.INTERNET
                    builder.append(usesPermissionName);

                    // get the detailed information of each permission
                    PermissionInfo permissionInfo;
                    try {
                        permissionInfo = pm.getPermissionInfo(usesPermissionName, 0);
                    } catch (Exception e) {
                        builder.append(" <no permission info>\n");
                        continue;
                    }

                    // get the detailed description of each permission
                    // such as: allow the app to create network sockets and use custom network protocols
                    try {
                        String permissionDescription = permissionInfo.loadDescription(pm).toString();
                        builder.append(" <").append(permissionDescription).append(">");
                    } catch (Exception e) {
                        builder.append(" <no description>");
                    }

                    // get if the app has the permission
                    try {
                        boolean isHasPermission = PackageManager.PERMISSION_GRANTED == pm.checkPermission(permissionInfo.name, data);
                        builder.append(" ").append(isHasPermission).append("\n");
                    } catch (Exception e) {
                        builder.append(" ").append("unknown").append("\n");
                    }
                }
            } catch (Exception e) {
                L.e("getAppPermissions", e);
            }
            return builder.toString();
        }
        return builder.toString();
    }

    public String getAppMainActivity(String packageName) {
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                return launchIntent.getComponent().getClassName();
            } else {
                L.d(packageName + " get main activity failed");
                return "";
            }
        }
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = pm.queryIntentActivities(mainIntent, null, 0, 0);
        for (ResolveInfo resolveInfo : appList) {
            String packageStr = resolveInfo.activityInfo.packageName;
            if (packageStr.equals(packageName)) {
                return resolveInfo.activityInfo.name;
            }
        }
        return "";
    }

    public String getAppDetail(String data) {
        StringBuilder builder = new StringBuilder();
        try {
            PackageInfo packageInfo = getPackageInfo(data);
            builder.append(packageInfo.firstInstallTime).append("\r");
            builder.append(packageInfo.lastUpdateTime).append("\r");
            builder.append(packageInfo.applicationInfo.dataDir).append("\r");
            builder.append(packageInfo.applicationInfo.nativeLibraryDir);
        } catch (InvocationTargetException | IllegalAccessException e) {
            L.e("getPackageInfo error:", e);
        }
        return builder.toString();
    }

    public Bitmap getUninstallAPKIcon(String apkPath) {
        Drawable icon = getApkIcon(context, apkPath);
        return Drawable2Bitmap(icon);
    }

    public static Drawable getApkIcon(Context context, String apkPath) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (packageInfo != null) {
            ApplicationInfo info = packageInfo.applicationInfo;
            info.sourceDir = apkPath;
            info.publicSourceDir = apkPath;
            try {
                return info.loadIcon(packageManager);
            } catch (Exception e) {
                L.e("getApkIcon", e);
            }
        }
        return null;
    }

    public synchronized Bitmap Drawable2Bitmap(String packageName) throws
            InvocationTargetException, IllegalAccessException {
        Drawable icon;

        PackageInfo packageInfo = getPackageInfo(packageName);
        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (applicationInfo == null) {
            L.e("applicationInfo == null");
            return null;
        }

        AssetManager assetManager;
        try {
            assetManager = AssetManager.class.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            L.e("getBitmap", e);
            return null;
        }

        try {
            assetManager.getClass().getMethod("addAssetPath", String.class).invoke(assetManager, applicationInfo.sourceDir);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            L.e("getBitmap", e);
            return null;
        }

        Resources resources = new Resources(assetManager, displayMetrics, configuration);
        try {
            icon = resources.getDrawable(applicationInfo.icon, null);
        } catch (Exception e) {
            L.e("getBitmap package error:" + applicationInfo.packageName);
            return null;
        }

        return Drawable2Bitmap(icon);
    }

    public byte[] getApkBitmapBytes(String path) {
        return Bitmap2Bytes(getUninstallAPKIcon(path));
    }

    public byte[] getBitmapBytes(String packageName) throws
            InvocationTargetException, IllegalAccessException {
        return Bitmap2Bytes(Drawable2Bitmap(packageName));
    }

    static public byte[] Bitmap2Bytes(Bitmap bm) {
        if (bm == null) {
            return new byte[0];
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, output);
        return output.toByteArray();
    }

    private Bitmap Drawable2Bitmap(Drawable icon) {
        try {
            if (icon == null) return null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon instanceof AdaptiveIconDrawable) {
                Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                icon.draw(canvas);
                return bitmap;
            } else {
                int w = icon.getIntrinsicWidth();
                int h = icon.getIntrinsicHeight();
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                icon.setBounds(0, 0, w, h);
                icon.draw(canvas);
                return bitmap;
            }
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressLint({"PrivateApi", "BlockedPrivateApi"})
    public byte[] getTaskThumbnail(int id) throws Exception {
        Class<?> cls = Class.forName("android.app.ActivityTaskManager");
        java.lang.reflect.Method services = cls.getDeclaredMethod("getService");
        Object iam = services.invoke(null);
        if (iam == null) return null;

        java.lang.reflect.Method snapshotMethod = iam.getClass().getDeclaredMethod("getTaskSnapshot", int.class, boolean.class);
        snapshotMethod.setAccessible(true);
        Object snapshot = snapshotMethod.invoke(iam, id, true);
        if (snapshot == null) return null;

        java.lang.reflect.Field buffer = snapshot.getClass().getDeclaredField("mSnapshot");
        buffer.setAccessible(true);
        Object hardBuffer = buffer.get(snapshot);

        Object colorSpace = snapshot.getClass().getMethod("getColorSpace").invoke(snapshot);
        Class<?> bitmapCls = Class.forName("android.graphics.Bitmap");

        java.lang.reflect.Method wrapHardwareBufferMethod = bitmapCls.getMethod("wrapHardwareBuffer", hardBuffer.getClass(), Class.forName("android.graphics.ColorSpace"));
        Bitmap bmp = (Bitmap) wrapHardwareBufferMethod.invoke(null, hardBuffer, colorSpace);
        if (bmp == null) return null;

        Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaledBmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private List<ActivityManager.RecentTaskInfo> getRecentTasks(int maxNum, int flags,
                                                               int userId) throws Exception {
        Object iam = getIAM();
        Object tasksParcelled = iam.getClass().getMethod("getRecentTasks", Integer.TYPE,
                Integer.TYPE, Integer.TYPE).invoke(iam, maxNum, flags, userId);
        if (tasksParcelled == null) return null;
        return (List<ActivityManager.RecentTaskInfo>) tasksParcelled.getClass().getMethod("getList").invoke(tasksParcelled);
    }

    private static Object getIAM() throws Exception {
        return ActivityManager.class.getMethod("getService").invoke(null);
    }

    @SuppressLint("BlockedPrivateApi")
    public JSONObject getRecentTasksJson(int maxNum, int flags, int userId) throws Exception {
        List<ActivityManager.RecentTaskInfo> tasks = getRecentTasks(maxNum, flags, userId);
        if (tasks == null) throw new Exception("getRecentTasks failed");

        JSONObject jsonObjectResult = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (ActivityManager.RecentTaskInfo taskInfo : tasks) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", taskInfo.id);
            jsonObject.put("persistentId", taskInfo.persistentId);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    java.lang.reflect.Field field = TaskInfo.class.getDeclaredField("displayId");
                    field.setAccessible(true);
                    Object displayId = field.get(taskInfo);
                    jsonObject.put("displayId", displayId);
                }
                jsonObject.put("topPackage", taskInfo.topActivity == null ? "" : taskInfo.topActivity.getPackageName());
                jsonObject.put("topActivity", taskInfo.topActivity == null ? "" : taskInfo.topActivity.getClassName());
                if (taskInfo.topActivity != null) {
                    PackageInfo packageInfo = getPackageInfo(taskInfo.topActivity.getPackageName());
                    jsonObject.put("label", getLabel(packageInfo.applicationInfo));
                } else {
                    jsonObject.put("label", "");
                }
            }
            jsonArray.put(jsonObject);
        }
        jsonObjectResult.put("data", jsonArray);
        return jsonObjectResult;
    }

    public VirtualDisplay createVirtualDisplay(Surface surface, int width, int height, int density) throws Exception {
        android.hardware.display.DisplayManager displayManager = DisplayManager.class.getDeclaredConstructor(Context.class).newInstance(new ContextWrapperWrapper(context));
        int flags = getVirtualDisplayFlags();
        return displayManager.createVirtualDisplay("AutoMobile", width, height, density, surface, flags);
    }

    private static int getVirtualDisplayFlags() {
        int VIRTUAL_DISPLAY_FLAG_PUBLIC = 1;
        int VIRTUAL_DISPLAY_FLAG_PRESENTATION = 1 << 1;
        int VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY = 1 << 3;
        int VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 << 8;
        int VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 << 10;
        int VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP = 1 << 11;
        int VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED = 1 << 12;

        int flags = VIRTUAL_DISPLAY_FLAG_PUBLIC | VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL | VIRTUAL_DISPLAY_FLAG_PRESENTATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            flags |= VIRTUAL_DISPLAY_FLAG_TRUSTED | VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP | VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED;
        }
        return flags;
    }

    public static String execReadOutput(String cmd) throws IOException, InterruptedException {
        Process process = new ProcessBuilder().command("sh", "-c", cmd).start();
        StringBuilder builder = new StringBuilder();
        String line;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while ((line = bufferedReader.readLine()) != null) builder.append(line).append("\n");
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) throw new IOException("command: " + cmd + "\nfailed with exit code: " + exitCode + "\ndebug info: " + builder);
        return builder.toString();
    }
}
