package com.emop.client.io;

import android.content.Context;

public class QQClient {
	public static final String apiRoute = "https://graph.qq.com/";
	private HttpTransport http = null;
	private Context ctx = null;
	
	public String accessToken = null;
	public String appId = null;
	public String openId = null;
	
	public QQClient(Context c){
		this.ctx = c;
		http = new HttpTransport(ctx, "", "");
	}
	
	public ApiResult userInfo(){
		//https://graph.qq.com/user/get_simple_userinfo
		String user = apiRoute + "user/get_info?format=json&access_token=" + accessToken + "&oauth_consumer_key=" + appId + "&openid=" + openId;
		
		ApiResult r = new ApiResult();
		
		http.getRequest(user, null, r);		
		
		return r;
	}
	
	public ApiResult userSimpleInfo(){
		//https://graph.qq.com/user/get_simple_userinfo
		String user = apiRoute + "user/get_simple_userinfo?format=json&access_token=" + accessToken + "&oauth_consumer_key=" + appId + "&openid=" + openId;
		
		ApiResult r = new ApiResult();
		
		http.getRequest(user, null, r);		
		
		return r;
	}
	
}
