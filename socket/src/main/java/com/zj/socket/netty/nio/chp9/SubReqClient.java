package com.zj.socket.netty.nio.chp9;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 基于JBoss Marshalling编解码框架的Netty客户端开发
 * @author wangzj
 *
 */
public class SubReqClient {

	public static void main(String[] args) throws Exception {
		new SubReqClient().connect("127.0.0.1", 8080);
	}
	
	public void connect(String host, int port) throws Exception {
		NioEventLoopGroup group = new NioEventLoopGroup();
		try{
			Bootstrap b = new Bootstrap();
			b.group(group)
			.channel(NioSocketChannel.class)
			.option(ChannelOption.TCP_NODELAY, true)
			.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingDecoder());
						ch.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingEncoder());
						ch.pipeline().addLast(new SubReqClientHandler());
				}
			});
			
			ChannelFuture f = b.connect(host, port).sync();
			
			f.channel().closeFuture().sync();
		}finally{
			group.shutdownGracefully();
		}
	}
}


class SubReqClientHandler extends ChannelHandlerAdapter{
	
	public SubReqClientHandler(){}
	
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();	//发生异常,关闭链路
	}



	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		for (int i = 0; i < 10; i++) {
			ctx.writeAndFlush(subReq(i));
		}
		//ctx.flush();
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



	private SubscribeReq subReq(int i){
		SubscribeReq subscribeReq = new SubscribeReq();
		subscribeReq.setSubReqID(i);
		subscribeReq.setUserName("zhangsan");
		subscribeReq.setPhoneNumber("138xxxxxxxx");
		subscribeReq.setProductName("Netty book for Marshalling");
		subscribeReq.setAddress("Nanjing YuHuaTai");
		
		return subscribeReq;
	}
	
	
}