package com.qs.qs5502demo.api;

import android.content.Context;
import android.util.Log;

import com.google.gson.reflect.TypeToken;
import com.qs.qs5502demo.model.AgvInfoResponse;
import com.qs.qs5502demo.model.AgvResponse;
import com.qs.qs5502demo.model.TaskPoint;
import com.qs.qs5502demo.model.TaskResultResponse;
import com.qs.qs5502demo.model.TaskSentRequest;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AGV调度系统API服务实现
 * 所有接口统一调用 /taskSent 接口
 */
public class AgvApiService {
    
    private static final String TAG = "AgvApiService";
    
    private Context context;
    
    public AgvApiService() {
    }
    
    public AgvApiService(Context context) {
        this.context = context;
        // 初始化ApiConfig
        if (context != null) {
            ApiConfig.init(context);
        }
    }
    
    /**
     * 获取AGV基础URL
     */
    private String getBaseUrl() {
        return ApiConfig.getAgvBaseUrl();
    }
    
    /**
     * 获取任务发送URL
     */
    private String getTaskSentUrl() {
        return getBaseUrl() + "/taskSent";
    }
    
    /**
     * 获取任务结果查询URL
     */
    private String getTaskResultUrl() {
        return getBaseUrl() + "/taskResult";
    }

    /**
     * 获取AGV信息URL
     */
    private String getAgvInfoUrl() {
        return getBaseUrl() + "/agvInfo";
    }
    
    /**
     * 发送任务（统一接口）
     */
    private AgvResponse sendTask(TaskSentRequest request, Context context) throws IOException {
        String json = HttpUtil.toJson(request);
        String response = HttpUtil.post(getTaskSentUrl(), json, context);
        
        AgvResponse agvResponse = HttpUtil.fromJson(response, AgvResponse.class);
        
        if (agvResponse.isSuccess()) {
            return agvResponse;
        } else {
            throw new IOException(agvResponse.getMessage());
        }
    }
    
    /**
     * 入库：呼叫入库
     * 从置换区取货，放到仓库库位
     */
    public AgvResponse callInbound(String swapStation, String binCode, String matCode, 
                                   String outID, Context context) throws IOException {
        TaskSentRequest request = new TaskSentRequest();
        request.setType("01");  // 取放货任务
        request.setOutID(outID);
        request.setLevel("2");   // 普通任务
        
        // 构建作业点：置换区取货 -> 库位放货
        List<TaskPoint> points = new ArrayList<>();
        points.add(new TaskPoint("01", swapStation, "02"));  // 取货
        points.add(new TaskPoint("02", binCode, "04", matCode));  // 放货
        
        request.setPoints(points);
        
        return sendTask(request, context);
    }
    
    /**
     * 送检：呼叫送检
     * 从库位取货，送到检测区站点
     */
    public AgvResponse callSendInspection(String binCode, String inspectionStation, 
                                         String matCode, String outID, Context context) throws IOException {
        TaskSentRequest request = new TaskSentRequest();
        request.setType("01");  // 取放货任务
        request.setOutID(outID);
        request.setLevel("2");   // 普通任务
        
        // 构建作业点：库位取货 -> 检测区放货
        List<TaskPoint> points = new ArrayList<>();
        points.add(new TaskPoint("01", binCode, "02"));  // 取货
        points.add(new TaskPoint("02", inspectionStation, "04", matCode));  // 放货
        
        request.setPoints(points);
        
        return sendTask(request, context);
    }
    
    /**
     * 送检：空托回库
     * 从检测区站点取空托盘，送回库位
     */
    public AgvResponse returnPalletFromInspection(String inspectionStation, String binCode, 
                                                  String outID, Context context) throws IOException {
        TaskSentRequest request = new TaskSentRequest();
        request.setType("01");  // 取放货任务
        request.setOutID(outID);
        request.setLevel("2");   // 普通任务
        
        // 构建作业点：检测区取空托 -> 库位放空托
        List<TaskPoint> points = new ArrayList<>();
        points.add(new TaskPoint("01", inspectionStation, "02"));  // 取空托
        points.add(new TaskPoint("02", binCode, "04"));  // 放空托（无物料）
        
        request.setPoints(points);
        
        return sendTask(request, context);
    }
    
    /**
     * 回库：呼叫托盘
     * 从库位取空托盘，送到检测区站点
     */
    public AgvResponse callPalletToInspection(String binCode, String inspectionStation, 
                                              String outID, Context context) throws IOException {
        TaskSentRequest request = new TaskSentRequest();
        request.setType("01");  // 取放货任务
        request.setOutID(outID);
        request.setLevel("2");   // 普通任务
        
        // 构建作业点：库位取空托 -> 检测区放空托
        List<TaskPoint> points = new ArrayList<>();
        points.add(new TaskPoint("01", binCode, "02"));  // 取空托
        points.add(new TaskPoint("02", inspectionStation, "04"));  // 放空托
        
        request.setPoints(points);
        
        return sendTask(request, context);
    }
    
    /**
     * 回库：阀门回库
     * 从检测区站点取载有阀门的托盘，送回库位
     */
    public AgvResponse returnValveToWarehouse(String inspectionStation, String binCode, 
                                               String matCode, String outID, Context context) throws IOException {
        TaskSentRequest request = new TaskSentRequest();
        request.setType("01");  // 取放货任务
        request.setOutID(outID);
        request.setLevel("2");   // 普通任务
        
        // 构建作业点：检测区取货 -> 库位放货
        List<TaskPoint> points = new ArrayList<>();
        points.add(new TaskPoint("01", inspectionStation, "02"));  // 取货
        points.add(new TaskPoint("02", binCode, "04", matCode));  // 放货
        
        request.setPoints(points);
        
        return sendTask(request, context);
    }
    
    /**
     * 出库：呼叫出库
     * 从库位取货，送到置换区
     */
    public AgvResponse callOutbound(String binCode, String swapStation, String matCode, 
                                    String outID, Context context) throws IOException {
        TaskSentRequest request = new TaskSentRequest();
        request.setType("01");  // 取放货任务
        request.setOutID(outID);
        request.setLevel("2");   // 普通任务
        
        // 构建作业点：库位取货 -> 置换区放货
        List<TaskPoint> points = new ArrayList<>();
        points.add(new TaskPoint("01", binCode, "02"));  // 取货
        points.add(new TaskPoint("02", swapStation, "04", matCode));  // 放货
        
        request.setPoints(points);
        
        return sendTask(request, context);
    }
    
    /**
     * 出库：空托回库
     * 从置换区取空托盘，送回库位
     */
    public AgvResponse returnPalletFromSwap(String swapStation, String binCode, 
                                            String outID, Context context) throws IOException {
        TaskSentRequest request = new TaskSentRequest();
        request.setType("01");  // 取放货任务
        request.setOutID(outID);
        request.setLevel("2");   // 普通任务
        
        // 构建作业点：置换区取空托 -> 库位放空托
        List<TaskPoint> points = new ArrayList<>();
        points.add(new TaskPoint("01", swapStation, "02"));  // 取空托
        points.add(new TaskPoint("02", binCode, "04"));  // 放空托（无物料）
        
        request.setPoints(points);
        
        return sendTask(request, context);
    }
    
    /**
     * 取消任务
     * 清空指定outID的任务
     */
    public boolean cancelTask(String outID, String operator, Context context) throws IOException {
        TaskSentRequest request = new TaskSentRequest();
        request.setType("13");  // 清空指定outID任务
        request.setClearOutID(outID);
        request.setOutID(outID);
        
        AgvResponse response = sendTask(request, context);
        return response.isSuccess();
    }
    
    /**
     * 查询任务结果
     */
    public TaskResultResponse queryTaskResult(String outID, Context context) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("outID", outID);
        
        String json = HttpUtil.toJson(params);
        String response = HttpUtil.post(getTaskResultUrl(), json, context);
        
        TaskResultResponse taskResult = HttpUtil.fromJson(response, TaskResultResponse.class);
        
        if (taskResult.isSuccess()) {
            return taskResult;
        } else {
            throw new IOException(taskResult.getMessage());
        }
    }

    /**
     * 查询AGV信息
     */
    public AgvInfoResponse queryAgvInfo(Context context) throws IOException {
        String response = HttpUtil.post(getAgvInfoUrl(), "{}", context);
        AgvInfoResponse agvInfoResponse = HttpUtil.fromJson(response, AgvInfoResponse.class);

        if (agvInfoResponse.isSuccess()) {
            return agvInfoResponse;
        } else {
            throw new IOException(agvInfoResponse.getMessage());
        }
    }
}
