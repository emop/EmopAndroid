package com.emop.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.emop.client.fragment.ShopListFragment;
import com.emop.client.io.ApiResult;
import com.emop.client.io.TaodianApi;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Item;
import com.emop.client.widget.WaterFallOption;
import com.emop.client.widget.WaterFallView;
import com.emop.client.widget.WaterFallView.OnRefreshListener;
import com.emop.client.widget.WaterFallView.OnScrollListener;
import com.emop.client.widget.item.FlowView;
import com.emop.client.wxapi.DensityUtil;
import com.tencent.mm.sdk.platformtools.Log;

public class MyFavoriteActivity extends PrivateTabActivity{
	private WaterFallDataLoader dataLoader = null;
	private static final int COLUME_NUM = 3;
	private static final int PIC_FRONT_SIZE = 10;
	private static final int PIC_MARGIN_SIZE = 2;
	
	
	private WaterFallView waterfallView;
	private ShopListFragment fragment;
	private View shopList = null;
	private LinearLayout errorView = null;
	private View tabView = null;
	private View taokeTab = null;
	private View shopTab = null;
	
	private Uri dataUri = null;
	public static List<Integer> removedList = new ArrayList<Integer>();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guang_item_list_view);        

        //Intent intent = this.getIntent();
    	//dataUri = intent.getData();

    	initWaterFallLayout();
		waterfallView.showHeadLoader();
		int padding = DensityUtil.dip2px(this, 50);
		waterfallView.setPadding(waterfallView.getPaddingLeft(), 
				waterfallView.getPaddingTop(), 
				waterfallView.getPaddingRight(), 
				padding);

    	errorView = (LinearLayout)this.findViewById(R.id.no_fav);
        TextView v = (TextView)findViewById(R.id.title);
        v.setText("我的收藏");
        
        View v2 = findViewById(R.id.img_main_menu_back);
        v2.setVisibility(View.GONE);
        
        tabView = findViewById(R.id.nav_fav_tabs);
        taokeTab = findViewById(R.id.fav_taoke);
        if(taokeTab != null){
        	taokeTab.setOnClickListener(tabSwitch);
        }
        shopTab = findViewById(R.id.fav_shop);
        if(shopTab != null){
        	shopTab.setOnClickListener(tabSwitch);
        }
        //shopList = findViewById(R.id.shopList);
        //fragment = (ShopListFragment)getSupportFragmentManager().findFragmentById(R.id.shopList);
        
        //Bundle args = new Bundle();
        //args.putString("uri", Schema.SHOP_LIST.toString());
        //f.setArguments(args);
    }
    
    protected void onResume(){
    	super.onResume();
    	if(client.isLogined() && waterfallView.isHeadLoading() && !waterfallView.isLoading()){
    		waterfallView.load();
    	}else {
    		Log.d(Constants.TAG_EMOP, "header:" + waterfallView.isHeadLoading() + ", loading:" + waterfallView.isLoading());
    	}
    	
    	//进入我的收藏，显示图片后，在进入详情页取消收藏。在回到收藏后，还是在收藏里面显示。
		for(Integer id : removedList){
			View v = waterfallView.findViewById(id);
			if(v != null){
				Log.d(Constants.TAG_EMOP, "remove item:" + id);
				waterfallView.deleteItems((FlowView)v);
			}
		}
		removedList.clear();
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
        
		waterfallView.commitWaterFall(fallOption);
	}
	
	private OnClickListener tabSwitch = new OnClickListener(){
		
		@Override
		public void onClick(View v) {
			if(v.getId() == R.id.fav_taoke){
				showTaoke(v);
			}else {
				showShop(v);
			}
		}
		
		private void showTaoke(View v){
			taokeTab.setEnabled(false);
			shopTab.setEnabled(true);
			tabView.setBackgroundResource(R.drawable.fav_select_left_bg);
			
			shopList.setVisibility(View.GONE);
			waterfallView.setVisibility(View.VISIBLE);
			
		}
		
		private void showShop(View v){
			taokeTab.setEnabled(true);
			shopTab.setEnabled(false);
			tabView.setBackgroundResource(R.drawable.fav_select_right_bg);
			
			fragment.reload();
			shopList.setVisibility(View.VISIBLE);
			waterfallView.setVisibility(View.GONE);
		}		
	};
	
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
		
		@Override
		public void onBottom() {
			Log.d("xxx", "on buttom");
			new Thread(){
				public void run(){
					if(isLoading) return;
					isLoading = true;
					loadMorePage();
					isLoading = false;
				}
			}.start();
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
			String myFavId = client.getFavoriteId();
			if(myFavId == null || myFavId.trim().length() == 0){
				Log.d("xx", "not found myfav id....");
				return;
			}
			
			dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/myfav/" + myFavId + "/list");
			
			Log.d("xx", "refresh....");
			hideEmptyView();
			cleanExpiredData();
			
			Builder uriBuilder = dataUri.buildUpon();
			uriBuilder.appendQueryParameter("pageNo", "0");
			uriBuilder.appendQueryParameter("pageSize", "20");
			uriBuilder.appendQueryParameter("startTime", startTime);
			//uriBuilder.appendQueryParameter("endTime", startTime);
			
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
				//	showToast("系统错误，请联系管理员。" + r.errorMsg());					
				}
			}
			if(lastRefreshCount > 0){
				this.addApiResultToContainer(r);
			}else {
				showEmptyView();
			}
		}
		
		private void loadMorePage(){
			Log.d("xx", "load more page....");			
			if(lastRefreshCount == 0){ 
				Log.d("xx", "last load is empty, ignore to load.");
				return;
			}
			
			Builder uriBuilder = dataUri.buildUpon();
			uriBuilder.appendQueryParameter("pageNo", this.refreshPage + "");
			uriBuilder.appendQueryParameter("pageSize", "20");
			//uriBuilder.appendQueryParameter("startTime", startTime);
			uriBuilder.appendQueryParameter("endTime", endTime);
			
			ApiResult r = client.refreshDataByUri(getContentResolver(), uriBuilder.build(), TaodianApi.STATUS_NORMAL, true);
			if(r != null && r.isOK){
				refreshPage++;
				String count = r.getString("data.item_count");
				if(count != null && count.length() > 0){
					lastRefreshCount = Integer.parseInt(count);
				}else {
					lastRefreshCount = 0;
				}
				if(lastRefreshCount > 0){
					this.addApiResultToContainer(r);
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
			handler.post(new Runnable(){
				public void run(){
					try {
						JSONObject json = r.json.getJSONObject("data");
						JSONArray jarray = json.getJSONArray("items");
						int itemId = 0;
						for(int i = 0; i < jarray.length(); i++){
							json = jarray.getJSONObject(i);
							itemId = json.getInt("item_id");
							if(loadedItem.get(itemId, -1) > 0){
								Log.d("dd", "already added to view:" + itemId);
								continue;
							}else {
								loadedItem.append(itemId, itemId);
							}
							itemId = json.getInt("id");
							float rectRate = 0;
							if(json.has(Item.RECT_RATE)){
								rectRate = (float)json.getDouble(Item.RECT_RATE);
							}else {
								rectRate = 0;
							}
							
							waterfallView.addImage(json.getString(Item.PIC_URL),
									(int) Math.ceil(waterfallView.loaded_count / (double) COLUME_NUM),
									itemId,
									(float)json.getDouble(Item.PRICE),
									rectRate);					
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	
	
}
