package com.zj.socket.netty.nio.chp10;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

/**
 * 基于http的Netty服务端程序开发
 * @author wangzj
 *
 */
public class HttpFileServer {

	private static final String DEFAULT_URL = "/";
	
	public static void main(String[] args) throws Exception {
		new HttpFileServer().run(DEFAULT_URL, 8080);
	}
	
	public void run(String url, int port) throws Exception {
		NioEventLoopGroup bossGroup = new NioEventLoopGroup();
		NioEventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try{
			
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					//HTTP请求消息解码器
					ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
					/*
					 * 解码器,作用是将多个消息转换为单一的FullHttpRequest或者FullHttpResponse,原因是http解码器在每个http消息中会生成多个消息对象:
					 * 1.HttpRequest/HttpResponse
					 * 2.HttpContent
					 * 3.LastHttpContent
					 */
					ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
					//响应消息编码器,对http消息进行编码
					ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
					//作用是支持异步发送的最大码流(例如大的文件传输),但不占用过多的内存,防止发生java内存溢出错误
					ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
					
					ch.pipeline().addLast(new HttpFileServerHandler(url));
				}
			});
			
			ChannelFuture f = b.bind("127.0.0.1", port).sync();
			
			System.out.println("http文件服务器启动,网址: " + "http://127.0.0.1:" + port + url);
			
			f.channel().closeFuture().sync();
		}finally{
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}

/**
 * Http文件服务器处理类
 * @author wangzj
 *
 */
class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>{
	private final static String CONTENT_TYPE = HttpHeaders.Names.CONTENT_TYPE.toString();
	private final static String LOCATION = HttpHeaders.Names.LOCATION.toString();
	private final static String CONNECTION = HttpHeaders.Names.CONNECTION.toString();
	private final String url;
	
	public HttpFileServerHandler(String url){
		this.url = url;
	}
	
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		/**
		 *  这是对请求消息进行解码，如果解码失败，直接HTTP 400错误返回
		 *	
		 *	public static final HttpResponseStatus BAD_REQUEST = new HttpResponseStatus(400, "Bad Request", true);
		 *  
		 *  400 是请求出错                 
		 */
		if(!request.getDecoderResult().isSuccess()){
			sendError(ctx,HttpResponseStatus.BAD_REQUEST);
			return;
		}
		/**
		 * 如果不是从浏览器或者Get请求，直接HTTP 405错误返回
		 * 
		 * METHOD_NOT_ALLOWED = new HttpResponseStatus(405, "Method Not Allowed", true);
		 * 
		 * 405 用来访问本页面的 HTTP 谓词不被允许（即 方法不被允许） 
		 */
		if(request.getMethod() != HttpMethod.GET){
			sendError(ctx,HttpResponseStatus.METHOD_NOT_ALLOWED);
			return;
		}
		
		final String uri = request.getUri();
		
		final String path = sanitizeUri(uri);//看方法的注释
		System.out.println("本次请求路径: " + path);
		/**
		 * 如果构造的uri不合法，则返回HTTP 403错误。
		 * 403 资源不可用错误。
		 */
		if(path == null){
			sendError(ctx,HttpResponseStatus.FORBIDDEN);
			return;
		}
		/**
		 * 经过上面层层的过滤，使用新组装的URI路径构造File对象，
		 */
		File file = new File(path);
		/**
		 * 如果文件不存在或者系统隐藏文件，刚HTTP 404错误返回
		 * 404页面就是当用户输入了错误的链接时，返回的页面。HTTP 404 错误意味着链接指向的网页不存在，即原始网页的URL失效。
		 */
		if(file.isHidden() || !file.exists()){
			sendError(ctx,HttpResponseStatus.NOT_FOUND);
			return;
		}
		if(file.isDirectory()){
			if(uri.endsWith("/")){
				sendListing(ctx,file);
			}else{
				sendRedirect(ctx,uri+'/');
			}
			return;
		}
		/**
		 *当用户在浏览器上点击超链接直接打开或者下载文件，代码会执行85行，对超链接的文件进行合法性判断，
		 *如果不是合法文件就会返回http 403错误。校验通过下面的代码用 RandomAccessFile（随机文件读写类）以只读的方式打开文件，如果找开失败就会返回HTTP 404错误
		 */
		if(!file.isFile()){
			sendError(ctx,HttpResponseStatus.FORBIDDEN);
			return;
		}
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(file, "r"); //以只读的方式打开。
		} catch (Exception e) {
			sendError(ctx,HttpResponseStatus.NOT_FOUND);
			return;
		}
		//获取文件的长度，构造成功的应答消息，然后在消息头中设置connection 为keep-live。
		long fileLength = randomAccessFile.length();
		HttpResponse response  =  new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpHeaders.setContentLength(response,fileLength);
		setContentTypeHeader(response, file);
		/**
		 * 讲解一下keep-alive:
		 * 大家知道 每个http请求都要求打开一个tpc socket连接，并且使用一次之后就断开这个tcp连接。
		 * 使用keep-alive可以改善这种状态，即在一次TCP连接中可以持续发送多份数据而不会断开连接。
		 * 通过使用keep-alive机制，可以减少tcp连接建立次数，也意味着可以减少TIME_WAIT状态连接，以此提高性能和提高httpd服务器的吞吐率(更少的tcp连接意味着更少的系统内核调用,socket的accept()和close()调用)。
		 * 但是，keep-alive并不是免费的午餐,长时间的tcp连接容易导致系统资源无效占用。配置不当的keep-alive，有时比重复利用连接带来的损失还更大。
		 * 所以，正确地设置keep-alive timeout时间非常重要。
		 */
		if(HttpHeaders.isKeepAlive(request)){//判断是否是keepalive,如果是就设置消息头connection为keep-alive
			response.headers().set(CONNECTION,HttpHeaders.Values.KEEP_ALIVE);
		}
		
		ctx.write(response);//发送响应消息
		/**
		 * 通过netty 的chunkedFile对象直接将文件写入到发送缓冲区中，最后为sendFileFuture添加ChannelProgressiveFutureListener，如果发送完成，返回
		 */
		ChannelFuture sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile,0,fileLength,8192),ctx.newProgressivePromise());
		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
			
			@Override
			public void operationComplete(ChannelProgressiveFuture future)
					throws Exception {
					System.out.println("Transfer complete.");
			}
			
			@Override
			public void operationProgressed(ChannelProgressiveFuture future,
					long progress, long total) throws Exception {
				if(total < 0 ){
					System.err.println("Transfer progress:"+progress);
				}else{
					System.err.println("Transfer progress:"+progress+"/"+total);
				}
			}
		});
		//切记：如果使用chunked编码，最后要发送一个编码结束的空消息体，将LastHttpContent.EMPTY_LAST_CONTENT 发送到缓冲区，标识所有的消息体已给发送完毕，同时flush方法刷新缓冲区，确保发送给对方。
		//如果是非keep-live,最后一包消息发送完成之后，服务器端会主动关闭连接。
		ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if(!HttpHeaders.isKeepAlive(request)){
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		if(ctx.channel().isActive()){
			sendError(ctx,HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}
	}
	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");
	/**
	 *  首先使用JDK的URLDecoder 对url进行解码，使用“UTF-8字符集，解码之后对uri进行合法性判断，
	 *  如果URI与访问的uri一致或者是其子目录（文件）”，则校验通过，否则返回空
	 * @param uri
	 * @return
	 */
	private String sanitizeUri(String uri){
		if(uri == null) {
			return null;
		}
		
		try {
			uri  = URLDecoder.decode(uri,"UTF-8");
		} catch (Exception e) {
			try {
				uri = URLDecoder.decode(uri,"ISO-8859-1");
			} catch (Exception e2) {
				throw new Error(e2);
			}
		}
		uri = url.substring(1) + uri;
		
		if(!uri.startsWith("/")){
			return null;
		}
		uri = uri.replace('/', File.separatorChar);//将硬编码的文件路径分隔符替换为本地操作系统的文件路径分隔符。
		//对新生成的uri进行二次合法性判断，如果校验失败，则返回null,否则对文件进行拼接，使用当前运行程序所在的工程目录+uri构造绝对路径返回。
		if(uri.contains(File.separator+'.')
				||uri.contains('.'+File.separator)
				||uri.startsWith(".")
				||uri.endsWith(".")||INSECURE_URI.matcher(uri).matches()){
			return null;
		}
		
		return System.getProperty("user.dir") + File.separator + uri;
	}
	
	private static final Pattern ALLOWEN_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");
	/**
	 * 该方法创建成的HTTP相应消息，随后设置消息头的类型为“text/html;charset=UTF-8”.
	 * @param ctx
	 * @param dir
	 */
	private static void sendListing(ChannelHandlerContext ctx,File dir){
		//new 一个响应消息，使用HTTP_1_1协议，返回码200 OK
		FullHttpResponse response  = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
		//设置消息头的类型：text/html;charset=UTF-8。
		response.headers().set(CONTENT_TYPE,"text/html;charset=UTF-8");
		//由于显示的消息要显示在浏览器上，所以我们组合成了html格式的消息，这里不对html进行介绍了，因为我也不熟悉啊，惭愧。
		StringBuilder buf = new StringBuilder();
		String dirPath = dir.getPath();
		buf.append("<!Doctype html>\r\n");
		buf.append("<html><head><title>");
		buf.append(dirPath);
		buf.append("目录：");
		buf.append("</title></head><body>\r\n");
		buf.append("<h3>");
		buf.append("netty文件目录服务器：");
		buf.append("</h3>\r\n");
		buf.append("<ul>");
		buf.append("<li>链接：<a href=\"../\">..</a></li>\r\n");
		//下面展示根目录下的所有文件夹和文件，同时使用超连接来表示
		for(File f : dir.listFiles()){
			if(f.isHidden()||!f.canRead()){
				continue;
			}
			String name = f.getName();
			String path = f.getPath();
			if(!ALLOWEN_FILE_NAME.matcher(name).matches()){
				continue;
			}
			buf.append("<li>链接：<a href=\"");
			buf.append(dirPath);
			buf.append(File.separator);
			buf.append(name);
			if(f.isFile()){
				
			}else{
				buf.append("/");
			}
			buf.append("\">");
			buf.append(name);
			buf.append("</a></li>\r\n");
		}
		buf.append("</ul></body></html>\r\n");
		//变成bytebuf，返回给客户端
		ByteBuf buffer = Unpooled.copiedBuffer(buf,CharsetUtil.UTF_8);
		response.content().writeBytes(buffer);//将信息放到HTTP应答消息中
		buffer.release();//释放缓冲区
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);//最后刷新到socketchannel中
	}
	
	private static void sendRedirect(ChannelHandlerContext ctx,String newUri){
		FullHttpResponse response  = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
		response.headers().set(LOCATION,newUri);
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
	
	private static void sendError(ChannelHandlerContext ctx,HttpResponseStatus status){
		FullHttpResponse response =  new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,Unpooled.copiedBuffer("Failure："+status.toString()+"\r\n",CharsetUtil.UTF_8));
		response.headers().set(CONTENT_TYPE,"text/plain;  charset=UTF-8");
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
	
	private static void setContentTypeHeader(HttpResponse response,File file){
		MimetypesFileTypeMap mimeTypeMap = new MimetypesFileTypeMap();
		response.headers().set(CONTENT_TYPE,mimeTypeMap.getContentType(file.getPath()));
	}
}




















