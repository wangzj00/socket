package com.nfbank.socket.nio.server;

import java.io.IOException;
import java.net.InetAddress;
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

	static class MultiplexerTimerServer implements Runnable{

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
				servChannel.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), port), 1024);
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
					SocketChannel sc = ssc.accept();
					sc.configureBlocking(false);
					sc.register(selector, SelectionKey.OP_READ);
				}
				if(key.isReadable()){
					SocketChannel sc = (SocketChannel)key.channel();
					ByteBuffer readBuf = ByteBuffer.allocate(1024);
					int readBytes = sc.read(readBuf);
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
		 * 处理些请求
		 * @param sc
		 * @param response
		 */
		private void doWrite(SocketChannel sc, String response) throws IOException {
			if(response != null && response.trim().length() > 0){
				byte[] bytes = response.getBytes();
				ByteBuffer writeBuf = ByteBuffer.allocate(bytes.length);
				writeBuf.put(bytes);
				writeBuf.flip();
				sc.write(writeBuf);
			}
		}
		
	}
	
}














