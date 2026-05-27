package com.qs.qs5502demo.model;

import java.io.Serializable;

public class OutsideEmptyPallet implements Serializable {
    private Long id;
    private String stationCode;
    private String targetBinCode;
    private String palletType;
    private String palletNo;
    private String sourceTaskNo;
    private String sourceType;
    private String status;
    private String returnTaskNo;
    private String errorMsg;
    private String createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getTargetBinCode() {
        return targetBinCode;
    }

    public void setTargetBinCode(String targetBinCode) {
        this.targetBinCode = targetBinCode;
    }

    public String getPalletType() {
        return palletType;
    }

    public void setPalletType(String palletType) {
        this.palletType = palletType;
    }

    public String getPalletNo() {
        return palletNo;
    }

    public void setPalletNo(String palletNo) {
        this.palletNo = palletNo;
    }

    public String getSourceTaskNo() {
        return sourceTaskNo;
    }

    public void setSourceTaskNo(String sourceTaskNo) {
        this.sourceTaskNo = sourceTaskNo;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReturnTaskNo() {
        return returnTaskNo;
    }

    public void setReturnTaskNo(String returnTaskNo) {
        this.returnTaskNo = returnTaskNo;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
