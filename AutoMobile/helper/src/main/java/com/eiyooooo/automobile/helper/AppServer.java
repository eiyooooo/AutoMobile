package com.eiyooooo.automobile.helper;

import android.content.Context;
import android.hardware.display.VirtualDisplay;
import android.os.Build;
import android.view.Display;
import android.view.SurfaceView;
import com.eiyooooo.automobile.helper.utils.L;
import com.eiyooooo.automobile.helper.utils.ServerUtil;
import com.eiyooooo.automobile.helper.utils.Workarounds;
import com.eiyooooo.automobile.helper.wrappers.DisplayManagerRef;
import com.eiyooooo.automobile.helper.wrappers.ServiceManager;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppServer extends NanoHTTPD {
    public AppServer(String address, int port) {
        super(address, port);
    }

    AppChannel appChannel;

    // Construct the server without a context
    public static void main(String[] args) throws Exception {
        L.d("Welcome!!!");
        AppServer server = ServerUtil.safeGetServer();
        if (server == null) return;
        Workarounds.prepareMainLooper();
        server.appChannel = new AppChannel();
        L.d("success start port ->" + server.getListeningPort() + "<-");
        writePort("/data/local/tmp/automobile", server.getListeningPort());
        // Make the thread wait forever
        while (true) {
            Thread.sleep(1000);
        }
    }

    // Construct the server with a context
    public static void startServerFromActivity(Context context) {
        AppServer server = ServerUtil.safeGetServer();
        if (server == null) return;
        writePort(context.getFilesDir().getPath(), server.getListeningPort());
        server.appChannel = new AppChannel(context);
        System.out.println("success start:" + server.getListeningPort());
        System.out.flush();
    }

    public static void writePort(String path, int port) {
        OutputStream out;
        try {
            String filePath = path + "/helper_port";
            out = new FileOutputStream(filePath);
            L.d("port file -> " + filePath);
            out.write((port + "").getBytes());
            out.close();
        } catch (IOException e) {
            L.e("writePort error", e);
        }
    }

    Map<Integer, VirtualDisplay> cache = new HashMap<>();

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, List<String>> parameters = session.getParameters();
        try {
            // Get phone info
            if (session.getUri().startsWith("/" + AppChannelProtocol.getPhoneInfo)) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", AppChannel.getPhoneInfo().toString());
            }

            // Get the recent tasks
            if (session.getUri().startsWith("/" + AppChannelProtocol.getRecentTask)) {
                List<String> line1 = parameters.get("maxNum");
                List<String> line2 = parameters.get("flags");
                List<String> line3 = parameters.get("userId");

                int maxNum = 25;
                if (line1 != null) maxNum = Integer.parseInt(line1.get(0));
                int flags = 0;
                if (line2 != null) flags = Integer.parseInt(line2.get(0));
                int userId = 0;
                if (line3 != null) userId = Integer.parseInt(line3.get(0));

                return newFixedLengthResponse(Response.Status.OK, "application/json", appChannel.getRecentTasksJson(maxNum, flags, userId).toString());
            }

            // Get the icon of the app
            if (session.getUri().startsWith("/" + AppChannelProtocol.getIconData)) {
                byte[] bytes;
                if (parameters.containsKey("path")) {
                    List<String> line = parameters.get("path");
                    String path = null;
                    if (line != null) path = line.get(0);
                    bytes = appChannel.getApkBitmapBytes(path);
                } else if (parameters.containsKey("package")) {
                    List<String> line = parameters.get("package");
                    String packageName = null;
                    if (line != null) packageName = line.get(0);
                    bytes = appChannel.getBitmapBytes(packageName);
                } else {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'path' or 'package' not found");
                }
                return newFixedLengthResponse(Response.Status.OK, "image/jpg", new ByteArrayInputStream(bytes), bytes.length);
            }

            // Get all app info
            // Including hidden and frozen apps
            if (session.getUri().startsWith("/" + AppChannelProtocol.getAllAppInfo)) {
                List<String> line = parameters.get("app_type");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'app_type' not found");
                int appType = Integer.parseInt(line.get(0));
                byte[] bytes = appChannel.getAllAppInfo(appType).getBytes();
                return newFixedLengthResponse(Response.Status.OK, "application/json", new ByteArrayInputStream(bytes), bytes.length);
            }

            // Get the information of the specified app list
            if (session.getUri().startsWith("/" + AppChannelProtocol.getAppInfos)) {
                List<String> packages = parameters.get("apps");
                if (packages == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'apps' not found");
                byte[] bytes = appChannel.getAppInfos(packages).getBytes();
                return newFixedLengthResponse(Response.Status.OK, "application/json", new ByteArrayInputStream(bytes), bytes.length);
            }

            // Get the detailed information of a single app
            if (session.getUri().startsWith("/" + AppChannelProtocol.getAppDetail)) {
                List<String> line = parameters.get("package");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'package' not found");
                String packageName = line.get(0);
                byte[] bytes = appChannel.getAppDetail(packageName).getBytes();
                return newFixedLengthResponse(Response.Status.OK, "application/json", new ByteArrayInputStream(bytes), bytes.length);
            }

            // Get the thumbnail
            if (session.getUri().startsWith("/" + AppChannelProtocol.getTaskThumbnail)) {
                List<String> line = parameters.get("id");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'id' not found");
                String id = line.get(0);
                byte[] bytes = appChannel.getTaskThumbnail(Integer.parseInt(id));
                if (bytes == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "get task thumbnail failed");
                return newFixedLengthResponse(Response.Status.OK, "image/jpg", new ByteArrayInputStream(bytes), bytes.length);
            }

            // Get the main activity by package name
            if (session.getUri().startsWith("/" + AppChannelProtocol.getAppMainActivity)) {
                List<String> line = parameters.get("package");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'package' not found");
                String packageName = line.get(0);
                byte[] bytes = appChannel.getAppMainActivity(packageName).getBytes();
                return newFixedLengthResponse(Response.Status.OK, "application/json", new ByteArrayInputStream(bytes), bytes.length);
            }

            // Create virtual display
            if (session.getUri().startsWith("/" + AppChannelProtocol.createVirtualDisplay)) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Virtual display is not supported before Android 11");
                }
                SurfaceView surfaceView = new SurfaceView(appChannel.context);
                List<String> line1 = parameters.get("width");
                List<String> line2 = parameters.get("height");
                List<String> line3 = parameters.get("density");
                if (line1 != null && line2 == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'width' found, but 'height' not found");
                if (line1 == null && line2 != null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'height' found, but 'width' not found");

                DisplayManagerRef dm = ServiceManager.getDisplayManager();
                JSONObject defaultDisplay = dm.getDisplayInfo(Display.DEFAULT_DISPLAY);
                int width, height, density;
                if (line1 != null) {
                    width = Integer.parseInt(line1.get(0));
                    height = Integer.parseInt(line2.get(0));
                } else {
                    int rotation = (int) defaultDisplay.get("rotation");
                    if (rotation == 1 || rotation == 3) {
                        width = (int) defaultDisplay.get("height");
                        height = (int) defaultDisplay.get("width");
                    } else {
                        width = (int) defaultDisplay.get("width");
                        height = (int) defaultDisplay.get("height");
                    }
                }
                if (line3 != null) density = Integer.parseInt(line3.get(0));
                else density = (int) defaultDisplay.get("density");

                VirtualDisplay display = appChannel.createVirtualDisplay(
                        surfaceView.getHolder().getSurface(),
                        width, height, density);
                if (display != null) cache.put(display.getDisplay().getDisplayId(), display);
                int[] displayIds = dm.getDisplayIds();
                for (int displayId : displayIds) {
                    L.d(">>>display -> " + dm.getDisplayInfo(displayId).get("name") + " -> " + displayId);
                }
                if (display == null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "create virtual display failed");
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "success create display, id -> " + display.getDisplay().getDisplayId());
            }

            // Resize display
            if (session.getUri().startsWith("/" + AppChannelProtocol.resizeDisplay)) {
                List<String> line1 = parameters.get("id");
                List<String> line2 = parameters.get("width");
                List<String> line3 = parameters.get("height");
                List<String> line4 = parameters.get("density");
                int id = 0;
                if (line1 != null) id = Integer.parseInt(line1.get(0));
                DisplayManagerRef dm = ServiceManager.getDisplayManager();
                JSONObject display = dm.getDisplayInfo(id);
                if (display == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "display not found");

                if (line2 == null && line3 == null && line4 == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "please give parameter 'width'&'height' or 'density'");
                if (line2 != null && line3 == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'width' found, but 'height' not found");
                if (line2 == null && line3 != null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'height' found, but 'width' not found");

                int width, height, density;
                if (line2 != null) {
                    width = Integer.parseInt(line2.get(0));
                    height = Integer.parseInt(line3.get(0));
                } else {
                    width = (int) display.get("width");
                    height = (int) display.get("height");
                }
                if (line4 != null) density = Integer.parseInt(line4.get(0));
                else density = (int) display.get("density");

                if (id == 0) {
                    if (line2 != null) AppChannel.execReadOutput("wm size " + width + "x" + height);
                    if (line4 != null) AppChannel.execReadOutput("wm density " + density);
                } else {
                    VirtualDisplay virtualDisplay = cache.get(id);
                    if (virtualDisplay == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "specified virtual display not found, it might not be created by this helper");
                    virtualDisplay.resize(width, height, density);
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "success resize display, id -> " + id);
            }

            // Release virtual display
            if (session.getUri().startsWith("/" + AppChannelProtocol.releaseVirtualDisplay)) {
                List<String> line = parameters.get("id");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'id' not found");
                String id = line.get(0);
                VirtualDisplay display = cache.get(Integer.parseInt(id));
                if (display == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "specified virtual display not found, it might not be created by this helper");
                display.release();
                cache.remove(Integer.parseInt(id));
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "success release display, id -> " + id);
            }

            // Open an app by package name
            if (session.getUri().startsWith("/" + AppChannelProtocol.openAppByPackage)) {
                List<String> line1 = parameters.get("package");
                List<String> line2 = parameters.get("activity");
                List<String> line3 = parameters.get("displayId");
                if (line1 == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'package' not found");
                String packageName = line1.get(0);

                String activity;
                if (line2 != null) activity = line2.get(0);
                else activity = appChannel.getAppMainActivity(packageName);

                String id = "0";
                if (line3 != null) id = line3.get(0);

                String error = appChannel.openApp(packageName, activity, Integer.parseInt(id));
                if (error != null) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", error);
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "success");
            }

            // Stop an app by package name
            if (session.getUri().startsWith("/" + AppChannelProtocol.stopAppByPackage)) {
                List<String> line = parameters.get("package");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'package' not found");
                String packageName = line.get(0);
                String cmd = "am force-stop " + packageName;
                L.d("stopActivity activity cmd: " + cmd);
                try {
                    AppChannel.execReadOutput(cmd);
                } catch (Exception e) {
                    L.e("stopActivity error", e);
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.toString());
                }
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "success");
            }

            // Get all activities of an app
            if (session.getUri().startsWith("/" + AppChannelProtocol.getAppActivity)) {
                List<String> line = parameters.get("package");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'package' not found");
                String packageName = line.get(0);
                byte[] bytes = appChannel.getAppActivities(packageName).getBytes();
                return newFixedLengthResponse(Response.Status.OK, "application/json", new ByteArrayInputStream(bytes), bytes.length);
            }

            // Get all permissions of an app
            if (session.getUri().startsWith("/" + AppChannelProtocol.getAppPermissions)) {
                List<String> line = parameters.get("package");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'package' not found");
                String packageName = line.get(0);
                byte[] bytes = appChannel.getAppPermissions(packageName).getBytes();
                return newFixedLengthResponse(Response.Status.OK, "application/json", new ByteArrayInputStream(bytes), bytes.length);
            }

            // Get all display info
            if (session.getUri().startsWith("/" + AppChannelProtocol.getDisplayInfo)) {
                DisplayManagerRef dm = ServiceManager.getDisplayManager();
                int[] displayIds = dm.getDisplayIds();
                JSONArray jsonArray = new JSONArray();
                for (int displayId : displayIds) {
                    jsonArray.put(dm.getDisplayInfo(displayId));
                }
                return newFixedLengthResponse(Response.Status.OK, "application/json", jsonArray.toString());
            }

            // Run shell command
            if (session.getUri().startsWith("/" + AppChannelProtocol.runShell)) {
                List<String> line = parameters.get("cmd");
                if (line == null) return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "parameter 'cmd' not found");
                String cmd = line.get(0);
                L.d("runShell cmd: " + cmd);
                String result = AppChannel.execReadOutput(cmd);
                return newFixedLengthResponse(Response.Status.OK, "text/plain", result);
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found");
        } catch (Exception e) {
            L.e("serve error", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.toString());
        }
    }
}
