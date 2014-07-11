package com.emop.client.fragment;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.LinearLayout;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SimpleAdapter.ViewBinder;

import com.baidu.mobstat.StatService;
import com.emop.client.Constants;
import com.emop.client.GuangItemListActivity;
import com.emop.client.HotActivity;
import com.emop.client.MutilFragmentActivity;
import com.emop.client.R;
import com.emop.client.io.FmeiClient;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Topic;
import com.emop.client.widget.CateImageView;
import com.emop.client.widget.HotCateImageView;
import com.emop.client.widget.SoildGridView;
import com.emop.client.wxapi.DensityUtil;

public class HotCateFragment extends Fragment{
	private ArrayList<HashMap<String, Object>> cateList;	
	private SimpleAdapter adapter = null;
	//private CursorAdapter adapter = null; //(Context context, Cursor c)
	private int cateHeight = 0;
	private int picMarginDp = 2;
	private int winWidth = 0;
    protected Handler handler = new Handler();
    private boolean isRunning = false;
    
	public void onResume(){
		super.onResume();
		isRunning = true;
	}
	
	public void onPause(){
		super.onPause();
		isRunning = false;
	}     
	
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedState){
		final SoildGridView v = new SoildGridView(getActivity());
		
        cateList = new ArrayList<HashMap<String, Object>>();  
		
		LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);	
		v.setLayoutParams(params);
		v.setFadingEdgeLength(0);
		v.setSelector(R.color.color_transparent);
		v.setNumColumns(3);
		v.setFocusable(false);
		
		final FmeiClient client = FmeiClient.getInstance(getActivity(), false);         
		
		Rect displayRectangle = new Rect();
		Window window = getActivity().getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);  
		winWidth = displayRectangle.width();
		
		cateHeight = (int)((winWidth- 6 * DensityUtil.dip2px(getActivity(), picMarginDp)) / 3 * 1.2);
		
        adapter = new SimpleAdapter(getActivity(), //没什么解释  
        		cateList, //数据来源   
        		R.layout.hot_view_item,//night_item的XML实现                    
                new String[] {"ItemImage", },   
                //ImageItem的XML文件里面的一个ImageView,两个TextView ID  
                new int[] {R.id.item_pic, }){
           	public View getView(int position, View convertView, ViewGroup parent){
        		final View v = super.getView(position, convertView, parent);
        		v.getLayoutParams().height = cateHeight;   
        		
        		int fontSize = DensityUtil.dip2px(getActivity(), 14);
        		View v2 = v.findViewById(R.id.item);
        		if(v2 instanceof HotCateImageView){
        			HotCateImageView cateImage = (HotCateImageView)v2;
        			HashMap<String, Object> item = (HashMap<String, Object>)adapter.getItem(position);
					cateImage.name = item.get("ItemText") + ""; 
					cateImage.pic = item.get("ItemImage") + ""; 
					cateImage.id = (Integer)item.get("ItemID"); 
					cateImage.tags = item.get("ItemTags") + ""; 
					cateImage.fontSize = fontSize;
										
					final String des = cateImage.pic;
					cateImage.setTag(cateImage.pic);
					Bitmap bm = client.tmpImgLoader.cache.get(des, winWidth /3, false, false);
					ImageView img = cateImage;
					if(bm != null){
						img.setImageBitmap(bm);
					}else {						
						client.tmpImgLoader.runTask(new Runnable(){
							@Override
							public void run() {
								final Bitmap newBm = client.tmpImgLoader.cache.get(des, winWidth /3, false, true);
								if(newBm != null){
									handler.post(new Runnable(){
										@Override
										public void run() {
											View vv = v.findViewWithTag(des);
											if(vv != null){
												ImageView v2 = (ImageView)vv;
												v2.setScaleType(ScaleType.CENTER_CROP);
												v2.setImageBitmap(newBm);
											}
										}
									});
								}
							}						
						});
					}					
        		}
				return v;
        	}        	
        	
        };
        
        adapter.setViewBinder(new ViewBinder(){
			@Override
			public boolean setViewValue(View v, Object data, final String picUrl) {
				return true;
			}}
        );
        
	    v.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long arg3) {
				HotCateImageView cateImage = (HotCateImageView)v.findViewById(R.id.item);
	    		StatService.onEvent(getActivity(), "click_hot", cateImage.id + "_" + cateImage.name, 1);

				if(cateImage != null){
					String tags = cateImage.tags;
					Log.d(Constants.TAG_EMOP, "click activity hot:" + cateImage.id + ", tags:" + cateImage.tags);
					if(tags == null || tags.trim().length() == 0 || tags.trim().equals("0")){
			        	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/hot/" + cateImage.id + "/list");
			    		Intent intent = new Intent().setClass(getActivity(), 
			    				GuangItemListActivity.class);
			    		
			    		intent.putExtra("title", cateImage.name);
			    		intent.setData(dataUri);
			    		startActivity(intent);   		
					}else if(tags.equals("shop")){
						Intent intent = new Intent();			
						intent.setClass(getActivity(), MutilFragmentActivity.class);	
						
						intent.putExtra("view_id", new int[]{R.layout.shop_list});
						startActivity(intent);
					}else if(tags.equals("rebate")){
						Intent intent = new Intent();			
						intent.setClass(getActivity(), MutilFragmentActivity.class);	
						
						intent.putExtra("view_id", new int[]{R.layout.rebate_list});
						startActivity(intent);							
					}
				}				
			}});
		
		v.setAdapter(adapter);	
		
		return v;
	}
	
	protected void updateData(Cursor c){
		cateList.clear();
		/*
    	HashMap<String, Object> shop = new HashMap<String, Object>();  
    	shop.put("ItemImage", "http://tdcms.b0.upaiyun.com/app/link_web/hot_undefined_03_07_0926.png");
    	shop.put("ItemText", "店铺");
    	shop.put("ItemID", 1); 
    	shop.put("ItemTags", "shop"); 
 		*/
		
    	HashMap<String, Object> rebate = new HashMap<String, Object>();  
    	rebate.put("ItemImage", "http://tdcms.b0.upaiyun.com/app/link_web/hot_11126_03_07_0927.png");
    	rebate.put("ItemText", "限时特惠");
    	rebate.put("ItemID", 2); 
    	rebate.put("ItemTags", "rebate");     	
		
    	//cateList.add(shop);
    	cateList.add(rebate);
    	
		if(c != null){		
			boolean hasMore = c.moveToFirst();
			int nameIndex = -1, picIndex = -1, idIndex = -1, tagIndex = -1;
			if(hasMore){
				picIndex = c.getColumnIndex(Topic.FRONT_PIC);
				idIndex = c.getColumnIndex(BaseColumns._ID);
				nameIndex = c.getColumnIndex(Topic.TITLE);	
				tagIndex = c.getColumnIndex(Topic.TAGS);
			}else {
				Log.d(Constants.TAG_EMOP, "Not found data by uri:");
			}
			
			for(;hasMore; hasMore = c.moveToNext()){
	        	HashMap<String, Object> map = new HashMap<String, Object>();  
	        	map.put("ItemImage", c.getString(picIndex));
	        	map.put("ItemText", c.getString(nameIndex));
	        	map.put("ItemID", c.getInt(idIndex)); 
	        	map.put("ItemTags", c.getInt(tagIndex)); 
	        	cateList.add(map);
			}
		}
	}
	
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		
		/**
		 * 更新Loader数据。
		 */
		//getLoaderManager().restartLoader(0, null, this);
		getLoaderManager().initLoader(0, null, 
			new LoaderCallbacks<Cursor>() {

				@Override
				public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
					return new CursorLoader(getActivity(), Schema.HOT_CATE_LIST,
							new String[] { BaseColumns._ID, Topic.TITLE,
									Topic.TAGS,
									Topic.ITEM_COUNT, Topic.DESC, Topic.FRONT_PIC,
									Topic.UPDATE_TIME }, null, null, null);
				}
				
				/**
				 * 在Loader创建成功时返回。有可能会在Fragment还没有初始化完。就被回
				 * 调了。在代码里面需要处理这种情况。
				 * @param arg0
				 * @param arg1
				 */
				@Override
				public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
					Log.d("xx", "onLoad finishied, count:" + cursor.getCount());
					if(adapter != null){
						Log.d("xx", "swapCursor finishied.");
						updateData(cursor);
						adapter.notifyDataSetChanged();
					}
				}

				@Override
				public void onLoaderReset(Loader<Cursor> arg0) {
					updateData(null);
				}
			}
		);
		
       getActivity().getContentResolver().registerContentObserver(Schema.HOT_CATE_LIST, false, new ContentObserver(new Handler()){
        	public void onChange(boolean selfChange) {
        		if(isRunning) return;
        		Log.d(Constants.TAG_EMOP, "host cat list is changed...");
        		getLoaderManager().getLoader(0).forceLoad();
        	}
        });
	}

}
