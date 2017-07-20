package com.nfbank.socket.aio.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;
/**
 * 异步AIO客户端编码
 * @author wangzj
 *
 */
public class AIOClient {

	private static final int port = 8080;
	
	public static void main(String[] args) {
		new Thread(new AsyncTimeHandlerClient("127.0.0.1", port), "AIO-AsyncTimeHandlerClient-001").start();
	}
}

class AsyncTimeHandlerClient implements CompletionHandler<Void, AsyncTimeHandlerClient>, Runnable{

	private AsynchronousSocketChannel client;
	private String host;
	private int port;
	private CountDownLatch latch;
	
	public AsyncTimeHandlerClient(String host, int port){
		this.host = host;
		this.port = port;
		try {
			client = AsynchronousSocketChannel.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		latch = new CountDownLatch(1);
		
		/*
		 * A attachment 用于回调时作为入参被传递
		 * CompletionHandler<Void,? super A> handler 异步操作回调通知接口,由调用者实现
		 */
		client.connect(new InetSocketAddress(host, port), this, this);
		
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void completed(Void result, AsyncTimeHandlerClient attachment) {
		byte[] req = "QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuf = ByteBuffer.allocate(req.length);
		writeBuf.put(req);
		writeBuf.flip();
		client.write(writeBuf, writeBuf, new CompletionHandler<Integer, ByteBuffer>() {

			@Override
			public void completed(Integer result, ByteBuffer attachment) {
				if(attachment.hasRemaining()){
					client.write(writeBuf, writeBuf, this);
				} else {
					ByteBuffer readBuf = ByteBuffer.allocate(1024);
					client.read(readBuf, readBuf, new CompletionHandler<Integer, ByteBuffer>() {

						@Override
						public void completed(Integer result, ByteBuffer buffer) {
							buffer.flip();
							byte[] bytes = new byte[buffer.remaining()];
							buffer.get(bytes);
							String body;
							try {
								body = new String(bytes, "utf-8");
								System.out.println("Now is : " + body);
								latch.countDown();
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}

						@Override
						public void failed(Throwable exc, ByteBuffer buffer) {
							try {
								client.close();
								latch.countDown();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				}
				
			}

			@Override
			public void failed(Throwable exc, ByteBuffer attachment) {
				try {
					client.close();
					latch.countDown();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	@Override
	public void failed(Throwable exc, AsyncTimeHandlerClient attachment) {
		try {
			client.close();
			latch.countDown();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}