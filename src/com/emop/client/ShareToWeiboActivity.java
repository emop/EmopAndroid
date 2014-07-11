package com.emop.client;

import java.io.File;

import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.emop.client.io.ApiResult;
import com.emop.client.io.AppConfig;
import com.emop.client.tasks.GetSinaUserInfoTask;
import com.weibo.net.AccessToken;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboParameters;
import com.weibo.sdk.android.sso.SsoHandler;

public class ShareToWeiboActivity extends BaseActivity {
	private ProgressDialog loading = null;	
	private SsoHandler sinaSSO = null;
	
	private TextView message = null;
	private ImageView imageView = null;
	private String picUrl = null;
	private String link = null;
	private Weibo weibo = null;
	private int loginTimes = 0;
	private ProgressBar progress = null;
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_to_weibo);
        
        loading = new ProgressDialog(this);
        loading.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading.setMessage("登录中...");        
        
    	Intent intent = this.getIntent();
    	
    	picUrl = intent.getStringExtra("picUrl");
    	String text = intent.getStringExtra("text");
    	link = intent.getStringExtra("link");
    	
    	message = (TextView)findViewById(R.id.share_text);
    	imageView = (ImageView)findViewById(R.id.taoke_pic);
    	
		Display display = null;
        display = getWindowManager().getDefaultDisplay();  
        int disWidth = display.getWidth();
		client.tmpImgLoader.loadImage(picUrl, imageView, disWidth, true); // .cache.get(picUrl, disWidth, true,true);
        
		message.setText(text);
		weibo = Weibo.getInstance();
		weibo.setupConsumerConfig(Constants.SINA_APPID, Constants.SINA_APPKEY);
		
        progress = (ProgressBar) findViewById(R.id.progressbar_loading);
        progress.setVisibility(View.GONE);        
        if(weibo.getAccessToken() == null){
        	String accessToken = client.getSettings(Constants.PREFS_SINA_ACCESS_TOKEN);
        	String expires_in = client.getSettings(Constants.PREFS_SINA_EXPIRES_IN);        	
        	if(accessToken != null){
	        	AccessToken token = new AccessToken(accessToken, Weibo.getAppSecret());
	        	token.setExpiresIn(expires_in);
	        	weibo.setAccessToken(token);
        	}
        }
     
    }
	
	public void onResume(){
		super.onResume();
		if(loginTimes < 3 && weibo.getAccessToken() == null && !loading.isShowing()){
			loginTimes++;
	    	sinaSSO = new SsoHandler(this, sinaHandler);
	    	loading.show();
	    	sinaSSO.authorize();			
		}
	}
	
	public void onShareToWeibo(View v){
		progress.setVisibility(View.VISIBLE);
		Button btn = (Button)v;
		btn.setClickable(false);
		
		new Thread(){
			public void run(){
				WeiboParameters bundle = new WeiboParameters();
				//bundle.add("access_token", token[0]);
		        String url = Weibo.SERVER + "statuses/upload.json";

		        String weiBo = message.getText().toString();
		        String json = null;
		        ApiResult result = new ApiResult();
		        
				//Display display = getWindowManager().getDefaultDisplay();  
				//final int disWidth = display.getWidth();
				
		        File cacheFile = client.tmpImgLoader.cache.getCachedFile(picUrl, 400);
		        if(cacheFile == null || !cacheFile.isFile()){
		        	showToast("获取图片失败啦~");
		        	return;
		        }
		        bundle.add("pic", cacheFile.getAbsolutePath());
		        
		        bundle.add("status", weiBo + " " + link);
		        
		        Log.d("weibo", "start to update weibo ...");
		        try {
		        	synchronized(weibo){
		        		//bundle.add("source", Weibo.getAppKey());
		    			if(weibo.getAccessToken() != null){
		    				bundle.add("access_token", weibo.getAccessToken().getToken());
		    			}		        		
						json = weibo.request(ShareToWeiboActivity.this, url, bundle, Utility.HTTPMETHOD_POST, 
							weibo.getAccessToken());
		        	}
					Log.i("weibo", json);
					result.json = (JSONObject) new JSONTokener(json).nextValue();
					if(result.getString("idstr") != null){
						showToast("分享微博成功");
					}else {
						showToast("分享微博失败, 错误code：" + result.getString("error_code"));
					}
					String code = result.getString("error_code");
					Log.w("weibo", "error code:" + code);
					if(code != null){
						if(code.equals("21301") || code.equals("21315") || 
						   code.equals("21327") || code.equals("21327")){
							client.removeSettings(Constants.PREFS_SINA_ACCESS_TOKEN);
							weibo.setAccessToken(null);
						}
					}
				} catch (Exception e) {
					Log.e("weibo", e.toString(), e);
					showToast("分享微博失败");
					//e.printStackTrace();
				}
			}
		}.start();

		finish();
	}
	
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	if(sinaSSO != null){
    		sinaSSO.authorizeCallBack(requestCode, resultCode, data);
    	}
    }   
    
    private Handler sinaHandler = new Handler(){
    	
    	public void handleMessage(final Message msg) {
    		Log.d("emop", "message id:" + msg.what +", msg:" + msg.obj);
    		String message = null;
    		if(msg.obj != null){
    			message = msg.obj.toString();
    			if(message != null){
    				Toast.makeText(ShareToWeiboActivity.this, message, Toast.LENGTH_LONG).show();
    			}
    		}
    		if(msg.what == GetSinaUserInfoTask.LOGIN_DONE){
    			loading.dismiss();    			
    		}else if(msg.what == SsoHandler.START_WEB_LOGIN || msg.what == SsoHandler.SSO_LOGIN_ERROR){
    			loading.dismiss();
    		}
    	}
    };	    
}
