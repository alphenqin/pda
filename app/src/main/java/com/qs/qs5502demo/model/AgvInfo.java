package com.qs.qs5502demo.model;

import com.google.gson.annotations.SerializedName;

/**
 * AGV信息
 */
public class AgvInfo {

    @SerializedName("agvCode")
    private String agvCode;

    @SerializedName("agvState")
    private String agvState;

    @SerializedName("agvEle")
    private String agvEle;

    @SerializedName("agvAngle")
    private String agvAngle;

    @SerializedName("x")
    private String x;

    @SerializedName("y")
    private String y;

    @SerializedName("height")
    private String height;

    @SerializedName(value = "isGoods", alternate = {"palletDetection"})
    private String isGoods;

    @SerializedName("direction")
    private String direction;

    @SerializedName("forkTiltAngle")
    private String forkTiltAngle;

    @SerializedName("forkRollover")
    private String forkRollover;

    @SerializedName("forkLRValue")
    private String forkLRValue;

    @SerializedName("forkHBValue")
    private String forkHBValue;

    @SerializedName("curOutID")
    private String curOutID;

    @SerializedName("errMes")
    private String errMes;

    public String getAgvCode() {
        return agvCode;
    }

    public void setAgvCode(String agvCode) {
        this.agvCode = agvCode;
    }

    public String getAgvState() {
        return agvState;
    }

    public void setAgvState(String agvState) {
        this.agvState = agvState;
    }

    public String getAgvEle() {
        return agvEle;
    }

    public void setAgvEle(String agvEle) {
        this.agvEle = agvEle;
    }

    public String getAgvAngle() {
        return agvAngle;
    }

    public void setAgvAngle(String agvAngle) {
        this.agvAngle = agvAngle;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getIsGoods() {
        return isGoods;
    }

    public void setIsGoods(String isGoods) {
        this.isGoods = isGoods;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getForkTiltAngle() {
        return forkTiltAngle;
    }

    public void setForkTiltAngle(String forkTiltAngle) {
        this.forkTiltAngle = forkTiltAngle;
    }

    public String getForkRollover() {
        return forkRollover;
    }

    public void setForkRollover(String forkRollover) {
        this.forkRollover = forkRollover;
    }

    public String getForkLRValue() {
        return forkLRValue;
    }

    public void setForkLRValue(String forkLRValue) {
        this.forkLRValue = forkLRValue;
    }

    public String getForkHBValue() {
        return forkHBValue;
    }

    public void setForkHBValue(String forkHBValue) {
        this.forkHBValue = forkHBValue;
    }

    public String getCurOutID() {
        return curOutID;
    }

    public void setCurOutID(String curOutID) {
        this.curOutID = curOutID;
    }

    public String getErrMes() {
        return errMes;
    }

    public void setErrMes(String errMes) {
        this.errMes = errMes;
    }
}
