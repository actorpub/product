package com.music.concertoplayer.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.music.concertoplayer.App;
import com.music.concertoplayer.Constant;
import com.music.concertoplayer.entity.WeiXin;
import com.music.concertoplayer.utils.RxBus;
import com.orhanobut.logger.Logger;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;


/**
 * 微信登陆分享回调Activity
 * @author 安辉
 * @create time 2015-05-25
 */
public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    private IWXAPI wxAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wxAPI = App.getWxAPI();
        wxAPI.handleIntent(getIntent(), this);
    }


    @Override
    public void onReq(BaseReq arg0) {
        Log.i("chen","WXEntryActivity onReq:"+arg0);
    }

    @Override
    public void onResp(BaseResp resp){
        Logger.d("onResp " + resp.errCode);
        switch (resp.errCode) {
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                break;
            case BaseResp.ErrCode.ERR_OK:
                if(resp.getType()==ConstantsAPI.COMMAND_SENDAUTH){//登陆
                    Log.i("chen", "微信登录操作.....");
                    SendAuth.Resp authResp = (SendAuth.Resp) resp;
                    WeiXin weiXin=new WeiXin(1,resp.errCode,authResp.code);
                    RxBus.getDefault().post(weiXin);
                }
                finish();
                break;
        }

    }
}
