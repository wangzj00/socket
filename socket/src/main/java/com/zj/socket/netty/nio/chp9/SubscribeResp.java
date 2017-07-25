package com.zj.socket.netty.nio.chp9;

import java.io.Serializable;

public class SubscribeResp implements Serializable {  
	  
    private int subReqID;  
  
    private int respCode;  
  
    private String desc;  
  
    public final int getSubReqID() {  
        return subReqID;  
    }  
  
    public final void setSubReqID(int subReqID) {  
        this.subReqID = subReqID;  
    }  
  
    public final int getRespCode() {  
        return respCode;  
    }  
  
    public final void setRespCode(int respCode) {  
        this.respCode = respCode;  
    }  
  
    public final String getDesc() {  
        return desc;  
    }  
  
    public final void setDesc(String desc) {  
        this.desc = desc;  
    }  
      
    /* 
     * (non-Javadoc) 
     * @see java.lang.Object#toString() 
     */  
    @Override  
    public String toString() {  
    return "SubscribeResp [subReqID=" + subReqID + ", respCode=" + respCode  
        + ", desc=" + desc + "]";  
    }  
      
}