package com.emop.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.emop.client.tasks.ClientDataRefresh;
import com.igexin.sdk.Consts;

public class GexinSdkMsgReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context ctx, Intent intent) {
		Bundle bundle = intent.getExtras();
		Log.d("GexinSdkDemo", "onReceive() action=" + bundle.getInt("action"));
		switch (bundle.getInt(Consts.CMD_ACTION)) {

		case Consts.GET_MSG_DATA:
			// 获取透传数据
			// String appid = bundle.getString("appid");
			byte[] payload = bundle.getByteArray("payload");

			if (payload != null) {
				String data = new String(payload);
				if(data != null && data.equals("update_data")){
					new ClientDataRefresh(ctx, null).start();
				}

				Log.d("GexinSdkDemo", "Got Payload:" + data);
			}
			break;
		case Consts.GET_CLIENTID:
			// 获取ClientID(CID)
			String cid = bundle.getString("clientid");
			Log.d("GexinSdkDemo", "clientid:" + cid);

			// TODO:
			// 第三方应用需要将CID上传到第三方服务器，并且将当前用户帐号和CID进行关联，以便日后通过用户帐号查找CID进行消息推送

			break;

		case Consts.BIND_CELL_STATUS:
			String cell = bundle.getString("cell");

			Log.d("GexinSdkDemo", "BIND_CELL_STATUS:" + cell);
			break;
		default:
			break;

		}
		
	}

}
