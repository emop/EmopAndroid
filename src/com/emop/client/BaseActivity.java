package com.emop.client;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.baidu.mobstat.StatService;
import com.emop.client.io.FmeiClient;

public class BaseActivity extends FragmentActivity {
	static final int LOGIN_REQUEST = 1;	
	static final int OPEN_TAOBAO = 2;	

	private long lastPressback = 0;
	protected final int TOAST_MSG = 2;
	protected FmeiClient client = null;
	protected boolean isRunning = false;
	protected ProgressDialog loading = null;
	
	//处理一个蛋疼的问题。回复Tab状态和
	protected int tabIndex = -1;
	
    protected Handler handler = new Handler(){
    	
    	public void handleMessage(final Message msg) {
    		String message = null;
    		if(msg.what == TOAST_MSG && msg.obj != null){
    			message = msg.obj.toString();
    			if(message != null){
    				Toast.makeText(BaseActivity.this, message, Toast.LENGTH_SHORT).show();
    			}
    		}
    	}

    };
    
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
        client = FmeiClient.getInstance(this, false);         
	}
	
    protected void onResume (){
    	super.onResume();
    	StatService.onResume(this);
    	isRunning = true;
    	client.appImgLoader.clear();
    	MainTabActivity tab = (MainTabActivity)this.getParent();    	
    	if(tab != null){
    		if(tabIndex < 0){
    			tabIndex = tab.tabHost.getCurrentTab();
    			Log.d("emop", "save cur tab index:"  + tabIndex);
    		}else {
    			Log.d("emop", "update tab to:"  + tabIndex);
    			tab.activeTab(tabIndex);
    		}
    	}    	
    }
    
    protected void onPause (){
    	super.onPause();
    	StatService.onPause(this);
    	isRunning = false;
    }
    
    protected void showToast(String msg){
    	Message obj = handler.obtainMessage(TOAST_MSG, msg);
    	handler.sendMessage(obj);
    }
    
    public void onBackPressed() {
    	Log.d(com.emop.client.Constants.TAG_EMOP, "on back pressed  on base view");
    	
    	if(this.getParent() == null){
    		finish();
    	}else { // in Tab
        	if(System.currentTimeMillis() - this.lastPressback > 3000){
        		this.lastPressback = System.currentTimeMillis(); 
        		Toast.makeText(this,"再按一次退出!", Toast.LENGTH_SHORT).show();
        	} else {
        		finish();
        	}
    	}
    	
		return;
    }
    
    public void doFinish(View v){
    	onBackPressed();
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
        	//Log.d(com.taidian.lookmash.Constants.TAG_FMEI, "on key pressed  on base view");
        	onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public void doLogin(View v){
		Intent intent = new Intent();			
		intent.setClass(this, LoginActivity.class);
		this.startActivityForResult(intent, LOGIN_REQUEST);    	
    }
    
	public void onFinish(View v){
		finish();
	}
	
	public void startActivityView(int viewId, Uri data, Bundle param){
		Intent intent = new Intent();			
		intent.setClass(this, MutilFragmentActivity.class);	
		
		intent.putExtra("view_id", new int[]{viewId});
		if(param != null){
			intent.putExtras(param);
		}
		if(data != null){
			intent.setData(data);
		}
		
		startActivity(intent);	
	}
	
	public void showLoading(String msg){
    	if(loading == null){
            loading = new ProgressDialog(this);
            loading.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	}
    	loading.setMessage(msg);    		
    	loading.show();		
	}
	
	public void closeLoading(){
		if(loading != null && loading.isShowing()){
			loading.cancel();
		}
	}
	
	public boolean isLoading(){
		return loading != null && loading.isShowing();
	}
}
