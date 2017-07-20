package com.nfbank.socket.nio.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO Client
 * @author wangzj
 */
public class NIOClient {

	private static final int port = 8080;
	
	public static void main(String[] args) {
		new Thread(new TimeClientHandle("127.0.0.1", port), "TimeClient-001").start();
	}
	
	
}

/**
 * NIO客户端处理类
 * @author wangzj
 *
 */
class TimeClientHandle implements Runnable{
	private String host;
	private int port; 
	private Selector selector;
	private SocketChannel socketChannel;
	private volatile boolean stop;
	
	public TimeClientHandle(String host, int port){
		this.host = host == null || "".equals(host) ? "127.0.0.1" : host;
		this.port = port;
		try {
			this.selector = Selector.open();
			this.socketChannel = SocketChannel.open();
			this.socketChannel.configureBlocking(false);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Override
	public void run() {
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(!stop){
			try {
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				while(it.hasNext()){
					SelectionKey key = it.next();
					it.remove();
					try{
						handleInput(key);
					}catch(Exception e){
						if(key != null){
							key.cancel();
							if(key.channel() != null){
								key.channel().close();
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		//多路复用器关闭后所有注册在上面的Channel,Pipe等资源都会被自动注册并关闭,所以并不需要重复去释放资源,jdk底层会自动释放所有跟该多路复用器关联的资源
		if(selector != null){
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void doConnect() throws IOException {
		if(socketChannel.connect(new InetSocketAddress(host, port))){
			socketChannel.register(selector, SelectionKey.OP_WRITE);
		}else{
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}
	}
	
	private void doWrite(SocketChannel sc) throws IOException{
		byte[] req = "QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuf = ByteBuffer.allocate(req.length);
		writeBuf.put(req);
		writeBuf.flip();
		sc.write(writeBuf);
		if(!writeBuf.hasRemaining()){
			System.out.println("Send order 2 server succeed.");
		}
	}
	
	private void handleInput(SelectionKey key) throws IOException {
		if(key.isValid()){
			SocketChannel sc = (SocketChannel)key.channel();
			if(key.isConnectable()){
				if (sc.finishConnect()) {
					sc.register(selector, SelectionKey.OP_WRITE);
				}else {
					System.exit(1);
				}
			}
			
			if (key.isWritable()) {
				doWrite(sc);
				sc.register(selector, SelectionKey.OP_READ);
			}
			if (key.isReadable()) {
				ByteBuffer readBuf = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuf);
				if (readBytes > 0) {
					readBuf.flip();
					byte[] bytes = new byte[readBuf.remaining()];
					readBuf.get(bytes);
					String body = new String(bytes, "utf-8");
					System.out.println("Now is : " + body);
					this.stop = true;

				} else if (readBytes < 0) {
					key.cancel();
					sc.close();
				} else {
					;
				}
			}
		}
	}
	
}