package com.emop.client.io;

import static com.emop.client.Constants.TAG_EMOP;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.weibo.net.Utility.MySSLSocketFactory;


public class HttpTransport {
    private static final int SET_CONNECTION_TIMEOUT = 6000;
    private static final int SET_SOCKET_TIMEOUT = 20000;
    
    private final static String API_ROUTE = "http://fmei.sinaapp.com/api/route";
	private final static String TAG = "client.http";
	private static HttpTransport ins = null;

	private URI server = null;
	public String encoding = "utf8";
	private ResultHandler handler = new ResultHandler();
	public HttpClient client = null;
	public String appKey = "";
	public String appSecret = "";
	//public ClientConfig config = new ClientConfig();
	private Context ctx = null;	
	private DateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	public HttpTransport(Context ctx, String appKey, String secret){
		this.ctx = ctx;
		this.appKey = appKey;
		this.appSecret = secret;
	}
	
	public ApiResult ping(){
		return call("cms_api_info", null);
	}
	
	public ApiResult call(String name, Map<String, Object> param){
		Map<String, Object> p = this.newDefaultParam();
		p.put("name", name);
		if(param == null){
			param = new HashMap<String, Object>();
		}
		if(param.containsKey("no_cache")){
			if(param.get("no_cache") != null){
				p.put("no_cache", param.get("no_cache"));
			}
			param.remove("no_cache");
		}
		
		JSONObject obj = new JSONObject(param);
		p.put("params", obj.toString());
		
		ApiResult r = new ApiResult();
		getRPCData(null, p, handler, r);
		if(!r.isOK){
			Log.e(TAG_EMOP, "Taobian API error:" + r.errorCode + ",msg:" + r.errorMsg());
		}
		///
		return r;
	}
	
	
	protected Map<String, Object> newDefaultParam(){
		Map<String, Object> p = new HashMap<String, Object>();
		String stamp = this.timestamp();
		p.put("app_id", this.appKey);
		p.put("time", stamp);
		
		String sign = this.appKey + "," + stamp + "," + this.appSecret;
		sign = MD5(sign);
		p.put("sign", sign);
		
		return p;
	}
	
    public static String MD5(String str)  
    {  
        MessageDigest md5 = null;  
        try  
        {
            md5 = MessageDigest.getInstance("MD5"); 
        }catch(Exception e)  
        {  
            e.printStackTrace();  
            return "";  
        }  
          
        char[] charArray = str.toCharArray();  
        byte[] byteArray = new byte[charArray.length];  
          
        for(int i = 0; i < charArray.length; i++)  
        {  
            byteArray[i] = (byte)charArray[i];  
        }  
        byte[] md5Bytes = md5.digest(byteArray);  
          
        StringBuffer hexValue = new StringBuffer();  
        for( int i = 0; i < md5Bytes.length; i++)  
        {  
            int val = ((int)md5Bytes[i])&0xff;  
            if(val < 16)  
            {  
                hexValue.append("0");  
            }  
            hexValue.append(Integer.toHexString(val));  
        }  
        return hexValue.toString();  
    } 
	
	private String timestamp(){
		Date date = new Date(System.currentTimeMillis());
		String time = sdf.format(date);
		return time;
	}
	
	private Object getRPCData(String api, Map<String, Object> param, ResultHandler h, ApiResult r){
		return getRPCData(api, param, h, r, false);
	}
	
	private Object getRPCData(String api, Map<String, Object> param, ResultHandler h, ApiResult r, boolean multi){
		HttpResponse response = null;
		Object result = null;
		List<NameValuePair> nameValuePairs = null;
		
		HttpPost request = new HttpPost(API_ROUTE);
        
        StringBuffer query = new StringBuffer(api + "?");        
        if(param == null) param = new HashMap<String, Object>();
        
        if(client == null){
        	client = getNewHttpClient(ctx);
        }
        
        try {
        	if(!multi){
	        	nameValuePairs = new ArrayList<NameValuePair>(param.size());
	            for(Entry<String, Object> item : param.entrySet()){
	            	query.append("&" + item.getKey() + "=" + item.getValue());
	            	nameValuePairs.add(new BasicNameValuePair(item.getKey(), item.getValue().toString()));
	            }
	        	if(nameValuePairs != null){
	        		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairs, "UTF-8");
	        		entity.setContentEncoding(HTTP.UTF_8);
	        		request.setEntity(entity);
	        	}
	            Log.d(TAG, "Request:" + query.toString());	        	
        	}else {
        		//Log.i(TAG, "charset:" + Charset.forName("utf8"));
                MultipartEntity m = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, 
                		Charset.forName("UTF-8"));                
                for(Entry<String, Object> item : param.entrySet()){
                	Object o = item.getValue();
                	if(o instanceof File){
                		query.append("&" + item.getKey() + "=" + o.toString());
                		m.addPart(item.getKey(), new FileBody((File)o));
                	}else {
                		query.append("&" + item.getKey() + "=" + o.toString());
                		StringBody s = new StringBody(o.toString(), "text/plain", Charset.forName("UTF-8"));
                		m.addPart(item.getKey(), s);                		
                	}
                }
                request.setEntity(m);
	            Log.d(TAG, "Post Request:" + query.toString());	
        	}
        	response = client.execute(request);
        	
        	if(response != null){
        		h.processInputStream(response.getEntity().getContent(), r);
        	}
		}catch (Throwable e) {
			this.processException(r, e);
			Log.e(TAG, "error:" + e.toString() + ", url:" + query.toString());
			this.client = null;
		}
		
		return result;
	}
	
	public Object getRequest(String api, ResultHandler h, ApiResult r){
		HttpResponse response = null;
		Object result = null;		
        
        StringBuffer query = new StringBuffer(api + "?");        
        
        if(client == null){
        	client = getNewHttpClient(ctx);
        }
        
        
        try {
            Log.d(TAG, "Request:" + query.toString());	  
    		HttpGet request = new HttpGet(query.toString());

        	response = client.execute(request);
        	h = h != null ? h : handler;
        	if(response != null){
        		h.processInputStream(response.getEntity().getContent(), r);
        	}
		}catch (Throwable e) {
			this.processException(r, e);
			Log.e(TAG, "error:" + e.toString() + ", url:" + query.toString());
			this.client = null;
		}
		
		return result;
	}	
	
	private void processException(ApiResult r, Throwable e){
		if(r == null) return;
		r.errorCode = ApiResult.ERR_NETWORKING_UNKOWN;
		r.errorMsg = e.getMessage();
	}
	
	public static HttpClient getNewHttpClient2(Context context) {
		return new DefaultHttpClient();
	}
	
    public static HttpClient getNewHttpClient(Context context) {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();

            HttpConnectionParams.setConnectionTimeout(params, 10000);
            HttpConnectionParams.setSoTimeout(params, 10000);

            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            // Set the default socket timeout (SO_TIMEOUT) // in
            // milliseconds which is the timeout for waiting for data.
            HttpConnectionParams.setConnectionTimeout(params, SET_CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, SET_SOCKET_TIMEOUT);
            HttpClient client = new DefaultHttpClient(ccm, params);
            if(context != null){
	            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	            if (!wifiManager.isWifiEnabled()) {
	                // 获取当前正在使用的APN接入点
	                Uri uri = Uri.parse("content://telephony/carriers/preferapn");
	                Cursor mCursor = context.getContentResolver().query(uri, null, null, null, null);
	                if (mCursor != null && mCursor.moveToFirst()) {
	                    // 游标移至第一条记录，当然也只有一条
	                    String proxyStr = mCursor.getString(mCursor.getColumnIndex("proxy"));
	                    Log.d(TAG, "APN proxy:" + proxyStr);
	                    if (proxyStr != null && proxyStr.trim().length() > 0) {
	                        HttpHost proxy = new HttpHost(proxyStr, 80);
	                        client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, proxy);
	                    }
	                    mCursor.close();
	                }
	            }else {
	            	Log.d(TAG, "connect to internet by wifi.");
	            }
            }
            return client;
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }	
	
}
