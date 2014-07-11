package com.emop.client;

import com.baidu.mobstat.StatService;
import com.emop.client.io.FmeiClient;
import com.igexin.slavesdk.MessageManager;
import com.taobao.top.android.TopAndroidClient;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import android.app.Application;

/**
 * 淘点货应用程序入口。所有第三方插件相关的初始化都可以放到onCreate里面。
 * @author deonwu
 *
 */
public class TaodianhuoApplication extends Application {
	private IWXAPI api;
	
	@Override  
	public void onCreate() {  
		super.onCreate();
		
	    //注册个推服务。
	    MessageManager.getInstance().initialize(this);
        
	    //注册微信服务。
        api = WXAPIFactory.createWXAPI(this, com.emop.client.wxapi.Constants.APP_ID, false);
        api.registerApp(Constants.APP_ID);
		
		TopAndroidClient.registerAndroidClient(getApplicationContext(), Constants.TAOBAO_APPID, 
				Constants.TAOBAO_APPKEY, 
				"callback://authresult");
		
		FmeiClient client = FmeiClient.getInstance(this, false);
		client.updateLocalTrackId();
		StatService.setAppChannel("TID_" + client.trackUserId);
	}

}
