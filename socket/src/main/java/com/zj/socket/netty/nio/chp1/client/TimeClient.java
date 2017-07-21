package com.zj.socket.netty.nio.chp1.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * 基于Netty的NIO的客户端代码
 * @author wangzj
 *
 */
public class TimeClient {

	private static final String host = "127.0.0.1";
	private static final int port = 8080;
	
	public static void main(String[] args) throws Exception {
		new TimeClient().connect(host, port);
	}
	
	public void connect(String host, int port) throws Exception {
		//配置客户端NIO线程组
		EventLoopGroup group = new NioEventLoopGroup();
		try{
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new TimeClientHandler());
				}
			});
			
			//发起异步连接操作
			ChannelFuture f = b.connect(new InetSocketAddress(host, port)).sync();
			
			//等待客户端链路关闭
			f.channel().closeFuture().sync();
			
		}finally{
			//释放NIO线程组
			group.shutdownGracefully();
		}
	}
}

/**
 * 客户端
 * @author wangzj
 *
 */
class TimeClientHandler extends ChannelHandlerAdapter{

	private final ByteBuf firstMessage;
	public TimeClientHandler(){
		byte[] req = "QUERY TIME ORDER".getBytes();
		firstMessage = Unpooled.buffer(req.length);
		firstMessage.writeBytes(req);
	}
	/*
	 * 发生异常时该方法别调用
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.err.println("Unexpected Exception from downstream : " + cause.getMessage());
		ctx.close();
	}

	/*
	 * 当客户端服务端TCP链路建立成功后,Netty的NIO线程会调用该方法
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.writeAndFlush(firstMessage);
	}

	/*
	 * 当客户端返回应答消息时,该方法被调用
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buf = (ByteBuf)msg;
		byte[] req = new byte[buf.readableBytes()];
		buf.readBytes(req);
		String body = new String(req, "utf-8");
		System.out.println("Now is : " + body);
	}
	
}
