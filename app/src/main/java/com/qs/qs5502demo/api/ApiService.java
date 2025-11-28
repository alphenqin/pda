package com.qs.qs5502demo.api;

import com.qs.qs5502demo.model.Pallet;
import com.qs.qs5502demo.model.Task;
import com.qs.qs5502demo.model.Valve;

import java.util.List;
import java.util.Map;

/**
 * WMS API接口定义
 * 注意：这里先定义接口结构，实际网络请求实现需要根据项目情况添加Retrofit或HttpURLConnection
 */
public interface ApiService {
    
    /**
     * 托盘扫码获取信息
     * @param barcode 托盘条码
     * @return 托盘信息
     */
    Pallet scanPallet(String barcode);

    /**
     * 阀门绑定
     * @param valve 阀门信息
     * @return 是否成功
     */
    boolean bindValve(Valve valve);

    /**
     * 根据条件查询阀门
     * @param params 查询参数（厂家名称、阀门编号、阀门型号、入库日期等）
     * @return 阀门列表
     */
    List<Valve> queryValves(Map<String, String> params);

    /**
     * 创建AGV任务（入库/送检/回库/出库）
     * @param taskType 任务类型
     * @param palletNo 托盘号
     * @param locationCode 库位号
     * @param fromStation 起始站点
     * @param toStation 目标站点
     * @param valveNo 阀门编号（可选）
     * @return 任务信息（包含任务编号）
     */
    Task createTask(String taskType, String palletNo, String locationCode, 
                    String fromStation, String toStation, String valveNo);

    /**
     * 查询任务列表
     * @param startDate 起始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 任务列表
     */
    List<Task> queryTasks(String startDate, String endDate);

    /**
     * 取消任务
     * @param taskNo 任务编号
     * @return 是否成功
     */
    boolean cancelTask(String taskNo);
}

