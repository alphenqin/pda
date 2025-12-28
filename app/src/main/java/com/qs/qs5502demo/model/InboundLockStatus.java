package com.qs.qs5502demo.model;

import com.google.gson.annotations.SerializedName;

/**
 * 入库锁定状态
 */
public class InboundLockStatus {

    @SerializedName("locked")
    private boolean locked;

    @SerializedName("count")
    private long count;

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
