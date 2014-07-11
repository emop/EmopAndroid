package com.emop.client.io;

import static com.emop.client.Constants.TAG_EMOP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.emop.client.Constants;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Item;
import com.emop.client.provider.model.Topic;

/**
 * 管理Fmei数据传输和界面交互的接口。
 * 
 * 向下控制网络传输，加载数据。
 * 向上接受界面的请求。在数据加载成功后，异步的通知界面更新。
 * 
 * 数据优先在本地数据库查询，过期或没有找到，通过网络加载。数据成功加载后，先保存到
 * 本地数据库。让后将本地数据库的查询结果返回给界面处理。
 * @author deonwu
 */
public class FmeiClient {
	//public ImageCache cache = null;
	//需要长时间保存的图片，例如分类，热门。
	public ImageLoader appImgLoader = null;
	
	//临时图片加载，比如瀑布流图片。
	public ImageLoader tmpImgLoader = null;
	
	public String userId = null;
	//推荐应用下载的用户ID. 应用里面的链接都是包含这个用的PID
	public String trackUserId = null;
	public String trackPID = null;
	
	/**
	 * 收藏夹ID.
	 */
	public String favoriteId = null;
	public boolean isInited = false;

	private static FmeiClient ins = null;
	private TaodianApi api = null;
	private Context ctx = null;
	private AppConfig config = null;
	
	//private stai
	
	public FmeiClient(){
		this.api = new TaodianApi();
	}
	
	public static FmeiClient getInstance(Context ctx){
		return getInstance(ctx, false);
	}
	
	public static synchronized FmeiClient getInstance(Context ctx, boolean check_conn){
		if(ins == null){
			ins = new FmeiClient();
		}
		if(ctx != null){
			ins.ctx = ctx;
			ins.api.ctx = ctx;
		}
		if(ins.appImgLoader == null && ctx != null){
			File picDir = null;
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				try{
					picDir = ctx.getExternalFilesDir("doudougou");
				}catch(Throwable e){
					Log.w(TAG_EMOP, "Error:" + e.toString(), e);
				}
				if(picDir == null){
					picDir = new File(Environment.getExternalStorageDirectory(), "doudougou");
				}
				Log.i(TAG_EMOP, "App cache dir:" + picDir.getAbsolutePath());
				if(!picDir.isDirectory()){
					if(!picDir.mkdirs()){
						picDir = ctx.getCacheDir();
					}
				}
				if(!picDir.canWrite()){
					picDir = ctx.getCacheDir();
				}
			}else {
				Log.i(TAG_EMOP, "The external storage is disabled.");				
				picDir = ctx.getCacheDir();				
			}
			Log.i(TAG_EMOP, "App image dir:" + picDir.getAbsolutePath());
			ins.appImgLoader = new ImageLoader(ctx, picDir, 
					0, 8); 
			ins.tmpImgLoader = ins.appImgLoader;
		}
		
		if(check_conn){
			ins.check_networking(ctx);
		}
		return ins;
	}
	
	public boolean isLogined(){
		return this.userId != null && this.userId.length() > 0 && Integer.parseInt(this.userId) > 0;
	}
	
	/**
	 * 链接一下taodian API看看网络是否正常。
	 * @param ctx
	 * @return
	 */
	public ApiResult check_networking(Context ctx){
		return this.api.connect(ctx);
	}
	
	/*
	 * 取得专题列表.
	 */
	public Cursor getTopicList(ContentResolver contentResolver){
		Cursor c = contentResolver.query(Schema.TOPIC_LIST, 
				new String[] {BaseColumns._ID, Topic.TITLE, Topic.ITEM_COUNT,
				Topic.DESC, 
				Topic.FRONT_PIC, 
				Topic.UPDATE_TIME,
				Topic.VIEW_ORDER
		}, null, null, null);
		return c;
	}
	
	/*
	 * 取得分类列表.
	 */
	public Cursor getCateList(ContentResolver contentResolver){
		Cursor c = contentResolver.query(Schema.CATE_LIST, 
				new String[] {BaseColumns._ID, Topic.TITLE, Topic.ITEM_COUNT,
				Topic.DESC, 
				Topic.FRONT_PIC, 
				Topic.UPDATE_TIME
		}, null, null, null);
		return c;
	}
	
	/*
	 * 取得热门分类列表.
	 */
	public Cursor getHotCateList(ContentResolver contentResolver){
		Cursor c = contentResolver.query(Schema.HOT_CATE_LIST, 
				new String[] {BaseColumns._ID, Topic.TITLE, Topic.ITEM_COUNT,
				Topic.DESC, 
				Topic.FRONT_PIC, 
				Topic.UPDATE_TIME
		}, null, null, null);
		return c;
	}	

	/*
	 * 取得专题列表.
	 */
	public Cursor getItemList(ContentResolver contentResolver, Uri uri){
		Log.d(com.emop.client.Constants.TAG_EMOP, "on getItemList:" + uri.toString());
		
		Cursor c = contentResolver.query(uri, 
				new String[] {BaseColumns._ID, Item.SHORT_KEY, Item.PIC_URL,
				Item.ITEM_CONTENT_TYPE, 
				Item.UPDATE_TIME, Item.WEIBO_ID,
				Item.PRICE,
				Item.MESSAGE,
				Item.RECT_RATE
		}, null, null, null);
		
		return c;
	}
	
	/**
	 * 通过uri地址更新内容列表。
	 * @param contentResolver
	 * @param uri
	 * @return
	 */
	public ApiResult refreshDataByUri(ContentResolver contentResolver, Uri uri){
		return refreshDataByUri(contentResolver, uri, TaodianApi.STATUS_NORMAL);
	}
	
	/**
	 * 通过uri地址更新内容列表。
	 * @param contentResolver
	 * @param uri
	 * @param async -- 是否异步返回结果。 如果为true数据在后台线程保存到数据库。网络返回后
	 * @return
	 */	
	public ApiResult refreshDataByUri(ContentResolver contentResolver, Uri uri, int status){
		return refreshDataByUri(contentResolver, uri, status, false);
	}	
	
	public ApiResult refreshDataByUri(ContentResolver contentResolver, Uri uri, int status, boolean sync){
		Log.e(Constants.TAG_EMOP, "refresh uri:" + uri.toString());
		ApiResult r = null;
        switch (Schema.FmeiUriMatcher.match(uri)) {
        	case Schema.TYPE_CATE_ITEM_LIST:
        	case Schema.TYPE_HOT_ITEM_LIST:
        	case Schema.TYPE_TOPIC_ITEM_LIST:
        		r = refreshTopicItemList(contentResolver, uri, sync, null);
        		break;
        	case Schema.TYPE_TOPICS:
        		r = this.refreshTopicList(contentResolver, status, "");
        		break;
        	case Schema.TYPE_CATES:
        		r = this.refreshCateList(contentResolver, status);
        		break;
        	case Schema.TYPE_HOTS:
        		r = this.refreshHotCatList(contentResolver, status);
        	case Schema.TYPE_ACT_ITEM_LIST:
        		r = this.refreshActivityItemList(contentResolver);
        	case Schema.TYPE_MYFAV_ITEM_LIST:
        		r = this.refreshMyFavoriteItemList(contentResolver);
        }
		
		return r;
	}
	
	/**
	 * 
	 * @param contentResolver
	 * @param uri
	 */
	public void refreshUri(ContentResolver contentResolver, Uri uri){
		
	}
	
	public ApiResult refreshTopicList(ContentResolver contentResolver){
		return refreshTopicList(contentResolver, TaodianApi.STATUS_NORMAL, "");
	}
	/**
	 * 刷新专题列表。
	 * @param contentResolver
	 */
	public ApiResult refreshTopicList(ContentResolver contentResolver, int status, String noCache){
		Log.e(Constants.TAG_EMOP, "refreshList....");
		ApiResult r = this.api.getTopicList(10, status, noCache);
		if(r.isOK){
			JSONObject json = null;
			try {
				json = r.json.getJSONObject("data");
				JSONArray jarray = json.getJSONArray("items");
				for(int i = 0; i < jarray.length(); i++){
					contentResolver.update(Schema.TOPIC_LIST, 
							Topic.convertJson(jarray.getJSONObject(i)), 
							null, null);
				}
				//contentResolver.notifyChange(Schema.TOPIC_LIST, null);
			} catch (JSONException e) {
			}
		}else {
			Log.d(Constants.TAG_EMOP, "Failed to refresh list:" + r.errorMsg());
		}
		return r;
	}
	
	public ApiResult refreshCateList(ContentResolver contentResolver, int status){
		Log.e(Constants.TAG_EMOP, "refresh cate List....");
		ApiResult r = this.api.getCateList(10, status);
		if(r.isOK){
			JSONObject json = null;
			try {
				json = r.json.getJSONObject("data");
				JSONArray jarray = json.getJSONArray("items");
				for(int i = 0; i < jarray.length(); i++){
					contentResolver.update(Schema.CATE_LIST, 
							Topic.convertJson(jarray.getJSONObject(i)), 
							null, null);
				}
				contentResolver.notifyChange(Schema.CATE_LIST, null);
			} catch (JSONException e) {
			}
		}else {
			Log.d(Constants.TAG_EMOP, "Failed to refresh list:" + r.errorMsg());
		}
		return r;
	}
	
	public ApiResult refreshHotCatList(ContentResolver contentResolver, int status){
		Log.e(Constants.TAG_EMOP, "refresh hot cate List....");
		ApiResult r = this.api.getHotCateList(10, status);
		if(r.isOK){
			JSONObject json = null;
			try {
				json = r.json.getJSONObject("data");
				JSONArray jarray = json.getJSONArray("items");
				for(int i = 0; i < jarray.length(); i++){
					contentResolver.update(Schema.HOT_CATE_LIST, 
							Topic.convertJson(jarray.getJSONObject(i)), 
							null, null);
				}
				contentResolver.notifyChange(Schema.CATE_LIST, null);
			} catch (JSONException e) {
			}
		}else {
			Log.d(Constants.TAG_EMOP, "Failed to refresh list:" + r.errorMsg());
		}
		return r;
	}
	
	public ApiResult refreshActList(ContentResolver contentResolver){
		Log.e(Constants.TAG_EMOP, "refresh act topic List....");
		ApiResult r = this.api.getActList(1, TaodianApi.STATUS_NORMAL);
		if(r.isOK){
			JSONObject json = null;
			try {
				json = r.json.getJSONObject("data");
				JSONArray jarray = json.getJSONArray("items");
				for(int i = 0; i < jarray.length(); i++){
					contentResolver.update(Schema.ACTIVITY_LIST, 
							Topic.convertJson(jarray.getJSONObject(i)), 
							null, null);
				}
			} catch (JSONException e) {
			}
		}else {
			Log.d(Constants.TAG_EMOP, "Failed to refresh list:" + r.errorMsg());
		}
		return r;
	}	
	
	/**
	 * 刷新专题图片列表。
	 * @param contentResolver
	 */
	public ApiResult refreshTopicItemList(ContentResolver contentResolver, int topic_id){
		Uri topicList = Uri.parse("content://" + Schema.AUTHORITY + "/topic/" + topic_id + "/list");
		
		return this.refreshTopicItemList(contentResolver, topicList);
	}
	
	/**
	 * 刷新活动图片列表。
	 * @param contentResolver
	 */
	public ApiResult refreshActivityItemList(ContentResolver contentResolver){
		ApiResult r = null;
		int topicId = getActivityTopicId(contentResolver);
		Uri topicList = Uri.parse("content://" + Schema.AUTHORITY + "/act/" + topicId + "/list");		
		r = this.refreshTopicItemList(contentResolver, topicList);
		return r;
	}	
	
	/**
	 * 刷新我的收藏活动图片列表。
	 * @param contentResolver
	 */
	public ApiResult refreshMyFavoriteItemList(ContentResolver contentResolver){
		ApiResult r = null;
		if(this.isLogined()){
			String fid = this.getFavoriteId();
			Uri topicList = Uri.parse("content://" + Schema.AUTHORITY + "/myfav/" + fid + "/list");	
			Log.e(Constants.TAG_EMOP, "refresh myfav item list:" + fid);
			r = this.api.getMyFavoriteItemList(fid, this.userId);
			if(r.isOK){
				JSONObject json = null;
				try {
					json = r.json.getJSONObject("data");
					JSONArray jarray = json.getJSONArray("items");
					for(int i = 0; i < jarray.length(); i++){
						contentResolver.update(topicList, 
								Item.convertJson(jarray.getJSONObject(i)), 
								null, null);
					}
				} catch (JSONException e) {
				}
			}else {
				Log.d(Constants.TAG_EMOP, "Failed to refresh list:" + r.errorMsg());
			}		
		}
		return r;
	}		
	
	/**
	 * 链接活动页，也是一个特殊的专题活动。取到专题的ID.
	 * @return
	 */
	public int getActivityTopicId(ContentResolver contentResolver){
		int topicId = 0;
		Cursor c = contentResolver.query(Schema.ACTIVITY_LIST, 
				new String[] {BaseColumns._ID }, null, null, null);
		if(c.getCount() == 0){
			this.refreshActList(contentResolver);
			if(c != null)c.close();
			c = contentResolver.query(Schema.ACTIVITY_LIST, 
					new String[] {BaseColumns._ID }, null, null, null);			
		}
		if(c.getCount() > 0){
			c.moveToFirst();
			int topic = c.getColumnIndex(BaseColumns._ID);
			topicId = c.getInt(topic);			
		}
		if(c != null)c.close();
		return topicId;
		
	}
	
	public ApiResult refreshTopicItemList(ContentResolver contentResolver, Uri topicList){
		return refreshTopicItemList(contentResolver, topicList, false, null);
	}
	public ApiResult refreshTopicItemList(final ContentResolver contentResolver, final Uri topicList, boolean sync, String force){
		int topic_id = Integer.parseInt(topicList.getPathSegments().get(1)); // (int) ContentUris.parseId(uri);
		String cate = topicList.getPathSegments().get(0);
		Log.e(Constants.TAG_EMOP, "refresh item list:" + topic_id);
		String pageSize = topicList.getQueryParameter("pageSize");
		String pageNo = topicList.getQueryParameter("pageNo");
		String noCache = topicList.getQueryParameter("no_cache");
		if(force != null && force.equals("y")){
			noCache = "y";
		}
		
		int size = 100;
		try{
			size = Integer.parseInt(pageSize);
		}catch(Throwable e){}
		final ApiResult r;
		if(cate.equals("act")){
			r = this.api.getTopicItemList(topic_id, size, pageNo);
		}else if(cate.equals("shop")){
			r = this.api.getShopItemList(topic_id, size, pageNo, trackUserId, noCache);
		}else {
			r = this.api.getTopicPidItemList(topic_id, size, pageNo, trackUserId, noCache);
		}
		if(r != null && r.isOK){
			Runnable task = new Runnable(){
				@Override
				public void run() {
					try {
						JSONObject json = r.json.getJSONObject("data");
						JSONArray jarray = json.getJSONArray("items");
						for(int i = 0; i < jarray.length(); i++){
							contentResolver.update(topicList, 
									Item.convertJson(jarray.getJSONObject(i)), 
									null, null);
						}
						contentResolver.notifyChange(topicList, null);
					} catch (JSONException e) {
						e.printStackTrace();
					}					
				}
				
			};
			
			if(sync){
				new Thread(task).start();
			}else {
				task.run();
			}
		}else {
			Log.d(Constants.TAG_EMOP, "Failed to refresh list:" + r.errorMsg());
		}
		return r;
	}
	
	/**
	 * 通过网络查询当前应用的最新版本。
	 * @return
	 */
	public ApiResult checkUpgradeVersion(){
		ApiResult r = this.api.call("cms_app_version_check", null);		
		//PackageManager packageManager = ctx.getPackageManager();
		//PackageInfo packInfo packageManager.getp		
		return r;
	}

	public ApiResult addFavorite(String weiboId, String desc, String picUrl, String numId,  String shopId, String short_url_key){
		return addFavorite(weiboId, desc, picUrl, numId, shopId, short_url_key, "taoke");
	}
	
	public ApiResult addFavorite(String weiboId, String desc, String picUrl, String numId,  String shopId, String short_url_key, String contentType){
		Log.d(TAG_EMOP, "add fav, weiboId:" + weiboId + ", numId:" + numId + ", shopId:" + shopId + ", picUrl:" + picUrl);
		ApiResult r = null;
		String fid = getFavoriteId();
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("user_id", userId);
		param.put("topic_id", fid);
		param.put("item_id", weiboId);
		param.put("pic_url", picUrl);
		param.put("content_type", contentType);
		param.put("num_iid", numId);
		param.put("shop_id", shopId);
		param.put("short_url_key", short_url_key);
		
		param.put("item_text", desc);
		r = api.call("tuji_topic_add_item", param);
		return r;
	}
	
	public ApiResult removeFavorite(String weiboId){
		Log.d(TAG_EMOP, "remove fav, weiboId:" + weiboId);
		ApiResult r = null;
		String fid = getFavoriteId();
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("user_id", userId);
		param.put("topic_id", fid);
		param.put("item_id", weiboId);

		r = api.call("tuji_topic_remove_items", param);
		return r;
	}
	
	
	/**
	 * 加载应用配置信息，例如：sina key,什么的。
	 * @return
	 */
	public AppConfig config(){
		if(config == null){
			config = new AppConfig();
			ApiResult r = api.call("cms_app_config_info", null);
			if(r != null && r.isOK){
				config.json = r.getJSONObject("data");
			}
		}
		return config;
	}
	
	/**
	 * 绑定外部登录用户信息，到美觅网系统。
	 * @param source
	 * @param userId
	 * @param token
	 * @return
	 */
	public ApiResult bindUserInfo(String source, String userId, String token){
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("source", source);
		param.put("ref_id", userId);
		param.put("access_token", token);
		if(this.userId != null){
			param.put("user_id", this.userId);
		}
		
		ApiResult r = api.call("user_bind_login", param);
		
		return r;
	}
	
	public ApiResult registerUser(Map<String, Object> param){	
		/**
		 * 有user_id是通过，第三方帐号绑定过来。已经生成了user_id.
		 * 没有user_id是在应用里面，直接注册的。
		 */
		ApiResult r = null;
		Object user_id = param.get("user_id");
		if(user_id != null && user_id.toString().length() > 0){
			r = api.call("user_update_info", param);
		}else {
			r = api.call("user_register_new", param);
		}
		
		return r;
	}
	
	public ApiResult login(String email, String password){	
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("email", email);
		param.put("password", password);
		
		ApiResult r = api.call("user_login", param);
		
		return r;
	}
	
	public void saveLoginUser(Activity ctx, String userId){
		this.userId = userId;
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(Constants.PREFS_OAUTH_ID, userId);
    	editor.commit();
    	Log.d(TAG_EMOP, "save user:" + userId);
	}

	public void saveRefUser(Activity ctx, String source, String userId, String nick){
		//this.userId = userId;
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	if(source.equals(Constants.AUTH_REF_SINA)){
    		editor.putString(Constants.PREFS_SINA_UID, userId);
        	editor.putString(Constants.PREFS_SINA_NICK, nick);
    	}else if(source.equals(Constants.AUTH_REF_TAOBAO)){
    		editor.putString(Constants.PREFS_TAOBAO_UID, userId);
        	editor.putString(Constants.PREFS_TAOBAO_NICK, nick);    		
    	}else if(source.equals(Constants.AUTH_REF_QQ)){
    		editor.putString(Constants.PREFS_QQ_UID, userId);
        	editor.putString(Constants.PREFS_QQ_NICK, nick);      		
    	}
    	editor.commit();
    	Log.d(TAG_EMOP, "save user:" + userId);
	}

	public void removeRefUser(Activity ctx, String source){
		//this.userId = userId;
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	if(source.equals(Constants.AUTH_REF_SINA)){
    		editor.remove(Constants.PREFS_SINA_UID);
    		editor.remove(Constants.PREFS_SINA_NICK);
    		editor.remove(Constants.PREFS_SINA_ACCESS_TOKEN);
    	}else if(source.equals(Constants.AUTH_REF_TAOBAO)){
    		editor.remove(Constants.PREFS_TAOBAO_UID);
    		editor.remove(Constants.PREFS_TAOBAO_NICK);
    	}else if(source.equals(Constants.AUTH_REF_QQ)){
    		editor.remove(Constants.PREFS_QQ_UID);
    		editor.remove(Constants.PREFS_QQ_UID);
    	}
    	editor.commit();
    	String sina = settings.getString(Constants.PREFS_SINA_UID, "");
    	String taobao = settings.getString(Constants.PREFS_TAOBAO_UID, "");
    	String qq = settings.getString(Constants.PREFS_QQ_UID, "");
    	
    	if((sina == null || sina.trim().length() == 0) &&
    	   (taobao == null || taobao.trim().length() == 0) &&
    	   (qq == null || qq.trim().length() == 0)
    	){
    		logout(ctx);
    	}    	
    	Log.d(TAG_EMOP, "save user:" + userId);
	}
	
	
	public void logout(Activity ctx){
		this.userId = null;
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(Constants.PREFS_OAUTH_ID, "0");
    	editor.commit();
    	Log.d(TAG_EMOP, "logout:" + userId);		
	}
	
	/**
	 * 我的收藏夹Id.
	 * @return
	 */
	public String getFavoriteId(){
		if((this.favoriteId == null || this.favoriteId.length() == 0) &&
				this.isLogined()){
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("user_id", userId);
			param.put("cate", 1);
			param.put("topic_name", "收藏夹");
			param.put("item_head_count", 0);
			param.put("status", TaodianApi.STATUS_NORMAL);
			
			ApiResult r = api.call("tuji_user_topic_list", param);
			if(r.isOK){
				 int count = Integer.parseInt(r.getString("data.item_count").toString());
				 if(count > 0){
					 JSONObject obj = r.getJSONObject("data");
					 JSONArray items;
					try {
						items = obj.getJSONArray("items");
						this.favoriteId = items.getJSONObject(0).getString("id");
					} catch (JSONException e) {
						Log.e(TAG_EMOP, "Get favoriate ID error:" + e.toString(), e);
					}
				 }
			}
			if(this.favoriteId == null || this.favoriteId.length() == 0){
				r = api.call("tuji_create_topic", param);
				if(r.isOK){
					this.favoriteId = r.getString("data.topic_id");
				}			
			}
		}
		return this.favoriteId;
	}
	
	/**
	 * 检查用户PID是否合法。先跳过不检查。
	 * @param id
	 * @return
	 */
	public ApiResult checkTrackId(String id){
		ApiResult r = new ApiResult();
		r.isOK = true;
		return r;
	}
	
	public void cleanExpiredData(ContentResolver contentResolver){
		Log.d(Constants.TAG_EMOP, "cleanExpiredData....");
		contentResolver.delete(Schema.TOPIC_LIST, Item.LOCAL_UPDATE_TIME + " < ?", 
				new String[]{(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 15) + ""});
		contentResolver.delete(Schema.ITME_LIST, Item.LOCAL_UPDATE_TIME + " < ?", 
				new String[]{(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 5) + ""});	
		
		contentResolver.delete(Schema.REBATE_LIST, Item.LOCAL_UPDATE_TIME + " < ?", 
				new String[]{(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 15) + ""});
		contentResolver.delete(Schema.SHOP_LIST, Item.LOCAL_UPDATE_TIME + " < ?", 
				new String[]{(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 15) + ""});		
	}
	
	/**
	 * 更加ID加载单条内容。主要用在，应用从外部启动后。直接进入详情页面。
	 * @param uuid
	 * @return
	 */
	public ApiResult getWeiboInfo(String uuid){		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("uuid", uuid);
		param.put("user_id", userId);
		
		ApiResult r = api.call("cms_get_uuid_content", param);		

		return r;
	}

	public ApiResult getTrackPid(){		
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("uid", this.trackUserId);
		ApiResult r = api.call("emop_user_pid", param);		
		this.trackPID = r.getString("data.pid");
		
		return r;
	}
	
	/**
	 * @param uuid
	 * @return
	 */
	public ApiResult updateTrackId(){
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("user_id", userId);
		param.put("track_user_id", trackUserId);
		
		ApiResult r = api.call("user_update_track_id", param);	
		if(r != null && r.isOK){
			String tid = r.getString("data.track_user_id");
			if(tid != null && tid.trim().length() > 0){
				trackUserId = tid;
				SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
		    	SharedPreferences.Editor editor = settings.edit();
		    	editor.putString(Constants.PREFS_TRACK_ID, tid);
		    	editor.commit();
			}
			Log.d("xx", "update task as:" + tid);
		}
		return r;
	}
	
	/**
	 * 1. 读取sd卡
	 * 2. 读取应用meta
	 */
	public void updateLocalTrackId(){
		boolean writeSetting = false;
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
		trackUserId = settings.getString(Constants.PREFS_TRACK_ID, null);
		if(trackUserId == null || trackUserId.trim().equals("")){
			writeSetting = true;
			trackUserId = readTrackIdFromSD();
		}
		if(trackUserId == null || trackUserId.trim().equals("")){
			trackUserId = getMetaDataValue("emop_track_id");
			//测试模式下，track id还没有替换时。默认是EMOP_USER
			if(trackUserId != null && trackUserId.equals("EMOP_USER")){
				trackUserId = "11";
			}
			Log.d(TAG_EMOP, "read track from meta:" + trackUserId);
		}
		if(trackUserId != null && trackUserId.trim().length() > 0){
			if(writeSetting){
				saveSettings(Constants.PREFS_TRACK_ID, trackUserId);
			}
			WriteTrackIdToSD(trackUserId);
		}
	}

	private void WriteTrackIdToSD(String tid){
		File track = null;
		String pid = null;
		OutputStream os = null;
		try{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				track = new File(Environment.getExternalStorageDirectory(), "taodianhuo.pid");
			}else {
				track = new File(ctx.getExternalFilesDir(null), "taodianhuo.pid");
			}
			Log.d(TAG_EMOP, "write track user pid:'" + tid + "' to:" + track.getAbsolutePath());
			os = new FileOutputStream(track);
			if(os != null){
				os.write(tid.getBytes());
			}
		}catch(Throwable e){
			Log.d(TAG_EMOP, "write error:" + e.toString(), e);
		}finally{
			if(os != null){
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
	}
		
	private String readTrackIdFromSD(){
		File track = null;
		String pid = null;
		BufferedReader input = null;
		try{
			if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
				track = new File(Environment.getExternalStorageDirectory(), "taodianhuo.pid");
			}else {
				track = new File(ctx.getExternalFilesDir(null), "taodianhuo.pid");
			}
			Log.d(TAG_EMOP, "read track user from:" + track.getAbsolutePath());
			if(track.isFile()){
				input = new BufferedReader(new InputStreamReader(new FileInputStream(track)));
				pid = input.readLine();
			}
			Log.d(TAG_EMOP, "read pid in sdcard:" + pid);
		}catch(Throwable e){
			Log.e(TAG_EMOP, "read pid error:" + e.toString(), e);
		}finally{
			if(input != null){
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
		return pid;
	}
	
    private String getMetaDataValue(String name) {
        Object value = null;
        PackageManager packageManager = ctx.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = packageManager.getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
            if (applicationInfo != null && applicationInfo.metaData != null) {
                value = applicationInfo.metaData.get(name);
            }
        } catch (NameNotFoundException e) {
            throw new RuntimeException(
                    "Could not read the name in the manifest file.", e);
        }
        if (value == null) {
            throw new RuntimeException("The name '" + name
                    + "' is not defined in the manifest file's meta data.");
        }
        return value.toString();
    }	
		
	public String getSettings(String key){
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
		return settings.getString(key, null);
	}

	public String saveSettings(String key, String value){
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(key, value);
    	editor.commit();	
		return settings.getString(key, null);
	}	
	public String removeSettings(String key){
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.remove(key);
    	editor.commit();	
		return settings.getString(key, null);
	}
}
