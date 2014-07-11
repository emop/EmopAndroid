package com.emop.client.io;

import com.emop.client.Constants;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.weibo.net.AccessToken;
import com.weibo.net.Oauth2AccessTokenHeader;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboParameters;

import static com.emop.client.Constants.TAG_EMOP;


public class WeiBoClient {
	private Context context = null;
	private static WeiBoClient ins = null;
	//private ClientConfig cfg = null;
	
	public WeiBoClient(Context ctx){
		ins = this;
		ins.context = ctx;
	}
	
	public static WeiBoClient getInstance(){
		return ins;
	}
	
	public List<WeiboUser> getUserFriends(){
		return this.getUserFriends(false);
	}
	
	public List<WeiboUser> getUserFriends(boolean refresh){
		SharedPreferences settings = context.getSharedPreferences(Constants.PREFS_NAME, 0);
		String data = settings.getString(Constants.PREFS_SINA_FRIENDS, "");
		
		JSONArray result = null;
		if(refresh || data == null || data.trim().length() == 0){
			result = getFromRemote();
			data = result.toString();
			
			Log.d(TAG_EMOP, "save friends:" + data);
	    	SharedPreferences.Editor editor = settings.edit();
	    	editor.putString(Constants.PREFS_SINA_FRIENDS, data);
	    	editor.commit();
		}else {
			Log.d(TAG_EMOP, "Get friends:" + data);
			try {
				result = (JSONArray) new JSONTokener(data).nextValue();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		List<WeiboUser> ul = new ArrayList<WeiboUser>(result.length());
		for(int i = 0; i < result.length(); i++){
			try {
				ul.add(new WeiboUser(result.getJSONObject(i)));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return ul;
	}
	
	private JSONArray getFromRemote(){
		Weibo weibo = Weibo.getInstance();
		WeiboParameters bundle = new WeiboParameters();
        String url = Weibo.SERVER + "friendships/friends.json";
		String json = null;
		JSONObject user = null;
		JSONArray r = new JSONArray();

		
		try {
	        bundle.add("source", Weibo.getAppKey());
	        
	        bundle.add("count", "150");
	        bundle.add("uid", "1");

			json = weibo.request(context, url, bundle, Utility.HTTPMETHOD_GET, 
					weibo.getAccessToken());			
			Log.d(TAG_EMOP, json);
			user = (JSONObject) new JSONTokener(json).nextValue();
			
			JSONObject u = null;
			if(user.has("users")){
				JSONArray users = user.getJSONArray("users"); 
				for(int i = 0; i < users.length(); i++){
					u = new JSONObject();
					u.put("id", users.getJSONObject(i).get("id"));
					u.put("screen_name", users.getJSONObject(i).get("screen_name"));
					u.put("name", users.getJSONObject(i).get("name"));					
					u.put("profile_image_url", users.getJSONObject(i).get("profile_image_url"));
					r.put(i, u);
				}				
			}			
		} catch (WeiboException e) {
			Log.d(TAG_EMOP, "msg:" + e.getMessage() + ", code:" + e.getStatusCode());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return r;		
	}
	
	public WeiboUser getProfile(String uid){
		
		Weibo weibo = Weibo.getInstance();
		WeiboParameters bundle = new WeiboParameters();
        String url = Weibo.SERVER + "users/show.json";
		String json = null;
		JSONObject user = null;
		WeiboUser r = null;

		
		try {
	        //bundle.add("source", Weibo.getAppKey());
			if(weibo.getAccessToken() != null){
				bundle.add("access_token", weibo.getAccessToken().getToken());
			}
	        bundle.add("uid", uid);

			json = weibo.request(context, url, bundle, Utility.HTTPMETHOD_GET, 
					weibo.getAccessToken());			
			Log.d(TAG_EMOP, json);
			user = (JSONObject) new JSONTokener(json).nextValue();
			if(user != null && user.has("screen_name")){
				r = new WeiboUser(user);
			}
		} catch (WeiboException e) {
			Log.d(TAG_EMOP, "msg:" + e.getMessage() + ", code:" + e.getStatusCode());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return r;
		
	}
	

	
	
}
