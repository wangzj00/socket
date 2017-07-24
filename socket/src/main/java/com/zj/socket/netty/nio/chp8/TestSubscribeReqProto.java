package com.zj.socket.netty.nio.chp8;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.zj.socket.netty.nio.chp8.SubscribeReqProto.SubscribeReq;

/**
 * 介绍Protobuf的使用
 * @author wangzj
 *
 */
public class TestSubscribeReqProto {

	
	public static void main(String[] args) throws InvalidProtocolBufferException {
		SubscribeReq subscribeReq = createSubscribeReq();
		System.out.println("Before encode : " + subscribeReq.toString());
		SubscribeReq subscribeReq2 = decode(encode(subscribeReq));
		System.out.println("After decode : " + subscribeReq2.toString());
		System.out.println("Assert equals :--> " + subscribeReq2.equals(subscribeReq));
	}
	
	/**
	 * 编码-序列化
	 * @param req
	 * @return
	 */
	private static byte[] encode(SubscribeReqProto.SubscribeReq req){
		return req.toByteArray();
	}
	
	/**
	 * 解码-反序列化
	 * @param body
	 * @return
	 * @throws InvalidProtocolBufferException
	 */
	private static SubscribeReqProto.SubscribeReq decode(byte[] body) throws InvalidProtocolBufferException{
		return SubscribeReqProto.SubscribeReq.parseFrom(body);
	}
	
	/**
	 * 创建对象
	 * @return
	 */
	private static SubscribeReqProto.SubscribeReq createSubscribeReq(){
		//创建构建器builder
		SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq.newBuilder();
		//通过builder对属性进行设置
		builder.setSubReqID(1);
		builder.setUserName("zhangsan");
		builder.setProductName("Netty Book");
		List<String> address = new ArrayList<String>(){
			private static final long serialVersionUID = 1L;

			{
				add("Nanjing YuHuaTai");
				add("Jiangsu LiuLiChang");
				add("Shenzhen HongShuLin");
			}
		};
		
		//对于集合类型通过addAllXXX()方法可以直接将对象设置到对应的属性中
		builder.addAllAddress(address);
		return builder.build();
	}
	
}
