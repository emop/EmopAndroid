package com.emop.client;

import java.util.List;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.baidu.mobstat.StatService;
import com.emop.client.fragment.ShopSummaryFragment;
import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.provider.model.Item;
import com.emop.client.widget.DialogBuilder;
import com.emop.client.wxapi.DensityUtil;
import com.emop.client.wxapi.Util;
import com.taobao.top.android.TopAndroidClient;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXWebpageObject;
import com.tencent.mm.sdk.platformtools.Log;

public class TaokeItemDetailActivity extends BaseActivity {
	private ProgressDialog loading = null;

	private SimpleCursorAdapter adapter = null;
	// private Cursor cursor = null;
	private TextView taokeDesc = null;
	private TextView taokePrice = null;
	private ImageView taokePic = null;
	private TextView favCountView = null;
	private TextView volumeView = null;
	private Button myFavBtn = null;
	//recent_volme_info
	
	private String shortKey = null;	
	
	private String weiboId = null;
	private String shopId = null;
	private String numId = null;
	private String picUrl = null;
	private String text = null;
	private String price = null;
	private String volume = null;
	private String favCount = null;
	private boolean isFav = false;
	private int curId = 0;
	
	private ProgressBar progress = null;
	private Dialog taobaoLoginDialog = null;
	private static long lastShowTaoboLogin = 0;
	/* 
	 * 用于在短地址跳转的时候，是否需要再次转换为移到设备连接。
	 * 如果已经是手机里面的链接，就不需要再次转换。
	 */
	private boolean fromMobile = false;
	private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.taoke_item_detail);
        
        taokeDesc = (TextView) findViewById(R.id.taoke_message);
        taokePrice = (TextView) findViewById(R.id.taoke_price);
        taokePic = (ImageView) findViewById(R.id.taoke_pic);
        favCountView = (TextView) findViewById(R.id.fav_count);
        volumeView = (TextView) findViewById(R.id.recent_volme_info);
        myFavBtn = (Button) findViewById(R.id.nav_btn_myfav);
        favCountView.setText("0");
        volumeView.setText("最近销量0件");
        
        taokeDesc.setText("加载中...");
        taokePrice.setText("0");
        taokePic.setImageResource(R.drawable.loading);         
        
        progress = (ProgressBar) findViewById(R.id.progressbar_loading);
        progress.setIndeterminate(true);
        client = FmeiClient.getInstance(this.getApplicationContext(), false);
        
        TextView v = (TextView)findViewById(R.id.nav_btn_myfav);
        v.setOnClickListener(navListener);
        v = (TextView)findViewById(R.id.nav_btn_share);
        v.setOnClickListener(navListener);
        v = (TextView)findViewById(R.id.nav_btn_friends);
        v.setOnClickListener(navListener);
        
        if(taokePic != null && false){
        	taokePic.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent().setClass(TaokeItemDetailActivity.this, 
							TaokeItemImageActivity.class);
					intent.setData(getIntent().getData());
					startActivity(intent);
				}
        	});
        	
        }
        
        View picFrame = findViewById(R.id.taoke_pic_frame);
        if(picFrame != null){
        	LayoutParams lp = picFrame.getLayoutParams();
        	if(lp != null){
        		Rect displayRectangle = new Rect();
        		Window window = getWindow();
        		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);         		
        		int height = displayRectangle.height();
        		int paddingHeight =  DensityUtil.dip2px(this, 244);
        		if(height > paddingHeight){
        			height -= paddingHeight;
        		}
        		lp.height = height;
        		Log.d("emop", "update pic height to:" + lp.height);
        	}else {
        		Log.d("emop", "no layout params in detail picture.");        		
        	}
        }
    }
    
    private BitmapDrawable createRepeater(int width, int height){
    	Bitmap src = BitmapFactory.decodeResource(getResources(), R.drawable.detail_info_bg);
    	Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);  
    	Canvas canvas = new Canvas(bitmap);
    	canvas.drawColor(Color.WHITE);
    	
    	Paint mPaint = new Paint();
    	mPaint.setColor(getResources().getColor(R.color.color_hot_item));
    	mPaint.setStrokeWidth(2);
    	
    	int count = (width + src.getWidth() - 1) / src.getWidth();
    	int bgheight = height - src.getHeight();
    	if(bgheight < 0) bgheight = 0;

    	for(int idx = 0; idx < count; ++ idx){
    		canvas.drawBitmap(src, idx * src.getWidth(), bgheight, null);  
    	}
    	
    	canvas.drawLine(0, 0, 0, height, mPaint);    	
    	canvas.drawLine(width-2, 0, width-2, height, mPaint);
    	    	
    	BitmapDrawable bd = new BitmapDrawable(bitmap);  
    	return bd;
    }
    
    protected void onResume (){
    	super.onResume();
    	
    	if(loading != null && loading.isShowing()){
    		loading.dismiss();
    	}
    	/*
    	handler.postDelayed(new Runnable(){
    		public void run(){
	            View info = findViewById(R.id.taoke_info);
	            if(info.getMeasuredHeight() > 0){
	            	info.setBackgroundDrawable(createRepeater(info.getMeasuredWidth(), info.getMeasuredHeight()));   
	            }
    		}
    	}, 100);
    	*/
    	
    	myFavBtn.setPressed(isFav);
    	
    	Intent intent = this.getIntent();
    	Uri dataUri = intent.getData();    	
    	if(dataUri != null && weiboId == null){
    		if(!intent.getBooleanExtra("FromWeb", false)){
    			fromMobile = true;
	    		String[] projection = new String[]{BaseColumns._ID, Item.PIC_URL,
	    				Item.MESSAGE, Item.PRICE, Item.SHORT_KEY,
	    				Item.WEIBO_ID, Item.NUM_IID, Item.SHOP_ID,
	    		};
	    		Cursor cursor = getContentResolver().query(dataUri, projection, null, null, null);
	    		if(cursor != null && cursor.getCount() > 0){
	    			initLayout(cursor);
	    		}
	    		if(cursor != null){
	    			cursor.close();
	    		}
    		}else if(intent.getStringExtra("uuid") != null){
    			progress.setVisibility(View.VISIBLE);
    			fromMobile = true;
    			new DetailLoadTask(intent.getStringExtra("uuid"), true).start();    			
    		}else {
    			progress.setVisibility(View.VISIBLE);
    			fromMobile = false;
    			new DetailLoadTask(null, true).start();
    		}
    	}
    }
    
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);	
		weiboId = null;
	}    
    
    protected void onStop(){
    	super.onStop();
    }
    
	public void onFinish(View v){
		finish();
	}    
    
    public void onClickGoBuy(View v){
    	/*
    	if(!checkTaobaoLogin()) {
    		return;
    	}*/
    	goBuy();
    }
    
    private void goBuy(){
    	StatService.onEvent(this, "go_taobao", numId + "_" + shortKey + "_" + client.userId, 1);
    	
    	String url = "http://c.emop.cn/c/" + shortKey + "?from=app";
    	if(fromMobile){
    		url += "&auto_mobile=n";
    	}else {
    		url += "&auto_mobile=y";    		
    	}
    	Intent intent = new Intent().setClass(this, WebViewActivity.class);
    	intent.putExtra("http_url", url);
    	intent.putExtra("taoke_num_iid", numId);
    	this.startActivity(intent);    	
    }
    
    private void showShop(String shopId, String numIId){
    	ShopSummaryFragment shop  = (ShopSummaryFragment)getSupportFragmentManager().findFragmentById(R.id.shop_summary);
    	if(shop != null){
    		shop.loadShop(shopId, numIId);
    	}
    }
    
    private boolean checkTaobaoLogin(){
    	String u = client.getSettings(Constants.PREFS_TAOBAO_UID);
    	if(u == null || u.trim().length() == 0){
    		if(taobaoLoginDialog == null){
    			taobaoLoginDialog = DialogBuilder.showTaobaoLogin(this, new OnClickListener(){
					@Override
					public void onClick(View v) {
						if(v.getId() == R.id.login_now){
					    	TopAndroidClient topClient = TopAndroidClient.getAndroidClientByAppKey(Constants.TAOBAO_APPID);
					    	if(topClient != null){
					    		String authLink = topClient.getAuthorizeLink();
						    	Intent intent = new Intent().setClass(TaokeItemDetailActivity.this, 
						    			WebViewActivity.class);
						    	Log.d(Constants.TAG_EMOP, "taobao auth link:" + authLink);
						    	intent.putExtra("http_url", authLink);
						    	intent.putExtra("title", "淘宝登陆");
						    	intent.putExtra("taobaoLogin", true);
						    	startActivity(intent);
					    	}else {
					    		showToast("啊哦，网速不给力啊~");
					    	}
						}else {
							goBuy();
						}
						taobaoLoginDialog.dismiss();
					}    				
    			});
    		} 
    		if(System.currentTimeMillis() - lastShowTaoboLogin > 1000 * 120 && taobaoLoginDialog != null){
    			lastShowTaoboLogin = System.currentTimeMillis();
    			taobaoLoginDialog.show();
    			return false;
    		}else {
    			Log.d("xx", "taobaoLoginDialog is null");
    		}
    	}else {
			Log.d("xx", "taobaoLoginDialog uid:" + u);    		
    	}
    	return true;
    }
    
    public void onClickAddFavorite(View v){
    	if(client.isLogined()){
    		if(this.weiboId != null && this.weiboId.length() > 0){
	    		new Thread(){
	    			public void run(){
	    				ApiResult r = null;
	    				String msg = "";
	    				if(isFav){
	    					r = client.removeFavorite(weiboId);
	    					msg = "取消收藏";
	    					MyFavoriteActivity.removedList.add(curId);
	    				}else {
		    	    		r = client.addFavorite(weiboId, text, picUrl, numId, shopId, shortKey);
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
    		intent.setClass(this, LoginActivity.class);
    		this.startActivity(intent);
    	}
    }    
    
    protected void initLayout(Cursor cursor){
    	if(cursor != null && cursor.moveToFirst()){
    		int index = cursor.getColumnIndex(Item.PRICE);
    	    float price = cursor.getFloat(index);
    	    String text = String.format("￥%1$1.2f", price);	  
    	    this.taokePrice.setText(text);

    	    index = cursor.getColumnIndex(Item.MESSAGE);
    	    text = cursor.getString(index);  
    	    this.text = text;
          if(text != null && text.trim().length() > 0){
    	    this.taokeDesc.setText(text); 
          }

    	    index = cursor.getColumnIndex(Item.SHORT_KEY);
    	    this.shortKey = cursor.getString(index);  

    	    index = cursor.getColumnIndex(Item.NUM_IID);
    	    text = cursor.getString(index);  
    	    this.numId = text;

    	    index = cursor.getColumnIndex(Item.SHOP_ID);
    	    text = cursor.getString(index);  
    	    this.shopId = text;
    	    showShop(shopId, numId);

    	    index = cursor.getColumnIndex(Item.WEIBO_ID);
    	    text = cursor.getString(index);  
    	    this.weiboId = text;
    	    
    	    index = cursor.getColumnIndex(Item.PIC_URL);
    	    text = cursor.getString(index);  
    	    this.picUrl = text;
    	    loadPicUrl(text);

    	    index = cursor.getColumnIndex(BaseColumns._ID);
    	    curId = cursor.getInt(index);    	    
    	    
			new DetailLoadTask("taodianhuo_" + weiboId + "_" + shortKey, false).start();
    	}
    }
    
    private void loadPicUrl(String text){
		Display display = null;
        display = getWindowManager().getDefaultDisplay();  
        
        Bitmap bm = client.tmpImgLoader.cache.get(text, display.getWidth(), true, false);
        if(bm != null && !bm.isRecycled()){
        	progress.setVisibility(View.GONE);
        	taokePic.setImageBitmap(bm);
        }else {
        	progress.setVisibility(View.VISIBLE);
        	taokePic.setImageResource(R.drawable.loading);
        	final String picUrl = text;
        	final int disWidth = display.getWidth();
        	client.tmpImgLoader.runTask(new Runnable(){
				@Override
				public void run() {
					final Bitmap bm = client.tmpImgLoader.cache.get(picUrl, disWidth, true,true);
					handler.post(new Runnable(){
						@Override
						public void run() {
							progress.setVisibility(View.GONE);
							if(bm != null){
								taokePic.setImageBitmap(bm);
							}
						}});
				}
			});
        }
    }
    
    private OnClickListener navListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			if(v.getId() == R.id.nav_btn_myfav){
				onClickAddFavorite(v);
			}else if(v.getId() == R.id.nav_btn_share){
				onShareToWeibo(v);
			}else if(v.getId() == R.id.nav_btn_friends){
				onShareToWX(v);				
			}
		}
    	
    };
    
    public void onShareToWeibo(View v){
    	String trackId = "0";
    	if(client.trackUserId != null && client.trackUserId.trim().length() > 0){
    		trackId = client.trackUserId;
    	}
		String link = String.format(Constants.WEB_LINK, trackId, weiboId, shortKey);
		StatService.onEvent(TaokeItemDetailActivity.this, "share_to_wx", numId + "_" + shortKey + "_" + client.userId, 1);
		
		Intent intent = new Intent();			
		intent.setClass(this, ShareToWeiboActivity.class);
		intent.putExtra("text", text);
		intent.putExtra("link", link);
		intent.putExtra("picUrl", this.picUrl);		
		startActivity(intent);
    }
    
    public void onShareToWX(View v){
    	if(api == null){
    		api = WXAPIFactory.createWXAPI(this, com.emop.client.wxapi.Constants.APP_ID, 
    				false);
    	}
    	if(weiboId == null){
    		showToast("不能分享来自外部应用商品.");
    		return;
    	}
    	if(!api.isWXAppInstalled() || !api.isWXAppSupportAPI()){
    		showToast("没有安装微信应用,不能分享到朋友圈.");
    		return;
    	}
    	if(loading == null){
            loading = new ProgressDialog(this);
            loading.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	}
    	loading.setMessage("正在加载微信...");    		
    	loading.show();

    	String trackId = "0";
    	if(client.trackUserId != null && client.trackUserId.trim().length() > 0){
    		trackId = client.trackUserId;
    	}

		String imageURL = String.format(Constants.WEB_LINK, trackId, weiboId, shortKey);
		
		try{
			//WXImageObject imgObj = new WXImageObject();
			//imgObj.imageUrl = imageURL;
			WXWebpageObject webObj = new WXWebpageObject(imageURL);
			
			WXMediaMessage msg = new WXMediaMessage();
			msg.mediaObject = webObj;
			msg.title = "亲，帮忙给点建议";
			msg.description = text;
						
			msg.thumbData = getThumbData(taokePic);
			
			SendMessageToWX.Req req = new SendMessageToWX.Req();
			req.transaction = buildTransaction("img");
			req.message = msg;
			if(api.sendReq(req)){
				StatService.onEvent(TaokeItemDetailActivity.this, "share_to_wx", numId + "_" + shortKey + "_" + client.userId, 1);
			}else {
				showToast("微信分享失败，确认微信已经打开。");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
    }
    
    private byte[] getThumbData(View v){
		//v.setDrawingCacheEnabled(true);			
		//Bitmap bmp = Bitmap.createBitmap(v.getDrawingCache());
		//v.setDrawingCacheEnabled(false);
		Display display = getWindowManager().getDefaultDisplay();  
		final int disWidth = display.getWidth();
		Bitmap bmp = client.tmpImgLoader.cache.get(this.picUrl, disWidth, true,true);
				
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
    
    class DetailLoadTask extends Thread{
    	String uuid = null;
    	boolean loadPic = true;
    	DetailLoadTask(String uuid, boolean loadPic){
    		this.uuid = uuid;
    		this.loadPic = loadPic;
    	}
    	
    	public void run(){
    		Uri dataUri = getIntent().getData();
    		if(uuid == null){
	    		List<String> seg = dataUri.getPathSegments();
	    		if(seg.size() >= 2){
	    			uuid = seg.get(1);
	    		}
    		}
    		if(uuid == null)return;
    		Log.d(Constants.TAG_EMOP, "load weibo with uuid:" + uuid + ", uri:" + dataUri.toString());
    		final ApiResult r = client.getWeiboInfo(uuid);

    		if(r != null && r.isOK){
    			text = r.getString("data.message");
    			shortKey = r.getString("data.short_url_key");
    			weiboId = r.getString("data.lib_id");
    			numId = r.getString("data.num_iid");
    			price = r.getString("data.price");
    			picUrl = r.getString("data.pic_url");
    			volume = r.getString("data.volume");
    			favCount = r.getString("data.fav_count");
    			isFav = (r.getString("data.is_fav") + "").equals("1");
    			
    			handler.post(new Runnable(){
    				public void run(){
    					if(text != null){
    						taokeDesc.setText(text);
    					}
    					if(price != null){
    						price = String.format("￥%1$1.2f", Float.parseFloat(price));	
    						taokePrice.setText(price);
    					}
    					if(favCount != null){
    						favCountView.setText(favCount);
    					}
    					if(volume != null){
    						volumeView.setText("最近销量" + volume + "件");    						
    					}
    					
	    				if(isFav){
	    					myFavBtn.setText("已收藏");
	    				}else {
	    					myFavBtn.setText("收藏");
	    				}	    					
						myFavBtn.setPressed(isFav);
    					
    					if(loadPic){
    						loadPicUrl(picUrl);
    					}
    					
    					
    				}
    			});
    		}else if(loadPic){
    			Log.d(Constants.TAG_EMOP, "failed to load weibo with uuid:" + uuid + ", uri:" + dataUri.toString());
    			handler.post(new Runnable(){
    				public void run(){
    					String error = "";
    					if(r != null){
    						error = "啊哦，网速不给力啊~";
    					}else {
    						error = "啊哦，网速不给力啊~";    						
    					}
    					showToast(error);
    					finish();
    				}
    			});
    		}
    	}
    }
    
}
