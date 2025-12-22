package com.qs.qs5502demo.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.qs.qs5502demo.model.ApiResponse;
import com.qs.qs5502demo.model.LoginRequest;
import com.qs.qs5502demo.model.LoginResponse;
import com.qs.qs5502demo.model.PageResponse;
import com.qs.qs5502demo.model.Pallet;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.Valve;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WMS API服务实现
 */
public class WmsApiService {
    
    private static final String TAG = "WmsApiService";
    
    private Context context;
    
    public WmsApiService() {
        // 默认构造函数，context需要从调用处传入
    }
    
    public WmsApiService(Context context) {
        this.context = context;
        // 初始化ApiConfig
        if (context != null) {
            ApiConfig.init(context);
        }
    }
    
    /**
     * 获取WMS基础URL
     */
    private String getBaseUrl() {
        return ApiConfig.getWmsBaseUrl();
    }
    
    /**
     * 登录接口
     */
    public LoginResponse login(LoginRequest request) throws IOException {
        String url = getBaseUrl() + "/auth/login";
        String json = HttpUtil.toJson(request);
        String response = HttpUtil.post(url, json, context);
        
        LoginResponse loginResponse = HttpUtil.fromJson(response, LoginResponse.class);
        return loginResponse;
    }
    
    /**
     * 托盘扫码接口
     */
    public Pallet scanPallet(String barcode, Context context) throws IOException {
        String url = getBaseUrl() + "/pallet/scan";
        
        Map<String, String> request = new HashMap<>();
        request.put("barcode", barcode);
        
        String json = HttpUtil.toJson(request);
        String response = HttpUtil.post(url, json, context);
        
        Type type = new TypeToken<ApiResponse<Pallet>>(){}.getType();
        ApiResponse<Pallet> apiResponse = HttpUtil.fromJson(response, type);
        
        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }
    
    /**
     * 阀门绑定接口
     */
    public boolean bindValve(Valve valve, Context context) throws IOException {
        String url = getBaseUrl() + "/valve/bind";
        String json = HttpUtil.toJson(valve);
        String response = HttpUtil.post(url, json, context);
        
        Type type = new TypeToken<ApiResponse<Map<String, Object>>>(){}.getType();
        ApiResponse<Map<String, Object>> apiResponse = HttpUtil.fromJson(response, type);
        
        if (apiResponse.isSuccess()) {
            return true;
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }
    
    /**
     * 阀门查询接口
     */
    public PageResponse<Valve> queryValves(Map<String, String> params, Context context) throws IOException {
        String url = getBaseUrl() + "/valve/query";
        String json = HttpUtil.toJson(params);
        String response = HttpUtil.post(url, json, context);
        
        Type type = new TypeToken<ApiResponse<PageResponse<Valve>>>(){}.getType();
        ApiResponse<PageResponse<Valve>> apiResponse = HttpUtil.fromJson(response, type);
        
        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }
    
    /**
     * 任务记录查询接口
     */
    public PageResponse<Task> queryTasks(Map<String, String> params, Context context) throws IOException {
        String url = getBaseUrl() + "/task/query";
        String json = HttpUtil.toJson(params);
        String response = HttpUtil.post(url, json, context);
        
        Type type = new TypeToken<ApiResponse<PageResponse<Task>>>(){}.getType();
        ApiResponse<PageResponse<Task>> apiResponse = HttpUtil.fromJson(response, type);
        
        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }
}

