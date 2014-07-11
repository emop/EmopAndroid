package com.emop.client.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.SyncStateContract.Columns;
import android.util.Log;
import android.view.Display;

import com.emop.client.Constants;
import com.emop.client.io.FmeiClient;
import com.emop.client.io.TaodianApi;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Topic;

/**
 * 刷新一些应用关键数据。
 * @author deonwu
 *
 */
public class ClientDataRefresh extends Thread{
	private Context ctx = null;
	private FmeiClient client = null;
	private Display display = null;
	public ClientDataRefresh(Context context, Display display){
		this.ctx = context;
		client = FmeiClient.getInstance(context);
		this.display = display;
	}
	
	public void run(){
		client.isInited = true;
		loadUserInfo();
		client.updateLocalTrackId();
		client.getTrackPid();
		
		client.refreshActivityItemList(ctx.getContentResolver());
		Cursor c = ctx.getContentResolver().query(Schema.TOPIC_LIST, new String[]{Columns._ID}, null, null, null);
		c.close();
		loadTopicImage();
		c = ctx.getContentResolver().query(Schema.CATE_LIST, new String[]{Columns._ID}, null, null, null);	
		c.close();
		
		c = ctx.getContentResolver().query(Schema.HOT_CATE_LIST, new String[]{Columns._ID}, null, null, null);
		c.close();
		
		client.cleanExpiredData(ctx.getContentResolver());		
	}
	
	public void loadTopicImage(){
		Cursor c = client.getTopicList(ctx.getContentResolver());
		boolean hasMore = c.moveToFirst();
		int picIndex = 0;
		if(hasMore){
			picIndex = c.getColumnIndex(Topic.FRONT_PIC);
		}
        if(display != null){
        	int width = display.getWidth();
			for(int i = 0; i < 5 && hasMore; hasMore = c.moveToNext(), i++){
				String pic = c.getString(picIndex);
				client.tmpImgLoader.loadToCache(pic, width);
			}
        }
		c.close();
	}
	
	
	private void loadUserInfo(){
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
		String userId = settings.getString(Constants.PREFS_OAUTH_ID, "");
		String trackId = settings.getString(Constants.PREFS_TRACK_ID, "");			
		
		Log.d(Constants.TAG_EMOP, "User id:" + userId);
		client.userId = userId;
		client.trackUserId = trackId;
	}
	
}
