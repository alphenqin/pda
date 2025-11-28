package com.qs.qs5502demo.model;

import java.io.Serializable;

/**
 * 阀门信息
 */
public class Valve implements Serializable {
    private static final long serialVersionUID = 1L;
    private String valveNo;         // 阀门编号，唯一标识
    private String valveModel;     // 阀门型号
    private String vendorName;      // 厂家名称
    private String inboundDate;     // 入库日期
    private String palletNo;        // 托盘号
    private String locationCode;    // 库位号

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

    public String getLocationCode() {
        return locationCode;
    }

    public void setLocationCode(String locationCode) {
        this.locationCode = locationCode;
    }
}

