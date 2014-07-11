package com.emop.client.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.SyncStateContract.Columns;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.mobstat.StatService;
import com.emop.client.Constants;
import com.emop.client.LoginActivity;
import com.emop.client.MyFavoriteActivity;
import com.emop.client.R;
import com.emop.client.ShareToWeiboActivity;
import com.emop.client.WebViewActivity;
import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Shop;
import com.emop.client.wxapi.Util;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXWebpageObject;

public class ItemActionBar extends Fragment {
	private View root = null;
	private boolean fromMobile = false;
	private String shortKey = "";
	private String logoPic = "";
	private String title = "";
	
    protected Handler handler = new Handler();
	private Button myFavBtn = null;
	private boolean isFav = true;    
	private String shopId = "";
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState){				
		root = inflater.inflate(R.layout.item_action_bar, container, false);
        myFavBtn = (Button) root.findViewById(R.id.nav_btn_myfav);
        View v = root.findViewById(R.id.nav_btn_buy);
        v.setOnClickListener(clickListener);
        myFavBtn.setOnClickListener(clickListener);
        
        v = root.findViewById(R.id.nav_btn_friends);
        v.setOnClickListener(clickListener);
        v = root.findViewById(R.id.nav_btn_share);
        v.setOnClickListener(clickListener);
		
		return root;
	}
	
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		
		shopId = this.getActivity().getIntent().getStringExtra("shop_id");

		if(shopId != null){
			getLoaderManager().initLoader(0, null, 
				new LoaderCallbacks<Cursor>() {

					@Override
					public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
				    	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/shop/" + shopId);

						return new CursorLoader(getActivity(), dataUri,
								new String[] {Columns._ID, Shop.SHOP_ID,  
								Shop.SHORT_KEY, Shop.SHOP_LOGO, Shop.SHOP_TITLE
								}, 
								null, null, null);
					}
					
					@Override
					public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
						Log.d("xx", "onLoad finishied, count:" + cursor.getCount());
						if(cursor.getCount() > 0){
							showShopInfo(cursor);							
						}
					}

					@Override
					public void onLoaderReset(Loader<Cursor> arg0) {
						//adapter.swapCursor(null);
					}
				}
			);
		}			
	}
	
	private void showShopInfo(Cursor cursor){
		cursor.moveToFirst();
		int shortKeyIndex = cursor.getColumnIndex(Shop.SHORT_KEY);		
		shortKey = cursor.getString(shortKeyIndex);
		
		shortKeyIndex = cursor.getColumnIndex(Shop.SHOP_LOGO);		
		logoPic = cursor.getString(shortKeyIndex);

		shortKeyIndex = cursor.getColumnIndex(Shop.SHOP_TITLE);		
		title = cursor.getString(shortKeyIndex);
		
		Log.d("emop", "short index:" + shortKeyIndex + ", short key:" + shortKey);
	}
	
	protected OnClickListener clickListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(v.getId() == R.id.nav_btn_buy){
				goBuy();
			}else if(v.getId() == R.id.nav_btn_myfav){
				onClickAddFavorite(v);
			}else if(v.getId() == R.id.nav_btn_share){
				onShareToWeibo(v);
			}else if(v.getId() == R.id.nav_btn_friends){
				onShareToWX(v);
			}
		}
		
	};
	
    public void goBuy(){
    	if(shortKey != null && shortKey.length() > 0){
	    	String url = "http://c.emop.cn/c/" + shortKey + "?from=app";
	    	if(fromMobile){
	    		url += "&auto_mobile=n";
	    	}else {
	    		url += "&auto_mobile=y";    		
	    	}
	    	StatService.onEvent(getActivity(), "go_shop", shopId + "_" + shortKey, 1);
	    	Intent intent = new Intent().setClass(getActivity(), WebViewActivity.class);
	    	intent.putExtra("http_url", url);
	    	this.startActivity(intent);    	
    	}else {
    		showToast("链接出错无法购买。");
    	}
    }

    
    public void onClickAddFavorite(View v){
		final FmeiClient client = FmeiClient.getInstance(getActivity(), false);         
		final String weiboId = "";
    	if(client.isLogined()){
    		if(true){
	    		new Thread(){
	    			public void run(){
	    				ApiResult r = null;
	    				String msg = "";
	    				if(isFav){
	    					r = client.removeFavorite(weiboId);
	    					msg = "取消收藏";
	    					MyFavoriteActivity.removedList.add(0);
	    				}else {
		    	    		r = client.addFavorite(shopId, title, logoPic, "0", shopId, shortKey, "shop");
		    	    		msg = "添加收藏";
	    				}
	    				isFav = !isFav;
	    				if(r == null){
	    					msg += "失败";
	    				}else if(r.isOK){
	    					msg += "成功";	    					
	    				}else if(!r.isOK){
	    					msg += "失败, 原因：" + r.errorMsg();
	    				}
	    				showToast(msg);
	    				handler.post(new Runnable(){
	    					public void run(){
	    	    				if(isFav){
	    	    					myFavBtn.setText("已收藏");
	    	    				}else {
	    	    					myFavBtn.setText("收藏");
	    	    				}	    						
	    						myFavBtn.setPressed(isFav);
	    					}
	    				});
	    			}
	    		}.start();
    		}else {
    			showToast("不能收藏来自网页的内容");
    		}
    	}else {
    		Intent intent = new Intent();			
    		intent.setClass(getActivity(), LoginActivity.class);
    		this.startActivity(intent);
    	}
    }
    
    public void onShareToWeibo(View v){
    	String trackId = "0";
    	FmeiClient client = FmeiClient.getInstance(null);
    	if(client.trackUserId != null && client.trackUserId.trim().length() > 0){
    		trackId = client.trackUserId;
    	}
		String link = String.format(Constants.WEB_SHOP_LINK, trackId, shopId, shortKey);
		
		Intent intent = new Intent();
		intent.setClass(getActivity(), ShareToWeiboActivity.class);
		intent.putExtra("text", title);
		intent.putExtra("link", link);
		intent.putExtra("picUrl", logoPic);		
		startActivity(intent);
    }  
    
    public void onShareToWX(View v){
    	FmeiClient client = FmeiClient.getInstance(null);
    	IWXAPI api = WXAPIFactory.createWXAPI(getActivity(), com.emop.client.wxapi.Constants.APP_ID, 
				false);
    	
    	if(api.isWXAppInstalled() && api.isWXAppSupportAPI()){
	    	String trackId = "0";
	    	if(client.trackUserId != null && client.trackUserId.trim().length() > 0){
	    		trackId = client.trackUserId;
	    	}
	
			String imageURL = String.format(Constants.WEB_SHOP_LINK, trackId, shopId, shortKey);
			
			try{
				WXWebpageObject webObj = new WXWebpageObject(imageURL);
				
				WXMediaMessage msg = new WXMediaMessage();
				msg.mediaObject = webObj;
				msg.title = "推荐你一个好店";
				msg.description = title;
							
				msg.thumbData = getThumbData();
				
				SendMessageToWX.Req req = new SendMessageToWX.Req();
				req.transaction = buildTransaction("img");
				req.message = msg;
				api.sendReq(req);
				Log.i(Constants.TAG_EMOP, "sendReq");
				
			} catch(Exception e) {
				e.printStackTrace();
			}
    	}else {
    		showToast("没有安装微信应用,不能分享到朋友圈.");
    	}
    }    
    
    protected void showToast(final String msg){
    	handler.post(new Runnable(){
    		public void run(){
    			Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    		}
    	});
    }
    
    private byte[] getThumbData(){
    	FmeiClient client = FmeiClient.getInstance(null);
		Bitmap bmp = client.tmpImgLoader.cache.get(logoPic, 0, false,true);
				
		int w = bmp.getWidth(), h = bmp.getHeight();
		double rate = 200 * 1.0 / w;
		rate = rate > 0.5 ? rate : 0.5;
		w = (int)(w * rate);
		h = (int)(h * rate);
		
		bmp = Bitmap.createScaledBitmap(bmp, w, h, true);
		
		int maxSize = 1024 * 32; 
		byte[] buffer = null;
		
		buffer = Util.bmpToByteArray(bmp, false, 95);
		
		//double rate = 1;
		Bitmap thumbBmp = null;
		while(buffer.length > maxSize){
			rate = Math.sqrt(maxSize * 1.0f / buffer.length);
			Log.d(Constants.TAG_EMOP, "cureate image size:" + buffer.length + ", scaled rate:" + rate);
			w = (int)(w * rate);
			h = (int)(h * rate);
			thumbBmp = bmp;
			bmp = Bitmap.createScaledBitmap(bmp, w, h, true);
			thumbBmp.recycle();
			buffer = Util.bmpToByteArray(bmp, false, 95);
		}
		bmp.recycle();
		
		Log.d(Constants.TAG_EMOP, "final image size:" + buffer.length + ", w:" + w + ", h:" + h);
		
    	return buffer;
    }    
    
	private String buildTransaction(final String type) {
		return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
	}    
    
}
