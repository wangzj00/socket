package com.zj.socket.bio.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * socket通信客户端
 * @author wangzj
 */
public class SocketClient {

	public static void main(String[] args) throws Exception {
		while(true){
			Thread.sleep(500);
			new Thread(new Task()).start();
		}
	}
	
	static class Task implements Runnable{
		@Override
		public void run() {
			Socket socket = null;
			OutputStream outputStream = null;
			PrintWriter writer = null;
			try {
				socket = new Socket(InetAddress.getLocalHost(), 8888);
				outputStream = socket.getOutputStream();
				writer = new PrintWriter(new OutputStreamWriter(outputStream));
				writer.println("client--hello ,thread Name= " + Thread.currentThread().getName());
				writer.flush();
				socket.shutdownOutput();
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String msg = null;
				while ((msg = reader.readLine()) != null && !"".equals(msg)) {
					sb.append(msg);
				}
				System.out.println("client receive message : " + sb.toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if(outputStream != null){
					try {
						outputStream.close();
					} catch (IOException e) {}
				}
				if(writer != null){
					writer.close();
				}
				try {
					if(socket != null){
						socket.close();
					}
				} catch (IOException e) {
					System.out.println("client--socket close Exception");
					e.printStackTrace();
				}
			}
			
		}
		
	}
}
