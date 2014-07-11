package com.emop.client;



import static com.emop.client.Constants.TAG_EMOP;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.emop.client.web.TaobaoLoginWebClient;
import com.taobao.top.android.TopAndroidClient;
import com.taobao.top.android.TopParameters;
import com.taobao.top.android.api.ApiError;
import com.taobao.top.android.api.TopApiListener;
import com.tencent.mm.sdk.platformtools.Log;

public class WebViewActivity extends BaseActivity {
	public final static int WEB_DONE = 1;
	public final static int WEB_MSG = 2;
	public final static int WEB_LOADING = 3;
	public final static int WEB_LOADED = 4;	
	
	private ProgressBar processBar = null;
	private WebView web = null;
	private TextView titleView = null;
	private String curURL = "";
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        
        this.web = (WebView)findViewById(R.id.web);
        this.processBar = (ProgressBar)findViewById(R.id.progressbar_loading);
        
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        //web.getSettings().s
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        web.setDownloadListener(new DownloadListener(){
			@Override
			public void onDownloadStart(String url, String userAgent, String contentDisposition,
					String mimetype, long contentLength) {
	            Uri uri = Uri.parse(url);  
	            Intent intent = new Intent(Intent.ACTION_VIEW, uri);  
	            startActivity(intent); 
			}
        });
        
        CookieSyncManager.createInstance(this);
        
        //web.setWebViewClient(new TaokeWebViewClient());
        
        titleView = (TextView)findViewById(R.id.title);
        
        titleView.setLongClickable(true);        
        titleView.setOnLongClickListener(new OnLongClickListener(){
			@Override
			public boolean onLongClick(View arg0) {
				if(curURL != null && curURL.length() > 1){
		            Uri uri = Uri.parse(curURL);  
		            Intent intent = new Intent(Intent.ACTION_VIEW, uri);  
		            startActivity(intent); 
				}
				return false;
			}			
        });        
    }
	
    protected void onResume (){
    	super.onResume();
    	
    	Intent intent = this.getIntent();
    	String title = intent.getStringExtra("title");
    	if(title != null && title.trim().length() > 0 && titleView != null){
    		titleView.setText(title);
    	}
    	if(intent.getBooleanExtra("taobaoLogin", false)){
    		web.setWebViewClient(new TaobaoLoginWebClient(this, processBar));
    		if(processBar != null){
    			processBar.setVisibility(View.INVISIBLE);
    		}
    	}else {
    		web.setWebViewClient(new TaokeWebViewClient());
    	} 	
    	//Uri dataUri = intent.getData();    
    	String http_url = intent.getStringExtra("http_url");
    	String num_iid = intent.getStringExtra("taoke_num_iid");

    	boolean autoMobile = autoConvertMobileLink(num_iid);
    	if(!autoMobile && http_url != null && http_url.startsWith("http")){
    		Log.d(Constants.TAG_EMOP, "loading url:" + http_url);
    		web.loadUrl(http_url);
    	}
    	
    	/**
    	 * 如果是淘宝商品，在客户端转换后跳转。
    	 */
    	if(autoMobile){
    		loadTaoboItem(num_iid, http_url);
    	}
    }
    
    /**
     * 判断是否需要自动转换，移动版链接。只有冒泡自己的帐号才需要转换链接。
     * @param num_iid
     * @return
     */
    protected boolean autoConvertMobileLink(String num_iid){
    	if(num_iid == null) return false;
    	if(client.trackUserId != null && !client.trackUserId.equals("11")){
    		return false;
    	}
    	return true;
    }
    
    protected void loadTaoboItem(final String numiid, final String shortUrl){
    	final TopAndroidClient client = TopAndroidClient.getAndroidClientByAppKey(Constants.TAOBAO_APPID);
    	TopParameters param = new TopParameters();
    	
    	param.setMethod("taobao.taobaoke.widget.items.convert");
    	param.addFields("click_url","num_iid");
    	param.addParam("is_mobile", "true");
    	param.addParam("num_iids", numiid);    	
		
    	TopApiListener listener = new TopApiListener(){

			@Override
			public void onComplete(JSONObject json) {
				// TODO Auto-generated method stub
				String click = null;
				
				try{
					JSONArray items = json.getJSONObject("taobaoke_widget_items_convert_response").
						getJSONObject("taobaoke_items").getJSONArray("taobaoke_item");
					JSONObject item = items.getJSONObject(0);
					click = item.getString("click_url");
					Log.i("emop", "num iid:" + numiid + ", convert click url:" + click);
				}catch(Exception e){
					Log.w("emop", "error e:" + e.toString(), e);
				}finally{
					if(click != null){
						loadMobileUrl(click);
					}else {						
						loadShortUrl(shortUrl);
					}
					
				}
			}

			@Override
			public void onError(ApiError error) {
				Log.w("emop", "error e:" + error.toString());
				loadShortUrl(shortUrl);
			}

			@Override
			public void onException(Exception e) {
				Log.w("emop", "error e:" + e.toString(), e);
				loadShortUrl(shortUrl);
			}
			
			private void loadMobileUrl(String url){
				web.loadUrl(url);
			}

			private void loadShortUrl(String url){
				web.loadUrl(url);
			}
			
    	};
    	client.api(param, null, listener, true);
    }
    
    public void onFinish(View v){
    	onBackPressed();
    }
    
    public void onBackPressed() {
    	if(web.canGoBack()){
    		web.goBack();
    	}else {
    		finish();
    	}
    }
    
    public Handler handler = new Handler(){
    	
    	public void handleMessage(final Message msg) {
    		String message = null;
    		if(msg.obj != null){
    			message = msg.obj.toString();
    			if(message != null){
    				Toast.makeText(WebViewActivity.this, message, Toast.LENGTH_LONG).show();
    			}
    		}
    		if(msg.what == WEB_DONE){
    			finish();
    		}else if(msg.what == WEB_LOADING){
    			processBar.setVisibility(View.VISIBLE);
    		}else if(msg.what == WEB_LOADED){
    			processBar.setVisibility(View.INVISIBLE);
    		}
    	}

    };    
    
    private class TaokeWebViewClient extends WebViewClient {
    	private boolean inTaobao = false;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d("xx", "url:" + url);
            curURL = url;
            //mSpinner.show();
            processBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d("xx", "done url:" + url);
            //curURL = url;
            
            /**
             * 刚进入宝贝详情页时，清空回退记录。这样在点回退的时候才能退出详情页。
             * 不然是退回到短网址页面，会再次进入详情页。
             */
            if((!inTaobao && isProductUrl(url)) || url.endsWith("taobao.com/")){
            	inTaobao = true;
            	web.clearHistory();
            }            
            processBar.setVisibility(View.INVISIBLE);
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG_EMOP, "Redirect URL: " + url);
            
            int i = url.indexOf("m.taobao.com");
        	if(i > 0 && i < 15 && isAvilible(getApplicationContext(), "com.taobao.taobao")){
        		Intent intent = new Intent();			
        		//intent.setClass(this, LoginActivity.class);
        		url = url.replaceFirst("http:", "itaobao:");
        		intent.setAction(Intent.ACTION_VIEW);
        		intent.setData(Uri.parse(url));
        	//	intent.setComponent(ComponentName.unflattenFromString("com.taobao.taobao/com.taobao.tao.detail.DetailActivity"))
        		startActivityForResult(intent, OPEN_TAOBAO);    	
        		finish();
        		return true;
        	}else {
        		return false;
        	}
        }
        
        public boolean isProductUrl(String url){
        	if(url.indexOf("s.click") > 0 || url.indexOf("view_shop.htm") > 0){
        		return false;
        	}
        	if(url.indexOf("tmall.com") > 0 || url.indexOf("m.taobao.com") > 0){
        		return true;
        	}
        	return false;
        }
    }
    
    private boolean isAvilible(Context context, String packageName){ 
        final PackageManager packageManager = context.getPackageManager();//获取packagemanager 
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);//获取所有已安装程序的包信息 
        List<String> pName = new ArrayList<String>();//用于存储所有已安装程序的包名 
        //从pinfo中将包名字逐一取出，压入pName list中 
        if(pinfo != null){ 
            for(int i = 0; i < pinfo.size(); i++){ 
                String pn = pinfo.get(i).packageName; 
                if(pn.startsWith(packageName)){
                	return true;
                }
                //pName.add(pn); 
            } 
        } 
        return false; //pName.contains(packageName);//判断pName中是否有目标程序的包名，有TRUE，没有FALSE 
  } 
}
