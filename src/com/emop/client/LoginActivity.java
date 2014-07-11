package com.emop.client;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.emop.client.io.ApiResult;
import com.emop.client.tasks.GetSinaUserInfoTask;
import com.taobao.top.android.TopAndroidClient;
import com.weibo.sdk.android.sso.SsoHandler;



public class LoginActivity extends BaseActivity {
	private static final int SINA_LOGIN_CODE = 1;
	private ProgressDialog loading = null;
	
	private SsoHandler sinaSSO = null;
	
	private EditText email = null;
	private EditText password = null;	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_view);
                
        loading = new ProgressDialog(this);
        loading.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading.setMessage("登录中...");
        
        email = (EditText)findViewById(R.id.login_email_text);
        password = (EditText)findViewById(R.id.login_passward_text);        
    }
    
    protected void onResume (){
    	super.onResume();
    	if(client.isLogined() && !loading.isShowing()){
    		Toast.makeText(this, "登录成功", Toast.LENGTH_LONG).show();
    		finish();
    	}
    }
    
    public void doLogin(View v){
    	
		loading.show();
		new LoginRequest().execute();
    }
    
    
    public void doLoginFromSina(View v){
    	Log.d("tag", "doLoginFromsina");
    	/*
		Intent intent = new Intent();			
		intent.setClass(this, WebLoginActivity.class);
		startActivity(intent);
		*/    	
    	sinaSSO = new SsoHandler(this, handler);
    	loading.show();
    	sinaSSO.authorize();
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	if(sinaSSO != null){
    		sinaSSO.authorizeCallBack(requestCode, resultCode, data);
    	}
    }
    
    public void doLoginFromQQ(View v){
    	Log.d("tag", "doLoginFromqq");
		Intent intent = new Intent();			
		intent.setClass(this, QQLoginActivity.class);
		startActivity(intent);  
    }
    
	public void onFinish(View v){
		finish();
	}    
    
    public void doLoginFromTaobao(View v){
    	Log.d("tag", "doLoginFrom taobao");
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
    		this.showToast("加载失败");
    	}
    }    
    
    public void doRegister(View v){
    	Log.d("tag", "do Register");
		Intent intent = new Intent();			
		intent.setClass(this, RegisterActivity.class);
		startActivity(intent); 
		finish();
    }
    
    
    public void doLogout(View v){
		//Log.d("tag", "sina key:" + client.config.sinaKey);
    }    
		
	private class LoginRequest extends AsyncTask<Void, Void, ApiResult> {

        protected void onPostExecute(ApiResult u) {
        	loading.dismiss();
        	if(u != null && u.isOK){
        		client.saveLoginUser(LoginActivity.this, u.getString("data.user_id"));
        		finish();
        	}else if(u != null){
        		Toast.makeText(LoginActivity.this, u.errorMsg(), Toast.LENGTH_LONG).show();
        	}
        }
        
		@Override
		protected ApiResult doInBackground(Void... arg0) {
			String emailStr = email.getText().toString();
			String passwordStr = password.getText().toString();
			
			ApiResult r = client.login(emailStr, passwordStr);
			return r;
		}		
	}
	
    private Handler handler = new Handler(){
    	
    	public void handleMessage(final Message msg) {
    		String message = null;
    		if(msg.obj != null){
    			message = msg.obj.toString();
    			if(message != null){
    				Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
    			}
    		}
    		if(msg.what == GetSinaUserInfoTask.LOGIN_DONE){
    			finish();
    		}else if(msg.what == SsoHandler.START_WEB_LOGIN || msg.what == SsoHandler.SSO_LOGIN_ERROR){
    			loading.dismiss();
    		}
    	}
    };	
	
	

}
