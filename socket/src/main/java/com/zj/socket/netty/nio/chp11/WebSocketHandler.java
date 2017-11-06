package com.zj.socket.netty.nio.chp11;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 * websocket服务端
 * @author wangzhijiang1
 *
 */
public class WebSocketHandler extends SimpleChannelInboundHandler<Object> {

	private static final Logger logger = Logger.getLogger(WebSocketHandler.class);
	
	private WebSocketServerHandshaker shaker;
	
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		//传统的http接入
		if(msg instanceof FullHttpRequest){
			handleHttpRequest(ctx, (FullHttpRequest)msg);
		}
		//websocket接入
		else if (msg instanceof WebSocketFrame){
			handleWebSocketFrame(ctx, (WebSocketFrame)msg);
		}
		
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
		//http解码失败返回http异常
		if(!req.getDecoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))){
			WebSocketHandler.sendHttpResponse(ctx, req, (FullHttpResponse) new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		
		//构造握手响应返回, 本机测试
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws://localhost:8080/websocket", null, false);
		if(wsFactory == null){
			WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			shaker.handshake(ctx.channel(), req);
		}
	}
	
	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame){
		//判断是否是关闭链路的指令
		if(frame instanceof CloseWebSocketFrame){
			shaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}
		//判断是否是ping消息
		if(frame instanceof PingWebSocketFrame){
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		//本例程仅支持文本消息不支持二进制消息
		if(!(frame instanceof TextWebSocketFrame)){
			throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
		}
		
		//返回应答消息
		String request = ((WebSocketFrame)frame).content().toString();
		
		ctx.channel().write(new TextWebSocketFrame(request + " , 欢迎使用Netty WebSocket 服务, 现在时刻 : " + new java.util.Date().toString()));
	}
	
	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res){
		//应答客户端消息
		if(res.getStatus().code() != 200){
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			//setContentLength(res, res.content().readableBytes());
		}
		//如果是非Keep-alive, 关闭连接
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if(!isKeepAlived(req) || res.getStatus().code() != 200){
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
	private static boolean isKeepAlived(FullHttpRequest req){
		if("keep-alive".equals(req.headers().get("Connection"))){
			return true;
		}
		return false;
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}


}
