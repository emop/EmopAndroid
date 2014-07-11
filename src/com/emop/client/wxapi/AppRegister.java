package com.emop.client.wxapi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.tencent.mm.sdk.openapi.BaseReq;
import com.tencent.mm.sdk.openapi.BaseResp;
import com.tencent.mm.sdk.openapi.ConstantsAPI;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.platformtools.Log;

public class AppRegister extends BroadcastReceiver {
	private Context context = null;	
	
	@Override
	public void onReceive(Context context, Intent data) {
		this.context = context;
		final IWXAPI api = WXAPIFactory.createWXAPI(context, null, false);
		// 将该app注册到微信
		api.registerApp(Constants.APP_ID);
	}


	
}
