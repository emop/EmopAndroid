package com.emop.client.web;

import static com.emop.client.Constants.TAG_EMOP;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.json.JSONObject;
import org.json.JSONTokener;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.emop.client.Constants;
import com.emop.client.WebLoginActivity;
import com.emop.client.WebViewActivity;
import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.taobao.top.android.TOPUtils;
import com.taobao.top.android.TopAndroidClient;
import com.taobao.top.android.auth.AccessToken;
import com.taobao.top.android.auth.AuthError;
import com.taobao.top.android.auth.AuthException;
import com.taobao.top.android.auth.AuthorizeListener;
import com.tencent.mm.sdk.platformtools.Log;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboParameters;

public class TaobaoLoginWebClient extends WebViewClient {
	
	private FmeiClient client = null; //FmeiClient.getInstance();
    private WebViewActivity web = null;
	private ProgressBar processBar = null;
    
	public TaobaoLoginWebClient(WebViewActivity web, ProgressBar processBar){
		this.web = web;
		client = FmeiClient.getInstance(web);
		this.processBar = processBar;
	}
	
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        //mSpinner.show();
        processBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        
        processBar.setVisibility(View.INVISIBLE);
    }	
	
	@Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Log.d(TAG_EMOP, "Taobao URL: " + url);
        if(url != null && url.startsWith("callback:")){
			Message msg = web.handler.obtainMessage(WebViewActivity.WEB_LOADING);
			web.handler.sendMessage(msg);        	
        	processTaobaoCallBack(url);
        	return true;
        }
        return false;
    }
	
	private void processTaobaoCallBack(String callback){
		
		
		final TopAndroidClient client = TopAndroidClient.getAndroidClientByAppKey(Constants.TAOBAO_APPID);
		Uri uri = Uri.parse(callback);
		if (uri.getScheme().equals("callback")) {

			String errorStr = uri.getQueryParameter("error");
			//AuthorizeListener listener = getAuthorizeListener();
			if (errorStr == null) {// 授权成功
			// String ret = url.substring(url.indexOf("#") + 1);
				String ret = uri.getFragment();
				String[] kv = ret.split("&");
				Bundle values = new Bundle();
				for (String each : kv) {
					String[] ss = each.split("=");
					if (ss != null && ss.length == 2) {
						values.putString(ss[0], ss[1]);
					}
				}
				final AccessToken token = TOPUtils.convertToAccessToken(values);
				// Android3.0后ui主线程中同步访问网络会有限制。
				// 使用ExecutorService.invokeAll()阻塞主线程的方式起一个线程再去调用api
				Callable<Date> task = new Callable<Date>() {
					@Override
					public Date call() throws Exception {
						Date date = client.getTime();
						return date;
					}
				};
				List<Callable<Date>> tasks = new ArrayList<Callable<Date>>();
				tasks.add(task);
				ExecutorService es = Executors.newSingleThreadExecutor();
				try {
					List<Future<Date>> results = es.invokeAll(tasks);
					Future<Date> future = results.get(0);
					token.setStartDate(future.get());
					
					client.addAccessToken(token);
				} catch (Exception e) {
					listener.onAuthException(new AuthException(e));
				}				
				listener.onComplete(token);
			} else {// 授权失败
				String errorDes = uri.getQueryParameter("error_description");
				AuthError error = new AuthError();
				error.setError(errorStr);
				error.setErrorDescription(errorDes);
				listener.onError(error);
			}
		}		
		
	}
	
	
	private AuthorizeListener listener = new AuthorizeListener(){

		@Override
		public void onComplete(AccessToken accessToken) {
			Log.d(Constants.TAG_EMOP, "callback");
			String id=accessToken.getAdditionalInformation().get(AccessToken.KEY_SUB_TAOBAO_USER_ID);
			if(id==null){
				id=accessToken.getAdditionalInformation().get(AccessToken.KEY_TAOBAO_USER_ID);
			}
			//MainActivity.this.userId=Long.parseLong(id);
			String nick=accessToken.getAdditionalInformation().get(AccessToken.KEY_SUB_TAOBAO_USER_NICK);
			if(nick==null){
				nick=accessToken.getAdditionalInformation().get(AccessToken.KEY_TAOBAO_USER_NICK);
			}
			
			Log.e(Constants.TAG_EMOP, "user id:" + id + ", nick:" + nick);
			
			String r2_expires = accessToken.getAdditionalInformation().get(
					AccessToken.KEY_R2_EXPIRES_IN);
			Date start = accessToken.getStartDate();
			Date end = new Date(start.getTime()
					+ Long.parseLong(r2_expires) * 1000L);
			if(id != null && nick != null){
				client.saveRefUser(web, Constants.AUTH_REF_TAOBAO, id, nick);
				new GetUserInfoTask(id, accessToken.getValue()).start();
			}else {
				Message msg = web.handler.obtainMessage(WebViewActivity.WEB_DONE);
				msg.obj = "查询登录用户信息错误, 请稍后重试。";
				web.handler.sendMessage(msg);
			}
		}

		@Override
		public void onError(AuthError e) {
			Log.e(Constants.TAG_EMOP, "error:" + e.getErrorDescription());
			Message msg = web.handler.obtainMessage(WebViewActivity.WEB_DONE);
			msg.obj = "网络出错，无法登陆，请稍后重试。error:" + e.getErrorDescription();
			web.handler.sendMessage(msg);
		}

		@Override
		public void onAuthException(AuthException e) {
			Log.e(Constants.TAG_EMOP, "Autha exception:" + e.getMessage());
			Message msg = web.handler.obtainMessage(WebViewActivity.WEB_DONE);
			msg.obj = "网络出错，无法登陆，请稍后重试。error:" + e.getMessage();
			web.handler.sendMessage(msg);			
		}};
    
	    class GetUserInfoTask extends Thread{
	    	private String uid = null;    	
	    	private String accessToken = null;
	    	public GetUserInfoTask(String uid, String accessToken){
	    		this.uid = uid;
	    		this.accessToken = accessToken;
	    	}
	    	
			@Override
			public void run(){
				ApiResult r = client.bindUserInfo("taobao", uid, accessToken);
				
				
				if(r != null && r.isOK){
					String status = r.getString("data.user_status");
					if(status != null && status.trim().equals("2001")){
						client.saveLoginUser(web, r.getString("data.user_id"));
					}else {
					}
					Message msg = web.handler.obtainMessage(WebViewActivity.WEB_DONE);
					web.handler.sendMessage(msg);
				}else {
					Message msg = web.handler.obtainMessage(WebViewActivity.WEB_DONE);
					msg.obj = "网络出错，无法登陆，请稍后重试。";
					web.handler.sendMessage(msg);
				}	
			}
	    } 
}
