package com.zj.socket.netty.nio.chp11;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class WebSocketServerHandler1 extends SimpleChannelInboundHandler<Object> {  
    private WebSocketServerHandshaker handshaker;  
    @Override  
    protected void messageReceived(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {  
  
        // WebSocket接入  
         if (o instanceof WebSocketFrame) {  
             System.out.println("request meesage body:"+o);  
            handleWebSocketFrame(channelHandlerContext, (WebSocketFrame) o);  
        }else if(o instanceof FullHttpRequest){  
             System.out.println("server receiver message is:"+o);  
             handleHttpRequest(channelHandlerContext, (FullHttpRequest) o);  
  
         }  
    }  
  
    @Override  
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {  
        ctx.flush();  
    }  
    private void handleHttpRequest(ChannelHandlerContext ctx,  
                                   FullHttpRequest req) throws Exception {  
  
  
  
        // 构造握手响应返回，本机测试  
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(  
                "ws://localhost:17888/websocket", null, false);  
        handshaker = wsFactory.newHandshaker(req);  
        if (handshaker == null) {  
            WebSocketServerHandshakerFactory.sendUnsupportedWebSocketVersionResponse(ctx.channel());  
        } else {  
            handshaker.handshake(ctx.channel(), req);  
        }  
    }  
  
    private void handleWebSocketFrame(ChannelHandlerContext ctx,  
                                      WebSocketFrame frame) {  
  
        // 判断是否是关闭链路的指令  
        if (frame instanceof CloseWebSocketFrame) {  
            handshaker.close(ctx.channel(),  
                    (CloseWebSocketFrame) frame.retain());  
            return;  
        }  
        // 判断是否是Ping消息  
        if (frame instanceof PingWebSocketFrame) {  
            ctx.channel().write(  
                    new PongWebSocketFrame(frame.content().retain()));  
            return;  
        }  
        // 本例程仅支持文本消息，不支持二进制消息  
        if (!(frame instanceof TextWebSocketFrame)) {  
            throw new UnsupportedOperationException(String.format(  
                    "%s frame types not supported", frame.getClass().getName()));  
        }  
  
        // 返回应答消息  
        String request = ((TextWebSocketFrame) frame).text();  
        System.out.println("server receiver message is:"+request);  
  
        ctx.channel().write(  
                new TextWebSocketFrame(request  
                        + " , welocme to："  
                        + new java.util.Date().toString()));  
    }  
  
    @Override  
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {  
        ctx.close();  
    }  
}