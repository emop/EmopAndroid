package com.emop.client.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class ResultHandler {
	private final static String TAG = "areaci.http";
	public boolean processInputStream(InputStream ins, ApiResult r) throws IOException{
		BufferedReader in = new BufferedReader(new InputStreamReader(ins, "utf8"));		
        StringBuffer sb = new StringBuffer("");
        String line = "";
        String NL = System.getProperty("line.separator");
        while ((line = in.readLine()) != null) {
            sb.append(line + NL);
        }
        in.close();
        Object o;
		try {
			//Log.d(TAG, "reps:" + sb.toString());
			o = new JSONTokener(sb.toString()).nextValue();
	       if(o instanceof JSONObject){
	        	r.json = (JSONObject)o; 
	       }else {
	        	Log.d(TAG, "error to parse data,:" + o.toString());
	       }
	       
	       if(r.json != null && r.json.has("status")){
	    	   if(r.json.getString("status").equals("ok")){
	    		   r.isOK = true;
	    	   }else {
	    		   if(r.json.has("msg")){
	    			   r.errorMsg = r.json.getString("msg");
	    		   }
	    		   if(r.json.has("code")){
	    			   r.errorCode = r.json.getString("code");
	    		   }
	    	   }
	       }else {
	    	   r.errorMsg = "网络通信错误";
	       }
		} catch (JSONException e) {
			r.errorCode = ApiResult.ERR_JSON_PARSE;
			r.errorMsg = "服务端返回结果，解析错误。";
			Log.d(TAG, "error:" + sb.toString());
		}
 
        in.close();        
        
        return true;
	}


}
