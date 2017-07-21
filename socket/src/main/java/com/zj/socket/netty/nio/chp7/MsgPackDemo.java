package com.zj.socket.netty.nio.chp7;

import java.util.ArrayList;
import java.util.List;

import org.msgpack.MessagePack;
import org.msgpack.template.Templates;

/**
 * MessagePack编解码框架示例
 * @author wangzj
 *
 */
public class MsgPackDemo {

	public static void main(String[] args) throws Exception {
		List<String> src = new ArrayList<String>(){
			{
				add("abc");
				add("def");
				add("ghi");
				add("jkl");
			}
		};
		
		MessagePack msgPack = new MessagePack();
		byte[] dst = msgPack.write(src);
		
		List<String> read = msgPack.read(dst, Templates.tList(Templates.TString));
		
		read.forEach(a->{System.out.println(a);});
	}
}
