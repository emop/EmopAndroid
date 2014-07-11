package com.emop.client.tasks;

import static com.emop.client.Constants.TAG_EMOP;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.emop.client.Constants;
import com.emop.client.RegisterActivity;
import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.io.WeiBoClient;
import com.emop.client.io.WeiboUser;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboParameters;

public class GetSinaUserInfoTask extends Thread{
	public final static int LOGIN_DONE = 1;
	public final static int LOGIN_MSG = 2;

	
	private String accessToken = null;  
	private String uid = null;
	private Activity context = null;
	private Handler handler = null;
	public GetSinaUserInfoTask(String accessToken, String uid, Activity context, Handler handler){
		this.accessToken = accessToken;
		this.uid = uid;
		this.context = context;
		this.handler = handler;
	}
	
	@Override
	public void run(){
		FmeiClient client = FmeiClient.getInstance(null);
		Weibo weibo = Weibo.getInstance();
		WeiboParameters bundle = new WeiboParameters();
		bundle.add("access_token", accessToken);
		String url = Weibo.SERVER + "oauth2/get_token_info";
		String json = null;
		JSONObject user = null;
		boolean isNew = true;
		ApiResult r = null;
		try {
			Log.d(TAG_EMOP, "start get weibo id:" + accessToken + ", uid:" + uid);
			if(uid == null){
				json = weibo.request(context, url, bundle, Utility.HTTPMETHOD_POST, 
						weibo.getAccessToken());
				user = (JSONObject) new JSONTokener(json).nextValue();
				if(user != null && user.has("uid")){
					uid = user.getString("uid");
					Log.d("tag", "get user id:" + uid);
				}
			}
			
			r = client.bindUserInfo("sina", uid, weibo.getAccessToken().getToken());
			if(uid != null){
				WeiBoClient weiboClient = new WeiBoClient(context);
				WeiboUser weiboUser = weiboClient.getProfile(uid);
				if(weiboUser != null){
					client.saveRefUser(context, Constants.AUTH_REF_SINA, uid, weiboUser.getString("screen_name"));
				}
			}
		} catch (WeiboException e) {
			Log.d("tag", "msg:" + e.getMessage() + ", code:" + e.getStatusCode());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if(r != null && r.isOK){
			String status = r.getString("data.user_status");
			if(status != null && status.trim().equals("2001")){
				//rememberUserID(r.getString("oauth_token"), r.getString("data.user_id"), accessToken, uid);
				client.saveLoginUser(context, r.getString("data.user_id"));
			}else {
				startRegisterNew(accessToken, uid, r.getJSONObject("data"));
			}				
			Message msg = handler.obtainMessage(LOGIN_DONE);
			handler.sendMessage(msg);
		}else {
			Message msg = handler.obtainMessage(LOGIN_DONE);
			msg.obj = "啊哦，网速不给力啊~";
			handler.sendMessage(msg);
			
		}
		
	}
	
	private void startRegisterNew(String token, String sinaId, JSONObject obj){
    	Log.d("tag", "do Register, uid:" + sinaId);
		Intent intent = new Intent();
		
		intent.putExtra(Weibo.TOKEN, token);
		intent.putExtra("uid", sinaId);
		try {
			intent.putExtra("userId", obj.getString("user_id"));
		} catch (JSONException e) {
		}

		intent.setClass(context, RegisterActivity.class);
		context.startActivity(intent);		
	}	


}