package com.eiyooooo.automobile.helper.utils;

import com.eiyooooo.automobile.helper.AppServer;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;

public class ServerUtil {
    // range of ports to try
    static final int RANGE_START = 14000;
    static final int RANGE_END = 14040;

    public static AppServer safeGetServer() {
        for (int i = RANGE_START; i < RANGE_END; i++) {
            AppServer server = new AppServer("127.0.0.1", i);
            try {
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
                return server;
            } catch (IOException e) {
                L.i("Port " + i + " is in use");
            }
        }
        L.e("No available ports");
        return null;
    }
}