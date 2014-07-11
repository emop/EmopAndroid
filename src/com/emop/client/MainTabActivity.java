package com.emop.client;

import static com.emop.client.Constants.TAG_EMOP;

import java.util.Date;
import java.util.List;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TabHost;

import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.provider.Schema;
import com.emop.client.tasks.ClientDataRefresh;
import com.emop.client.tasks.UpgradeCheckTask;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

public class MainTabActivity extends TabActivity{
	private IWXAPI api;
	private LinearLayout navBackGroup = null;
	private LinearLayout navText = null;
	private int tabCount = 0;
	public int curTabIndex = -1;
	public TabHost tabHost = null;

	private NavMenuListener navListener = null;
	//private LookMashClient client = null;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    Log.d(Constants.TAG_EMOP, "on create on tab view");
	    	    
	    //注册微信插件服务。
        api = WXAPIFactory.createWXAPI(this, com.emop.client.wxapi.Constants.APP_ID, false);
        api.registerApp(Constants.APP_ID);
        
	    setContentView(R.layout.tab_main);
	    
	    Resources res = getResources(); // Resource object to get Drawables
	    tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab
	    
	    intent = new Intent().setClass(this, GuangActivity.class);
	    spec = tabHost.newTabSpec("guang").setIndicator("逛街",
	                      res.getDrawable(R.drawable.ic_launcher))
	                  .setContent(intent);
	    tabHost.addTab(spec);	    

	    intent = new Intent().setClass(this, HotActivity.class);
	    spec = tabHost.newTabSpec("hot").setIndicator("热门",
	                      res.getDrawable(R.drawable.ic_launcher))
	                  .setContent(intent);
	    tabHost.addTab(spec);	    

	    
	    intent = new Intent().setClass(this, CateListActivity.class);
	    spec = tabHost.newTabSpec("cate_list").setIndicator("分类",
	                      res.getDrawable(R.drawable.ic_launcher))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    
	    intent = new Intent().setClass(this, MyFavoriteActivity.class);
	    spec = tabHost.newTabSpec("my_favorite").setIndicator("收藏",
	                      res.getDrawable(R.drawable.ic_launcher))
	                  .setContent(intent);
	    tabHost.addTab(spec);	   
	    intent = new Intent().setClass(this, SettingActivity.class);
	    spec = tabHost.newTabSpec("my_setting").setIndicator("设置",
	                      res.getDrawable(R.drawable.ic_launcher))
	                  .setContent(intent);
	    tabHost.addTab(spec);	   
	    
	    int curTab = 0;
	    if(savedInstanceState != null){
	    	curTab = savedInstanceState.getInt("curTab", 0);
	    	if(curTab > 0){
	    		activeTab(curTab);
	    	}
	    }
	    tabHost.setCurrentTab(curTab);
	    
	    this.tabCount = tabHost.getTabWidget().getChildCount();
	    
	    
	    initNavEventListner();	    
	    checkLaunchUri();
	    checkNewVersion();
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);	
	    checkLaunchUri();
	}
	
	public void activeTab(int t){ 
    	if(navListener != null){
    		navListener.onClick(navText.getChildAt(t));
    	}		
	}
	
    
    protected void onSaveInstanceState(Bundle outState){
    	outState.putInt("curTab", getTabHost().getCurrentTab());
    }	
	
    protected void onResume (){
    	super.onResume();  
    }
    
    protected void checkLaunchUri(){
    	FmeiClient c = FmeiClient.getInstance(this);
		new ClientDataRefresh(this,
				getWindowManager().getDefaultDisplay()		
		).start();
    	
    	String strUri = this.getIntent().toUri(Intent.URI_INTENT_SCHEME);
    	
    	Uri uri = null;
    	try {
    		uri = Uri.parse(strUri);
    		Log.e(Constants.TAG_EMOP, "start with uri:" + strUri + ", schema:" + getIntent().getScheme());
    		if(getIntent().getScheme() != null && getIntent().getScheme().equals("taodianhuo")){
	    		List<String> path = uri.getPathSegments();
	    		if(path.size() > 2){
	    			if(path.get(0).equals("tid")){
	    				startActivityWith(path.get(1), path.get(2));
	    			}
	    		}
    		}
		} catch (Exception e) {
			Log.e(Constants.TAG_EMOP, "error url:" + e.toString(), e);
		} 
    }
    
    protected void checkNewVersion(){
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		long lastCheckTime = settings.getLong("last_check_version", 0);
		Log.d("emop", "last upgrade check time," + lastCheckTime + ", date:" + new Date(lastCheckTime));
		if(System.currentTimeMillis() - lastCheckTime > 60 * 60 * 24 * 1000){
			new UpgradeCheckTask(MainTabActivity.this, null, true).execute(null);
			Editor editor = settings.edit();
			editor.putLong("last_check_version", System.currentTimeMillis());
			editor.commit();
		}
    }
    
    //通过Web启动应用后，切换到之前显示的商品详细信息。
    protected void startActivityWith(String trackId, String viewItem){
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		String oldTrackId = settings.getString(Constants.PREFS_TRACK_ID, "");
		if((oldTrackId == null || oldTrackId.trim().length() == 0 || oldTrackId.trim().equals("0")) &&
				trackId != null && trackId.length() > 0){
			//new CheckTrackId().execute(trackId);
		}else {
			Log.d(TAG_EMOP, "already have track id:" + oldTrackId);
		}
		
		if(viewItem != null && viewItem.trim().length() > 0){
	    	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/item/" + viewItem);
			Intent intent = new Intent().setClass(this, 
					TaokeItemDetailActivity.class);
			intent.setData(dataUri);
			intent.putExtra("FromWeb", true);
			startActivity(intent); 
		}
    }
	
    /*
     * 把自定义的导航栏和TabHost的导航联动起来。
     */
	private void initNavEventListner(){
		FrameLayout layout =  (FrameLayout) findViewById(R.id.main_menu_container);
		
		navBackGroup = (LinearLayout)layout.getChildAt(0);
		navText = (LinearLayout)layout.getChildAt(1);
		
		navListener = new NavMenuListener();		
		for(int i = 0; i < navText.getChildCount(); i++){
			navText.getChildAt(i).setOnClickListener(navListener);
		}		
	}
		
	
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.context_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	return super.onOptionsItemSelected(item);
    }
    
    class NavMenuListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			View tv = null;
			int tmpIndex = -1;
			for(int i = 0; i < navText.getChildCount(); i++){
				tv = navText.getChildAt(i);
				tv.setEnabled(true);
				if(tv.getId() == v.getId()){
					tmpIndex = i;
				}
								
				tv = navBackGroup.getChildAt(i);
				if(tmpIndex == i){
					tv.setVisibility(View.VISIBLE);
				}else {
					tv.setVisibility(View.INVISIBLE);
				}
			}
			v.setEnabled(false);
			
			if(tmpIndex >= 0 && curTabIndex < tabCount){
				curTabIndex = tmpIndex;
				getTabHost().setCurrentTab(curTabIndex);
			}			
		}
    	
    }
    
    class CheckTrackId extends AsyncTask<String, Void, Void>{

		@Override
		protected Void doInBackground(String... ids) {
			String tid = ids[0];
			FmeiClient client = FmeiClient.getInstance(MainTabActivity.this, true);
			ApiResult r = client.checkTrackId(tid);
			if(r != null && r.isOK){
				client.trackUserId = tid;
				SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		    	SharedPreferences.Editor editor = settings.edit();
		    	editor.putString(Constants.PREFS_TRACK_ID, tid);
		    	editor.commit();
		    	Log.d(TAG_EMOP, "save track user:" + tid);
			}
			
			return null;
		}
    	
    }
}
