package com.qs.qs5502demo.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.qs.qs5502demo.model.ApiResponse;
import com.qs.qs5502demo.model.AvailableBin;
import com.qs.qs5502demo.model.AvailablePallet;
import com.qs.qs5502demo.model.InboundLockStatus;
import com.qs.qs5502demo.model.LoginRequest;
import com.qs.qs5502demo.model.LoginResponse;
import com.qs.qs5502demo.model.PalletScanConfig;
import com.qs.qs5502demo.model.PageResponse;
import com.qs.qs5502demo.model.Pallet;
import com.qs.qs5502demo.model.PalletTypeOption;
import com.qs.qs5502demo.model.TaskDispatchResult;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.Valve;
import com.qs.qs5502demo.model.AgvInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Collections;
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
     * 获取托盘扫码开关
     */
    public boolean getPalletScanEnabled(String deviceCode, Context context) throws IOException {
        String url = getBaseUrl() + "/pallet/scan/config";

        Map<String, String> request = new HashMap<>();
        request.put("deviceCode", deviceCode);

        String json = HttpUtil.toJson(request);
        String response = HttpUtil.post(url, json, context);

        Type type = new TypeToken<ApiResponse<PalletScanConfig>>(){}.getType();
        ApiResponse<PalletScanConfig> apiResponse = HttpUtil.fromJson(response, type);

        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData().isEnabled();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }

    /**
     * 获取可用库位
     */
    public AvailableBin getAvailableBin(Context context) throws IOException {
        String url = getBaseUrl() + "/bin/available";
        String response = HttpUtil.post(url, "{}", context);

        Type type = new TypeToken<ApiResponse<AvailableBin>>(){}.getType();
        ApiResponse<AvailableBin> apiResponse = HttpUtil.fromJson(response, type);

        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }

    /**
     * 获取托盘类型列表
     */
    public List<PalletTypeOption> listPalletTypes(Context context) throws IOException {
        String url = getBaseUrl() + "/pallet/type/list";
        String response = HttpUtil.post(url, "{}", context);

        Type type = new TypeToken<ApiResponse<List<PalletTypeOption>>>(){}.getType();
        ApiResponse<List<PalletTypeOption>> apiResponse = HttpUtil.fromJson(response, type);

        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }

    /**
     * 按托盘类型获取可用托盘
     */
    public AvailablePallet getAvailablePallet(Long palletTypeId, Context context) throws IOException {
        String url = getBaseUrl() + "/pallet/available";

        Map<String, Object> request = new HashMap<>();
        request.put("palletTypeId", palletTypeId);

        String json = HttpUtil.toJson(request);
        String response = HttpUtil.post(url, json, context);

        Type type = new TypeToken<ApiResponse<AvailablePallet>>(){}.getType();
        ApiResponse<AvailablePallet> apiResponse = HttpUtil.fromJson(response, type);

        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
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

    /**
     * 下发任务（PDA → WMS → AGV）
     */
    public TaskDispatchResult dispatchTask(Map<String, String> params, Context context) throws IOException {
        String url = getBaseUrl() + "/task/dispatch";
        String json = HttpUtil.toJson(params);
        String response = HttpUtil.post(url, json, context);

        Type type = new TypeToken<ApiResponse<TaskDispatchResult>>(){}.getType();
        ApiResponse<TaskDispatchResult> apiResponse = HttpUtil.fromJson(response, type);

        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }

    /**
     * 取消任务（PDA → WMS → AGV）
     */
    public boolean cancelTask(String outID, String deviceCode, Context context) throws IOException {
        String url = getBaseUrl() + "/task/cancel";
        Map<String, String> request = new HashMap<>();
        request.put("outID", outID);
        request.put("deviceCode", deviceCode);
        String json = HttpUtil.toJson(request);
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
     * 查询入库锁定状态（全局）
     */
    public InboundLockStatus getInboundLockStatus(Context context) throws IOException {
        String url = getBaseUrl() + "/task/inbound/lock";
        String response = HttpUtil.post(url, "{}", context);

        Type type = new TypeToken<ApiResponse<InboundLockStatus>>(){}.getType();
        ApiResponse<InboundLockStatus> apiResponse = HttpUtil.fromJson(response, type);

        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }

    /**
     * 查询送检锁定状态（全局）
     */
    public InboundLockStatus getInspectionLockStatus(Context context) throws IOException {
        String url = getBaseUrl() + "/task/inspection/lock";
        String response = HttpUtil.post(url, "{}", context);

        Type type = new TypeToken<ApiResponse<InboundLockStatus>>(){}.getType();
        ApiResponse<InboundLockStatus> apiResponse = HttpUtil.fromJson(response, type);

        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
            return apiResponse.getData();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }

    /**
     * 查询AGV信息（PDA → WMS → AGV）
     */
    public List<AgvInfo> queryAgvInfo(Context context) throws IOException {
        String url = getBaseUrl() + "/agv/info";
        String response = HttpUtil.post(url, "{}", context);

        Type type = new TypeToken<ApiResponse<List<AgvInfo>>>(){}.getType();
        ApiResponse<List<AgvInfo>> apiResponse = HttpUtil.fromJson(response, type);

        if (apiResponse.isSuccess()) {
            return apiResponse.getData() != null ? apiResponse.getData() : Collections.emptyList();
        } else {
            throw new IOException(apiResponse.getMessage());
        }
    }
}

