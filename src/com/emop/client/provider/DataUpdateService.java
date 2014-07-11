package com.emop.client.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentProvider;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.emop.client.Constants;
import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.io.TaodianApi;
import com.emop.client.provider.model.Rebate;
import com.emop.client.provider.model.Shop;
import com.emop.client.provider.model.Topic;

public class DataUpdateService {
	private final static long MAX_CACHE_TIME = 1000 * 60 * 60 * 24 * 3;
	private Context context = null;
	private ContentProvider provider = null;
	private TaodianApi api = null;
	private LruCache<String, Lock> lockCache;
	private File dataPath = null;
	private boolean hastDirty = false;
	private Timer syncTimer = null;
	
	private Map<String, CacheItem> cache = new HashMap<String, CacheItem>();
	
	public DataUpdateService(Context context, ContentProvider provider){
		this.context = context;
		this.provider = provider;
		lockCache = new LruCache<String, Lock>(200) {
	        @Override
	        protected int sizeOf(String key, Lock obj) {
	            return 1;
	        }
	    };
	    dataPath = context.getDatabasePath("lastUpdate.db");
	    
	    loadLastUpadte();
	    syncTimer = new Timer();
	    syncTimer.scheduleAtFixedRate(new TimerTask(){

			@Override
			public void run() {
				saveLastUpdate();
			}}, 1000, 1000 * 60);
	    
	}	
	
	/**
	 * 检查是否过期，或许要重新加载。如果数据需要重新从网络服务器加载，回调用API加载
	 * 后生成一个新的Cursor替换原来的Cursor返回。
	 * 
	 * @param uri
	 * @param c
	 * @return
	 */
	public Cursor syncCheck(final Uri uri, Cursor c){
		boolean isForceRefresh = isForceRefresh(uri);
		if(c.getCount() == 0 || isForceRefresh){
			if((isExpried(uri, 60 * 1000) || isForceRefresh) && !allowEmpty(uri)){
				ApiResult r = refreshDataByUri(uri);
				if(r != null && r.isOK){
					try{
						JSONObject data = r.json.getJSONObject("data");
						String[] names = c.getColumnNames();
						c.close();
						c = new JSONCursor(data.getJSONArray("items"), names);
					}catch(Exception e){
						Log.w("emop", "Result parsse error:" + e.toString(), e);
					}
				}
			}else {
				Log.w("emop", "Ignore refresh empty uri:" + uri.toString());
			}
		}else if(isExpried(uri, 1000 * 60 * 60 * 3)){
			new Thread(){
				public void run(){
					Log.e(Constants.TAG_EMOP, "refresh expired uri:" + uri.toString());					
					refreshDataByUri(uri);
					context.getContentResolver().notifyChange(uri, null);
				}
			}.start();
		}
		
		return c;
	}
	
	public ApiResult refreshDataByUri(Uri uri){
		String ck = getCacheKey(uri);
		Lock lock = getLock(uri.toString());
		ApiResult r = null;
		if(lock.tryLock()){
			Log.e(Constants.TAG_EMOP, "Start to refresh uri:" + uri.toString());
			/*
			 * 避免网络通信期间,导致次刷新请求.
			 */
	        CacheItem item = new CacheItem();
	        item.cacheTime = System.currentTimeMillis();
	        cache.put(ck, item);
	        hastDirty = true;
			try{
		        switch (Schema.FmeiUriMatcher.match(uri)) {
		        	case Schema.TYPE_SHOPS:
		        	case Schema.TYPE_SHOPS_CATE:
		        		r = refreshShopList(uri);
		        		break;			        		
		        	case Schema.TYPE_SHOP_TAOKE_LSIT:
		        		r = refreshShopTaokeList(uri);
		        		break;
		        	case Schema.TYPE_REBATES:
		        	case Schema.TYPE_REBATES_CATE:
		        		r = refreshRebateList(uri);
		        		break;		        	
		        	case Schema.TYPE_SHOP_ID:
		        		r = refreshOneShop(uri);
		        		break;
		        	case Schema.TYPE_REBATE_CATES:
		        		r = this.refreshRebateCateList(uri);
		        		break;
		        	case Schema.TYPE_TOPIC_ITEM_LIST:
		        		r = this.refreshTopicItemList(uri);
		        		break;
		        	case Schema.TYPE_TOPICS:
		        		r = this.refreshTopicList(uri);
		        		break;
		        	case Schema.TYPE_CATES:
		        		r = this.refreshCateList(uri);
		        		break;
		        	case Schema.TYPE_HOTS:
		        		r = this.refreshHotCateList(uri);	
		        		break;
		        		
		        	default:
		        		Log.e(Constants.TAG_EMOP, "unkown in data update uri:" + uri.toString());
		        }
			}finally{
				lock.unlock();
			}
		}else {
			Log.e(Constants.TAG_EMOP, "Failed to get refresh lock:" + uri.toString());
		}
		
		return r;
	}
	
	public boolean isExpried(Uri uri){
		return isExpried(uri, 1000 * 60 * 30);
	}
	public boolean isExpried(Uri uri, int timeOut){
		String ck = getCacheKey(uri);
		CacheItem item = cache.get(ck);
		if(item == null || System.currentTimeMillis() - item.cacheTime > timeOut){
			return true;
		}
		return false;
	}
	
	private boolean isForceRefresh(Uri uri){
		String q = uri.getQueryParameter("force_refresh");
		return q != null && q.equals("y");
	}
	
	private boolean allowEmpty(Uri uri){
		String q = uri.getQueryParameter("empty");
		return q != null && q.equals("y");
	}	
	
	public String getCacheKey(Uri u){
		return u.toString();
	}
	
	public synchronized Lock getLock(String url){
		Lock o = lockCache.get(url);
		if(o == null){
			o = new ReentrantLock();
			lockCache.put(url, o);
		}
		return o;
	}
	
	public TaodianApi taoDianApi(){
		if(api == null){
			api = new TaodianApi();
			api.connect(null);
		}
		
		return api;
	}
	
	public ApiResult refreshShopList(final Uri shopList){
		String cate = shopList.getQueryParameter("cate");
		
		FmeiClient client = FmeiClient.getInstance(null);
		
		int pageSize = this.getIntParamter(shopList, "page_size", 40);
		int pageNo = this.getIntParamter(shopList, "page_no", 0);
		
		
		final ApiResult r = taoDianApi().getShopList(client.trackUserId, cate, pageSize, pageNo);
		if(r.isOK){
			new Thread(){
				public void run(){
					try {
						JSONObject json = r.json.getJSONObject("data");
						JSONArray jarray = json.getJSONArray("items");
						for(int i = 0; i < jarray.length(); i++){
							provider.update(Schema.SHOP_LIST, 
									Shop.convertJson(jarray.getJSONObject(i)), 
									null, null);
						}
					} catch (Exception e) {
						Log.w("emop", "JSON:" + r.json.toString());
						Log.w("emop", "Refresh shop listerror:" + e.toString(), e);
					}
				}
			}.start();
		}
		
		return r;
	}
	
	public ApiResult refreshOneShop(final Uri shopList){
		String cate = shopList.getQueryParameter("cate");
		FmeiClient client = FmeiClient.getInstance(null);
		
		int pageSize = this.getIntParamter(shopList, "page_size", 40);
		int pageNo = this.getIntParamter(shopList, "page_no", 0);
		
		List<String> seg = shopList.getPathSegments();
		String shopId = seg.get(1);
		
		final ApiResult r = taoDianApi().getOneShop(client.trackUserId, shopId, pageSize, pageNo);
		if(r.isOK){
			new Thread(){
				public void run(){
					try {
						JSONObject json = r.json.getJSONObject("data");
						JSONArray jarray = json.getJSONArray("items");
						for(int i = 0; i < jarray.length(); i++){
							provider.update(Schema.SHOP_LIST, 
									Shop.convertJson(jarray.getJSONObject(i)), 
									null, null);
						}
					} catch (Exception e) {
						Log.w("emop", "JSON:" + r.json.toString());
						Log.w("emop", "Refresh shop listerror:" + e.toString(), e);
					}
				}
			}.start();
		}
		
		return r;
	}	
	
	public ApiResult refreshRebateList(final Uri rebateList){
		String cate = rebateList.getQueryParameter("cate");
		
		FmeiClient client = FmeiClient.getInstance(null);
		
		int pageSize = this.getIntParamter(rebateList, QueryParam.PAGE_SIZE, 40);
		int pageNo = this.getIntParamter(rebateList, QueryParam.PAGE_NO, 0);
		Log.w("emop", "Refresh rebate listerror:...");
		
		final ApiResult r = taoDianApi().getRebateList(client.trackUserId, cate, pageSize, pageNo);
		if(r.isOK){
			new Thread(){
				public void run(){
					try {
						JSONObject json = r.json.getJSONObject("data");
						JSONArray jarray = json.getJSONArray("items");
						for(int i = 0; i < jarray.length(); i++){
							provider.update(Schema.REBATE_LIST, 
									Rebate.convertJson(jarray.getJSONObject(i)), 
									null, null);
						}
					} catch (Exception e) {
						Log.w("emop", "JSON:" + r.json.toString());
						Log.w("emop", "Refresh rebate listerror:" + e.toString(), e);
					}					
				}
			}.start();
		}
		
		return r;
	}
	
	public ApiResult refreshRebateCateList(final Uri rebateList){
		Log.w("emop", "Refresh rebate cate listerror:...");
		
		final ApiResult r = taoDianApi().getRebateCateList(50, 1001);
		if(r.isOK){
			new Thread(){
				public void run(){
					try {
						JSONObject json = r.json.getJSONObject("data");
						JSONArray jarray = json.getJSONArray("items");
						for(int i = 0; i < jarray.length(); i++){
							provider.update(Schema.REBATE_CATE_LIST, 
									Topic.convertJson(jarray.getJSONObject(i)), 
									null, null);
						}
					} catch (Exception e) {
						Log.w("emop", "JSON:" + r.json.toString());
						Log.w("emop", "Refresh rebate listerror:" + e.toString(), e);
					}					
				}
			}.start();
		}
		
		return r;
	}	
	
	public ApiResult refreshShopTaokeList(final Uri shopList){
		FmeiClient client = FmeiClient.getInstance(null);	
		String force = isForceRefresh(shopList) ? "y": "n";
		return client.refreshTopicItemList(context.getContentResolver(), shopList, true, force);
	}
	
	public ApiResult refreshTopicItemList(final Uri topic){
		FmeiClient client = FmeiClient.getInstance(null);	
		String force = isForceRefresh(topic) ? "y": "n";
		return client.refreshTopicItemList(context.getContentResolver(), topic, true, force); 
	}
	
	public ApiResult refreshTopicList(final Uri topic){
		FmeiClient client = FmeiClient.getInstance(null);		
		String force = isForceRefresh(topic) ? "y": "n";
		return client.refreshTopicList(context.getContentResolver(), TaodianApi.STATUS_NORMAL, force);
	}
	
	public ApiResult refreshHotCateList(final Uri topic){
		FmeiClient client = FmeiClient.getInstance(null);		
		return client.refreshHotCatList(context.getContentResolver(), TaodianApi.STATUS_NORMAL);
	}
	
	public ApiResult refreshCateList(final Uri topic){
		FmeiClient client = FmeiClient.getInstance(null);		
		return client.refreshCateList(context.getContentResolver(), TaodianApi.STATUS_NORMAL);
	}
	
	protected void loadLastUpadte(){
		if(dataPath.isFile() && dataPath.canRead()){
			InputStream in = null;
			try {
				in = new FileInputStream(dataPath);
				ObjectInputStream obj2 = null;
					obj2 = new ObjectInputStream(new GZIPInputStream(in));
					Object data = obj2.readObject();
					if(data instanceof Map){
						Map<String, CacheItem> tmp = (Map<String, CacheItem>)data;
						cache.putAll(tmp);
						Log.i("emop", "Read last udpate data from cache, size:" + tmp.size());
					}
			} catch (Exception e) {
				Log.d("emop", "read data error:" + e.toString(), e);
			}finally{
				if(in != null){
					try {
						in.close();
					} catch (IOException e) {
					}
				}
			}
		}else {
			Log.d("emop", "cant read data file:" + dataPath.getAbsolutePath());
		}		
	}
	
	protected void saveLastUpdate(){
		if(!this.hastDirty) return;
		hastDirty = false;
		OutputStream outs = null;
		try {
			outs = new FileOutputStream(dataPath);
			ObjectOutputStream obj2 = new ObjectOutputStream(new GZIPOutputStream(outs));
			Map<String, CacheItem> tmp = new HashMap<String, CacheItem>();
			Collection<String> keys = new ArrayList<String>();
			keys.addAll(cache.keySet());
			for(String k: keys){
				CacheItem item = cache.get(k);
				if(item != null && System.currentTimeMillis() - item.cacheTime < MAX_CACHE_TIME){
					tmp.put(k, item);
				}
			}
			obj2.writeObject(tmp);
			obj2.flush();
			Log.d("emop", "Write cache item, size:" + tmp.size());			
		} catch (Exception e) {
			Log.d("emop", "read data error:" + e.toString(), e);
		}finally{
			if(outs != null){
				try {
					outs.close();
				} catch (IOException e) {
				}
			}
		}		
	}
	
	private int getIntParamter(final Uri uri, String name, int def){
		int i = def;
		String val = uri.getQueryParameter(name);
		if(val != null && val.length() > 0){
			try{
				i = Integer.parseInt(val);
			}catch(Throwable e){}			
		}
		return i;
	}
	
	
	static class CacheItem implements Serializable{
		private static final long serialVersionUID = 2541899275403465619L;
		public long cacheTime = 0;
		public boolean lastStatus = false;
	}

}
