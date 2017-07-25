package com.zj.socket.netty.nio.chp9.net;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import com.zj.socket.netty.nio.chp9.SubscribeReq;

public class SubReqClientHandler extends ChannelHandlerAdapter {

	/**
	 * Creates a client-side handler.
	 */
	public SubReqClientHandler() {

	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		for (int i = 0; i < 10; i++) {
			ctx.writeAndFlush(subReq(i));
		}
		// ctx.flush();
	}

	private SubscribeReq subReq(int i) {
		SubscribeReq req = new SubscribeReq();
		req.setAddress("NanJing YuHuaTai");
		req.setPhoneNumber("136xxxxxxxxx");
		req.setProductName("Netty Book For Marshalling");
		req.setSubReqID(i);
		req.setUserName("Lilinfeng");
		return req;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		System.out.println("Receive server response : [" + msg + "]");
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}