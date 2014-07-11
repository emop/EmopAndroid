package com.emop.client;

import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.Display;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.baidu.mobstat.StatService;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Topic;
import com.tencent.mm.sdk.platformtools.Log;

public class GuangActivity extends BaseActivity {
	private SimpleCursorAdapter adapter = null;
	private int screenWidth = 0;
	private ListView listView = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guang_view);
        listView = (ListView) findViewById(R.id.activity_list);
        
        Display display = getWindowManager().getDefaultDisplay();
        screenWidth = display.getWidth();
        
        adapter = new SimpleCursorAdapter(this, R.layout.guang_view_item, 
        		null,
                new String[] {Topic.TITLE, Topic.ITEM_COUNT,
        			Topic.UPDATE_TIME, Topic.FRONT_PIC
        		}, 
                new int[] {R.id.topic_title, 
        		R.id.item_count, R.id.topic_update_time, 
        		R.id.topic_front_pic}
        ){
        	public View getView(int position, View v, ViewGroup p){
        		v = super.getView(position, v, p);
        		View container = v.findViewById(R.id.main_menu_container);
        		container.getLayoutParams().height = screenWidth / 2;
				return v;
        	}        	
        };
                	
        adapter.setViewBinder(new TopicItemViewBinder());        
        listView.setAdapter(adapter); 
        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                int position, long id) {
            	long itemId = adapter.getItemId(position);
            	Log.d(Constants.TAG_EMOP, "click activity item:" + itemId);
            	StatService.onEvent(GuangActivity.this, "click_topic", itemId + "", 1);
            	
            	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/topic/" + itemId + "/list");
        		startActivityView(R.layout.topic_item_list, dataUri, null);
            }
          });
        
        getSupportLoaderManager().initLoader(0, null, new GLoaderCallback(Schema.TOPIC_LIST));
        getContentResolver().registerContentObserver(Schema.TOPIC_LIST, false, new ContentObserver(handler){
        	public void onChange(boolean selfChange) {
        		Log.d(Constants.TAG_EMOP, "topic list is changed...");
        		getSupportLoaderManager().getLoader(0).forceLoad();
        	}
        });
        
        View v = findViewById(R.id.title);
        v.setLongClickable(true);        
        v.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View arg0) {
				if(!isLoading()){
					showLoading("正在加载专题列表...");
					Uri u = Schema.TOPIC_LIST.buildUpon().appendQueryParameter("force_refresh", "y").build();
			        getSupportLoaderManager().restartLoader(0, null, new GLoaderCallback(u));
				}
				return false;
			}			
        });
        
        
    }
    
    protected void onResume (){
    	super.onResume();
    }
    
    protected void onStop(){
    	super.onStop();
    }
    
    class TopicItemViewBinder implements SimpleCursorAdapter.ViewBinder{
    	private int front_pic_index = -1;
    	private int item_count_index = -1;
    	private int update_time_index = -1;
    	private int desc_index = -1;    
    	private int order_index = -1;   

    	
		@Override
		public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
			if(front_pic_index < 0){
				initColumnIndex(cursor);
			}			
			if(view.getId() == R.id.topic_update_time){
				String des = cursor.getString(update_time_index);
				 TextView o = (TextView)view;
				 String[] times = des.split(" ");
				 o.setText(times[0].trim());
				 return true;
			}else if(view.getId() == R.id.item_count){
				String des = cursor.getString(item_count_index);
				 TextView o = (TextView)view;
				 o.setText(des);
				 return true;
			}else if(view.getId() == R.id.topic_front_pic){
				final String des = cursor.getString(front_pic_index);
				view.setTag(des);
				Bitmap bm = client.tmpImgLoader.cache.get(des, screenWidth, true, false);
				ImageView v = (ImageView)view;
				//Log.d("xxx", "load xxx:" + des);
				if(bm != null){
					v.setScaleType(ScaleType.CENTER_CROP);
					v.setImageBitmap(bm);
				}else {
					v.setScaleType(ScaleType.CENTER_INSIDE);
					v.setImageResource(R.drawable.loading);
					client.tmpImgLoader.runTask(new Runnable(){
						@Override
						public void run() {
							//Log.d("xxx", "load xxx2:" + des);
							final Bitmap newBm = client.tmpImgLoader.cache.get(des, screenWidth, true, true);
							if(newBm != null){
								handler.post(new Runnable(){
									@Override
									public void run() {
										//Log.d("xxx", "load xxx3:" + des);
										View v = listView.findViewWithTag(des);
										if(v != null){
											ImageView v2 = (ImageView)v;
											v2.setScaleType(ScaleType.CENTER_CROP);
											v2.setImageBitmap(newBm);
										}
									}
								});
							}
						}						
					});
				}
				
				return true;
			}
			return false;
		}
		
		private void initColumnIndex(Cursor cursor){
			front_pic_index = cursor.getColumnIndex(Topic.FRONT_PIC);
			item_count_index = cursor.getColumnIndex(Topic.ITEM_COUNT);
			update_time_index = cursor.getColumnIndex(Topic.UPDATE_TIME);
			desc_index = cursor.getColumnIndex(Topic.DESC);
			order_index = cursor.getColumnIndex(Topic.VIEW_ORDER);
		}
    }
    
	class GLoaderCallback implements LoaderCallbacks<Cursor>{
		private Uri data = null;
		public GLoaderCallback(Uri data){
			this.data = data;
		}

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			return new CursorLoader(GuangActivity.this, data,
					new String[] {BaseColumns._ID, Topic.TITLE, Topic.ITEM_COUNT,
					Topic.DESC, 
					Topic.FRONT_PIC, 
					Topic.UPDATE_TIME,
					Topic.VIEW_ORDER
					}, 
					null, null, null);
		}
		
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			closeLoading();
			if(cursor != null){
				adapter.changeCursor(cursor); 
				adapter.notifyDataSetChanged();
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			//adapter.swapCursor(null);
		}
	};    
    
     
}
