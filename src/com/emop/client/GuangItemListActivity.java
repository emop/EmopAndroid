package com.emop.client;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mobstat.StatService;
import com.emop.client.io.ApiResult;
import com.emop.client.io.TaodianApi;
import com.emop.client.provider.model.Item;
import com.emop.client.widget.WaterFallOption;
import com.emop.client.widget.WaterFallView;
import com.emop.client.widget.WaterFallView.OnRefreshListener;
import com.emop.client.widget.WaterFallView.OnScrollListener;
import com.emop.client.wxapi.DensityUtil;

/**
 * 专题的瀑布流展示View.
 * @author deonwu
 *
 */
public class GuangItemListActivity extends BaseActivity {
	private WaterFallDataLoader dataLoader = null;
	private static final int COLUME_NUM = 3;
	private static final int PIC_FRONT_SIZE = 10;
	private static final int PIC_MARGIN_SIZE = 2;
	
	
	private WaterFallView waterfallView;
	private LinearLayout errorView = null;
	//private TextView errorInfo = null;
	
	private Uri dataUri = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.guang_item_list_view);        

        Intent intent = this.getIntent();
    	dataUri = intent.getData();
    	String title = intent.getStringExtra("title");
    	if(title != null && title.trim().length() >0){
            TextView v = (TextView)findViewById(R.id.title);
            v.setText(title);    		
    	}

    	initWaterFallLayout();
		waterfallView.showHeadLoader();

    	errorView = (LinearLayout)this.findViewById(R.id.error_view);
    	List<String> seg = dataUri.getPathSegments();
    	if(seg.size() > 2){
    		StatService.onEvent(this, seg.get(0) + "_" + seg.get(1), "view", 1);
    	}
    	
    	View tab = findViewById(R.id.nav_fav_tabs);
    	if(tab != null){
    		tab.setVisibility(View.GONE);
    	}
    }
    
    protected void onResume(){
    	super.onResume();
    	if(waterfallView.isHeadLoading() && !waterfallView.isLoading()){
    		waterfallView.load();
    	}else {
    		Log.d(Constants.TAG_EMOP, "header:" + waterfallView.isHeadLoading() + ", loading:" + waterfallView.isLoading());
    	}
    } 
    
    protected void onStop(){
    	super.onStop();
    }
    
	private void initWaterFallLayout() {
		dataLoader = new WaterFallDataLoader();
		
		//1 初始化waterfall 
		waterfallView = (WaterFallView) findViewById(R.id.waterfall_scroll);
		//2 初始化显示容器
		LinearLayout waterfall_container = (LinearLayout) findViewById(R.id.waterfall_container);
		//3,设置滚动监听
		waterfallView.setOnScrollListener(dataLoader);
		waterfallView.setOnRefrreshListener(dataLoader);
		//4,实例一个设置
		WaterFallOption fallOption = new WaterFallOption(waterfall_container,
				0, COLUME_NUM);
		
		fallOption.headLoader = findViewById(R.id.head_loader);
		Display display = null;
        display = getWindowManager().getDefaultDisplay();
        fallOption.itemWidth = display.getWidth() / COLUME_NUM;
        fallOption.itemFontSize = DensityUtil.dip2px(this, PIC_FRONT_SIZE);
        fallOption.itemMarginSize = DensityUtil.dip2px(this, PIC_MARGIN_SIZE);
        //fallOption.itemWidth -= fallOption.itemMarginSize * 2;
        
		waterfallView.commitWaterFall(fallOption);
		
	}
	
	public void onFinish(View v){
		finish();
	}
	
	/**
	 * 管理瀑布流的数据加载，相关功能。
	 * 1. 先从网络下载一次40条。
	 * 2. 如果加载失败，检查本地是否有数据。 没有，提示网络失败。有，加载本地数据。
	 * 
	 * 3. 每次先从加载10条。如果没有更多数据了，从网上刷新。
	 * 
	 * @author deonwu
	 */
	class WaterFallDataLoader implements OnScrollListener, OnRefreshListener{
		//private int loadedCount = 0;
		//private int pageSize = 10;
		//private int curPage = 0;
		//服务器端更新的页数。
		private int refreshPage = 0;
		private int lastRefreshCount = 0;
		//private boolean isInited = false;
		private String startTime = "1999-10-10";
		private String endTime = "2999-10-10";
		public SparseIntArray loadedItem = new SparseIntArray();
		private boolean isLoading = false;
		private long lastShowNoItem = System.currentTimeMillis();
		//private View bottomLoader = null;
		private int reRefreshTimes = 0;
		
		@Override
		public void onBottom() {
			Log.d("xxx", "on buttom");
			if(lastRefreshCount > 0){
				final View v = findViewById(R.id.bottom_loader);
				new Thread(){
					public void run(){
						if(isLoading) return;
						isLoading = true;
						loadMorePage();					
						isLoading = false;
					}
				}.start();
			}else {
				if(System.currentTimeMillis() - lastShowNoItem > 1000 * 5){
					lastShowNoItem = System.currentTimeMillis();
					showToast("亲,没有宝贝啦~~");				
				}
			}
		}
		
		public void cleanExpiredData(){
			Log.d(Constants.TAG_EMOP, "cleanExpiredData....");
			getContentResolver().delete(dataUri, Item.LOCAL_UPDATE_TIME + " < ?",
					new String[]{(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2) + ""});
		}
	
		@Override
		public void onTop() {
			
		}
	
		@Override
		public void onScroll() {
			
		}
		
		@Override
		public void onAutoScroll(int l, int t, int oldl, int oldt) {
			
		}
		
		@Override
		public void onRefresh() {
			Log.d("xx", "refresh....");
			hideEmptyView();
			cleanExpiredData();
			
			Builder uriBuilder = dataUri.buildUpon();
			uriBuilder.appendQueryParameter("pageNo", "0");
			uriBuilder.appendQueryParameter("pageSize", "30");
			uriBuilder.appendQueryParameter("startTime", startTime);
			//uriBuilder.appendQueryParameter("endTime", startTime);
			if(reRefreshTimes > 3){
				uriBuilder.appendQueryParameter("no_cache", "y");
				reRefreshTimes = 0;
			}else {
				reRefreshTimes++;				
			}
			ApiResult r = client.refreshDataByUri(getContentResolver(), uriBuilder.build(), TaodianApi.STATUS_NORMAL, true);
			if(r != null && r.isOK){
				refreshPage = 1;
				String count = r.getString("data.item_count");
				if(count != null && count.length() > 0){
					lastRefreshCount = Integer.parseInt(count);
				}else {
					lastRefreshCount = 0;
				}
			}else {
				if(r == null){
					showToast("啊哦，网速不给力啊~");
				}else {
				//	showToast("系统错误，请联系管理员" + r.errorMsg());					
				}
			}
			if(lastRefreshCount > 0){
				this.addApiResultToContainer(r);
				handler.post(new Runnable(){
					public void run(){
						final View v = findViewById(R.id.bottom_loader);
						if(v != null){
							v.setVisibility(View.VISIBLE);
						}
					}
				});	
			}else {
				showEmptyView();
			}
		}
		
		private void loadMorePage(){
			Log.d("xx", "load more page....");
			
			Builder uriBuilder = dataUri.buildUpon();
			uriBuilder.appendQueryParameter("pageNo", this.refreshPage + "");
			uriBuilder.appendQueryParameter("pageSize", "30");
			//uriBuilder.appendQueryParameter("startTime", startTime);
			uriBuilder.appendQueryParameter("endTime", endTime);
			
			ApiResult r = client.refreshDataByUri(getContentResolver(), uriBuilder.build(), TaodianApi.STATUS_NORMAL, true);

			if(r != null && r.isOK){
				refreshPage++;
				JSONObject o = r.getJSONObject("data"); // getString("data.item_count");
				JSONArray array = null;
				try {
					array = o.getJSONArray("items");
				} catch (JSONException e) {
				}
				if(array != null && array.length() > 0){
					lastRefreshCount = array.length();
				}else {
					lastRefreshCount = 0;
				}
				if(lastRefreshCount > 0){
					this.addApiResultToContainer(r);
				}else {
					handler.post(new Runnable(){
						public void run(){
							final View v = findViewById(R.id.bottom_loader);
							if(v != null){
								v.setVisibility(View.GONE);
							}
						}
					});						
				}
			}else{
				if(r == null){
					showToast("啊哦，网速不给力啊~");
				}else {
				//	showToast("系统错误，请联系管理员。" + r.errorMsg());					
				}
			}			
		}
		
		private void hideEmptyView(){
			handler.post(new Runnable() {
				@Override
				public void run() {
					if(errorView != null){
						errorView.setVisibility(View.GONE);
					}
				}}
			);
		}		
		
		private void showEmptyView(){
			handler.post(new Runnable() {
				@Override
				public void run() {
					if(errorView != null){
						errorView.setVisibility(View.VISIBLE);
					}
				}}
			);
		}
		
		private void addApiResultToContainer(final ApiResult r) {
			final long st = System.currentTimeMillis();
			try{
				JSONObject json = r.json.getJSONObject("data");
				JSONArray jarray = json.getJSONArray("items");
				int itemId = 0;
				for(int i = 0; i < jarray.length(); i++){
					try{
						json = jarray.getJSONObject(i);
						itemId = json.getInt("item_id");
						if(itemId <= 0)continue;
						//Log.d("dd", "ading:" + itemId);
						if(loadedItem.get(itemId, -1) > 0){
							Log.d("dd", "already added to view:" + itemId);
							continue;
						}else {
							loadedItem.append(itemId, itemId);
						}
						itemId = json.getInt("id");
						waterfallView.addImage(json.getString(Item.PIC_URL),
								(int) Math.ceil(waterfallView.loaded_count / (double) COLUME_NUM),
								itemId,
								(float)json.getDouble(Item.PRICE),
								(float)json.getDouble(Item.RECT_RATE));
					}catch (JSONException e) {
						Log.d(Constants.TAG_EMOP, "json error:" + e.toString(), e);
					}
				}
			}catch (JSONException e1) {
				Log.d(Constants.TAG_EMOP, "json error:" + e1.toString(), e1);
			}
		}
	}
	
}
