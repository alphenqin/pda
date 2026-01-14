package com.qs.qs5502demo.model;

public class TaskLockStatus {
    private boolean inboundLocked;
    private boolean inspectionLocked;
    private boolean inspectionEmptyReturnLocked;
    private boolean returnCallLocked;
    private boolean returnValveLocked;
    private boolean outboundLocked;
    private boolean outboundEmptyReturnLocked;

    public boolean isInboundLocked() {
        return inboundLocked;
    }

    public void setInboundLocked(boolean inboundLocked) {
        this.inboundLocked = inboundLocked;
    }

    public boolean isInspectionLocked() {
        return inspectionLocked;
    }

    public void setInspectionLocked(boolean inspectionLocked) {
        this.inspectionLocked = inspectionLocked;
    }

    public boolean isInspectionEmptyReturnLocked() {
        return inspectionEmptyReturnLocked;
    }

    public void setInspectionEmptyReturnLocked(boolean inspectionEmptyReturnLocked) {
        this.inspectionEmptyReturnLocked = inspectionEmptyReturnLocked;
    }

    public boolean isReturnCallLocked() {
        return returnCallLocked;
    }

    public void setReturnCallLocked(boolean returnCallLocked) {
        this.returnCallLocked = returnCallLocked;
    }

    public boolean isReturnValveLocked() {
        return returnValveLocked;
    }

    public void setReturnValveLocked(boolean returnValveLocked) {
        this.returnValveLocked = returnValveLocked;
    }

    public boolean isOutboundLocked() {
        return outboundLocked;
    }

    public void setOutboundLocked(boolean outboundLocked) {
        this.outboundLocked = outboundLocked;
    }

    public boolean isOutboundEmptyReturnLocked() {
        return outboundEmptyReturnLocked;
    }

    public void setOutboundEmptyReturnLocked(boolean outboundEmptyReturnLocked) {
        this.outboundEmptyReturnLocked = outboundEmptyReturnLocked;
    }
}
