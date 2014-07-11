package com.emop.client;

import static com.emop.client.Constants.TAG_EMOP;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.emop.client.io.ApiResult;
import com.emop.client.io.QQClient;
import com.tencent.tauth.TencentOpenAPI;
import com.tencent.tauth.bean.UserInfo;
import com.tencent.tauth.http.Callback;

public class QQLoginActivity extends BaseActivity {
	private static final String CALLBACK = "taodianhuoqqauth://tauth.qq.com/";
	
	public String mAppid = "100366010";//申请时分配的appid
	public String mAccessToken, mOpenId;

	private String scope = "get_simple_userinfo,get_user_profile,add_share,get_info";//授权范围
	private boolean isLogin = false;
	private ProgressBar processBar = null;	
	private TextView titleView = null;
	private WebView web = null;
	private Bundle authInfo = new Bundle();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        processBar = (ProgressBar)findViewById(R.id.progressbar_loading);     
        processBar.setVisibility(View.GONE);
        titleView = (TextView)findViewById(R.id.title);
        titleView.setText("QQ登录");
        
        this.web = (WebView)findViewById(R.id.web);
        web.setWebViewClient(new QQWebClient());
        
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        
        CookieSyncManager.createInstance(this);        
        
        WebView.enablePlatformNotifications();
        //registerIntentReceivers();
    }
    
    public void onResume(){
    	super.onResume();
    	String url = "https://graph.qq.com/oauth2.0/authorize?response_type=token&display=mobile&client_id=" + mAppid+ "&redirect_uri=" + CALLBACK + "&scope=" + scope;
    	
    	isLogin = false;
    	web.loadUrl(url);
    	
    }
    
    
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }

	
	class QQWebClient extends WebViewClient {
        
		@Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG_EMOP, "QQ onPage: " + url);   
            if(url.startsWith("http://qzs.qq.com/open/mobile/")){
        		processLoginResult(url);
        	}else {            
        		super.onPageStarted(view, url, favicon);
        		processBar.setVisibility(View.VISIBLE);
        	}
        }
		
		@Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        Log.d(TAG_EMOP, "shouldOverride URL: " + url);
	        if(url != null){
	        	if(url.startsWith("taodianhuoqqauth:")){
	        		//finish();
	        		return true;
	        	}else if(url.startsWith("http://qzs.qq.com/open/mobile/")){
	        		processLoginResult(url);
	        	}
	        }
	        return false;
	    }
		
        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG_EMOP, "onPageFinished URL: " + url);
            super.onPageFinished(view, url);
            
            processBar.setVisibility(View.GONE);
        }
	}
	
	private void processLoginResult(String url){
		processBar.setVisibility(View.VISIBLE);

		Log.d(TAG_EMOP, "processLoginResult URL: " + url);
		//http://qzs.qq.com/open/mobile/login/proxy.html?#&openid=0676BB8860070788A73655A5554D6347&appid=100366010&access_token=5C30FF074AA3BFC27EAFF5745155814D&key=9b84ea6e58a2ab7a8080bb8af725530c&expires_in=7776000
		Uri uri = Uri.parse(url);
		String ret = uri.getFragment();
		if(ret == null) return; 
		String[] kv = ret.split("&");
		
		for (String each : kv) {
			String[] ss = each.split("=");
			if (ss != null && ss.length == 2) {
				authInfo.putString(ss[0], ss[1]);
			}
		}
		Log.d(Constants.TAG_EMOP, "qq ok, open id:" + authInfo.getString("openid"));
		Log.d(Constants.TAG_EMOP, "qq ok, access_token:" + authInfo.getString("access_token"));
		mOpenId = authInfo.getString("openid");
		mAccessToken = authInfo.getString("access_token");
		
		if(!isLogin && mOpenId != null && mAccessToken != null){
			isLogin = true;
			new GetUserInfoTask(mOpenId, mAccessToken).start();
		}
		
	}
	    
    class GetUserInfoTask extends Thread{
    	private String uid = null;    	
    	private String accessToken = null;
    	public GetUserInfoTask(String uid, String accessToken){
    		this.uid = uid;
    		this.accessToken = accessToken;
    	}
    	
		@Override
		public void run(){
			QQClient qq = new QQClient(QQLoginActivity.this);
			qq.appId = mAppid;
			qq.openId = mOpenId;
			qq.accessToken = mAccessToken;
			
			ApiResult qqResult = qq.userInfo();	
			String nick = qqResult.getString("data.nick");
			
			if(nick == null){
				qqResult = qq.userSimpleInfo();
				nick = qqResult.getString("nickname");
			}
			if(nick == null){
				nick = "unkown";
			}
			
			Log.d(Constants.TAG_EMOP, "用户信息:" + nick);
			client.saveRefUser(QQLoginActivity.this, Constants.AUTH_REF_QQ, 
					mOpenId, 
					nick);
			
			
			ApiResult r = client.bindUserInfo("qq", uid, accessToken);
			
			
			if(r != null && r.isOK){
				String status = r.getString("data.user_status");
				if(status != null && status.trim().equals("2001")){
					client.saveLoginUser(QQLoginActivity.this, r.getString("data.user_id"));
				}else {
				}
			}else {
				showToast("啊哦，网速不给力啊~" + r.errorMsg());
			}
			finish();
		}
    }     
}
