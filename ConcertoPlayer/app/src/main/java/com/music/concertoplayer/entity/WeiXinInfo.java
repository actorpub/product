package com.music.concertoplayer.entity;

/**
 * @author ansen
 * @create time 2017-09-14
 */
public class WeiXinInfo {
    private String openid;//
    private String headimgurl;//用户头像URL
    private String nickname="";
    private int age;
    private String unionid;// 只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段。

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public String getHeadimgurl() {
        return headimgurl;
    }

    public void setHeadimgurl(String headimgurl) {
        this.headimgurl = headimgurl;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    @Override
    public String toString() {
        return "WeiXinInfo{" +
                "openid='" + openid + '\'' +
                ", headimgurl='" + headimgurl + '\'' +
                ", nickname='" + nickname + '\'' +
                ", age=" + age +
                ", unionid='" + unionid + '\'' +
                '}';
    }
}
