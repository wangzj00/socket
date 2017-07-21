package com.zj.socket.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO服务端
 * @author wangzj
 *
 */
public class NIOServer {
	
	private static final int port = 8080;
	
	public static void main(String[] args) {
		MultiplexerTimerServer timerServer = new MultiplexerTimerServer(port);
		new Thread(timerServer, "NIO-MultiplexerTimerServer-001").start();
	}
}



/**
 * NIO线程服务类
 * @author wangzj
 *
 */
class MultiplexerTimerServer implements Runnable{

	private Selector selector;
	
	private ServerSocketChannel servChannel;
	
	private volatile boolean stop;
	
	/**
	 * 初始化多路复用器绑定监听端口
	 */
	
	public MultiplexerTimerServer(int port){
		try {
			selector = Selector.open();
			servChannel = ServerSocketChannel.open();
			servChannel.configureBlocking(false);
			servChannel.socket().bind(new InetSocketAddress("127.0.0.1", port), 1024);
			servChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("The time server is start in port : " + port);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * 停止方法
	 */
	public void stop(){
		this.stop = true;
	}
	@Override
	public void run() {
		while(!stop){
			try {
				selector.select(1000);
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				SelectionKey key = null;
				while(it.hasNext()){
					key = it.next();
					it.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if(key != null){
							key.cancel();
							if(key.channel() != null){
								key.channel().close();
							}
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//多路复用器关闭后,所有注册在上面的channel和Pipe等资源都会被自动去注册并且关闭,所以不需要重复释放资源
		if(selector != null){
			try {
				selector.close();
			} catch (Exception e) {
				;
			}
		}
	}
	
	/**
	 * 处理服务端的监听请求和读请求
	 * @param key
	 */
	private void handleInput(SelectionKey key) throws IOException {
		if(key.isValid()){
			//处理接入的请求消息
			if (key.isAcceptable()) {
				ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				SocketChannel sc = ssc.accept();	//完成了这个请求即相当于完成了TCP的三次握手,TCP物理链路正式建立,可以设置TCP参数
				sc.configureBlocking(false);
				sc.register(selector, SelectionKey.OP_READ);
			}
			if(key.isReadable()){
				/**
				 * 读取客户消息
				 */
				SocketChannel sc = (SocketChannel)key.channel();
				//创建缓冲区
				ByteBuffer readBuf = ByteBuffer.allocate(1024);
				int readBytes = sc.read(readBuf);	//已经将SocketChannel设置为异步非阻塞的,所以read方法是非阻塞的,如果返回-1,那么链路已经关闭,需要关闭SockeChannel释放资源
				if(readBytes > 0){
					readBuf.flip();
					byte[] bytes = new byte[readBuf.remaining()];
					readBuf.get(bytes);
					String body = new String(bytes, "utf-8");
					System.out.println("The time server receive order : " + body);
					String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date().toString() : "BAD ORDER";
					
					doWrite(sc, currentTime);
				}else if (readBytes < 0){
					//对链路关闭
					key.cancel();
					sc.close();
					
				}else{
					;	//读到0字节,忽略
				}
				
			}
			
		}
	}
	
	/**
	 * 异步将消息应答给客户端
	 * @param sc
	 * @param response
	 */
	private void doWrite(SocketChannel sc, String response) throws IOException {
		if(response != null && response.trim().length() > 0){
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuf = ByteBuffer.allocate(bytes.length);
			writeBuf.put(bytes);
			writeBuf.flip();
			/*
			 * 将缓冲区的字节数组发送出去,由于socketChannel是异步非阻塞的,他并不保证一次性的把需要发送的字节数发送完,故可能会出现写半包的问题,我们需要注册写操作,不断的轮训Selector将没有发送完的ByteBuffer发送
			 * 完毕,然后可以通过ByteBuffer的hasRemain()方法判断消息是否发送完成.后续会演示写半包的场景
			 */
			sc.write(writeBuf);
			
		}
	}
	
}










