package com.emop.client.fragment;

import java.util.TreeSet;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StrikethroughSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;

import com.baidu.mobstat.StatService;
import com.emop.client.R;
import com.emop.client.WebViewActivity;
import com.emop.client.io.FmeiClient;
import com.emop.client.provider.QueryParam;
import com.emop.client.provider.model.Rebate;
import com.emop.client.utils.TimeHelper;

public class RebateListFragment extends ListFragment{
	public final static int MAX_LIST_COUNT = 600;
	public int cateId = 0;
	private RebateListAdapter adapter = null;
    protected Handler handler = new Handler();
    protected boolean isRunning = false;
    protected FmeiClient client = null;
    protected View loadMore = null;
    protected DataLoaderCallback loadingMoreLoaderCallBack = null;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState){				
		View v = inflater.inflate(R.layout.shop_list_view, container, false);
		if(cateId > 0){
			v.setTag(cateId);
		}
		ListView listView = (ListView)v.findViewById(android.R.id.list);
		listView.setCacheColorHint(0);
		
		
		return v;
	}	
	
	public void onResume(){
		super.onResume();
		isRunning = true;
	}
	
	public void onPause(){
		super.onPause();
		isRunning = false;
		
	}
	
	public void onDestroy(){
		super.onDestroy();
		getLoaderManager().destroyLoader(0);
	}
	
	
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		Bundle args = this.getArguments();
		final String data = args.getString("uri");
		
		client = FmeiClient.getInstance(null);
		
		adapter = new RebateListAdapter(getActivity(), 0);
		loadMore = getLayoutInflater(null).inflate(R.layout.rebate_load_more, null);
		getListView().addFooterView(loadMore, null, false);
		
		setListAdapter(adapter);
		
		final Uri dataSource = Uri.parse(data);
		if(data != null){
			getLoaderManager().initLoader(0, null, new DataLoaderCallback(dataSource, false));
			loadingMoreLoaderCallBack = new DataLoaderCallback(dataSource, true);
		}
		this.getListView().setOnScrollListener(scrollListener);
			
	}
	
	public void onListItemClick(ListView l, View v, int position, long id){
		
		RebateItem item = this.adapter.getItem(position);
		String shortKey = item.shortKey;
		String numIid = item.numIId + "";
		
		Log.d("emop", "onItemClick, id:" + id + ", short key:" + shortKey);
		if(shortKey != null && shortKey.length() > 0){
			StatService.onEvent(getActivity(), "go_rebate", numIid + "_" + shortKey + "_" + client.userId, 1);
	    	String url = "http://c.emop.cn/c/" + shortKey + "?from=app&auto_mobile=n";
	    	Intent intent = new Intent().setClass(getActivity(), WebViewActivity.class);
	    	intent.putExtra("http_url", url);
	    	intent.putExtra("taoke_num_iid", numIid);
	    	startActivity(intent);
		}
	}
	
	private void loadMoreData(){
		if(adapter.getCount() > MAX_LIST_COUNT){
			notFoundMoreData();
		}else {		
			if(loadingMoreLoaderCallBack != null && !loadingMoreLoaderCallBack.isLoading){
				loadingMoreLoaderCallBack.isLoading = true;
				if(isRunning){
					getLoaderManager().restartLoader(0, null, loadingMoreLoaderCallBack);
				}
			}else {
				Log.d("Emop", "current loader is in loading status, pageSize:" + loadingMoreLoaderCallBack.pageNo);
			}
		}
	}
	
	private void showNoItems(){
		View v = getView().findViewById(R.id.progressbar_loading);
		if(v != null){
			v.setVisibility(View.GONE);
		}		
		
		v = getView().findViewById(R.id.no_items);
		if(v != null){
			v.setVisibility(View.VISIBLE);
		}		
		
	}
	
	private void notFoundMoreData(){
		if(loadMore != null){
			//getListView().getFooterViewsCount()
			getListView().removeFooterView(loadMore);
		}
	}	
	
	class Items{
		ImageView picUrl = null;
		TextView couponRate = null;
		TextView couponPrice = null;
		TextView price = null;
		TextView title = null;
		TextView endTime = null;

		public Items(View root){
			picUrl = (ImageView)root.findViewById(R.id.pic_url);
			couponRate = (TextView)root.findViewById(R.id.coupon_reate);
			couponPrice = (TextView)root.findViewById(R.id.coupon_price);
			endTime = (TextView)root.findViewById(R.id.coupon_end_time);
			price = (TextView)root.findViewById(R.id.price);
			title = (TextView)root.findViewById(R.id.title);
		}
	}
	
	private OnScrollListener scrollListener = new OnScrollListener(){
		private int visibleLastIndex = 0; 
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			visibleLastIndex  = firstVisibleItem + visibleItemCount - 1;
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			/**
			 * 如果最后一个记录已经显示出了了。开始自动加载下一页数据。
			 */
			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE
					&& visibleLastIndex >= adapter.getCount() -2 ){
				loadMoreData();
			}
		}
		
	};
	
	class RebateItem{
		public long numIId = 0;
		public String picUrl = null;
		public float couponRate = 0;
		public String couponPrice = "0.0";		
		public String price = "0.0";
		public String title = "";
		public String endTime = "";
		public String shortKey = "";
	}
	
	class RebateListAdapter extends ArrayAdapter<RebateItem>{
		public TreeSet<Long> loadedItem = new TreeSet<Long>();
		private int winWidth = 0;
		
		public RebateListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			Rect displayRectangle = new Rect();
			Window window = getActivity().getWindow();
			window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);  
			winWidth = displayRectangle.width();
		}
		
		public View getView(int position, View convertView, ViewGroup parent){
			if(convertView == null){
				convertView = getLayoutInflater(null).inflate(R.layout.rebate_list_item, null);
				/*
				convertView.setLongClickable(true);
				convertView.setOnLongClickListener(new OnLongClickListener(){

					@Override
					public boolean onLongClick(View v) {
						View img = v.findViewById(R.id.pic_url);
						Log.d("image tag:", "imag tag:" + img.getTag());
						// TODO Auto-generated method stub
						return false;
					}
					
				});
				*/
			}
			Items tag = (Items)convertView.getTag();
			if(tag == null){
				tag = new Items(convertView);
				convertView.setTag(tag);
			}
			final RebateItem item = this.getItem(position);
			if(tag.title != null){
				tag.title.setText(item.title);
			}
			if(tag.picUrl != null){
				tag.picUrl.setTag(item.picUrl);
				Bitmap bm = client.tmpImgLoader.cache.get(item.picUrl, winWidth, true, false);
				ImageView img = (ImageView)tag.picUrl;
				if(bm != null){
					img.setScaleType(ScaleType.CENTER_CROP);
					img.setImageBitmap(bm);
				}else {
					img.setScaleType(ScaleType.CENTER_INSIDE);
					img.setImageResource(R.drawable.loading);
					client.tmpImgLoader.runTask(new Runnable(){
						@Override
						public void run() {
							final Bitmap newBm = client.tmpImgLoader.cache.get(item.picUrl, winWidth, true, true);
							if(newBm != null){
								handler.post(new Runnable(){
									@Override
									public void run() {
										if(isRunning){
											View v = getListView().findViewWithTag(item.picUrl);
											if(v != null){
												ImageView v2 = (ImageView)v;
												v2.setScaleType(ScaleType.CENTER_CROP);
												v2.setImageBitmap(newBm);
											}
										}
									}
								});
							}
						}						
					});
				}				
			}
			
			if(tag.price != null){
				String price = item.price; 
				SpannableString spanText = new SpannableString("￥" + price);
				spanText.setSpan(new StrikethroughSpan(), 1, 1 + price.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
				tag.price.setText(spanText);
			}
			
			if(tag.couponPrice != null){
				tag.couponPrice.setText("￥" + item.couponPrice);
			}

			if(tag.couponRate != null){
				tag.couponRate.setText(String.format("%1$1.1f 折", item.couponRate / 1000));
			}	
			
			if(tag.endTime != null){
				String time = TimeHelper.formatRemainHour(item.endTime, 10);
				tag.endTime.setText(time);
			}
			
			return convertView;
		}
		
		/**
		 * 加入一个商品到折扣列表。如果商品已经存在，则忽略操作。
		 */
		public void add(RebateItem item){
			if(loadedItem.add(item.numIId)){
				super.add(item);
			}
		}
		
		public void clear(){
			super.clear();
			this.loadedItem.clear();
		}
	}
	
	class DataLoaderCallback implements LoaderCallbacks<Cursor>{
		//public Lock loading =  
		public boolean isLoading = false;
		private Uri dataSource = null;
		private boolean isLoadMore = false;
		private int pageSize = 20;
		private int pageNo = 0;
		
		public DataLoaderCallback(Uri source, boolean isLoadMore){
			this.dataSource = source;
			this.isLoadMore = isLoadMore;
		}
		
		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			Builder b = dataSource.buildUpon();
			if(isLoadMore){ //加载下一页数据。
				pageNo++;
			}
			b.appendQueryParameter(QueryParam.PAGE_SIZE, pageSize + "");
			b.appendQueryParameter(QueryParam.PAGE_NO, pageNo + "");
			
			return new CursorLoader(getActivity(), b.build(),
					new String[] { BaseColumns._ID, Rebate.NUM_IID, Rebate.TITLE, Rebate.PIC_URL, Rebate.COUPON_PRICE,
				Rebate.PRICE, Rebate.SHORT_KEY, Rebate.COUPON_END_TIME,
				Rebate.COUPON_RATE},
					null, null, null);
		}
		
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
			Log.d("xx", "onLoad finishied, count:" + c.getCount());
			if(c.getCount() == 0){
				if(isLoadMore){
					notFoundMoreData();
				}else {
					showNoItems();
				}
			}else {
				//不是加载更多，清空原始数据。
				if(!isLoadMore){
					adapter.clear();
				}
				
				boolean hasMore = c.moveToFirst();
				int numIndex, titleIndex, picIndex, couponPrice, priceIndex;
				int shortIndex, couponEndIndex, couponRateIndex;
				numIndex = c.getColumnIndex(Rebate.NUM_IID);
				titleIndex = c.getColumnIndex(Rebate.TITLE);
				picIndex = c.getColumnIndex(Rebate.PIC_URL);
				couponPrice = c.getColumnIndex(Rebate.COUPON_PRICE);
				priceIndex = c.getColumnIndex(Rebate.PRICE);
				shortIndex = c.getColumnIndex(Rebate.SHORT_KEY);
				couponEndIndex = c.getColumnIndex(Rebate.COUPON_END_TIME);
				couponRateIndex = c.getColumnIndex(Rebate.COUPON_RATE);
				for( ; hasMore; hasMore = c.moveToNext()){
					RebateItem item = new RebateItem();
					item.numIId = c.getLong(numIndex);
					item.title = c.getString(titleIndex);
					item.picUrl = c.getString(picIndex);
					item.price = c.getString(priceIndex);
					item.couponPrice = c.getString(couponPrice);
					item.couponRate = c.getFloat(couponRateIndex);
					item.shortKey = c.getString(shortIndex);
					item.endTime = c.getString(couponEndIndex);
					adapter.add(item);
				}
				adapter.notifyDataSetChanged();
			}
			isLoading = false; 
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			isLoading = false;
		}		
	}	
}
