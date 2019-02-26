
**本文的demo地址：** https://github.com/poecook/AndroidWebScoket
## 一、原理
### 什么是WebSocket
> WebSocket协议是基于TCP的一种新的网络协议。它实现了浏览器与服务器全双工(full-duplex)通信——允许服务器主动发送信息给客户端。
WebSocket通信协议于2011年被IETF定为标准RFC 6455，并被RFC7936所补充规范。通俗的讲就是服务器和客户端可以都可以主动的向对方发送消息，而不是请求-响应的模式了。

- 上面这段话是百度百科上描述的WebSocket，WebSocket是应用层的一种协议，是建立在TCP(传输层)协议基础上的，主要特点就是全双工通信。
- websocket通信能够极大减轻网路拥塞。和传统的轮询操作保持长连接的方式相比也极大了减小了损耗。

### WebSocket VS HTTP
#### 相同点
- 都是位于应用层的传输协议。
- 都经过握手环节建立通信。
- 都是基于TPC协议基础之上。

### 不同点
- HTTP的通信过程在经历握手建立连接之后的通信都是请求响应的方式，服务端不能主动的向客户端发送消息；而WebSocket在服务端和客户端建立连接之后，服务端可以主动的向客户端发送消息。
- websocket传输的数据是二进制流，是以帧为单位的；http传输的是明文传输，是字符串传输。
- WebSockt是长连接，原则上除非主动关闭连接，在建立连接之后，双发可以互发消息；而HTTP1.0是客端户发起请求，服务端响应然后就结束了，如果客端户和服务端再有交流还要重新发起握手然后请求-响应。在HTTP1.1中有了改进（加了keep-alive），在一次连接允许多次的“请求-应答”。
- 再就是在HTTP和WebSocket的请求头部会有区别，这一点后文详解。且通信过程中通信头部比较轻。
![image](https://user-gold-cdn.xitu.io/2018/3/19/1623bef835885c18?w=1022&h=562&f=png&s=57641)

## 二、Andoid中WebSocketd的实现
 
### （一）Android 中使用java-webSocket
#### 库文件
java-websocket-1.3.8.jar [oracle官方API](http://www.oracle.com/technetwork/articles/java/jsr356-1937161.html)
#### 连接
```
 private static WebSocketClient webSocketClient;
  private void connetToServer(final String str){
       try {
           webSocketClient = new WebSocketClient(new URI("ws://192.168.25.188:8080/websocket"), new Draft_6455() {},null,100000) {
               @Override
               public void onOpen(ServerHandshake handshakedata) {
               }
               @Override
               public void onMessage(String message) {
               }
               @Override
               public void onClose(int code, String reason, boolean remote) {
               }
               @Override
               public void onClosing(int code, String reason, boolean remote) {
                   super.onClosing(code, reason, remote);
               }
               @Override
               public void onError(Exception ex) {
               }
           };
       } catch (URISyntaxException e) {
           e.printStackTrace();
       }
       webSocketClient.connect();
   }

```
- 首先创建一个WebSocketClient实例，创建实例的时候，传入地址、Websocket Draft（协议版本）、头部、连接超时时间。
- 创建实例时候，会有些回调，根据名字就能看出来，连接成功后打开状态回调open(),关闭时候调用onClosing(),出错时候调用onError()等等。
- 最后就调用WebSocket的connet()进行连接。

###### WebSocket的握手
这里需要注意的是，WwebSocket在握手连接阶段借用了http协议，握手连接阶段的报文跟http报文一样。差不多如下形式：

```
GET / HTTP/1.1
Host: ws://192.168.25.188:8080/
Connection: Upgrade
Pragma: no-cache
Cache-Control: no-cache
Upgrade: websocket
Origin: null
Sec-WebSocket-Version: 13
User-Agent: Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36
Accept-Encoding: gzip, deflate, sdch
Accept-Language: zh-CN,zh;q=0.8
Sec-WebSocket-Key: VR+OReqwhymoQ21dBtoIMQ==
Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits
```
跟http协议请求头部，主要不一样的是Connetiong:Upgrad和Upgrade: websocket以及Sec-WebSocket-Version: 13这些就告诉服务器说我要握手的完成的是哪个版本的websocket协议。
#### 发送消息
连接完成之后，可以向服务端发送消息，直接调用WebSocketClient中的发送方法。有下面两个发送消息的方法。
```
public void send( String text ) 
}

public void send( byte[] data ) 
}
```
#### 关闭连接
正常关闭 调用webSocket的close()方法。
### （二）java-websocket内部原理（源码分析）
![image](https://user-gold-cdn.xitu.io/2018/3/19/1623bef84293ac15?w=935&h=412&f=png&s=20439)

WebSocketClient和WebSocketImpl使用了代理模式。

#####  (1) 握手连接阶段

```
public void connect() {
    if( writeThread != null )
    		throw new IllegalStateException( "WebSocketClient objects are not reuseable" );
    writeThread = new Thread( this );
    writeThread.start();
	}
```
websocketclient的connet()方法，启动一个线程，这个线程里面的操作是什么呢？就查看WebSocketClient的继承在Runable实现的run()方法。

对run（）方法做了适合的化简
```
public void run() {
		InputStream istream;
	    
		socket = new Socket( proxy );
		socket.setTcpNoDelay( isTcpNoDelay() );
		socket.setReuseAddress( isReuseAddr() );

		istream = socket.getInputStream();
		ostream = socket.getOutputStream();
		sendHandshake();
		writeThread = new Thread( new WebsocketWriteThread() );
		writeThread.start();
		byte[] rawbuffer = new byte[ WebSocketImpl.RCVBUF ];
		int readBytes;

		while ( !isClosing() && !isClosed() && ( readBytes = istream.read( rawbuffer ) ) != -1 ) {
			engine.decode( ByteBuffer.wrap( rawbuffer, 0, readBytes ) );
		}
		engine.eot();
	}
```
在run方法中做了五件事：
1. 创建socket
2. 在socket中获取inputStream和outputStream两个流
3. 	sendHandshake();这个方法就是往engine中的发送队列outQueue中放入挥手信息。
4. 	创建并启动WebsocketWriteThread线程，这个线程的工作就是再outQueue中取出信息写入ostream流中。
5. 	循环读流istream，取出字节交给engingDecode处理。

发送握手之后，在流中接收到服务端的返回，就交给engine的decode()方法，decode（）方法最终会调用onWebsocketHandshakeReceivedAsClient（）方法和onOpen（）
至此完成握手连接。

##### (2)打开阶段
//NOT_YET_CONNECTED, CONNECTING, OPEN, CLOSING, CLOSED

###### a.发送消息

完成握手连接建立的通道之后，发送消息就很简单了。关键代码如下：

**webSocketClient**：

```
public void send( String text ) throws NotYetConnectedException {
		engine.send( text );
	}
```
**WebsocketImpl:**

```
private void send( Collection<Framedata> frames ) {
	ArrayList<ByteBuffer> outgoingFrames = new ArrayList<ByteBuffer>();
	for( Framedata f : frames ) {
		outgoingFrames.add( draft.createBinaryFrame( f ) );
	}
	write( outgoingFrames );
}
	
private void write( ByteBuffer buf ) {
    outQueue.put( buf );
    wsl.onWriteDemand( this );
}

```
客户端向服务器发送消息，归根到底就是engin把要发送给的字节数组或者字符串转换成ByteBuffer，然后放在outQueue的队列中。等待WebSocketClient中启动的写线程在outQueue队列读出来，写入socket的流中。

###### b.接受消息

接受消息在websocketClient的run（）方法中一直在流中读数据，交给engin的decode()处理。

websocket:
```
private void decodeFrames( ByteBuffer socketBuffer ) {
	List<Framedata> frames;

	frames = draft.translateFrame( socketBuffer );
	for( Framedata f : frames ) {
		if( DEBUG )
			System.out.println( "matched frame: " + f );
		draft.processFrame( this, f );
	}
}
```
Draft_6455:

```

public void processFrame( WebSocketImpl webSocketImpl, Framedata frame ) throws InvalidDataException {
	Framedata.Opcode curop = frame.getOpcode();
	if( curop == Framedata.Opcode.CLOSING ) {
	}else if( curop == Framedata.Opcode.PING ) {
		webSocketImpl.getWebSocketListener().onWebsocketPing( webSocketImpl, frame );
	} else if( curop == Framedata.Opcode.PONG ) {
		webSocketImpl.updateLastPong();
		webSocketImpl.getWebSocketListener().onWebsocketPong( webSocketImpl, frame );
	} else if( !frame.isFin() || curop == Framedata.Opcode.CONTINUOUS ) {	
	} else if( current_continuous_frame != null ) {
	} else if( curop == Framedata.Opcode.TEXT ) {
		webSocketImpl.getWebSocketListener().onWebsocketMessage( webSocketImpl, Charsetfunctions.stringUtf8( frame.getPayloadData() ) );	
	} else if( curop == Framedata.Opcode.BINARY ) {
			webSocketImpl.getWebSocketListener().onWebsocketMessage( webSocketImpl, frame.getPayloadData() );
	}
}
```
在Draft_draft_6455中首先检查接受的数据帧的类型，连续的帧、还是字节、还是text或或者心跳检测帧，然后做相应的处理，回调WebSocketListener相应的方法，接受到正常的String或者字节的时候，最终回调到onMessage()方法，用户就接受到信息了。

##### (3)关闭阶段
因为WebSocket是基于TCP协议的，而正常的TCP协议的断开需要经过“四次挥手”。为什么呢？TCP是双工通信的，客户端和服务端都正常的向对方推送消息，所以挥手的过程如下：

```
客户端：我要关闭了啊。
服务端：好的，我知道你要关闭了。
服务端：我要关闭了兄弟。
客户端:好的我知道你要关闭了
```
经过以上过程，整个TCP协议就关闭了。

当然也会存在单项关闭的问题。客户端关了，这时候客户端不能向服务端发送消息了，但是服务端没有关，服务端可以向客户端发送消息。相反也是。

那么再java-websocket中是如何实现关闭的呢？

在WebsocketImpl中close方法向服务端发送一个关闭帧。
```
public synchronized void close( int code, String message, boolean remote ) {
    if( isOpen() ) {
		CloseFrame closeFrame = new CloseFrame();
		closeFrame.setReason( message );
		closeFrame.setCode( code );
		closeFrame.isValid();
		sendFrame( closeFrame );
	}
	setReadyState( READYSTATE.CLOSING );
	}
```
服务端接受到关闭后会向客户端发送一个关闭确认帧，客户端接收到之后最会调用WebsocketImpl的closeConnetion()。

```
	public synchronized void closeConnection( int code, String message, boolean remote ) {
		if( getReadyState() == READYSTATE.CLOSED ) {
			return;
		}
		if( getReadyState() == READYSTATE.OPEN ) {
			if( code == CloseFrame.ABNORMAL_CLOSE ) {
				setReadyState( READYSTATE.CLOSING );
			}
		}
		this.wsl.onWebsocketClose( this, code, message, remote );
		setReadyState( READYSTATE.CLOSED );
	}
```
closeconnetion做的主要事情就是把客户端的状态设置为closed,然后回调WebSocketLisetener的onWebsocketClose（）。
至此，关闭了。

可能会有疑问如果客户端已经处于关闭状态了，如果服务端发送来关闭消息，客户端就没有办法向服端关闭确认了。这个问题的解决，是使用超时机制，也就是服务端再发送关闭信息一段时间后没有接受到确认，他就默认是客户端收到了他的关闭信息。


## 关于断线重连
需要 特别提出的是，WebSocket可能会出现不可预知的断开连接了，但是这时候客户端还不知道，客端户依旧开心的处于“Open”的状态。这个问题怎么解决呢？

开心的是java-websocket中提供了心跳检测，一旦检测到不在线了连接断了就会启动重新连接机制。具体这个心跳检测多长时间检测一次我们可以通过WebSocketClient的如下方法

```
 public void setConnectionLostTimeout( int connectionLostTimeout )
```
进行设置，系统默认的是60秒。通过判断ping和pong一旦检测到websocket断开了就会调用engine的closeConnetion（）方法。

- 具体的断重连是如何实现的呢？

当在监听close方法中监听到是ping不通的状态导致的关闭的时候，就启动一个线程，做五次连接操作，这部分的具体实现，可以在Demo中查看，再次具体不做详解了。

### （二）使用OkHttp做WebSocket连接
#### 基本使用
- 使用OkHttpClient创建WebSocket，响应的闯入监听回调
```
  private static OkHttpClient sClient;
    private static WebSocket  sWebSocket;
    public static  WebSocket startRequest(String url,WebSocketListener socketListener) {
        if (sClient == null) {
            sClient = new OkHttpClient();
        }
        if (sWebSocket == null) {
            Request request = new Request.Builder().url(url).build();
            sWebSocket = sClient.newWebSocket(request, socketListener);
        }
        return sWebSocket;
    }
```

                
- WebSocket有 连接中、打开中、关闭中、已关闭这几种状态、每种状态都会有响应的回调。如下：

```
  webSocketClient =  WebSocketClient.startRequest("ws://192.168.25.188:8080/websocket", new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
            }
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
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
```
当然可以根据需要继承WebSocketListener然后自己实现，比如把onMessage(WebSocket webSocket, String text)
和onMessage(WebSocket webSocket, ByteString bytes) 方法合并同一回调onMessage(String msg);
- 发送消息就通过webSocket.send()方法，接受消息在监听的onMessage方法中监听。

## 小结
- WebSocket的生命周期连接中，打开，关闭中，已关闭。
- 通过三次握手建立连接之后websocket处于open状态。
- 处于open状态的websocket 可以向对方收发消息。
- websocket的关闭，四次挥手，客户端向服务端挥手，服务端返回确认；服务端向客户端挥手，客户端返回确认。
- websocket通过固定周期的ping、pong保连接。

