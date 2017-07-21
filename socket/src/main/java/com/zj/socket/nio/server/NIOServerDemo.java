package com.zj.socket.nio.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * NIO 编程步骤
 * 
 * @author wangzj
 */
public class NIOServerDemo {

	private static final int port = 8888;

	public static void main(String[] args) throws IOException {
		
		// 1.打开ServerSocketChannel 监听客户端的连接,是多有客户端连接的父管道
		ServerSocketChannel acceptorSvr = ServerSocketChannel.open();

		// 2.绑定监听端口,设置连接为非阻塞
		acceptorSvr.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), port));
		acceptorSvr.configureBlocking(false);
		
		// 3.创建Reactor线程,创建多路复用器并启动线程
		Selector selector = Selector.open();
		//new Thread()
		
		// 4.将ServerSocketChannel注册到Reactor线程的多路复用器Selector上,监听accept事件
		acceptorSvr.register(selector, SelectionKey.OP_ACCEPT);
		
		SocketChannel channel = null;
		ByteBuffer receivedBuf = ByteBuffer.allocate(2048);
		while (true) {
			// 5.多路复用器在run方法的无线循环中
			int num = selector.select();
			if (num != 0) {
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					//deal with I/O event...
					
					if(key.isAcceptable()){
						// 6. 多路复用器Selector监听到有新的客户端接入,处理接入请求,完成TCP三次握手,建立物理链路
						channel = acceptorSvr.accept();
						
						// 7.设置客户端链路为非阻塞
						channel.configureBlocking(false);
						channel.socket().setReuseAddress(true);
						
						// 8.将新接入的客户端连接注册到Reactor线程的多路复用器上,监听读操作,读取客户端发送的网络消息
						channel.register(selector, SelectionKey.OP_READ);
						
					}
					
					if(key.isReadable()){
						
						// 9.异步读取客户端请求消息到缓冲区
						int readNumber = channel.read(receivedBuf);
						
						// 10.对ByteBuffer消息进行编码,如果有半包消息指针reset,继续读取后续的报文,将解码成功的消息封装成Task投递到业务线程池当中,进行业务逻辑的编排
						Object message = null;
						while(receivedBuf.hasRemaining()){
							receivedBuf.mark();
							//message = decode(receivedBuf);
							if(message == null){
								receivedBuf.reset();
								break;
							}
							//messageList.add(message);
						}
						if(!receivedBuf.hasRemaining()){
							receivedBuf.clear();
						}else{
							receivedBuf.compact();
						}
						//handle task..
					}
				}
			}
		}
	}
}
