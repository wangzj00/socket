package com.zj.socket.netty.nio.chp6;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;

/**
 * 自己写的序列化性能和jdk的自带的序列化性能对比
 * @author wangzj
 *
 */
public class PerformTestUserInfo {

	public static void main(String[] args) throws Exception {
		UserInfo info = new UserInfo();
		info.buildUserID(100).buildUserName("welcome to Netty");
		int loop = 1000000;
		ByteArrayOutputStream bos 	= null;
		ObjectOutputStream os 		= null;
		
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < loop; i++) {
			bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);
			os.writeObject(info);
			os.flush();
			os.close();
			bos.flush();
			byte[] b = bos.toByteArray();
			bos.close();
		}
		long endTime = System.currentTimeMillis();
		double jdkCostTime = endTime - startTime;
		System.out.println("The jdk serializable cost time is : " + jdkCostTime + "ms");
		
		System.out.println("----------------------------------");
		
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		startTime = System.currentTimeMillis();
		for (int i = 0; i < loop; i++) {
			byte[] b = info.codeC(buffer);
		}
		endTime = System.currentTimeMillis();
		double bufferSeriTime = endTime - startTime;
		System.out.println("The byte array serializable cost time is : " + bufferSeriTime + "ms");
		
		System.out.println("----------------------------------");
		System.out.println("两种性能比较,自/jdk = " + bufferSeriTime / jdkCostTime);
	}
}
