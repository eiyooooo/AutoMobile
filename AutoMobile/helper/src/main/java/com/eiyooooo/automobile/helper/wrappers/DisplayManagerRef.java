package com.eiyooooo.automobile.helper.wrappers;

import android.view.Display;
import com.eiyooooo.automobile.helper.utils.L;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.eiyooooo.automobile.helper.AppChannel.execReadOutput;

public final class DisplayManagerRef {
    private final Object manager; // instance of hidden class android.hardware.display.DisplayManagerGlobal

    public DisplayManagerRef(Object manager) {
        this.manager = manager;
    }

    public static JSONObject parseDisplayInfo(String dumpsysDisplayOutput, int displayId) throws Exception {
        Pattern regex = Pattern.compile(
                "^    mOverrideDisplayInfo=DisplayInfo\\{\"(.*)\".*?, displayId " + displayId + ".*?(, FLAG_.*)?, real ([0-9]+) x ([0-9]+).*?, "
                        + "rotation ([0-9]+).*?, density ([0-9]+).*?, layerStack ([0-9]+)",
                Pattern.MULTILINE);
        Matcher m = regex.matcher(dumpsysDisplayOutput);
        if (!m.find()) return null;
        JSONObject output = new JSONObject();
        output.put("name", m.group(1));
        output.put("id", displayId);
        output.put("width", Integer.parseInt(Objects.requireNonNull(m.group(3))));
        output.put("height", Integer.parseInt(Objects.requireNonNull(m.group(4))));
        output.put("density", Integer.parseInt(Objects.requireNonNull(m.group(6))));
        output.put("rotation", Integer.parseInt(Objects.requireNonNull(m.group(5))));
        output.put("layerStack", Integer.parseInt(Objects.requireNonNull(m.group(7))));
        output.put("flags", parseDisplayFlags(m.group(2)));
        return output;
    }

    private static JSONObject getDisplayInfoFromDumpsysDisplay(int displayId) {
        try {
            L.d("Trying to get display info from \"dumpsys display\" output");
            String dumpsysDisplayOutput = execReadOutput("dumpsys display");
            return parseDisplayInfo(dumpsysDisplayOutput, displayId);
        } catch (Exception e) {
            L.e("Could not get display info from \"dumpsys display\" output", e);
            return new JSONObject();
        }
    }

    private static int parseDisplayFlags(String text) {
        Pattern regex = Pattern.compile("FLAG_[A-Z_]+");
        if (text == null) {
            return 0;
        }

        int flags = 0;
        Matcher m = regex.matcher(text);
        while (m.find()) {
            String flagString = m.group();
            try {
                Field filed = Display.class.getDeclaredField(flagString);
                flags |= filed.getInt(null);
            } catch (ReflectiveOperationException e) {
                // Silently ignore, some flags reported by "dumpsys display" are @TestApi
            }
        }
        return flags;
    }

    public JSONObject getDisplayInfo(int displayId) {
        try {
            Object displayInfo = manager.getClass().getMethod("getDisplayInfo", int.class).invoke(manager, displayId);
            // fallback when displayInfo is null
            if (displayInfo == null) return getDisplayInfoFromDumpsysDisplay(displayId);
            Class<?> cls = displayInfo.getClass();
            // width and height already take the rotation into account
            JSONObject output = new JSONObject();
            output.put("name", cls.getDeclaredField("name").get(displayInfo));
            output.put("id", displayId);
            output.put("width", cls.getDeclaredField("logicalWidth").getInt(displayInfo));
            output.put("height", cls.getDeclaredField("logicalHeight").getInt(displayInfo));
            output.put("density", (int) cls.getDeclaredField("logicalDensityDpi").getFloat(displayInfo));
            output.put("rotation", cls.getDeclaredField("rotation").getInt(displayInfo));
            output.put("layerStack", cls.getDeclaredField("layerStack").getInt(displayInfo));
            output.put("flags", cls.getDeclaredField("flags").getInt(displayInfo));
            return output;
        } catch (Exception e) {
            L.e("Could not get display info", e);
            return new JSONObject();
        }
    }

    public int[] getDisplayIds() {
        try {
            return (int[]) manager.getClass().getMethod("getDisplayIds").invoke(manager);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}