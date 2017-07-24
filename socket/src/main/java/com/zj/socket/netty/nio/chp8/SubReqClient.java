package com.zj.socket.netty.nio.chp8;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.ArrayList;
import java.util.List;

import com.zj.socket.netty.nio.chp8.SubscribeReqProto.SubscribeReq.Builder;

/**
 * Netty结合Protobuf进行开发Client
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
					ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
					
					//客户端需要解码的对象是订购响应,所以入参为这个SubscribeRespProto.SubscribeResp
					ch.pipeline().addLast(new ProtobufDecoder(SubscribeRespProto.SubscribeResp.getDefaultInstance()));
					ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
					ch.pipeline().addLast(new ProtobufEncoder());
					
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
			ctx.write(subReq(i));
		}
		ctx.flush();
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



	private SubscribeReqProto.SubscribeReq subReq(int i){
		Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
		builder.setSubReqID(i);
		builder.setUserName("zhangsan");
		builder.setProductName("Netty book for protobuf");
		List<String> address = new ArrayList<String>(){
			
			private static final long serialVersionUID = 1L;

			{
				add("Nanjing YuHuaTai");
				add("Jiangsu LiuLiChang");
				add("Shenzhen HongShuLin");
			}
		};
		builder.addAllAddress(address);
		
		return builder.build();
	}
	
	
}
