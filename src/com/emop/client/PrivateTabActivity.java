package com.emop.client;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import static com.emop.client.Constants.TAG_EMOP;


public class PrivateTabActivity extends BaseActivity {
	static final int LOGIN_REQUEST = 1;	
		
    protected void onResume (){
    	super.onResume();
    	
    	Log.d(TAG_EMOP, "on resume private tab....");
    	if(!client.isLogined()){
    		doLogin(null);
    	}
    }
    
    protected void onPause (){
    	super.onPause();
    }            
    
    public void doLogin(View v){
		Intent intent = new Intent();			
		intent.setClass(this, LoginActivity.class);
		this.startActivityForResult(intent, LOGIN_REQUEST);
    }
    
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
    	Log.d(TAG_EMOP, "onActivity....:" + requestCode);
    	if(requestCode == LOGIN_REQUEST){
        	if(!client.isLogined()){
        		if(this.getParent() == null){
        			finish();
        		}else {
        			Log.d(TAG_EMOP, "switch to public view.");
        			MainTabActivity tab = (MainTabActivity)this.getParent();
        			tab.activeTab(0);
        			/*
        			Log.d(Constants.TAG_FMEI, "parent:" + this.getParent().getClass().toString());
	        		Intent intent = new Intent();			
	        		intent.setClass(this, MainTabActivity.class);
	        		startActivity(intent);
	        		*/
        		}
        	}
    	}else {
    		super.onActivityResult(requestCode, resultCode, data);
    	}
    }
    
}
