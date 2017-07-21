package com.zj.socket.netty.nio.chp7;

import org.msgpack.MessagePack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * messagePack编码器开发
 * 负责将Object类型的msg消息转成byte数组然后写入到out中
 * @author wangzj
 *
 */
public class MsgpackEncoder extends MessageToByteEncoder<Object> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out)
			throws Exception {
		/*MessagePack messagePack = new MessagePack();
		byte[] raw = messagePack .write(msg);
		out.writeBytes(raw);*/
		MessagePack msgPack = new MessagePack();
		System.out.println(msg == null);
		byte[] raw = null;
		raw = msgPack.write(msg);
		out.writeBytes(raw);
	}
	
}
