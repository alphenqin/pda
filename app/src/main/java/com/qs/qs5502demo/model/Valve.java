package com.qs.qs5502demo.model;

import java.io.Serializable;

/**
 * 阀门信息
 */
public class Valve implements Serializable {
    private static final long serialVersionUID = 1L;
    private String valveNo;         // 阀门唯一编号（WMS 内部主键）
    private String matCode;         // 物料编码，对接 AGV 使用
    private String valveModel;     // 阀门型号
    private String vendorName;      // 厂家名称
    private String inboundDate;     // 入库日期 yyyy-MM-dd
    private String palletNo;        // 托盘号
    private String binCode;         // 库位号，与调度系统 binCode 一致
    private String locationCode;    // 库位号（兼容旧字段）
    private String valveStatus;     // 阀门状态：IN_STOCK/IN_INSPECTION/INSPECTED/OUTBOUND
    private String inspectionTargetBin; // 送检目标站点（如 F1/F2/F3/F4）

    public Valve() {
    }

    public Valve(String valveNo, String valveModel, String vendorName, String inboundDate) {
        this.valveNo = valveNo;
        this.valveModel = valveModel;
        this.vendorName = vendorName;
        this.inboundDate = inboundDate;
    }

    public String getValveNo() {
        return valveNo;
    }

    public void setValveNo(String valveNo) {
        this.valveNo = valveNo;
    }

    public String getValveModel() {
        return valveModel;
    }

    public void setValveModel(String valveModel) {
        this.valveModel = valveModel;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getInboundDate() {
        return inboundDate;
    }

    public void setInboundDate(String inboundDate) {
        this.inboundDate = inboundDate;
    }

    public String getPalletNo() {
        return palletNo;
    }

    public void setPalletNo(String palletNo) {
        this.palletNo = palletNo;
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

    public String getValveStatus() {
        return valveStatus;
    }

    public void setValveStatus(String valveStatus) {
        this.valveStatus = valveStatus;
    }

    public String getInspectionTargetBin() {
        return inspectionTargetBin;
    }

    public void setInspectionTargetBin(String inspectionTargetBin) {
        this.inspectionTargetBin = inspectionTargetBin;
    }
}

