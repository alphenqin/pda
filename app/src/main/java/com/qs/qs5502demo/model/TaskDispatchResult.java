package com.qs.qs5502demo.model;

import com.google.gson.annotations.SerializedName;

/**
 * 任务下发结果
 */
public class TaskDispatchResult {

    @SerializedName("outID")
    private String outID;

    @SerializedName("taskType")
    private String taskType;

    @SerializedName("status")
    private String status;

    @SerializedName("toBinCode")
    private String toBinCode;

    public String getOutID() {
        return outID;
    }

    public void setOutID(String outID) {
        this.outID = outID;
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

    public String getToBinCode() {
        return toBinCode;
    }

    public void setToBinCode(String toBinCode) {
        this.toBinCode = toBinCode;
    }
}
