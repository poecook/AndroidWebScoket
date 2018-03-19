package com.weshape3d.websocket.okhttpwebsocket;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.weshape3d.websocket.R;


import org.java_websocket.framing.CloseFrame;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class OkhttpWebsocketActivity extends AppCompatActivity implements View.OnClickListener {
    private WebSocket webSocketClient;
    private EditText et_send;
    private Button bt_send;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ohttp_websocket);
        initView();
        webSocketClient =  WebSocketClient.startRequest("ws://192.168.1.18:8080/websocket", new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);

            }
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                Log.d("drummor","接收到了来自服务端的消息："+text);
            }
            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
            }
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
            }
            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
            }
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
            }
        });

    }

    private void initView(){
        et_send = (EditText) findViewById(R.id.et_send);
        bt_send = (Button) findViewById(R.id.bt_send);
        bt_send.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        webSocketClient.send(et_send.getText().toString()+"");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(webSocketClient!=null){
            webSocketClient.close(CloseFrame.NORMAL,"");
            webSocketClient = null;
        }

    }
}
