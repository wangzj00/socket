package com.zj.socket.netty.nio.chp6;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
/**
 * 测试自己编写的序列化码流大小和jdk自带的序列化码流大小
 * @author wangzj
 *
 */
public class UserInfoTest {

	public static void main(String[] args) throws IOException {
		UserInfo info = new UserInfo();
		info.buildUserID(100).buildUserName("welcome to Netty");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		ObjectOutputStream os = new ObjectOutputStream(bos);
		os.writeObject(info);
		os.flush();
		os.close();
		byte[] b = bos.toByteArray();
		System.out.println("The JDK Serializable length is : " + b.length);
		bos.close();
		
		System.out.println("-------------------------------");
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		System.out.println("The byte array serializable length is : " + info.codeC(buffer).length);
		
		System.out.println("长度比例: " + b.length / info.codeC(buffer).length);
	}
}
