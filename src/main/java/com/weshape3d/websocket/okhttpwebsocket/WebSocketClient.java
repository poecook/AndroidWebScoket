package com.weshape3d.websocket.okhttpwebsocket;

/**
 * Created by WESHAPE-DEV02 on 2018/3/13.
 */


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
public class WebSocketClient {
    private static OkHttpClient sClient;
    private static WebSocket  sWebSocket;
    public static  WebSocket startRequest(String url,WebSocketListener socketListener) {
        if (sClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.retryOnConnectionFailure(true);
            sClient = builder.build();
        }
        if (sWebSocket == null) {
            Request request = new Request.Builder().url(url).build();
            sWebSocket = sClient.newWebSocket(request, socketListener);
        }
        return sWebSocket;
    }
}