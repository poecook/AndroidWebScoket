package com.weshape3d.websocket.javawebsocket;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;


public class SocketService extends Service {

    private static WebSocketClient webSocketClient;
    private ServiceHandler serviceHandler = new ServiceHandler();
    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return super.bindService(service, conn, flags);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connetToServer(null);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }
   class MyBinder extends Binder{
       public SocketService getService(){
           return SocketService.this;
       }
   }

    class ServiceHandler  extends Handler {
       @Override
       public void handleMessage(Message msg) {
           if(socketListener!=null){
               socketListener.onMessageArrived((String)(msg.obj));
           }
       }
     }
    /**
     * 连接
     */
    int max = 5;
    int count = 0;
   private void connetToServer(final String str){
       try {
           webSocketClient = new WebSocketClient(new URI("ws://192.168.1.18:8080/websocket"), new Draft_6455() {},null,5000) {
               @Override
               public void onWebsocketHandshakeSentAsClient(WebSocket conn, ClientHandshake request) throws InvalidDataException {super.onWebsocketHandshakeSentAsClient(conn, request);
                Log.d("drummor","发送握手了");

               }

               @Override
               public void onWebsocketHandshakeReceivedAsClient(WebSocket conn, ClientHandshake request, ServerHandshake response) throws InvalidDataException {
                   super.onWebsocketHandshakeReceivedAsClient(conn, request, response);
                   Log.d("drummor","接受到握手了");
               }
               @Override
               public void onOpen(ServerHandshake handshakedata) {

               }
               @Override
               public void onMessage(String message) {

                   Message msg = Message.obtain();
                   msg.obj = message;
                   serviceHandler.sendMessage(msg);
               }
               @Override
               public void onClose(int code, String reason, boolean remote) {
                   Log.d("drummor","已经关闭"+code);
                   if( CloseFrame.ABNORMAL_CLOSE == code)
                       reconnet();
               }

               @Override
               public void onClosing(int code, String reason, boolean remote) {
                   Log.d("drummor","关闭中");
                   super.onClosing(code, reason, remote);
               }

               @Override
               public void onError(Exception ex) {
                   Log.d("drummor","连接失败");
               }
           };
           //设置间隔检查
           webSocketClient.setConnectionLostTimeout(5000);
       } catch (URISyntaxException e) {

           e.printStackTrace();
       }
       webSocketClient.connect();
   }

    /**
     * 关闭
     */
   public void closeSocket(){
       if(webSocketClient!=null){
           webSocketClient.close();
       }
   }

    /**
     * 发送消息
     * @param message
     */
    public void sendMessage(String message){

        if(webSocketClient!=null&&webSocketClient.isOpen()){
            Log.d("drummor","进来的觉得可以发送的:"+message);
            webSocketClient.send(message);
        }else {
            Log.d("drummor","已经断线,需要重新。。。");
            reconnet();
        }
    }

    private void reconnet(){
        if(webSocketClient!=null){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (webSocketClient.isClosed()&&!webSocketClient.reconnectBlocking()&&max>count){
                            Log.d("drummor","第"+count+"次重连");
                            count++;
                        }
                        count = 0;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
    private SocketListener socketListener = null;
    public void setSocketListener(SocketListener socketListener) {
        this.socketListener = socketListener;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(webSocketClient!=null&&webSocketClient.isOpen()){
            webSocketClient.close();
            webSocketClient = null;
        }
    }

}
