package com.emop.client.fragment;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SyncStateContract.Columns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mobstat.StatService;
import com.emop.client.BaseActivity;
import com.emop.client.Constants;
import com.emop.client.R;
import com.emop.client.WebViewActivity;
import com.emop.client.fragment.adapter.CreditAdapter;
import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.io.TaodianApi;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Item;
import com.emop.client.provider.model.Shop;
import com.taobao.top.android.TopAndroidClient;
import com.taobao.top.android.TopParameters;
import com.taobao.top.android.api.ApiError;
import com.taobao.top.android.api.TopApiListener;

public class ShopSummaryFragment extends Fragment {
	private View root = null;
	private Items items = null;
    protected Handler handler = new Handler();	
    private String shopId = null;
    private String dataFrom = "";
    private boolean isRunning = false;
    private int shopRefreshTimes = 0;
    private String shortUrl = null;
    private boolean loaded = false;
    
    
	public void onResume(){
		super.onResume();
		isRunning = true;
	}
	
	public void onPause(){
		super.onPause();
		isRunning = false;
	}    
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState){				
		if(dataFrom != null && dataFrom.equals("taoke_detail")){
			root = inflater.inflate(R.layout.taoke_detail_shop_header, container, false);
		}else {
			root = inflater.inflate(R.layout.shop_detail_header, container, false);			
		}	
		
		items = new Items(root);
		
		Log.d("emop", "onCreateView data from:" + dataFrom);

		root.setClickable(true);
		root.setOnClickListener(new OnClickListener(){
			public void onClick(View arg){
				Log.d("emop", "click shop in item detail");
				ItemActionBar bar = (ItemActionBar)getFragmentManager().findFragmentById(R.id.nav_footer);
				if(bar != null){
					bar.goBuy();
				}else {
					goShopBuy();
				}
			}
		});
		
		return root;
	}
	
    @Override 
    public void onInflate(Activity activity, AttributeSet attrs,

            Bundle savedInstanceState) {

        super.onInflate(activity, attrs, savedInstanceState);
        TypedArray a = activity.obtainStyledAttributes(attrs, R.styleable.FragmentArguments);
        dataFrom = a.getString(R.styleable.FragmentArguments_data_source);
        a.recycle();  
        
		Log.d("emop", "onInflate data from:" + dataFrom);

    }	
    
    public void goShopBuy(){
    	if(shopId != null && shopId.length() > 1 && shopId.length() < 10 && loaded){
    		String url = String.format("http://shop%s.taobao.com/", shopId);
    		if(shortUrl != null && shortUrl.length() > 0){
    			url = "http://c.emop.cn/c/" + shortUrl + "?from=app";
    		}
	    	StatService.onEvent(getActivity(), "go_shop", shopId + "_" + "no_short", 1);
	    	Intent intent = new Intent().setClass(getActivity(), WebViewActivity.class);
	    	intent.putExtra("http_url", url);
	    	this.startActivity(intent);    	
    	}else {
    		Toast.makeText(getActivity(), "店铺信息加载中，稍后再试。", Toast.LENGTH_SHORT).show();
    	}
    }
	
	public void loadShop(String id, String numIId){
		this.shopId = id;
		boolean isOk = false;
		if(shopId != null){
			long tempId = Long.parseLong(shopId);
			if(tempId > 0 && tempId < 1000000000){
				isOk = true;
				handler.post(new Runnable(){
					public void run(){
						if(isRunning){
							getLoaderManager().initLoader(0, null, callbacks);	
						}
					}
				});
			}
		}
		if(!isOk && numIId != null){
			Log.d("emop", "load shop info for num iid:" + numIId);
			FmeiClient.getInstance(null).appImgLoader.runTask(new ShopIdFixPatch(numIId));
		}		
	}	
	
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		
		//Bundle args = this.getArguments();		
		shopId = this.getActivity().getIntent().getStringExtra("shop_id");
		
		loadShop(shopId, null);
	}
	
	
	private LoaderCallbacks<Cursor> callbacks = new LoaderCallbacks<Cursor>() {
		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
	    	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/shop/" + shopId);
			return new CursorLoader(getActivity(), dataUri,
					new String[] {Columns._ID, Shop.SHOP_ID, Shop.SHOP_TITLE, Shop.USER_NICK, Shop.SHOP_TYPE, 
					Shop.SHOP_DESC, Shop.SHOP_LOGO, Shop.SHOP_CREDIT, Shop.SHORT_KEY
					}, 
					null, null, null);
		}
		
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
			if(cursor == null || !isRunning) return;
			Log.d("xx", "onLoad finishied, count:" + cursor.getCount());
			if(cursor.getCount() > 0){
				showShopInfo(cursor);							
			}else if(shopRefreshTimes < 2){
				shopRefreshTimes++;
				FmeiClient.getInstance(null).appImgLoader.runTask(new ForceReloadShop());
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			//adapter.swapCursor(null);
		}
	};
	
	private void showShopInfo(Cursor c){
		int titleIndex = -1, nickIndex, logoIndex, descIndex, 
		shopTypeIndex, shopIdIndex, creditIndex, shortUrlIndex;
		if(c.getCount() == 0) return;
		loaded = true;
		
		c.moveToFirst();
		titleIndex = c.getColumnIndex(Shop.SHOP_TITLE);
		nickIndex = c.getColumnIndex(Shop.USER_NICK);
		logoIndex = c.getColumnIndex(Shop.SHOP_LOGO);
		descIndex = c.getColumnIndex(Shop.SHOP_DESC);
		shopTypeIndex = c.getColumnIndex(Shop.SHOP_TYPE);
		shopIdIndex = c.getColumnIndex(Shop.SHOP_ID);
		creditIndex = c.getColumnIndex(Shop.SHOP_CREDIT);	
		shortUrlIndex = c.getColumnIndex(Shop.SHORT_KEY);

		final FmeiClient client = FmeiClient.getInstance(null);

		if(items.shopTitle != null && titleIndex >= 0){
			items.shopTitle.setText(c.getString(titleIndex));
		}
		if(items.userNick != null && nickIndex >= 0){
			items.userNick.setText(c.getString(nickIndex));
		}
		if(items.shopDesc != null && descIndex >= 0){
			items.shopDesc.setText(c.getString(descIndex));
		}
		
		if(shortUrlIndex >= 0){
			shortUrl = c.getString(shortUrlIndex);
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
			if(des != null && des.length() > 0 && !des.equals("null")){
				client.tmpImgLoader.loadImage(des, items.shopLogo, items.shopLogo.getMeasuredWidth(), false);
			}else if(shopRefreshTimes < 2){
				shopRefreshTimes++;
				client.tmpImgLoader.runTask(new ForceReloadShop());
			}
		}
		
	}
	
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
	
	/**
	 * 这个是个很恶心的错误。手机版大小商品没有相关的ShopId. 需要一个修复的步骤。
	 * 1。 根据商品ID查询到卖家名称。
	 * 2。 根据卖家名称，查询到shop id
	 * 3. 根据商品ID,更新手机商品库的shop id.
	 * @author deonwu
	 *
	 */
	class ShopIdFixPatch extends Thread{
		String iid = null;
		TaodianApi api = new TaodianApi();
		ShopIdFixPatch(String id){
			this.iid = id;
		}
		public void run(){
			//if(!isRunning) {
			//	Log.d("emop", "Fragment not in running status, num id:" + iid);
			//	return;
			//}
	    	final TopAndroidClient client = TopAndroidClient.getAndroidClientByAppKey(Constants.TAOBAO_APPID);
	    	TopParameters param = new TopParameters();
	    	
	    	param.setMethod("taobao.taobaoke.widget.items.convert");
	    	param.addFields("click_url","num_iid,nick");
	    	param.addParam("is_mobile", "true");
	    	param.addParam("num_iids", iid);    		    	
			
	    	TopApiListener listener = new TopApiListener(){
				@Override
				public void onComplete(JSONObject json) {
					// TODO Auto-generated method stub
					String nick = null;
					
					try{
						JSONArray items = json.getJSONObject("taobaoke_widget_items_convert_response").
							getJSONObject("taobaoke_items").getJSONArray("taobaoke_item");
						JSONObject item = items.getJSONObject(0);
						nick = item.getString("nick");
						Log.i("emop", "num iid:" + iid + ", shop nick:" + nick);
						if(nick != null && isRunning){
							getShopId(nick);
						}
					}catch(Throwable e){
						Log.w("emop", "error e:" + e.toString(), e);
					}
				}

				@Override
				public void onError(ApiError error) {
					Log.w("emop", "error e:" + error.toString());
				}

				@Override
				public void onException(Exception e) {
					Log.w("emop", "error e:" + e.toString(), e);
				}
				
	    	};
	    	client.api(param, null, listener, true);
		}
		
		private void getShopId(final String nick){
			handler.post(new Runnable(){
				public void run(){					
					items.userNick.setText(nick);
				}
			});
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("seller_nick", nick);
			param.put("fields", "shop_id,shop_title,user_nick,seller_credit,shop_type");
			
			ApiResult r = api.call("shop_items_list_get", param);
			if(r != null && r.isOK){
				try{
					JSONArray list = r.json.getJSONArray("data");
					JSONObject shop = list.getJSONObject(0);
					shopId = shop.getString("shop_id");
					final String shopTitle = shop.getString("shop_title");
					final int creditLevel = shop.getInt("seller_credit");
					if(creditLevel > 0){
						handler.post(new Runnable(){
							public void run(){					
								items.shopTitle.setText(shopTitle);
								items.credit.setAdapter(new CreditAdapter(getActivity(), creditLevel));
								items.credit.setVisibility(View.GONE);							
							}
						});					
					}
			
					updateShopId();
					loadShop(shopId, null);
				}catch(Exception e){
					Log.w("emop", "error e:" + e.toString(), e);
				}
			}
		}
		
		private void updateShopId(){
			if(shopId == null) return;
			
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("num_iid", iid);
			param.put("shop_id", shopId);
			ContentValues v = new ContentValues();
			v.put("num_iid", iid);
			v.put(Item.SHOP_ID, shopId);			
			getActivity().getContentResolver().update(Schema.ITME_LIST, v, null, null);
						
			ApiResult r = api.call("shop_update_mobile_item_id", param);
			if(r != null && r.isOK){
				Log.w("emop", String.format("Update shop id ok, %s->%s", iid, shopId));
			}			
		}
		
	}
	
	/**
	 * 强制刷新店铺信息。
	 * @author deonwu
	 *
	 */
	class ForceReloadShop extends Thread{
		public void run(){
	    	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/shop/" + shopId);
	    	dataUri = dataUri.buildUpon().appendQueryParameter("force_refresh", "y").build();
			getActivity().getContentResolver().query(dataUri, new String[]{Columns._ID}, null, null, null);
			handler.post(new Runnable(){
				public void run(){
					if(!isDetached() && isRunning){
						getLoaderManager().restartLoader(0, null, callbacks);
					}
				}
			});
		}
	}
}
