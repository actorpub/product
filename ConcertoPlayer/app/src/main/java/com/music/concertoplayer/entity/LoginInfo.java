package com.music.concertoplayer.entity;

/**
 * Created by chen on 2018/4/12.
 */

/*{"result":0,"expiry":"20180506"} */
public class LoginInfo {
    private int result;
    private String expiry;

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }
}
