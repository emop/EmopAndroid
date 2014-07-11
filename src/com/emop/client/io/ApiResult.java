package com.emop.client.io;

import static com.emop.client.Constants.TAG_EMOP;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class ApiResult {
	public static final String ERR_JSON_PARSE = "err_json_parse";
	public static final String ERR_NETWORKING_UNKOWN = "err_networking_unkown";
	
	public JSONObject json = null;
	public boolean isOK = false;
	public String errorMsg = "";
	public String errorCode = "";
	

	
	public String getString(String key){
		String str = null;
		if(json != null){
			try {
				JSONObject v = json;
				Object o = null;
				for(String k: key.split("\\.")){
					if(!v.has(k)) {
						v = null;
						break;
					}
					o = v.get(k);
					if(o instanceof JSONObject){
						v = (JSONObject)o;
					}else{
						str = o.toString();
					}
				}
			} catch (JSONException e) {
				//e.printStackTrace();
				Log.e(TAG_EMOP, "JSONException error:" + e.toString(), e);
			}
		}
		return str;
	}
	
	public JSONObject getJSONObject(String key){
		JSONObject data = null;
		if(json != null){
			try {
				JSONObject v = json;
				Object o = null;
				for(String k: key.split("\\.")){
					if(!v.has(k)) {
						v = null;
						break;
					}
					o = v.get(k);
					if(o instanceof JSONObject){
						v = (JSONObject)o;
						data = v;
					}
				}
			} catch (JSONException e) {
				//e.printStackTrace();
				Log.e(TAG_EMOP, "JSONException error:" + e.toString(), e);
			}
		}
		return data;
	}	
	
	public String errorMsg(){		
		if(this.errorMsg != null && this.errorMsg.trim().length() > 0){
			return this.errorMsg.trim();
		}else{
			return this.errorCode;			
		}
	}
	
	//private String getString(String key, )
}
