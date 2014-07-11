package com.emop.client;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.emop.client.QQLoginActivity.GetUserInfoTask;
import com.emop.client.io.AppConfig;
import com.emop.client.io.WeiboLoginService;
import com.emop.client.tasks.GetSinaUserInfoTask;
import com.weibo.net.AccessToken;
import com.weibo.net.DialogError;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;

public class WebLoginActivity extends BaseActivity {
	private WebView web = null;
	private WeiboLoginService sinaService = null;
	private SinaWeiboDialogListener loginListener = new SinaWeiboDialogListener();
	private ProgressBar processBar = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        
        this.web = (WebView)findViewById(R.id.web);
        this.processBar = (ProgressBar)findViewById(R.id.progressbar_loading);
        
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.getSettings().setJavaScriptEnabled(true);
        
        TextView t = (TextView)findViewById(R.id.title);
        t.setText("新浪登录");
    }
	
    protected void onResume (){
    	super.onResume();
    	
    	processBar.setVisibility(View.VISIBLE);
    	Weibo mWeibo = Weibo.getInstance();
		mWeibo.setupConsumerConfig(Constants.SINA_APPID, Constants.SINA_APPKEY);		
		mWeibo.setRedirectUrl(Constants.SINA_CALLBACK);
    	
    	sinaService = new WeiboLoginService(this, web, Weibo.getInstance(), loginListener, processBar);
    	sinaService.startLogin();    			
    	/*
    	new Thread(){
    		public void run(){
    			AppConfig cfg = client.config();
    			Weibo weibo = Weibo.getInstance();
    			String key = cfg.getString(AppConfig.SINA_APP_KEY);
    			String secret = cfg.getString(AppConfig.SINA_APP_SECRET);
    			String callback = cfg.getString(AppConfig.SINA_APP_CALLBACK);
    			if(key != null && key.length() > 0 && secret != null && secret.length() > 0){
	    			weibo.setupConsumerConfig(key, secret);		
	    			weibo.setRedirectUrl(callback);
	    	    	sinaService.startLogin();    			
    			}else {
    				Message msg = handler.obtainMessage(GetSinaUserInfoTask.LOGIN_MSG, "应用配置信息更新错误。");
    			}
    		}
    	}.start();
    	*/
    }
    
	/**
	 * 登录完成。
	 */
	private void doneLogin(){
		finish();		
	}
	
    
    class SinaWeiboDialogListener implements WeiboDialogListener{

		@Override
		public void onComplete(Bundle values) {
			String token = values.getString("access_token");
			String expires_in = values.getString("expires_in");
			
			AccessToken accessToken = new AccessToken(token, Weibo.getAppSecret());
			accessToken.setExpiresIn(expires_in);
			Weibo.getInstance().setAccessToken(accessToken);
			
			client.saveSettings(Constants.PREFS_SINA_ACCESS_TOKEN, token);
			client.saveSettings(Constants.PREFS_SINA_EXPIRES_IN, expires_in);			
			
			Log.d("tag", "done, token:" + token + ", expires_in:" + expires_in);
			/**
			 * 如果用户已经登录，不需要再次绑定用户信息。只是刷新一下围脖授权。
			 */
			
			new GetSinaUserInfoTask(token, values.getString("uid"), WebLoginActivity.this, handler).start();
			//new GetUserInfoTask(token, values.getString("uid")).start();
		}

		@Override
		public void onWeiboException(WeiboException e) {
			// TODO Auto-generated method stub
			Log.d("tag", "onWeiboException" );
		}

		@Override
		public void onError(DialogError e) {
			Log.d("tag", "onError:" + e.toString());
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onCancel() {
			Log.d("tag", "onCancel" );
			// TODO Auto-generated method stub
			
		}
    	
    }
    
    private Handler handler = new Handler(){
    	
    	public void handleMessage(final Message msg) {
    		String message = null;
    		if(msg.obj != null){
    			message = msg.obj.toString();
    			if(message != null){
    				Toast.makeText(WebLoginActivity.this, message, Toast.LENGTH_LONG).show();
    			}
    		}
    		if(msg.what == GetSinaUserInfoTask.LOGIN_DONE){
    			finish();
    		}
    	}

    };
    
   
    
 
    
}
