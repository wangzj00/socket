package com.zj.socket.netty.nio.chp7;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import org.msgpack.MessagePack;

/**
 * messagepack解码器
 * ByteBuffer 要解码的类型
 * @author wangzj
 *
 */
public class MsgpackDecoder extends MessageToMessageDecoder<ByteBuf>{

	/**
	 * 从byteBuf中获取需要解码的byte数组,然后调用messagePack的read方法将其反序列化为Object对象,将解码后的对象加入到out集合中,这样就完成了MessagePack的解码工作
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
		final byte[] array;
		final int length = msg.readableBytes();
		array = new byte[length];
		msg.getBytes(msg.readerIndex(), array, 0, length);
		
		MessagePack msgPack = new MessagePack();
		out.add(msgPack.read(array));
	}
}
