package com.zj.socket.netty.nio.tcp粘包;

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
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;

/**
 * 基于Netty的NIO的客户端代码,模拟tcp粘包
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
					/*
					 * 通过添加LineBasedFrameDecoder和StringDecoder则两个解码器来解决TCP粘包问题
					 */
					ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
					ch.pipeline().addLast(new StringDecoder());
					
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

	private int counter;
	private byte[] req;
	
	public TimeClientHandler(){
		req = ("QUERY TIME ORDER" + System.getProperty("line.separator")).getBytes();
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
		ByteBuf message = null;
		for (int i = 0; i < 100; i++) {
			message = Unpooled.buffer(req.length);
			message.writeBytes(req);
			ctx.writeAndFlush(message);
		}
	}

	/*
	 * 当客户端返回应答消息时,该方法被调用
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		/*ByteBuf buf = (ByteBuf)msg;
		byte[] req = new byte[buf.readableBytes()];
		buf.readBytes(req);
		String body = new String(req, "utf-8");
		+ ++cSystem.out.println("Now is : " + body + " ; the counter is : " + ++counter);*/
		
		//解决TCP粘包问题的代码
		String body = (String) msg;
		System.out.println("Now is : " + body + " ; the counter is : " + ++counter);
	}
	
}
