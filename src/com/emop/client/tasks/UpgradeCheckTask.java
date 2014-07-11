package com.emop.client.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.emop.client.Constants;
import com.emop.client.R;
import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.widget.DialogBuilder;

public class UpgradeCheckTask extends AsyncTask<Activity, Void, ApiResult> {
	private static final int DOWN_UPDATE = 1;
	private static final int DOWN_OVER = 2;
	
	private Activity context;
	private Dialog versionDialog = null;
	private Dialog downloaddialog = null;

	private PackageInfo curVersion = null;
	private FmeiClient client = null;
	private TextView newVrsionInfo = null;
	private TextView updateNote = null;
	private ApiResult upgradeInfo = null;
	private String apkUrl = null;
	private ProgressBar mProgress;	
	private File localPath = null;
	private int progress = 0;
	private boolean interceptFlag = false;
	private OnClickListener callback = null;
	private boolean inBackground = false;
	
	
	public UpgradeCheckTask(Activity context, OnClickListener callback, boolean inBackground){
		init(context);
		this.context = context;
		this.callback = callback;
		this.inBackground = inBackground;
		client = FmeiClient.getInstance(null);
	}
	
	private void init(Activity context){
		versionDialog = DialogBuilder.showVersionCheck(context, listener);
		PackageManager packageManager = context.getPackageManager();
		try {
			curVersion = packageManager.getPackageInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}	
		
		newVrsionInfo = (TextView)versionDialog.findViewById(R.id.new_version_info);
		updateNote = (TextView)versionDialog.findViewById(R.id.new_version_note);
	}
	
	public void close(){
		versionDialog.dismiss();
	}

	@Override
	protected ApiResult doInBackground(Activity... arg0) {
		if(!inBackground){
			this.mHandler.post(new Runnable(){
				public void run(){
					versionDialog.show();					
				}
			});
		}
		ApiResult r = client.checkUpgradeVersion();

		return r;
	}
	
	protected OnClickListener listener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			Log.d(Constants.TAG_EMOP, "click:" + v.getId());
			versionDialog.dismiss();			
			if(v.getId() == R.id.upgrade_now){
				onUpgradeVersion(v);
			}else{
			}
			if(callback != null){
				callback.onClick(v);
			}			
		}};
	
	protected void onPostExecute(ApiResult result) {
		if(!result.isOK){
			Toast.makeText(context, result.errorMsg(), 
					Toast.LENGTH_LONG).show();
		}else{
			long ver = Integer.parseInt(result.getString("data.num_version"));
			if(ver <= curVersion.versionCode){
				newVrsionInfo.setText(String.format("已经是最新版本:%1s 。", curVersion.versionName));
				View v = versionDialog.findViewById(R.id.noNeedUpgrade);
				v.setVisibility(View.VISIBLE);
				
				v = versionDialog.findViewById(R.id.needUpgrade);
				v.setVisibility(View.GONE);
			}else {
				if(inBackground){
					versionDialog.show();
				}
				upgradeInfo = result;
				String newVersion = "有新版本：" + result.getString("data.version_name");				
				//newVersion += "\n" + result.getString("data.version_update");
				newVrsionInfo.setText(newVersion);
				newVersion = result.getString("data.version_update");
				newVersion = newVersion.replace("\r", "");
				updateNote.setVisibility(View.VISIBLE);
				updateNote.setText(newVersion);
				View v = versionDialog.findViewById(R.id.needUpgrade);
				v.setVisibility(View.VISIBLE);
				
				v = versionDialog.findViewById(R.id.noNeedUpgrade);
				v.setVisibility(View.GONE);
			}
		}
	}
	
	public void onUpgradeVersion(View v){
		versionDialog.dismiss();
		
		if(upgradeInfo != null){
			apkUrl = upgradeInfo.getString("data.download_url");
			Log.d(Constants.TAG_EMOP, "start updrage from:" + apkUrl);
			//client.upgradeNewVersion(apkUrl);
			downloaddialog = DialogBuilder.showInstallAPI(context, null);
			mProgress = (ProgressBar)downloaddialog.findViewById(R.id.progress);
			downloaddialog.show();
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state)) {
				downloadApk();
			}else {
				Log.d(Constants.TAG_EMOP, "not mount stoarge:" + state);
			}
		}
	}

    private void downloadApk(){
    	Thread downLoadThread = new Thread(mdownApkRunnable);
    	downLoadThread.start();
    }
    
    private void installApk(){
    	if(downloaddialog != null && downloaddialog.isShowing()){
    		downloaddialog.dismiss();
    	}
    	if(localPath != null && localPath.isFile()){
    		//finish();

	    	Intent i = new Intent(Intent.ACTION_VIEW);
	    	Uri path = Uri.fromFile(localPath);
	    	Log.d(Constants.TAG_EMOP, "download uri:" + path.toString());
	    	i.setDataAndType(Uri.fromFile(localPath), 
	    		"application/vnd.android.package-archive"); 
	    	context.startActivity(i);  
    		//stopSystem();
    	}
    }
    
    
    private void stopSystem(){
    	Intent intent = new Intent(Intent.ACTION_MAIN);  
        intent.addCategory(Intent.CATEGORY_HOME);  
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
        context.startActivity(intent);  
        android.os.Process.killProcess(android.os.Process.myPid());
    }    
    
    private Runnable mdownApkRunnable = new Runnable() {

		@Override
		public void run() {
			URL url = null;
			try {
				url = new URL(apkUrl);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				 conn.connect();
				 int length = conn.getContentLength();
				 InputStream is = conn.getInputStream();
				 
				 File root = new File(Environment.getExternalStorageDirectory(), "downloads"); // .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
				 if(!root.exists()){
					 if(!root.mkdirs()){
						 Log.d(Constants.TAG_EMOP, "failed to create dir:" + root.getAbsolutePath());
					 }
				 }
				 
				 localPath = new File(root, "doudougou.apk");
				 Log.d(Constants.TAG_EMOP, "download local file:" + localPath.getAbsolutePath() + ", size:" + length);
				 FileOutputStream fos = new FileOutputStream(localPath);
				 int count = 0;
				 byte buf[] = new byte[10240];
				 do{        
					 int numread = is.read(buf);
					 count += numread;
					 progress =(int)(((float)count / length) * 100);
					 mHandler.sendEmptyMessage(DOWN_UPDATE);
					 if(numread <= 0){    
						 break;
					 }
					 Log.d(Constants.TAG_EMOP, "download size:" + numread);
					 fos.write(buf,0,numread);
				 }while(!interceptFlag);//点击取消就停止下载.
				 fos.close();
				 is.close();
				 mHandler.sendEmptyMessage(DOWN_OVER);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}    
    	
    }; 
    
    private Handler mHandler = new Handler(){
    	public void handleMessage(Message msg) {
    		 switch (msg.what) {
    		 	case DOWN_UPDATE:
    		 		mProgress.setProgress(progress);
    		 		break;
    		 	case DOWN_OVER:
    		 		downloaddialog.dismiss();
    		 		installApk();
    		 		break;
    		 	default:
    		 		break;
    		 }
    	}
    	
    };    
}