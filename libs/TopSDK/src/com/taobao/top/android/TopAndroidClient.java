/**
 * 
 */
package com.taobao.top.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.taobao.top.android.api.ApiError;
import com.taobao.top.android.api.TaobaoUtils;
import com.taobao.top.android.api.TopApiListener;
import com.taobao.top.android.api.TopTqlListener;
import com.taobao.top.android.api.WebUtils;
import com.taobao.top.android.auth.AccessToken;
import com.taobao.top.android.auth.AuthError;
import com.taobao.top.android.auth.AuthException;
import com.taobao.top.android.auth.AuthorizeListener;

/**
 * top客户端实现，封装了用户授权和API调用<br>
 * 一个客户端支持多个appkey
 * 
 * @author junyan.hj
 * 
 */
public class TopAndroidClient {
	private static final ConcurrentHashMap<String, TopAndroidClient> CLIENT_STORE = new ConcurrentHashMap<String, TopAndroidClient>();

	private static final String OAUTH_CLIENT_ID = "client_id";
	private static final String OAUTH_REDIRECT_URI = "redirect_uri";
	private static final String OAUTH_CLIENT_SECRET = "client_secret";
	private static final String OAUTH_REFRESH_TOKEN = "refresh_token";

	private static final String SDK_TRACK_ID = "track-id";
	private static final String SDK_DEVICE_UUID = "device-uuid";
	private static final String SDK_TIMESTAMP = "timestamp";
	private static final String SDK_CLIENT_SYSVERSION = "client-sysVersion";
	private static final String SDK_CLIENT_SYSNAME = "client-sysName";
	private static final String SDK_VERSION = "sdk-version";

	private static final String SYS_NAME = "Android";
	private static final String SESSION_DIR = "top.session";
	private static final String LOG_TAG = "TopAndroidClient";

	private String appKey;
	private String appSecret;
	private String redirectURI;
	private ConcurrentHashMap<Long, AccessToken> tokenStore = new ConcurrentHashMap<Long, AccessToken>();
	private Context context;
	private Env env;
	private int connectTimeout = 10000;// 10秒
	private int readTimeout = 30000;// 30秒

	/**
	 * 注册client所需信息。appKey/appSecret/redirectURI的值必须和开放平台开发者中心里注册的信息保持一致
	 * 
	 * @param context
	 * @param appKey
	 * @param appSecret
	 * @param redirectURI
	 */
	public static void registerAndroidClient(Context context, String appKey,
			String appSecret, String redirectURI) {
		registerAndroidClient(context, appKey, appSecret, redirectURI,
				Env.PRODUCTION);
	}

	/**
	 * 注册client所需信息。appKey/appSecret/redirectURI的值必须和开放平台开发者中心里注册的信息保持一致
	 * @see Env
	 * @param context
	 * @param appKey
	 * @param appSecret
	 * @param redirectURI
	 * @param env
	 *            appKey对应的运行环境
	 */
	public static void registerAndroidClient(Context context, String appKey,
			String appSecret, String redirectURI, Env env) {
		if (context == null) {
			throw new IllegalArgumentException("context must not null.");
		}
		if (TextUtils.isEmpty(appKey) || TextUtils.isEmpty(appSecret)
				|| TextUtils.isEmpty(redirectURI)) {
			throw new IllegalArgumentException(
					"appKey,appSecret and redirectURI must not null.");
		}
		TopAndroidClient client = new TopAndroidClient();
		client.setAppKey(appKey);
		client.setAppSecret(appSecret);
		client.setRedirectURI(redirectURI);
		client.setContext(context);
		if (env == null) {
			env = Env.PRODUCTION;
		}
		client.setEnv(env);

		client.revertAccessToken();

		CLIENT_STORE.put(appKey, client);
	}

	/**
	 * 获得事先已经注册的client对象。如果事先没有注册的话返回null
	 * 
	 * @see #registerAndroidClient(Context, String, String, String)
	 * @see #registerAndroidClient(Context, String, String, String, Env)
	 * @param appKey
	 * @return
	 */
	public static TopAndroidClient getAndroidClientByAppKey(String appKey) {
		return CLIENT_STORE.get(appKey);
	}

	private TopAndroidClient() {
	}

	/**
	 * 刷新access token的操作。
	 * 
	 * @see #authorize(Activity)
	 * @see #addAccessToken(AccessToken)
	 * @see AuthorizeListener
	 * @param userId
	 *            不能为null，通过userId确定需要刷新哪个access token。如果对应的access
	 *            token不存在会抛出IllegalArgumentException
	 * @param listener
	 *            不能为null，处理刷新结果的回调函数。
	 * @param async
	 *            是否是异步执行刷新操作
	 * @throws IllegalArgumentException
	 */
	public void refreshToken(Long userId, final AuthorizeListener listener,
			boolean async) {
		if (userId == null) {
			throw new IllegalArgumentException("userId must not null.");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener must not null.");
		}
		final AccessToken token = this.tokenStore.get(userId);
		if (token == null) {
			throw new IllegalArgumentException("userId:" + userId
					+ " can't found access token.");
		}

		if (async) {
			new Thread() {
				@Override
				public void run() {
					doRefresh(token, listener);
				}
			}.start();
		} else {// 同步
			doRefresh(token, listener);
		}
	}

	/**
	 * 调用浏览器显示授权页面。
	 * 
	 * @param activity
	 *            通过{@code Intent}方式在浏览器上打开授权页面的{@code Activity}，不能为null
	 */
	public void authorize(Activity activity) {
		if (activity == null) {
			throw new IllegalArgumentException("activity must not null.");
		}
		Map<String, String> params = getProtocolParams();
		params.put(OAUTH_CLIENT_ID, appKey);
		params.put(OAUTH_REDIRECT_URI, redirectURI);
		Random random = new Random();
		params.put("rand", String.valueOf(random.nextInt()));

		String str = "";
		try {
			URL url = WebUtils.buildGetUrl(env.getAuthUrl(), params, null);
			str = url.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);// won't happen
		}
		Uri uri = Uri.parse(str);
		Intent it = new Intent(Intent.ACTION_VIEW, uri);
		try {
			ComponentName name = new ComponentName("com.android.browser",
					"com.android.browser.BrowserActivity");
			// 判断系统自带浏览器是否安装
			context.getPackageManager().getActivityInfo(name,
					PackageManager.GET_INTENT_FILTERS);
			it.setComponent(name);
		} catch (Exception e) {
			/*
			 * if an activity with the given class name can not be found on the
			 * system
			 */
			Log.e(LOG_TAG, e.getMessage(), e);
		}
		activity.startActivity(it);
	}
	
	public String getAuthorizeLink(){
		Map<String, String> params = getProtocolParams();
		params.put(OAUTH_CLIENT_ID, appKey);
		params.put(OAUTH_REDIRECT_URI, redirectURI);
		Random random = new Random();
		params.put("rand", String.valueOf(random.nextInt()));

		String str = "";
		try {
			URL url = WebUtils.buildGetUrl(env.getAuthUrl(), params, null);
			str = url.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);// won't happen
		}
		return str;
	}

	/**
	 * 调用TOP API
	 * 
	 * @see TopApiListener
	 * @param params
	 *            系统及业务参数
	 * @param userId
	 *            需要使用哪个用户授权的access token来调用api，当API不需要session key时此参数可以为null
	 * @param listener
	 *            api调用回调处理监听器，不能为null
	 * @param async
	 *            true:异步调用；false:同步调用。Android 3.0以后会限制在UI主线程中同步访问网络，使用同步方式需谨慎
	 * @throws IllegalArgumentException
	 *             当参数<code>params</code>或<code>listener</code>为null时
	 */
	public void api(final TopParameters params, final Long userId,
			final TopApiListener listener, final boolean async) {
		if (params == null) {
			throw new IllegalArgumentException("params must not null.");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener must not null.");
		}
		final AccessToken tk = getStoredAccessToken(userId);
		if (async) {// 异步调用
			new Thread() {
				@Override
				public void run() {
					invokeApi(params, listener, tk);
				}
			}.start();
		} else {// 同步
			invokeApi(params, listener, tk);
		}

	}

	/**
	 * useId为null时返回null，如果userId对应的access token不存在则抛出异常
	 * 
	 * @param userId
	 * @return
	 * @throws IllegalArgumentException
	 */
	private AccessToken getStoredAccessToken(final Long userId) {
		AccessToken token = null;
		if (userId != null) {
			token = this.tokenStore.get(userId);
			if (token == null) {
				throw new IllegalArgumentException("userId:" + userId
						+ " can't found access token.");
			}
		}
		return token;
	}

	/**
	 * 调用TQL服务
	 * 
	 * @see TopApiListener
	 * @param ql
	 * @param userId
	 *            需要使用哪个用户授权的access token来调用api，当API不需要session key时此参数可以为null
	 * @param listener
	 *            tql调用回调处理监听器，不能为null
	 * @param async
	 *            true:异步调用；false:同步调用。Android 3.0以后会限制在UI主线程中同步访问网络，使用同步方式需谨慎
	 * @throws IllegalArgumentException
	 *             当参数<code>ql</code>或<code>listener</code>为null时
	 */
	public void tql(final String ql, final Long userId,
			final TopTqlListener listener, final boolean async) {
		if (TextUtils.isEmpty(ql)) {
			throw new IllegalArgumentException("ql must not null.");
		}
		if (listener == null) {
			throw new IllegalArgumentException("listener must not null.");
		}
		final AccessToken token = getStoredAccessToken(userId);

		if (async) {// 异步调用
			new Thread() {
				@Override
				public void run() {
					invokeTql(ql, token, listener);
				}
			}.start();
		} else {// 同步
			invokeTql(ql, token, listener);
		}

	}

	private void invokeTql(final String ql, AccessToken token,
			TopTqlListener listener) {
		TreeMap<String, String> params = new TreeMap<String, String>();
		params.put("ql", ql);
		params.put("app_key", appKey);
		params.put("sign_method", "hmac");
		params.put("top_tql_seperator","true");
		if (token != null) {
			params.put("session", token.getValue());
		}
		String sign;
		try {
			sign = TaobaoUtils.signTopRequestNew(params, appSecret);
			params.put("sign", sign);

			String jsonStr = WebUtils.doPost(context, env.getTqlUrl(), params,
					this.getProtocolParams(), connectTimeout, readTimeout,
					false);
			Log.d(LOG_TAG, jsonStr);
			listener.onComplete(jsonStr);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			listener.onException(e);
		}
	}

	/**
	 * 获取淘宝系统时间
	 * 注意这里使用的是同步的方式调用api
	 * 
	 * @return
	 */
	public Date getTime() {
		TopParameters params = new TopParameters();
		params.setMethod("taobao.time.get ");
		final List<Date> list = new ArrayList<Date>();

		this.api(params, null, new TopApiListener() {
			@Override
			public void onComplete(JSONObject json) {
				JSONObject j = json.optJSONObject("time_get_response");
				if (j != null) {
					String timeStr = j.optString("time");
					if (!TextUtils.isEmpty(timeStr)) {
						SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss");
						try {
							Date date = sdf.parse(timeStr);
							list.add(date);
						} catch (ParseException e) {
							Log.e(LOG_TAG, e.getMessage(), e);
						}
					}
				}

			}

			@Override
			public void onError(ApiError error) {
			}

			@Override
			public void onException(Exception e) {
			}
		}, false);
		if (list.size() > 0) {
			return list.get(0);
		}
		return new Date();
	}

	private void doRefresh(AccessToken accessToken, AuthorizeListener listener) {
		Map<String, String> header = getProtocolParams();
		Map<String, String> params = new HashMap<String, String>();
		params.put(OAUTH_CLIENT_ID, appKey);
		params.put(OAUTH_CLIENT_SECRET, appSecret);
		params.put(OAUTH_REDIRECT_URI, redirectURI);
		params.put(OAUTH_REFRESH_TOKEN, accessToken.getRefreshToken()
				.getValue());

		try {
			String jsonStr = WebUtils.doPost(context, env.getRefreshUrl(),
					params, header, connectTimeout, readTimeout, true);
			JSONObject json = new JSONObject(jsonStr);
			String error = json.optString("error");
			if (!TextUtils.isEmpty(error)) {
				Log.e(LOG_TAG, jsonStr);

				AuthError authError = new AuthError();
				authError.setError(error);
				authError.setErrorDescription(json
						.optString("error_description"));
				listener.onError(authError);
			} else {
				AccessToken token = TOPUtils.convertToAccessToken(json);
				token.setStartDate(this.getTime());
				addAccessToken(token);
				listener.onComplete(token);
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			listener.onAuthException(new AuthException(e));
		}

	}

	private void invokeApi(TopParameters params, TopApiListener listener,
			AccessToken token) {
		try {
			String jsonStr = WebUtils.doPost(context, env.getApiUrl(),
					this.generateApiParams(params, token),
					this.getProtocolParams(), params.getAttachments(),
					connectTimeout, readTimeout);
			Log.d(LOG_TAG, jsonStr);
			handleApiResponse(listener, jsonStr);
		} catch (Exception e) {
			Log.e(LOG_TAG, e.getMessage(), e);
			listener.onException(e);
		}
	}

	private void handleApiResponse(TopApiListener listener, String jsonStr)
			throws JSONException {
		JSONObject json = new JSONObject(jsonStr);
		ApiError error = this.parseError(json);
		if (error != null) {// failed
			Log.e(LOG_TAG, jsonStr);
			listener.onError(error);
		} else {
			listener.onComplete(json);
		}
	}

	private ApiError parseError(JSONObject json) throws JSONException {
		JSONObject resp = json.optJSONObject("error_response");
		if (resp == null) {
			return null;
		}
		String code = resp.optString("code");
		String msg = resp.optString("msg");
		String sub_code = resp.optString("sub_code");
		String sub_msg = resp.optString("sub_msg");
		ApiError error = null;
		if (!TextUtils.isEmpty(code) || !TextUtils.isEmpty(sub_code)) {
			error = new ApiError();
			error.setErrorCode(code);
			error.setMsg(msg);
			error.setSubCode(sub_code);
			error.setSubMsg(sub_msg);
		}
		return error;
	}

	private Map<String, String> generateApiParams(TopParameters topParameters,
			AccessToken token) throws IOException {
		TreeMap<String, String> params = new TreeMap<String, String>();
		params.put("timestamp", String.valueOf(System.currentTimeMillis()));
		params.put("v", "2.0");
		params.put("app_key", appKey);
		params.put("partner_id", "top-android-sdk");
		params.put("format", "json");
		if (token != null) {
			params.put("session", token.getValue());
		}
		params.put("sign_method", "hmac");
		params.put("method", topParameters.getMethod());

		Map<String, String> map = topParameters.getParams();
		if (map != null) {
			Set<Entry<String, String>> set = map.entrySet();
			for (Entry<String, String> entry : set) {
				params.put(entry.getKey(), entry.getValue());
			}
		}
		List<String> list = topParameters.getFields();
		if (list != null) {
			String fileds = TextUtils.join(",", list);
			if (!TextUtils.isEmpty(fileds)) {
				params.put("fields", fileds);
			}
		}
		String sign = TaobaoUtils.signTopRequestNew(params, appSecret);
		params.put("sign", sign);
		return params;
	}

	private Map<String, String> getProtocolParams() {
		String sign = JNIUtils.getTrackId(context, appKey, appSecret);
		String trackId = sign.substring(0, sign.indexOf("|"));
		String timestamp = sign.substring(sign.indexOf("|") + 1);

		Map<String, String> params = new HashMap<String, String>();
		params.put(SDK_CLIENT_SYSNAME, SYS_NAME);
		params.put(SDK_CLIENT_SYSVERSION, android.os.Build.VERSION.RELEASE);
		params.put(SDK_DEVICE_UUID, TOPUtils.getDeviceId(context));
		params.put(SDK_TRACK_ID, trackId);
		params.put(SDK_TIMESTAMP, timestamp);
		params.put(SDK_VERSION, JNIUtils.getSDKVersion());

		return params;
	}

	private void revertAccessToken() {
		File sessionDir = new File(context.getFilesDir(), SESSION_DIR);
		if (!sessionDir.exists()) {
			return;
		}

		File[] tokenFiles = sessionDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String prefix = new StringBuilder(appKey).append("_")
						.toString();
				return name.startsWith(prefix);
			}
		});

		if (tokenFiles != null && tokenFiles.length > 0) {
			for (File tokenFile : tokenFiles) {
				FileInputStream in = null;
				ObjectInputStream objIn = null;
				try {
					in = new FileInputStream(tokenFile);
					objIn = new ObjectInputStream(in);
					AccessToken token = (AccessToken) objIn.readObject();

					Long userId = getUserIdFromAccessToken(token);
					if (userId != null) {
						tokenStore.put(userId, token);
					}
				} catch (Exception e) {
					// 出现异常先跳过，只记录日志
					Log.e(LOG_TAG, e.getMessage(), e);
				} finally {
					if (objIn != null) {
						try {
							objIn.close();
						} catch (IOException e) {
							Log.e(LOG_TAG, e.getMessage(), e);
						}
					}
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							Log.e(LOG_TAG, e.getMessage(), e);
						}
					}
				}

			}
		}
	}

	private void persistenceAccessToken(String appkey, AccessToken token)
			throws IOException {
		if (TextUtils.isEmpty(appkey)) {
			throw new IllegalArgumentException("appkey must not empty.");
		}
		if (token == null) {
			throw new IllegalArgumentException("token must not null.");
		}
		File sessionDir = new File(context.getFilesDir(), SESSION_DIR);
		if (!sessionDir.exists()) {
			sessionDir.mkdir();
		}
		Long userId = getUserIdFromAccessToken(token);
		String fileName = new StringBuilder(appkey).append("_").append(userId)
				.toString();
		File tokenFile = new File(sessionDir, fileName);

		FileOutputStream output = null;
		ObjectOutputStream objOutput = null;
		try {
			output = new FileOutputStream(tokenFile);
			objOutput = new ObjectOutputStream(output);
			objOutput.writeObject(token);
			objOutput.flush();
		} finally {
			if (output != null) {
				output.close();
			}
			if (objOutput != null) {
				objOutput.close();
			}
		}
	}

	private Long getUserIdFromAccessToken(AccessToken accessToken) {
		String idStr = accessToken.getAdditionalInformation().get(
				AccessToken.KEY_SUB_TAOBAO_USER_ID);
		Long userId = idStr == null ? null : Long.valueOf(idStr);
		if (userId == null) {
			idStr = accessToken.getAdditionalInformation().get(
					AccessToken.KEY_TAOBAO_USER_ID);
			userId = idStr == null ? null : Long.parseLong(idStr);
		}
		return userId;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
	}

	public String getRedirectURI() {
		return redirectURI;
	}

	public void setRedirectURI(String redirectURI) {
		this.redirectURI = redirectURI;
	}

	public AccessToken getAccessToken(Long userId) {
		return tokenStore.get(userId);
	}
	
	/**
	 * 保存access token在内存和文件系统中
	 * @see #getAccessToken(Long)
	 * @param accessToken
	 * @throws IOException
	 */
	public void addAccessToken(AccessToken accessToken) throws IOException {
		Long userId = getUserIdFromAccessToken(accessToken);
		if (userId == null) {
			return;
		}

		tokenStore.put(userId, accessToken);
		persistenceAccessToken(appKey, accessToken);
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public ConcurrentHashMap<Long, AccessToken> getTokenStore() {
		return tokenStore;
	}

	public void setTokenStore(ConcurrentHashMap<Long, AccessToken> tokenStore) {
		this.tokenStore = tokenStore;
	}

	public Env getEnv() {
		return env;
	}

	public void setEnv(Env env) {
		this.env = env;
	}
	
	/**
	 * client运行环境枚举值
	 * @author junyan.hj
	 *
	 */
	public static enum Env {
		/**
		 * 生产环境
		 */
		PRODUCTION,
		/**
		 * 沙箱环境
		 */
		SANDBOX,
		/**
		 * 淘宝内部测试环境，isv禁止使用
		 */
		DAILY;

		private static final Map<String, String> URL_CONFIG = new HashMap<String, String>();
		static {
			URL_CONFIG.put("CONTAINER_URL_PRODUCTION",
					"https://oauth.taobao.com");
			URL_CONFIG.put("CONTAINER_URL_SANDBOX",
					"https://oauth.tbsandbox.com");
			URL_CONFIG.put("CONTAINER_URL_DAILY",
					"https://oauth.daily.taobao.net");

			URL_CONFIG.put("TOP_URL_PRODUCTION", "http://gw.api.taobao.com");
			URL_CONFIG.put("TOP_URL_SANDBOX", "http://gw.api.tbsandbox.com");
			URL_CONFIG.put("TOP_URL_DAILY", "http://10.232.127.144");
		}

		private String getConfigedValue(String keyPrefix) {
			String key = new StringBuilder(keyPrefix).append(toString())
					.toString();
			String url = URL_CONFIG.get(key);
			return url;
		}

		public String getApiUrl() {
			String url = getConfigedValue("TOP_URL_");
			return new StringBuilder(url).append("/router/rest").toString();
		}

		public String getTqlUrl() {
			String url = getConfigedValue("TOP_URL_");
			return new StringBuilder(url).append("/tql/2.0/json").toString();
		}

		public String getAuthUrl() {
			String url = getConfigedValue("CONTAINER_URL_");
			return new StringBuilder(url).append(
					"/authorize?response_type=token&view=wap").toString();
		}

		public String getRefreshUrl() {
			String url = getConfigedValue("CONTAINER_URL_");
			return new StringBuilder(url).append(
					"/token?grant_type=refresh_token").toString();
		}
	}
}
