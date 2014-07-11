package com.emop.client;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.emop.client.io.FmeiClient;
import com.emop.client.tasks.GetSinaUserInfoTask;
import com.emop.client.tasks.UpgradeCheckTask;
import com.taobao.top.android.TopAndroidClient;
import com.weibo.net.Weibo;
import com.weibo.sdk.android.sso.SsoHandler;

public class SettingActivity extends BaseActivity {
	private PackageInfo curVersion = null;
	private FmeiClient client = null;
	private SsoHandler sinaSSO = null;
	private ProgressDialog loading = null;
	
	private boolean isLoginSina = false;
	private boolean isLoginTaobao = false;	
	private boolean isLoginQQ = false;		
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    Log.d(Constants.TAG_EMOP, "on create on tab view");
	            
	    setContentView(R.layout.settings_view);
	    
        loading = new ProgressDialog(this);
        loading.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading.setMessage("登录中...");
	    
	    client = FmeiClient.getInstance(SettingActivity.this);

	    TextView version = (TextView) findViewById(R.id.setting_verison_name);
		PackageManager packageManager = getPackageManager();
		try {
			curVersion = packageManager.getPackageInfo(getPackageName(), 0);
			version.setText("当前版本是（"+curVersion.versionName+"）");
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}
	
    protected void onResume (){
    	super.onResume();
    	if(!loading.isShowing()){
	    	this.showSinaInfo();
	    	//this.showTaobaoInfo();
	    	this.showQQInfo();
    	}
    }
    
    public void doLoginFromSina(View v){
    	Log.d("tag", "doLoginFrom sina");
    	if(this.isLoginSina){
    		logoutRef(Constants.AUTH_REF_SINA);
    		Weibo.getInstance().setAccessToken(null);
    	}else {
        	sinaSSO = new SsoHandler(this, sinaHandler);
        	loading.show();
        	sinaSSO.authorize();
    	}
    }
    
    public void doLoginFromQQ(View v){
    	Log.d("tag", "doLoginFrom qq");
    	if(this.isLoginQQ){
    		logoutRef(Constants.AUTH_REF_QQ);
    	}else {
			Intent intent = new Intent();			
			intent.setClass(this, QQLoginActivity.class);
			startActivity(intent);
    	}
    }
    
	public void onFinish(View v){
		finish();
	}    
    
    public void doLoginFromTaobao(View v){
    	Log.d("tag", "doLoginFrom taobao");
    	if(this.isLoginTaobao){
    		logoutRef(Constants.AUTH_REF_TAOBAO);
    	}else {
	    	TopAndroidClient topClient = TopAndroidClient.getAndroidClientByAppKey(Constants.TAOBAO_APPID);
	    	if(topClient != null){
	    		String authLink = topClient.getAuthorizeLink();
		    	Intent intent = new Intent().setClass(this, WebViewActivity.class);
		    	Log.d(Constants.TAG_EMOP, "taobao auth link:" + authLink);
		    	intent.putExtra("http_url", authLink);
		    	intent.putExtra("title", "淘宝登陆");
		    	intent.putExtra("taobaoLogin", true);
		    	startActivity(intent);
	    	}else {
	    		this.showToast("啊哦，网速不给力啊~");
	    	}
    	}
    }    
    
    private void showSinaInfo(){
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		String userId = settings.getString(Constants.PREFS_SINA_UID, "");
		String name = null;
		TextView v = (TextView)this.findViewById(R.id.setting_sina_user);
		ImageButton btn = (ImageButton)this.findViewById(R.id.login_btn_sina);
		if(userId != null && userId.length() > 0){
			name = settings.getString(Constants.PREFS_SINA_NICK, "");
			v.setText("已绑定用户:" + name);
			isLoginSina = true;
			btn.setBackgroundResource(R.drawable.settings_logout_btn);
		}else {
			isLoginSina = false;
			v.setText("设置新浪微博登录");
			btn.setBackgroundResource(R.drawable.settings_login_btn);
		}
    }
    
    private void showQQInfo(){
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		String userId = settings.getString(Constants.PREFS_QQ_UID, "");
		String name = null;
		TextView v = (TextView)this.findViewById(R.id.setting_qq_user);
		ImageButton btn = (ImageButton)this.findViewById(R.id.login_btn_qq);
		if(userId != null && userId.length() > 0){
			name = settings.getString(Constants.PREFS_QQ_NICK, "");
			v.setText("已绑定用户:"  + name);
			isLoginQQ = true;
			btn.setBackgroundResource(R.drawable.settings_logout_btn);
		}else {
			isLoginQQ = false;
			v.setText("设置QQ账号登录");
			btn.setBackgroundResource(R.drawable.settings_login_btn);
		}
    }
    
    /*
    private void showTaobaoInfo(){
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		String userId = settings.getString(Constants.PREFS_TAOBAO_UID, "");
		String name = null;
	//	TextView v = (TextView)this.findViewById(R.id.setting_taobao_user);
	//	ImageButton btn = (ImageButton)this.findViewById(R.id.login_btn_taobao);
		if(userId != null && userId.length() > 0){
			name = settings.getString(Constants.PREFS_TAOBAO_NICK, "");
			v.setText("已绑定用户:"  + name);
			isLoginTaobao = true;
			btn.setBackgroundResource(R.drawable.settings_logout_btn);
		}else {
			isLoginTaobao = false;
			v.setText("设置淘宝账号登录");
			btn.setBackgroundResource(R.drawable.settings_login_btn);
		} 
    }
    */
	
	public void onCheckUpgradeVersion(View v){
		new UpgradeCheckTask(this, null, false).execute(this);
	}
	
	public void doLogout(View v){
		client.logout(this);
		Toast.makeText(this, "已注销", Toast.LENGTH_SHORT);
	}
	
	private void logoutRef(String ref){
		client.removeRefUser(this, ref);
		if(ref.equals(Constants.AUTH_REF_TAOBAO)){
		//	showTaobaoInfo();
		}else if(ref.equals(Constants.AUTH_REF_SINA)){
			showSinaInfo();
		}else if(ref.equals(Constants.AUTH_REF_QQ)){
			showQQInfo();
		}
	}
	

	
	public void cleanupImageCache(View v){
		client.appImgLoader.lowPirorTheadPool.execute(new Runnable(){
			public void run(){
				FmeiClient.getInstance(null).appImgLoader.cache.cleanUpDiskCache();
				showToast("图片缓存清理成功.");				
			}
		});
	}
	
	public void onCloseVersionCheck(View v){
	}
	
	public void onAppList(View v){
    	Intent intent = new Intent().setClass(this, WebViewActivity.class);
    	intent.putExtra("http_url", "http://taodianhuo.sinaapp.com/app/app_list");
    	intent.putExtra("title", "手机应用推荐");
    	
    	this.startActivity(intent);				
	}
	
	public void onMessageList(View v){
    	Intent intent = new Intent().setClass(this, WebViewActivity.class);
    	intent.putExtra("http_url", "http://taodianhuo.sinaapp.com/app/message_list");
    	intent.putExtra("title", "消息中心");
    	
    	this.startActivity(intent);				
	}	

	public void onAboutMe(View v){
    	Intent intent = new Intent().setClass(this, AboutMeActivity.class);
    	
    	this.startActivity(intent);				
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
    				Toast.makeText(SettingActivity.this, message, Toast.LENGTH_LONG).show();
    			}
    		}
    		if(msg.what == GetSinaUserInfoTask.LOGIN_DONE){
    			loading.dismiss();
    			showSinaInfo();
    		}else if(msg.what == SsoHandler.START_WEB_LOGIN || msg.what == SsoHandler.SSO_LOGIN_ERROR){
    			loading.dismiss();
    		}
    	}
    };	
	
}
