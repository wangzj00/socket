package com.zj.socket.netty.nio.chp8;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 下载ProtoBuf资源包
 * @author wangzj
 *
 */
public class DownLoadProtoBuf {

	public static void main(String[] args) throws MalformedURLException {
		String urlString = "https://github.com/google/protobuf/releases/download/v3.0.0/protoc-3.0.0-win32.zip";
		URL url = new URL(urlString);
		BufferedInputStream bufIn = null;
		BufferedOutputStream bufOut = null;
		try {
			HttpURLConnection connection = (HttpURLConnection)url.openConnection();
			connection.setRequestProperty("Connection", "keep-alive");
			
			connection.connect();
			bufIn = new BufferedInputStream(connection.getInputStream());
			bufOut = new BufferedOutputStream(new FileOutputStream(new File("E:/protoc-3.0.0-win32.zip")));
			int len = 0;
			byte[] buf = new byte[2048];
			while(true){
				len = bufIn.read(buf);
				System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + " -- read " + len + "bytes");
				if(len < 0){
					break;
				}
				bufOut.write(buf, 0, len);
				
			}
			System.out.println("download successfully! data : " + new File("E:/protoc-3.2.0-win32.zip").length() + "b");
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(bufIn != null){
				try {
					bufIn.close();
				} catch (IOException e) {
					;
				}
			}
			if(bufOut != null){
				try {
					bufOut.close();
				} catch (IOException e) {
					;
				}
			}
		}
	}
}
