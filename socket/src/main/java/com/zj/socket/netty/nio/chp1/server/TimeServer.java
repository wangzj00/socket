package com.zj.socket.netty.nio.chp1.server;


import java.util.Date;

import io.netty.bootstrap.ServerBootstrap;
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
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 基于Netty的NIO服务端代码
 * @author wangzj
 *
 */
public class TimeServer {

	private static final int port = 8080;
	
	public static void main(String[] args) throws Exception {
		new TimeServer().bind(port);
	}

	public void bind(int port) throws Exception {
		// 配置服务端的nio线程组
		EventLoopGroup bossGroup = new NioEventLoopGroup();	//Reactor线程组,用户服务端接收客户端连接
		EventLoopGroup workerGroup = new NioEventLoopGroup();	//用于进行socketChannel的网络读写
		try {

			//用于启动netty服务端的辅助启动类,目的是降低服务端的开发难度
			ServerBootstrap b = new ServerBootstrap();	
			b.group(bossGroup, workerGroup)
					//对应于jdk中的ServerSocketChannel
					.channel(NioServerSocketChannel.class)	
					.option(ChannelOption.SO_BACKLOG, 1024)
					//绑定io事件的处理类
					.childHandler(new ChildChannelHandler());	

			// 绑定端口,同步等待成功,sync方法等待綁定操作完成,返回值类似于jdk中的Future,用于异步操作的通知回调
			ChannelFuture f = b.bind(port).sync();

			// 等待服务端监听端口关闭,等待服务端的链路关闭之后main函数才退出
			f.channel().closeFuture().sync();
		} finally {
			// 优雅退出,释放线程池资源
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	/**
	 * io事件的处理类
	 * 
	 * @author wangzj
	 *
	 */
	private class ChildChannelHandler extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ch.pipeline().addLast(new TimeServerHandler());

		}

	}
}

/**
 * netty服务器的服务端
 * 
 * @author wangzj
 */
class TimeServerHandler extends ChannelHandlerAdapter {

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		/*
		 * 发生异常时关闭ChannelHandlerContext释放相关联的句柄资源
		 */
		ctx.close();
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		ByteBuf buf = (ByteBuf)msg;//类似于jdk中的ByteBuffer对象
		byte[] req = new byte[buf.readableBytes()];	//获得缓冲区的可读字节数
		buf.readBytes(req);
		String body = new String(req, "utf-8");
		System.out.println("The time server receive order : " + body);
		String currentTime = "QUERY TIME ORDER".equals(body) ? new Date().toString() : "BAD ORDER";
		ByteBuf resp = Unpooled.copiedBuffer(currentTime.getBytes());
		ctx.write(resp);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		/*
		 * 将消息发送队列中的消息写入到SocketChannel中发送给对方,为了防止频繁的唤醒Selector进行消息发送,Netty的write方法并不是直接将消息写入SocketChannel,只是写到发送缓冲数组中.flush方法
		 * 才将消息全部写入到socketChannel 中
		 */
		ctx.flush();
	}
	
}