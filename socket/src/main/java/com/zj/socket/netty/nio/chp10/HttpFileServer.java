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
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

/**
 * 基于http的Netty服务端程序开发
 * @author wangzj
 *
 */
public class HttpFileServer {

	private static final String DEFAULT_URL = "/src/";
	
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
 * 
 * @author wangzj
 *
 */
class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private final String url;

	public HttpFileServerHandler(String url) {
		this.url = url;
	}

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

		if (!request.getDecoderResult().isSuccess()) {
			sendError(ctx, HttpResponseStatus.BAD_REQUEST);
			return;
		}
		if (request.getMethod() != HttpMethod.GET) {
			sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
			return;
		}

		final String uri = request.getUri();
		final String path = sanitizeUri(uri);
		
		System.out.println("路径转换,由uri = " + uri + ", 转换成了 path = " + path);
		
		if (path == null) {
			sendError(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}

		File file = new File(path);
		if (file.isHidden() || !file.exists()) {
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
			return;
		}
		if (file.isDirectory()) {
			if (uri.endsWith("/")) {
				sendListing(ctx, file);
			} else {
				sendRedirect(ctx, uri + "/");
			}
			return;
		}
		if (!file.isFile()) {
			sendError(ctx, HttpResponseStatus.FORBIDDEN);
			return;
		}

		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException fnfd) {
			sendError(ctx, HttpResponseStatus.NOT_FOUND);
			return;
		}

		long fileLength = randomAccessFile.length();
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		HttpHeaders.setContentLength(response, fileLength);
		setContentTypeHeader(response, file);

		if (HttpHeaders.isKeepAlive(request)) {
			response.headers().set(HttpHeaders.Names.CONNECTION,
					HttpHeaders.Values.KEEP_ALIVE);
		}

		ctx.write(response);
		/**
		 * 采用Netty的ChunkedFile对象直接将文件写入到发送缓冲区
		 */
		ChannelFuture sendFileFuture = null;
		sendFileFuture = ctx.write(new ChunkedFile(randomAccessFile, 0, fileLength, 8192), ctx.newProgressivePromise());
		sendFileFuture.addListener(new ChannelProgressiveFutureListener() {

			@Override
			public void operationComplete(ChannelProgressiveFuture future) throws Exception {
				System.out.println("Transfer complete.");

			}

			@Override
			public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
				if (total < 0)
					System.err.println("Transfer progress: " + progress);
				else
					System.err.println("Transfer progress: " + progress + "/" + total);
			}
		});

		/**
		 * 如果使用chunked编码,最后需要发送一个编码结束的空消息体,将LastHttpContent的EMPTY_LAST_CONTENT发送到缓冲区中,标识所有的消息体都已经发送完成,同时调用flash方法将
		 * 之前在发送缓冲区中的消息刷新到SocketChannel中发送给对方.
		 * 如果是非Keep-Alive的,最后一个包发送完成后,服务端要主动关闭连接.
		 */
		ChannelFuture lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
		if (!HttpHeaders.isKeepAlive(request)){
			lastContentFuture.addListener(ChannelFutureListener.CLOSE);
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		if (ctx.channel().isActive())
			sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
	}

	private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

	private String sanitizeUri(String uri) {
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			try {
				uri = URLDecoder.decode(uri, "ISO-8859-1");
			} catch (UnsupportedEncodingException e1) {
				throw new Error();
			}
		}

		if (!uri.startsWith(url))
			return null;
		if (!uri.startsWith("/"))
			return null;

		uri = uri.replace('/', File.separatorChar);
		if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri.startsWith(".") || uri.endsWith(".") || INSECURE_URI.matcher(uri).matches()) {
			return null;
		}
		return System.getProperty("user.dir") + File.separator + uri;
	}

	private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[A-Za-z0-9][-_A-Za-z0-9\\.]*");

	private static void sendListing(ChannelHandlerContext ctx, File dir) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");
		//response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");
		//response.headers().set(HttpHeaders.Names.LOCATION, "http://www.baidu.com");

		String dirPath = dir.getPath();
		StringBuilder buf = new StringBuilder();

		buf.append("<!DOCTYPE html>\r\n");
		buf.append("<html><head><title>");
		buf.append(dirPath);
		buf.append("目录:");
		buf.append("</title></head><body>\r\n");

		buf.append("<h3>");
		buf.append(dirPath).append(" 目录：");
		buf.append("</h3>\r\n");
		buf.append("<ul>");
		buf.append("<li>链接：<a href=\"../\">..</a></li>\r\n");
		
		for (File f : dir.listFiles()) {
			if (f.isHidden() || !f.canRead()) {
				continue;
			}
			String name = f.getName();
			if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
				continue;
			}

			buf.append("<li>链接：<a href=\"");
			buf.append(name);
			buf.append("\">");
			buf.append(name);
			buf.append("</a></li>\r\n");
		}

		buf.append("</ul></body></html>\r\n");
		
		System.out.println("输出html文件 : \n" + buf.toString());

		ByteBuf buffer = Unpooled.copiedBuffer(buf, CharsetUtil.UTF_8);
		response.content().writeBytes(buffer);
		buffer.release();
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
		response.headers().set(HttpHeaders.Names.LOCATION, newUri);
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

	private static void setContentTypeHeader(HttpResponse response, File file) {
		MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, mimetypesFileTypeMap.getContentType(file.getPath()));
	}
}


















