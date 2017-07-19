package com.nfbank.socket.bio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
/**
 * socket 通信(同步阻塞IO中的伪异步IO(通过线程池和队列实现,后端通过一个线程池处理多个客户端的请求接入,通过线程池可以灵活的调配线程资源,防止海量并发接入而导致线程被耗尽))
 * @author wangzj
 */
public class SocketServer {

	private static int count = 0;
	private static ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(10);
	
	private static ThreadPoolExecutor pool = new ThreadPoolExecutor(4, 10, 300, TimeUnit.SECONDS, workQueue, new MyThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
	
	public static void main(String[] args)  throws Exception{
		ServerSocket serverSocket = null;
		try{
			serverSocket = new ServerSocket();
		
			serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), 8888));
			while (true) {
				Socket socket = null;
				try {
					//独立的acceptor线程来负责监听客户端的连接,监听到客户端的请求后会为每个客户端创建一个新的线程进行链路处理
					//一请求一线程,一处理,一销毁,大并发情况下线程过多,不能支持很好的并发
					socket = serverSocket.accept();	
					++count;
					System.out.println(count + "th request begin!");
					pool.submit(new ServerTask(socket));
					//new Thread(new ServerTask(socket)).start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}finally{
			if(serverSocket != null){
				serverSocket.close();
			}
		}
		
	}
	
	static class ServerTask implements Runnable{

		private Socket socket;
		public ServerTask(Socket socket){
			this.socket = socket;
		}
		@Override
		public void run() {
			try {
				InputStream inputStream = socket.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				StringBuilder sb = new StringBuilder();
				
				while (true) {
					String msg = reader.readLine();
					if(msg == null || "".equals(msg)) break;
					sb.append(msg);
				}
				System.out.println("server receive message : " + sb.toString());
				
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				writer.println("server--hello");
				writer.flush();
				socket.shutdownOutput();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(socket != null){
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		
	}
	
	static class MyThreadFactory implements ThreadFactory{
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r);
		}
		
	}
}
