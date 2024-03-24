package com.eiyooooo.automobile.helper.utils;

import android.util.Log;
import android.text.format.DateFormat;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class L {
    private static PrintStream fileOut;

    public static void i(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("INFO: ");
        sb.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        Log.i("automobile_helper", message);
        flush(sb);
    }

    public static void d(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("DEBUG: ");
        sb.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        Log.d("automobile_helper", message);
        flush(sb);
    }

    public static void w(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("WARN: ");
        sb.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        if (throwable != null) {
            sb.append(": ").append(Log.getStackTraceString(throwable));
        }
        Log.w("automobile_helper", message, throwable);
        flush(sb);
    }

    public static void w(String message) {
        w(message, null);
    }

    public static void w(Throwable throwable) {
        w("", throwable);
    }

    public static void e(String message, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append("ERROR: ");
        sb.append("<").append(DateFormat.format("HH:mm:ss", new java.util.Date())).append("> ");
        sb.append(message);
        if (throwable != null) {
            sb.append(": ").append(Log.getStackTraceString(throwable));
        }
        Log.e("automobile_helper", message, throwable);
        flush(sb);
    }

    public static void e(String message) {
        e(message, null);
    }

    public static void e(Throwable throwable) {
        e("", throwable);
    }

    public static void flush(StringBuilder sb) {
        System.out.println(sb);
        System.out.flush();

        // Append to file
        try {
            if (fileOut == null) {
                fileOut = new PrintStream(new FileOutputStream("/data/local/tmp/automobile/helper_log", false));
            }
            fileOut.println(sb);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}