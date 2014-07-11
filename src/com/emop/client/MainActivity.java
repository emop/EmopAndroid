package com.emop.client;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.SyncStateContract.Columns;
import android.widget.TextView;
import android.widget.Toast;

import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.io.WeiBoClient;
import com.emop.client.provider.Schema;

public class MainActivity extends Activity {
	private FmeiClient client;
	private WeiBoClient weiboClient;
	private boolean isInited = false;
	private long startingTime = 0L;
	private long loadingShowTime = 2000;
	
	private static final int NETWORKING_INIT = 1001;
	private static final int FIRST_USE_ERROR = 2001;
	private static final int FIRST_USE_DONE = 2002;
	
	private static final int LAUNCH_STEP = 3001;
	private static final int LAUNCH_STEP_OK = 3002;
	private static final int LAUNCH_STEP_ERROR = 3003;
	
	private static final int LAUNCH_OK = 5001;
	private static final int LAUNCH_WITH_NETWORK_ERROR = 5002;
	private static final int OPEN_TAB_VIEW = 6001;
	
	
	private TextView statusView = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        client = FmeiClient.getInstance(this, false);
        weiboClient = new WeiBoClient(this);
        setContentView(R.layout.main);        
        
        statusView = (TextView)this.findViewById(R.id.launch_status);
        
		Uri u = Schema.TOPIC_LIST.buildUpon().appendQueryParameter("empty", "y").build();
		Cursor c = getContentResolver().query(u, new String[]{Columns._ID}, null, null, null);
		boolean isFirstUse = c.getCount() == 0;
		c.close();
		if(!isFirstUse){
			openMainTab();
		}
    }
    
    protected void onResume (){
    	super.onResume();
    	
    	/**
    	 * 如果是第一次进入，显示应用封面并且检查一些初始化数据
    	 * 1. 检查网络连接状态
    	 * 2. 检查是否第一次使用应用程序
    	 */
    	if(!isInited){
    		this.startingTime = System.currentTimeMillis();
    		new ClientLaunchTask().execute(this);
    	}else {
    		openMainTab();
    	}
    }
        
    private Handler handler = new Handler(){
    	
    	public void handleMessage(final Message msg) {
    		handleMessageInUi(msg);
    	}
    	public void handleMessageInUi(Message msg) {
    		String message = null;
    		if(msg.obj != null){
    			message = msg.obj.toString();
    			if(message != null && statusView != null){
    				statusView.setText(message);
    			}
    		}
    		
	   		switch (msg.what) {
	   		 	case NETWORKING_INIT:
	   		 		break;
	   		 	case LAUNCH_STEP:
	   		 		break;
	   		 	case LAUNCH_STEP_OK:
	   		 		break;
	   		 	case LAUNCH_STEP_ERROR:
	   		 		break;
	   		 	case FIRST_USE_ERROR:
	   		 		break;
	   		 	case FIRST_USE_DONE:
	   		 	case LAUNCH_OK:
	   		 		launchTabViewWithDelay();
	   		 		break;
	   		 	case LAUNCH_WITH_NETWORK_ERROR:
	   		 		Toast.makeText(MainActivity.this, message, 
	   		 				Toast.LENGTH_LONG).show();
	   		 		launchTabViewWithDelay();
	   		 		break;
	   		 	case OPEN_TAB_VIEW:
	   		 		openMainTab();
	   		 	default:
	   		 		break;
	   		}
    	}
    	
    	private void launchTabViewWithDelay(){
    		Message msg = this.obtainMessage(OPEN_TAB_VIEW);
			long et = System.currentTimeMillis() - startingTime;
			long delay = loadingShowTime - et;
			this.sendMessageDelayed(msg, delay > 0? delay: 0);    		
    	}
    };
    
    protected void openMainTab(){
		Intent intent = new Intent().setClass(this, MainTabActivity.class);
		this.startActivity(intent);
		this.finish();
		isInited = true;
    }
    
    class ClientLaunchTask extends AsyncTask<Activity, Void, ApiResult> {

		@Override
		protected ApiResult doInBackground(Activity... arg0) {
			boolean isFirstUse = false;
			Message msg = null;
			
			ApiResult r = null;
			//this.loadUserInfo();
			Uri u = Schema.TOPIC_LIST.buildUpon().appendQueryParameter("empty", "y").build();
			Cursor c = getContentResolver().query(u, new String[]{Columns._ID}, null, null, null);
			isFirstUse = c.getCount() == 0;
			c.close();
			if(isFirstUse){ //第一使初始化数据
				msg = handler.obtainMessage(NETWORKING_INIT);
				handler.sendMessage(msg);			
				r = client.check_networking(getApplicationContext());
				if(r.isOK){
					msg = handler.obtainMessage(LAUNCH_STEP, "网络连接成功");
					handler.sendMessage(msg);
				}else {
					msg = handler.obtainMessage(LAUNCH_STEP, "网络连接失败");
					handler.sendMessage(msg);
				}				
				msg = handler.obtainMessage(LAUNCH_STEP, "第一次使用，初始化环境");
			 	handler.sendMessage(msg);
				
				r = client.refreshDataByUri(getContentResolver(), Schema.TOPIC_LIST);
				
				if(r != null && r.isOK){
					msg = handler.obtainMessage(FIRST_USE_DONE, "初始化成功");
					handler.sendMessage(msg);
					new Thread(){
						public void run(){
							client.refreshDataByUri(getContentResolver(), Schema.HOT_CATE_LIST);
							client.refreshDataByUri(getContentResolver(), Schema.CATE_LIST);
							client.refreshActivityItemList(getContentResolver());
						}
					}.start();
				}else {
					String err = "";
					if(r != null){
						err = r.errorMsg();
					}
					msg = handler.obtainMessage(FIRST_USE_ERROR, String.format("初始化失败%s, 请检查网络后重试:", err));
					handler.sendMessage(msg);
				}
				
			}
						
			return r;
		}
		

    }
    

}