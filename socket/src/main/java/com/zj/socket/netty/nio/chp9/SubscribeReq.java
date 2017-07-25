package com.zj.socket.netty.nio.chp9;

import java.io.Serializable;

public class SubscribeReq implements Serializable {  
	  
    private int subReqID;  
  
    private String userName;  
  
    private String productName;  
  
    private String phoneNumber;  
  
    private String address;  
  
    public final int getSubReqID() {  
        return subReqID;  
    }  
  
    public final void setSubReqID(int subReqID) {  
        this.subReqID = subReqID;  
    }  
  
    public final String getUserName() {  
        return userName;  
    }  
  
    public final void setUserName(String userName) {  
        this.userName = userName;  
    }  
  
    public final String getProductName() {  
        return productName;  
    }  
  
    public final void setProductName(String productName) {  
        this.productName = productName;  
    }  
  
    public final String getPhoneNumber() {  
        return phoneNumber;  
    }  
  
    public final void setPhoneNumber(String phoneNumber) {  
        this.phoneNumber = phoneNumber;  
    }  
  
    public final String getAddress() {  
        return address;  
    }  
  
    public final void setAddress(String address) {  
        this.address = address;  
    }  
  
    /* 
     * (non-Javadoc) 
     * @see java.lang.Object#toString() 
     */  
    @Override  
    public String toString() {  
    return "SubscribeReq [subReqID=" + subReqID + ", userName=" + userName  
        + ", productName=" + productName + ", phoneNumber="  
        + phoneNumber + ", address=" + address + "]";  
    }  
      
}