package com.emop.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.emop.client.io.FmeiClient;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Item;
import com.tencent.mm.sdk.platformtools.Log;

public class HotActivity extends BaseActivity {
	//private FmeiClient client = null;
	private BaseAdapter actAdapter = null;
	//private
	//private LinearLayout cateLayout = null;
	private int winWidth = 0;
	
	private int picMarginDp = 2;
	private Map<Integer, String> hotName = new HashMap<Integer, String>();
	private Timer timer = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hot_cate_view);
        
        client = FmeiClient.getInstance(this);
        
        Gallery mGallery = (Gallery)findViewById(R.id.header_banner);

        actAdapter = this.loadLinkActivityAdapter();
        mGallery.setAdapter(actAdapter);
        mGallery.setOnItemClickListener(itemClick);
        
        //Log.d(Constants.TAG_EMOP, "dp:150 -> px:" + cateHeight);
		Rect displayRectangle = new Rect();
		Window window = getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);  
		winWidth = displayRectangle.width();
		
		timer = new Timer();
		
		AutoChangePager l = new AutoChangePager(mGallery);
		timer.scheduleAtFixedRate(l, 1000, 3000);
		mGallery.setOnFocusChangeListener(l);
		//mGallery.s
		
    }
    
    private OnItemClickListener itemClick = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> v, View v2, int position,
				long id) {
			HashMap<String, Object> item = (HashMap<String, Object>)actAdapter.getItem(position);
			String itemText = (String)item.get("ItemText");
			itemText = itemText.trim();
			String itemId = item.get("id").toString();
        	Log.d(Constants.TAG_EMOP, "click activity:" + itemText + ", id:" + itemId);
        	if(itemText.startsWith("/")){
            	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + itemText.trim());
        		Intent intent = new Intent().setClass(HotActivity.this, 
        				GuangItemListActivity.class);
        		//intent.putExtra("title", itemText);
        		intent.setData(dataUri);
        		Log.d(Constants.TAG_EMOP, "click hot:" + dataUri.toString());
        		startActivity(intent);
        	}else if(itemText.toLowerCase().startsWith("http:")){
            	Intent intent = new Intent().setClass(HotActivity.this, WebViewActivity.class);
            	intent.putExtra("http_url", itemText.trim());
            	startActivity(intent);
        	}else {
        		showToast(itemText);
        	}
		}    	
    };
    
    private BaseAdapter loadLinkActivityAdapter(){
    	int topicId = client.getActivityTopicId(getContentResolver());
    	Uri topicList = Uri.parse("content://" + Schema.AUTHORITY + "/act/" + topicId + "/list");	
    	Cursor c = client.getItemList(getContentResolver(), topicList);
        ArrayList<HashMap<String, Object>> lstImageItem = new ArrayList<HashMap<String, Object>>();  
        
		boolean hasMore = c.moveToFirst();
		
		int picIndex = -1;
		int textIndex = -1;
		int id = -1;

		if(hasMore){
			picIndex = c.getColumnIndex(Item.PIC_URL);
			textIndex = c.getColumnIndex(Item.MESSAGE);
			id = c.getColumnIndex(BaseColumns._ID);
		}else {
			Log.d(Constants.TAG_EMOP, "Not found data by uri:");
		}
		
		for(;hasMore; hasMore = c.moveToNext()){
        	HashMap<String, Object> map = new HashMap<String, Object>();  
        	map.put("ItemImage", c.getString(picIndex));
        	map.put("ItemText", c.getString(textIndex));
        	map.put("id", c.getInt(id)); 
        	lstImageItem.add(map); 
		}
		
		if(c != null){
			c.close();
		}
    	
		
		return new ImageAdapter(lstImageItem);
    }
    
    private class ImageAdapter extends BaseAdapter{
    	private ArrayList<HashMap<String, Object>> itemList = null;
    	public ImageAdapter(ArrayList<HashMap<String, Object>> lstImageItem){
    		itemList = lstImageItem;
    	}

		@Override
		public int getCount() {
			return itemList.size();
		}

		@Override
		public Object getItem(int index) {
			return itemList.get(index);
		}

		@Override
		public long getItemId(int index) {
			HashMap<String, Object> map = (HashMap<String, Object>)this.getItem(index);
			Integer i = (Integer)map.get("id");
			return (int)i;
		}

		@Override
		public View getView(int index, View oldView, ViewGroup arg2) {
			// TODO Auto-generated method stub
			LinearLayout liner = new LinearLayout(HotActivity.this);
			liner.setBackgroundColor(getResources().getColor(R.color.color_hot_item));
			liner.setLayoutParams(new Gallery.LayoutParams(Gallery.LayoutParams.FILL_PARENT,
					Gallery.LayoutParams.FILL_PARENT
					));
			
			ImageView i = new ImageView (HotActivity.this); 
			i.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
					LinearLayout.LayoutParams.FILL_PARENT
					));			
			i.setScaleType(ScaleType.CENTER_CROP);
			
			liner.addView(i);
			HashMap<String, Object> item = (HashMap<String, Object>)this.getItem(index);
			String url = (String)item.get("ItemImage");			
			client.appImgLoader.loadImage(url, i, winWidth, true);
			//client.appImgLoader.loadImage(url, i);
			
			return liner;  
		}
    }
    
    public void onRefresh(View v){
		Toast.makeText(HotActivity.this, "热门刷新成功", Toast.LENGTH_SHORT).show();
    }
    
    class AutoChangePager extends TimerTask implements OnFocusChangeListener{
    	Gallery mGallery = null;
    	private long lastChangedTime = 0;
    	private int dir = 0;
    	AutoChangePager(Gallery mGallery){
    		this.mGallery = mGallery;
    	}

		@Override
		public void run() {
			if(System.currentTimeMillis() - lastChangedTime > 3000){
				autoChange();
				lastChangedTime = System.currentTimeMillis();
			}			
		}

		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			lastChangedTime = System.currentTimeMillis();
		}
		
		private void autoChange(){
			if(mGallery.getAdapter().getCount() < 2){
				return;
			}
			
			int i = mGallery.getSelectedItemPosition();
			//Log.d("emop", "cur selected item position:" + i);
			
			if(i >= mGallery.getAdapter().getCount() - 1){
				dir = -1;
			}else if(i == 0){
				dir = 1;
			}
			
			handler.post(new Runnable(){
				@Override
				public void run() {
					if(dir == -1){
						mGallery.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, new KeyEvent(0, 0));
					}else {
						mGallery.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(0, 0));				
					}
				}
			});
		}
    	
    }
    
    

}
