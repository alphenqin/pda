package com.qs.qs5502demo.model;

import java.io.Serializable;

/**
 * 任务信息
 */
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String TYPE_INBOUND = "INBOUND";              // 入库
    public static final String TYPE_SEND_INSPECTION = "SEND_INSPECTION";  // 送检
    public static final String TYPE_RETURN = "RETURN";                // 回库
    public static final String TYPE_OUTBOUND = "OUTBOUND";            // 出库

    public static final String STATUS_PENDING = "PENDING";            // 待执行
    public static final String STATUS_EXECUTING = "EXECUTING";        // 执行中
    public static final String STATUS_COMPLETED = "COMPLETED";        // 已完成
    public static final String STATUS_CANCELLED = "CANCELLED";        // 已取消
    public static final String STATUS_FAILED = "FAILED";              // 失败

    private String outID;           // 任务编号（AGV生成），如 "R20250715145830999"
    private String taskNo;          // 任务编号（兼容旧字段）
    private String taskType;        // 业务任务类型
    private String status;           // 任务状态
    private String createTime;       // 创建时间
    private String palletNo;         // 托盘号
    private String valveNo;          // 出厂编号
    private String matCode;          // 物料编码
    private String binCode;          // 库位号，与调度系统 binCode 一致
    private String locationCode;     // 库位号（兼容旧字段）

    public Task() {
    }

    public Task(String taskNo, String taskType, String status, String createTime) {
        this.taskNo = taskNo;
        this.taskType = taskType;
        this.status = status;
        this.createTime = createTime;
    }

    public String getOutID() {
        return outID != null ? outID : taskNo;
    }

    public void setOutID(String outID) {
        this.outID = outID;
        this.taskNo = outID; // 兼容旧字段
    }

    public String getTaskNo() {
        return taskNo != null ? taskNo : outID;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
        this.outID = taskNo; // 同步更新
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getPalletNo() {
        return palletNo;
    }

    public void setPalletNo(String palletNo) {
        this.palletNo = palletNo;
    }

    public String getValveNo() {
        return valveNo;
    }

    public void setValveNo(String valveNo) {
        this.valveNo = valveNo;
    }

    public String getMatCode() {
        return matCode;
    }

    public void setMatCode(String matCode) {
        this.matCode = matCode;
    }

    public String getBinCode() {
        return binCode != null ? binCode : locationCode;
    }

    public void setBinCode(String binCode) {
        this.binCode = binCode;
        this.locationCode = binCode; // 兼容旧字段
    }

    public String getLocationCode() {
        return locationCode != null ? locationCode : binCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
        this.binCode = locationCode; // 同步更新
    }

    /**
     * 获取任务类型的中文显示
     */
    public String getTaskTypeDisplay() {
        if (TYPE_INBOUND.equals(taskType)) {
            return "入库";
        } else if (TYPE_SEND_INSPECTION.equals(taskType)) {
            return "送检";
        } else if (TYPE_RETURN.equals(taskType)) {
            return "回库";
        } else if (TYPE_OUTBOUND.equals(taskType)) {
            return "出库";
        }
        return taskType;
    }

    /**
     * 获取任务状态的中文显示
     */
    public String getStatusDisplay() {
        if (STATUS_PENDING.equals(status)) {
            return "待执行";
        } else if (STATUS_EXECUTING.equals(status)) {
            return "执行中";
        } else if (STATUS_COMPLETED.equals(status)) {
            return "已完成";
        } else if (STATUS_CANCELLED.equals(status)) {
            return "已取消";
        } else if (STATUS_FAILED.equals(status)) {
            return "失败";
        }
        return status;
    }

    /**
     * 是否可以取消任务
     */
    public boolean canCancel() {
        return STATUS_PENDING.equals(status);
    }
    
    /**
     * 获取任务编号（优先使用outID）
     */
    public String getTaskId() {
        return outID != null ? outID : taskNo;
    }
}

