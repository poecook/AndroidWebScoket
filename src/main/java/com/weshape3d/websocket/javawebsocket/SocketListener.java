package com.weshape3d.websocket.javawebsocket;

/**
 * Created by WESHAPE-DEV02 on 2018/3/10.
 */

public  interface SocketListener {
     void onMessageArrived(String msg);
     void onConnetting();
    void onOpend();
     void onClosed();
}
