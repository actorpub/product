package com.music.concertoplayer.entity;

/**
 * Created by chen on 2018/4/12.
 */
/*{

    "data": {

       "from": "1",

       "unionid": "0123456789",

       "openid": "0123456",

       "nickname": "lvhongbin",

       "mobile": "",

       "latitude": "000",

       "longitude": "000",

       "device_type": "设备类型_1",

       "deviceid": "设备类型_1_id_2"

       }

}*/

/*
* { "data" : {"device_type":"1","deviceid":"1","from":"1","latitude":"1","longitude":"1","mobile":"1","nickname":"1","openid":"1","unionid":"1"}}
* */
public class Data {
    private String from;
    private String unionid;
    private String openid;
    private String nickname;
    private String mobile;
    private String latitude;
    private String longitude;
    private String device_type;
    private String deviceid;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getDevice_type() {
        return device_type;
    }

    public void setDevice_type(String device_type) {
        this.device_type = device_type;
    }

    public String getDeviceid() {
        return deviceid;
    }

    public void setDeviceid(String deviceid) {
        this.deviceid = deviceid;
    }

    @Override
    public String toString() {
        return "Data{" +
                "from='" + from + '\'' +
                ", unionid='" + unionid + '\'' +
                ", openid='" + openid + '\'' +
                ", nickname='" + nickname + '\'' +
                ", mobile='" + mobile + '\'' +
                ", latitude='" + latitude + '\'' +
                ", longitude='" + longitude + '\'' +
                ", device_type='" + device_type + '\'' +
                ", deviceid='" + deviceid + '\'' +
                '}';
    }
}
