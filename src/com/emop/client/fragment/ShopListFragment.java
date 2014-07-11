package com.emop.client.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SyncStateContract.Columns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListView;
import android.widget.TextView;

import com.emop.client.Constants;
import com.emop.client.MutilFragmentActivity;
import com.emop.client.R;
import com.emop.client.fragment.adapter.CreditAdapter;
import com.emop.client.io.FmeiClient;
import com.emop.client.provider.JSONCursor;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Shop;
import com.emop.client.wxapi.DensityUtil;

public class ShopListFragment extends ListFragment{
	public int cateId = 0;
	private CursorAdapter adapter = null;
    protected Handler handler = new Handler();
    private String dataSource = "";
    private String dataFrom = "";
    private boolean isRunning = false;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState){		
		
		View v = inflater.inflate(R.layout.shop_list_view, container, false);
		if(cateId > 0){
			v.setTag(cateId);
		}
		
		ListView listView = (ListView)v.findViewById(android.R.id.list);
		listView.setCacheColorHint(0);		
		
		return v;
	}
	
    @Override 
    public void onInflate(Activity activity, AttributeSet attrs,

            Bundle savedInstanceState) {

        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.FragmentArguments);
        dataFrom = a.getString(R.styleable.FragmentArguments_data_source);
        a.recycle();    
    }
	
	public void reload(){
		initParam();
		Log.d("xx", "reload shop list...." + dataSource);
		getLoaderManager().restartLoader(0, null, callbacks);
	}
	
	public void onResume(){
		super.onResume();
		isRunning = true;
	}
	
	public void onPause(){
		super.onPause();
		isRunning = false;
	}	
	
	private void initParam(){
		final FmeiClient client = FmeiClient.getInstance(null);
		
		Bundle args = this.getArguments();
		if(args != null){
			dataSource = args.getString("uri");
		}
		
		Log.d("xx", "shop list froms:" + dataFrom);
		if(dataFrom != null && dataFrom.equals("fav")){
			String myFavId = client.getFavoriteId();
			if(myFavId == null || myFavId.trim().length() == 0){
				Log.d("xx", "not found myfav id....");
				return;
			}
			dataSource = Schema.SHOP_LIST.toString();		
		}	
	}
	
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		final FmeiClient client = FmeiClient.getInstance(null);
		initParam();
		
		final int picWidth = DensityUtil.dip2px(getActivity(), 80);

		
		adapter = new CursorAdapter(getActivity(), JSONCursor.EMPTY, true){
			private int titleIndex = -1, nickIndex, logoIndex, descIndex, 
				shopTypeIndex, shopIdIndex, creditIndex;
			
			@Override
			public void bindView(View v, Context ctx, Cursor c) {
				if(titleIndex < 0){
					initCursorIndex(c);
				}
				if(shopIdIndex >= 0){
					v.setId(c.getInt(shopIdIndex));
				}
				Items items = (Items)v.getTag();
				if(items == null){
					items = new Items(v);
					v.setTag(items);
				}
				if(items.shopTitle != null && titleIndex >= 0){
					items.shopTitle.setText(c.getString(titleIndex));
				}
				if(items.userNick != null && nickIndex >= 0){
					items.userNick.setText(c.getString(nickIndex));
				}
				if(items.shopDesc != null && descIndex >= 0){
					items.shopDesc.setText(c.getString(descIndex));
				}
				
				String shopType = "";
				if(items.shopTypeLogo != null && shopTypeIndex >= 0){
					shopType = c.getString(shopTypeIndex);
					if(shopType.equals("B")){
						items.shopTypeLogo.setVisibility(View.VISIBLE);
						if(items.credit != null){
							items.credit.setVisibility(View.GONE);
						}
					}else {
						items.shopTypeLogo.setVisibility(View.GONE);						
					}					
				}
				if(items.credit != null && creditIndex >= 0 && shopType.equals("C")){
					int creditLevel = c.getInt(creditIndex);
					items.credit.setAdapter(new CreditAdapter(getActivity(), creditLevel));
					items.credit.setVisibility(View.VISIBLE);
					items.credit.setVerticalScrollBarEnabled(false);
					items.credit.setHorizontalScrollBarEnabled(false);
				}
				
				if(items.shopLogo != null && logoIndex >= 0){
					final String des = c.getString(logoIndex);
					items.shopLogo.setTag(des);
					Bitmap bm = client.tmpImgLoader.cache.get(des, picWidth, false, false);
					ImageView img = (ImageView)items.shopLogo;
					if(bm != null){
						img.setScaleType(ScaleType.CENTER_CROP);
						img.setImageBitmap(bm);
					}else {
						img.setScaleType(ScaleType.CENTER_INSIDE);
						img.setImageResource(R.drawable.loading);
						client.tmpImgLoader.runTask(new Runnable(){
							@Override
							public void run() {
								//Log.d("xxx", "load xxx2:" + des);
								final Bitmap newBm = client.tmpImgLoader.cache.get(des, 80, false, true);
								if(newBm != null){
									handler.post(new Runnable(){
										@Override
										public void run() {
											if(isRunning){
												View v = getListView().findViewWithTag(des);
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
				if(items.addToFav != null){
					items.addToFav.setOnClickListener(addToFavorite);
				}
			}

			@Override
			public View newView(Context ctx, Cursor c, ViewGroup root) {
				Log.d("xx", "newView finishied, index:" + c.getPosition());
				
				View v = getLayoutInflater(null).inflate(R.layout.shop_list_item, null);
				v.setTag(new Items(v));
				return v;
			}
			
			private void initCursorIndex(Cursor c){
				titleIndex = c.getColumnIndex(Shop.SHOP_TITLE);
				nickIndex = c.getColumnIndex(Shop.USER_NICK);
				logoIndex = c.getColumnIndex(Shop.SHOP_LOGO);
				descIndex = c.getColumnIndex(Shop.SHOP_DESC);
				shopTypeIndex = c.getColumnIndex(Shop.SHOP_TYPE);
				shopIdIndex = c.getColumnIndex(Shop.SHOP_ID);
				creditIndex = c.getColumnIndex(Shop.SHOP_CREDIT);
			}
		};
		
		setListAdapter(adapter);
		
		if(dataSource != null && dataSource.length() > 0){
			getLoaderManager().initLoader(0, null, callbacks);
			if(Uri.parse(dataSource) != null){
				Log.d(Constants.TAG_EMOP, "registerContent:" + dataSource);
				getActivity().getContentResolver().registerContentObserver(Uri.parse(dataSource), false, 
		    		new ContentObserver(new Handler()){
			        	public void onChange(boolean selfChange) {
			        		if(isRunning){
				        		Log.d(Constants.TAG_EMOP, "shop list is changed:" + dataSource);
				        		getLoaderManager().getLoader(0).forceLoad();
			        		}
			        	}
		        });		
			}
		}
		

	}
	
	protected LoaderCallbacks<Cursor> callbacks = new LoaderCallbacks<Cursor>() {

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			return new CursorLoader(getActivity(), Uri.parse(dataSource),
					new String[] {Columns._ID, Shop.SHOP_ID, Shop.SHOP_TITLE, Shop.USER_NICK, Shop.SHOP_TYPE, 
					Shop.SHOP_DESC, Shop.SHOP_LOGO, Shop.SHOP_CREDIT
					}, 
					null, null, null);
		}
		
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			if(cursor != null){
				Log.d("xx", "onLoad finishied, count:" + cursor.getCount());
				if(cursor.getCount() == 0){
					showNoItems();							
				}
				adapter.swapCursor(cursor);
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			adapter.swapCursor(null);
		}
	};
	
	public void onListItemClick(ListView l, View v, int position, long id){
		//Log.d("emop", "onListItemClick.....");
		Log.d("emop", "onItemClick, id:" + id);
		Intent intent = new Intent();			
		intent.setClass(getActivity(), MutilFragmentActivity.class);	
		
		//intent.set
		intent.putExtra("view_id", new int[]{R.layout.shop_detail});
		intent.putExtra("shop_id", id + "");
		startActivity(intent);
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
	
	private OnClickListener addToFavorite = new OnClickListener(){

		@Override
		public void onClick(View v) {
			Log.d("emop", "click view:" + v);
		}
		
	};
	
	class Items{
		TextView shopTitle = null;
		TextView userNick = null;
		ImageView shopLogo = null;
		TextView shopDesc = null;
		ImageView shopTypeLogo = null;
		Button addToFav = null;
		GridView credit = null;
		public Items(View root){
			shopTitle = (TextView)root.findViewById(R.id.shop_title);
			userNick = (TextView)root.findViewById(R.id.user_nick);
			shopLogo = (ImageView)root.findViewById(R.id.shop_logo);
			shopDesc = (TextView)root.findViewById(R.id.shop_desc);
			shopTypeLogo = (ImageView)root.findViewById(R.id.shop_type_logo);
			addToFav = (Button)root.findViewById(R.id.add_to_favorite);
			credit = (GridView)root.findViewById(R.id.taobao_credit);
		}
	}
	
}
