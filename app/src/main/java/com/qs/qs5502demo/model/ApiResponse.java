package com.qs.qs5502demo.model;

import com.google.gson.annotations.SerializedName;

/**
 * 统一API响应格式
 */
public class ApiResponse<T> {
    @SerializedName("code")
    private int code;
    
    @SerializedName("msg")
    private String message;
    
    @SerializedName("data")
    private T data;
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public boolean isSuccess() {
        return code == 200;
    }
}

