package com.zj.socket.netty.nio.chp7;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import com.zj.socket.netty.nio.chp6.UserInfo;
public class EchoClient {
    private final String host;
    private final int port;
    private final int sendNumber;
    public EchoClient(int port,String host,int sendNumber){
        this.host=host;
        this.port=port;
        this.sendNumber=sendNumber;
    }

    public void run() throws Exception{
        EventLoopGroup group=new NioEventLoopGroup();
        try{
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .handler(new ChannelInitializer<SocketChannel>(){
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //LengthFieldBasedFrameDecoder用于处理半包消息
                    //这样后面的MsgpackDecoder接收的永远是整包消息
                    //ch.pipeline().addLast("frameDecoder",new LengthFieldBasedFrameDecoder(65535,0,2,0,2));
                    ch.pipeline().addLast("msgpack decoder",new MsgpackDecoder());
                    //在ByteBuf之前增加2个字节的消息长度字段
                    //ch.pipeline().addLast("frameEncoder",new LengthFieldPrepender(2)); 
                    ch.pipeline().addLast("msgpack encoder",new MsgpackEncoder());
                    ch.pipeline().addLast(new EchoClientHandler(sendNumber));
                }

            });
            ChannelFuture f= b.connect(host,port).sync();
            f.channel().closeFuture().sync();
        }finally{
            group.shutdownGracefully();
        }
    }
    public static void main(String[] args) throws Exception{
        int port=8080;
        new EchoClient(port,"127.0.0.1",1).run();
    }
}
class EchoClientHandler extends ChannelHandlerAdapter{
    private final int sendNumber;
    private int counter;
    public EchoClientHandler(int sendNumber){
        this.sendNumber=sendNumber;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        UserInfo [] infos = UserInfo();
        for(UserInfo infoE : infos){
           // ctx.write(infoE);
        	ctx.writeAndFlush(infoE);
        }
        //ctx.flush();
    }
    private UserInfo[] UserInfo(){
        UserInfo [] userInfos=new UserInfo[sendNumber];
        UserInfo userInfo=null;
        for(int i=0;i<sendNumber;i++){
            userInfo=new UserInfo();
            userInfo.setUserID(i);
            userInfo.setUserName("ABCDEFG --->"+i);
            userInfos[i]=userInfo;
        }
        return userInfos;
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx,Object msg) throws Exception{
        System.out.println("This is " + ++counter + " times receive server : [" + msg + "]");
        //ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)throws Exception{
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }
}

