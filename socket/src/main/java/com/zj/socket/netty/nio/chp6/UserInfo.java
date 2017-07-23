package com.zj.socket.netty.nio.chp6;

import java.io.Serializable;
import java.nio.ByteBuffer;

import org.msgpack.annotation.Message;

/**
 * 测试序列化成字节数组的大小
 * 
 * @author wangzj
 *
 */
@Message
public class UserInfo{

	//private static final long serialVersionUID = -6891143772542101986L;

	private String userName;
	private int userID;

	public UserInfo buildUserName(String userName) {
		this.userName = userName;
		return this;
	}

	public UserInfo buildUserID(int userID) {
		this.userID = userID;
		return this;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public int getUserID() {
		return userID;
	}

	public void setUserID(int userID) {
		this.userID = userID;
	}

	@Override
	public String toString() {
		return "UserInfo [userName=" + userName + ", userID=" + userID + "]";
	}

	public byte[] codeC(ByteBuffer buffer) {
		buffer.clear();
		byte[] value = this.userName.getBytes();
		buffer.putInt(value.length);
		buffer.put(value);
		buffer.putInt(userID);
		buffer.flip();
		value = null;
		byte[] result = new byte[buffer.remaining()];
		buffer.get(result);
		return result;
	}
}
