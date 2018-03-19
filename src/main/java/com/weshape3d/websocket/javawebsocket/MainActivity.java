package com.weshape3d.websocket.javawebsocket;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.weshape3d.websocket.R;
import com.weshape3d.websocket.okhttpwebsocket.OkhttpWebsocketActivity;

public class MainActivity extends Activity implements ServiceConnection,View.OnClickListener {
    private SocketService socketService = null;
    private EditText editText = null;
    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectAndStart();
        initView();
    }
    private void initView(){
        editText = (EditText) findViewById(R.id.et_send);
        textView = (TextView) findViewById(R.id.tv);
        findViewById(R.id.bt_send).setOnClickListener(this);//发送
        findViewById(R.id.bt_close).setOnClickListener(this);//关闭
        findViewById(R.id.bt_secend).setOnClickListener(this);//关闭
    }
    private void connectAndStart(){
        startService(new Intent(this, SocketService.class));
        bindService(new Intent(this, SocketService.class),this,0);
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        socketService = ((SocketService.MyBinder) service).getService();
        socketService.setSocketListener(new SocketListener() {
            @Override
            public void onMessageArrived(String msg) {
                textView.setText(msg);
            }
            @Override
            public void onConnetting() {
                textView.setText("连接中。。。。");
            }
            @Override
            public void onOpend() {
                textView.setText("处于打开状态。。。。");
            }
            @Override
            public void onClosed() {
                textView.setText("已经关闭。。。。");
            }
        });
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if(id == R.id.bt_send){
            if(socketService != null){
                socketService.sendMessage(editText.getText().toString()+"from Android");
            }
        } else if(id == R.id.bt_close){
            if(socketService!=null){
                socketService.closeSocket();
            }
        }else if (id == R.id.bt_secend){
            startActivity(new Intent(this,OkhttpWebsocketActivity.class));
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
         unbindService(this);
        stopService(new Intent(this,SocketService.class));
    }
}

