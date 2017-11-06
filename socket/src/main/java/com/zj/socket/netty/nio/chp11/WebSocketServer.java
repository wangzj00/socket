package com.zj.socket.netty.nio.chp11;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * webSocket的服务端
 * @author wangzhijiang1
 *
 */
public class WebSocketServer {

	public static void main(String[] args) throws Exception {
		int port = 8080;
		new WebSocketServer().run(port);
	}
	
	public void run(int port) throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workGroup = new NioEventLoopGroup();
		
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					//将http请求或者应答消息编码或者解码
					pipeline.addLast("http-codec", new HttpServerCodec());
					//将http多个消息组合成一条完成的消息
					pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
					//向客户端发送h5文件,主要用于websocket通信
					pipeline.addLast("http-chunked", new ChunkedWriteHandler());
					//websocket服务端handler
					pipeline.addLast("handler", new WebSocketServerHandler1());
					
				}
			});
			
			Channel ch = b.bind(port).sync().channel();
			System.out.println("WebSocket server started at port : " + port);
			System.out.println("open your browser an navigate to http://localhost:" + port + "/");
			ch.closeFuture().sync();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workGroup.shutdownGracefully();
		}
		
	}
}
