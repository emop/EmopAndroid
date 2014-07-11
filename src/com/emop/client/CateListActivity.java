package com.emop.client;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;

import com.baidu.mobstat.StatService;
import com.emop.client.io.FmeiClient;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Topic;
import com.emop.client.widget.CateImageView;
import com.emop.client.wxapi.DensityUtil;

public class CateListActivity extends BaseActivity {
	private ArrayList<HashMap<String, Object>> cateList;
	//private FmeiClient client = null;
	private SimpleAdapter adapter = null;
	private GridView gridview = null;
	//private float scaleRate = 0;
	private int cateFontSize = 0;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cate_view);
        
        client = FmeiClient.getInstance(this.getApplicationContext(), false);
        cateList = new ArrayList<HashMap<String, Object>>();  

        loadCateList(cateList);
        
        Display display = getWindowManager().getDefaultDisplay();
        final int screenWidth = display.getWidth();    
        cateFontSize = DensityUtil.dip2px(this, 14);
       
        adapter = new SimpleAdapter(this, //没什么解释  
        		cateList, //数据来源   
                R.layout.cate_item,//night_item的XML实现                    
                new String[] {"ItemImage", },   
                  
                //ImageItem的XML文件里面的一个ImageView,两个TextView ID  
                new int[] {R.id.item_pic, }){
        	public View getView(int position, View convertView, ViewGroup parent){
        		View v = super.getView(position, convertView, parent);
        		v.getLayoutParams().height = screenWidth / 3;    
        		View v2 = v.findViewById(R.id.item_pic);
        		if(v2 instanceof CateImageView){
        			CateImageView cv = (CateImageView)v2;
        			HashMap<String, Object> item = (HashMap<String, Object>)adapter.getItem(position);
        			cv.name = item.get("ItemText") + "";
        		}
				return v;
        	}
        };  
        
        //adapter.
        
        adapter.setViewBinder(new ViewBinder(){
			@Override
			public boolean setViewValue(View v, Object data, final String picUrl) {
				//Log.d(Constants.TAG_EMOP, "picUrl:" + picUrl + ", data:" + data);
				//HashMap<String, Object> item = (HashMap<String, Object>)data;
				if(v instanceof CateImageView){
					final CateImageView iv = (CateImageView)v;
					v.setTag(picUrl);
					//iv.setImageResource(R.drawable.chi);
					//iv.setImageResource(R.drawable.cate_img_bg);
					Bitmap bitmap = client.appImgLoader.cache.get(picUrl, 300, false, false);
					if(bitmap != null){
						iv.setImageBitmap(bitmap);
					}else {
						iv.setImageResource(R.drawable.cate_img_bg);
						client.appImgLoader.runTask(new Runnable(){
							public void run(){
								final Bitmap bitmap2 = client.appImgLoader.cache.get(picUrl, 300, false, true);
								if(bitmap2 != null){
									handler.post(new Runnable(){
										public void run(){
											ImageView vv = (ImageView)gridview.findViewWithTag(picUrl);
											if(vv != null){
												vv.setImageBitmap(bitmap2);
											}
										}
									});
								}
							}
						});
					}
					iv.fontSize = cateFontSize;
					return true;
				}
				return false;
			}});
        
        gridview = (GridView)findViewById(R.id.gridView1); 
        gridview.setAdapter(adapter);
        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
            		HashMap<String, Object> item = (HashMap<String, Object>)adapter.getItem(position);
            		long itemId = (Integer)item.get(BaseColumns._ID);
            		String title = item.get("ItemText") + "";
            		
            		StatService.onEvent(CateListActivity.this, "click_cate", itemId + "_" + title, 1);
                	Log.d(Constants.TAG_EMOP, "click activity item:" + itemId);
                	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/cate/" + itemId + "/list");
            		Intent intent = new Intent().setClass(CateListActivity.this, 
            				GuangItemListActivity.class);
            		intent.setData(dataUri);
            		intent.putExtra("title", title);
            		startActivity(intent);            	
                }
           });
        
        getContentResolver().registerContentObserver(Schema.TOPIC_LIST, false, new ContentObserver(handler){
        	public void onChange(boolean selfChange) {
        		Log.d(Constants.TAG_EMOP, "cate list is changed...");        		
                loadCateList(cateList);
                adapter.notifyDataSetChanged();
        	}
        });
    }
    
    private void loadCateList(ArrayList<HashMap<String, Object>> lstImageItem){
    	lstImageItem.clear();
    	Cursor c = client.getCateList(getContentResolver());
    	
		boolean hasMore = c.moveToFirst();
		
		int picIndex = -1;
		int id = -1;
		int nameIndex = -1;
		if(hasMore){
			picIndex = c.getColumnIndex(Topic.FRONT_PIC);
			id = c.getColumnIndex(BaseColumns._ID);
			nameIndex = c.getColumnIndex(Topic.TITLE);
		}else {
			Log.d(Constants.TAG_EMOP, "Not found data by uri:");
		}
		
		for(;hasMore; hasMore = c.moveToNext()){
        	HashMap<String, Object> map = new HashMap<String, Object>();  
        	map.put("ItemImage", c.getString(picIndex));
        	map.put("ItemText", c.getString(nameIndex));
        	map.put("id", c.getInt(id)); 
        	map.put(BaseColumns._ID, c.getInt(id)); 
        	lstImageItem.add(map); 
		}
		
		if(c != null){
			c.close();
		}    	
    } 
       
    public void doKeywordSearch(View v){
    	TextView text = (TextView)this.findViewById(R.id.search_keyword);
    	String keyword = text.getText().toString();
    	if(keyword.trim().length() > 0){
	    	String searchUrl = String.format("http://s.m.taobao.com/search.htm?q=%s&pid=mm_%s_0_0",
	    			Uri.encode(keyword), client.trackPID);
	
	    	StatService.onEvent(this, "search", keyword + "_" + client.trackPID + "_" + client.userId, 1);
	    	
	    	Intent intent = new Intent().setClass(this, WebViewActivity.class);
	    	intent.putExtra("http_url", searchUrl);
	    	intent.putExtra("title", keyword);
	    	
	    	this.startActivity(intent);
    	}else {
    		showToast("请输入需要搜索的关键词。");
    	}
    }
      
}
