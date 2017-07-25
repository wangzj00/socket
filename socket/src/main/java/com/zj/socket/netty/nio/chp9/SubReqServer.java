package com.zj.socket.netty.nio.chp9;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.marshalling.DefaultMarshallerProvider;
import io.netty.handler.codec.marshalling.DefaultUnmarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallerProvider;
import io.netty.handler.codec.marshalling.MarshallingDecoder;
import io.netty.handler.codec.marshalling.MarshallingEncoder;
import io.netty.handler.codec.marshalling.UnmarshallerProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;

/**
 * 采用JBoss Marshalling框架进行Netty服务端的开发
 * @author wangzj
 *
 */
public class SubReqServer {

	public static void main(String[] args) throws Exception {
		new SubReqServer().bind(8080);
	}
	
	
	public void bind(int port) throws Exception {
		
		NioEventLoopGroup bossGroup = new NioEventLoopGroup();
		NioEventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try{
			
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.option(ChannelOption.SO_BACKLOG, 100)
			.handler(new LoggingHandler(LogLevel.WARN))
			.childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					/*
					 * Netty的JBoss Marshalling编解码器自动支持半包消息处理
					 */
					ch.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingDecoder());
					ch.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingEncoder());
					ch.pipeline().addLast(new SubReqServerHandler());
					
				}
			});
			
			ChannelFuture f = b.bind(port).sync();
			
			f.channel().closeFuture().sync();
			
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}

/**
 * 自定义 JBoss Marshalling 编解码器工厂类
 * @author wangzj
 *
 */
final class MarshallingCodeCFactory {
	/**
	 * 创建JBoss Marshalling 解码器 MarshallingDecoder
	 * @return
	 */
	public static MarshallingDecoder buildMarshallingDecoder(){
		final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
		final MarshallingConfiguration configuration = new MarshallingConfiguration();
		configuration.setVersion(5);
		UnmarshallerProvider provider = new DefaultUnmarshallerProvider(marshallerFactory, configuration);
		/*
		 * maxObjectSize the maximum byte length of the serialized object.
		 * 单个消息序列化后的最大长度
		 * if the length of the received object is greater than this value,
		 * TooLongFrameException will be raised.
		 */
		
		MarshallingDecoder marshallingDecoder = new MarshallingDecoder(provider, 1024);
		return marshallingDecoder;
	}
	
	/**
	 * 创建JBoss Marshalling 编码器MarshallingEncoder,用于将实现序列化接口的对象转化成二进制数组
	 * @return
	 */
	public static MarshallingEncoder buildMarshallingEncoder(){
		final MarshallerFactory marshallerFactory = Marshalling.getProvidedMarshallerFactory("serial");
		final MarshallingConfiguration configuration = new MarshallingConfiguration();
		configuration.setVersion(5);
		MarshallerProvider provider = new DefaultMarshallerProvider(marshallerFactory, configuration);
		MarshallingEncoder encoder = new MarshallingEncoder(provider);
		return encoder;
	}
}

@Sharable
class SubReqServerHandler extends ChannelHandlerAdapter{

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();	//发生异常,关闭链路
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		SubscribeReq req = (SubscribeReq)msg;
		
		if("zhangsan".equalsIgnoreCase(req.getUserName())){
			System.out.println("Service accept client subscribe req : [" + req.toString() + "]");
			//由于使用了ProtobufEncoder所以不需要对SubscribeRespProto.SubscribeResp进行手工编码
			ctx.writeAndFlush(resp(req.getSubReqID()));
		}
	}
	
	private SubscribeResp resp(int subReqID) {  
        SubscribeResp resp = new SubscribeResp();  
        resp.setSubReqID(subReqID);  
        resp.setRespCode(0);  
        resp.setDesc("Netty book order succeed, 3 days later, sent to the designated address");  
        return resp;  
    }
	
	
}





























