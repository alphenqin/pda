package com.qs.qs5502demo.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * AGV信息查询响应
 * code为字符串类型：20000成功，90000失败
 */
public class AgvInfoResponse {

    @SerializedName("code")
    private String code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private List<AgvInfo> data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AgvInfo> getData() {
        return data;
    }

    public void setData(List<AgvInfo> data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return "20000".equals(code);
    }
}
