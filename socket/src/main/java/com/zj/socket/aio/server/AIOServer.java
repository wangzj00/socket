package com.zj.socket.aio.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * AIO server 服务端
 * @author wangzj
 *
 */
public class AIOServer {

	private static final int port = 8080;
	public static void main(String[] args) {
		new Thread(new AsyncTimeServerHandler(port), "AIO-AsyncTimeServerHandler-001").start();
	}
}

/**
 * 异步io的处理类
 * @author wangzj
 *
 */
class AsyncTimeServerHandler implements Runnable{

	CountDownLatch latch;
	AsynchronousServerSocketChannel asynchronousServerSocketChannel;
	
	public AsyncTimeServerHandler(int port){
		try {
			asynchronousServerSocketChannel = AsynchronousServerSocketChannel.open();
			asynchronousServerSocketChannel.bind(new InetSocketAddress(port));
			System.out.println("The time server is start in port : " + port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		latch = new CountDownLatch(1);
		doAccept();
		try{
			latch.await();
		}catch (InterruptedException e){
			e.printStackTrace();
		}
		
	}
	
	public void doAccept(){
		asynchronousServerSocketChannel.accept(this, new AcceptCompletionHandler());
	}
	
}
/**
 * 监听连接到达时候的处理类,用于接收accept操作成功之后的通知消息
 * @author wangzj
 *
 */
class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler>{

	@Override
	public void completed(AsynchronousSocketChannel channel, AsyncTimeServerHandler attachment) {
		//此处调用accept方法时允许监听到本地连接接入后还允许继续监听其他的客户端进行接入,还会回调本方法,会一直循环下去;即每当一个客户端接入后再异步接收新的客户端连接
		attachment.asynchronousServerSocketChannel.accept(attachment, this);
		
		//接收消息
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		/*
		 * param1:接收缓冲区用于从异步channel中读取数据包
		 * param2:异步channel携带的附件,通知CompletionHandler回调的时候作为入参(附件)使用
		 * param3:接收回调的业务Handler
		 */
		channel.read(buffer, buffer, new ReadCompletionHandler(channel));
	}

	@Override
	public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
		exc.printStackTrace();
		attachment.latch.countDown();
	}

}
/**
 * 读完成之后的回调
 * @author wangzj
 *
 */
class ReadCompletionHandler implements CompletionHandler<Integer, ByteBuffer>{
	private AsynchronousSocketChannel channel;
	public ReadCompletionHandler(AsynchronousSocketChannel channel){
		if(this.channel == null){
			this.channel = channel;
		}
	}
	
	@Override
	public void completed(Integer result, ByteBuffer buffer) {
		buffer.flip();
		byte[] body = new byte[buffer.remaining()];
		buffer.get(body);
		
		String req;
		try {
			req = new String(body, "utf-8");
		
			System.out.println("The time server receive order: " + req);
			String currentTime = "QUERY TIME ORDER".equals(req) ? new Date().toString() : "BAD ORDER";
			doWrite(currentTime);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void failed(Throwable exc, ByteBuffer attachment) {
		try {
			channel.close();
		} catch (IOException e) {
			;
		}
		
	}
	
	private void doWrite(String response){
		if(response != null && response.trim().length() > 0){
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuf = ByteBuffer.allocate(response.length());
			writeBuf.put(bytes);
			writeBuf.flip();
			channel.write(writeBuf, writeBuf, new CompletionHandler<Integer, ByteBuffer>() {

				@Override
				public void completed(Integer result, ByteBuffer buffer) {
					//如果没有发送完成,继续发送,如果还有剩余的字节可以写,那么继续写,知道写完该字节序列(发送成功)
					if(buffer.hasRemaining()){
						channel.write(buffer, buffer, this);
					}
				}

				@Override
				public void failed(Throwable exc, ByteBuffer attachment) {
					try {
						channel.close();
					} catch (IOException e) {
						;
					}
				}
			});
		}
	}
	
}
