package com.qs.qs5502demo.model;

import java.io.Serializable;

/**
 * 托盘信息
 */
public class Pallet implements Serializable {
    private static final long serialVersionUID = 1L;
    private String palletNo;        // 托盘编号，如 "11-01"
    private String palletType;      // 托盘型号：SMALL / LARGE
    private String swapStation;     // 置换区站点，如 "WAREHOUSE_SWAP_1"
    private String binCode;         // 库位号，与调度系统 binCode 一致，如 "2-01"
    private String locationCode;    // 库位号（兼容旧字段），如 "2-01"
    private String outsideSite;     // 库外站点，如 "Z1-装卸点"

    public Pallet() {
    }

    public Pallet(String palletNo, String palletType, String swapStation, String locationCode) {
        this.palletNo = palletNo;
        this.palletType = palletType;
        this.swapStation = swapStation;
        this.locationCode = locationCode;
    }

    public String getPalletNo() {
        return palletNo;
    }

    public void setPalletNo(String palletNo) {
        this.palletNo = palletNo;
    }

    public String getPalletType() {
        return palletType;
    }

    public void setPalletType(String palletType) {
        this.palletType = palletType;
    }

    public String getSwapStation() {
        return swapStation;
    }

    public void setSwapStation(String swapStation) {
        this.swapStation = swapStation;
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

    public String getOutsideSite() {
        return outsideSite;
    }

    public void setOutsideSite(String outsideSite) {
        this.outsideSite = outsideSite;
    }
}

