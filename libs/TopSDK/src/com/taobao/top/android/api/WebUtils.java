package com.taobao.top.android.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

public abstract class WebUtils {
	public static final String DEFAULT_CHARSET = "UTF-8";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_GET = "GET";
	private static final String USER_AGENT = "TopAndroidSDK";

	private WebUtils() {

	}

	/**
	 * 执行HTTP POST请求。
	 * 
	 * @param context
	 *            TODO
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param headers
	 *            TODO
	 * @param responseError 响应码非200时是否返回响应内容。false:会throw IOException
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(Context context, String url,
			Map<String, String> params, Map<String, String> headers,
			int connectTimeout, int readTimeout, boolean responseError) throws IOException {
		return doPost(context, url, params, headers, DEFAULT_CHARSET,
				connectTimeout, readTimeout, responseError);
	}

	/**
	 * 执行HTTP POST请求。
	 * 
	 * @param context
	 *            TODO
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param headers
	 *            TODO
	 * @param charset
	 *            字符集，如UTF-8, GBK, GB2312
	 * @param responseError TODO
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(Context context, String url,
			Map<String, String> params, Map<String, String> headers,
			String charset, int connectTimeout, int readTimeout, boolean responseError)
			throws IOException {
		String ctype = "application/x-www-form-urlencoded;charset=" + charset;
		String query = buildQuery(params, charset);
		byte[] content = {};
		if (query != null) {
			content = query.getBytes(charset);
		}
		return doPost(context, url, ctype, content, headers, connectTimeout,
				readTimeout, responseError);
	}

	/**
	 * 执行HTTP POST请求。
	 * 
	 * @param context
	 *            TODO
	 * @param url
	 *            请求地址
	 * @param ctype
	 *            请求类型
	 * @param content
	 *            请求字节数组
	 * @param headers
	 *            请求header
	 * @param responseError TODO
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(Context context, String url, String ctype,
			byte[] content, Map<String, String> headers, int connectTimeout,
			int readTimeout, boolean responseError) throws IOException {
		HttpURLConnection conn = null;
		OutputStream out = null;
		String rsp = null;
		try {
			try {
				conn = getConnection(context, new URL(url), METHOD_POST, ctype);
				if (headers != null) {
					Set<Entry<String, String>> set = headers.entrySet();
					for (Entry<String, String> entry : set) {
						conn.addRequestProperty(entry.getKey(),
								entry.getValue());
					}

				}

				conn.setConnectTimeout(connectTimeout);
				conn.setReadTimeout(readTimeout);
			} catch (IOException e) {
				// Map<String, String> map = getParamsFromUrl(url);
				// TaobaoLogger.logCommError(e,
				// url,map.get("app_key"),map.get("method"), content);
				throw e;
			}
			try {
				out = conn.getOutputStream();
				out.write(content);
				rsp = getResponseAsString(conn, responseError);
			} catch (IOException e) {
				// Map<String, String> map = getParamsFromUrl(url);
				// TaobaoLogger.logCommError(e,
				// conn,map.get("app_key"),map.get("method"), content);
				throw e;
			}

		} finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}

	/**
	 * 执行带文件上传的HTTP POST请求。
	 * 
	 * @param context
	 *            TODO
	 * @param url
	 *            请求地址
	 * @param fileParams
	 *            文件请求参数
	 * @param textParams
	 *            文本请求参数
	 * 
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(Context context, String url,
			Map<String, String> params, Map<String, String> headers,
			Map<String, FileItem> fileParams, int connectTimeout,
			int readTimeout) throws IOException {
		if (fileParams == null || fileParams.isEmpty()) {
			return doPost(context, url, params, headers, DEFAULT_CHARSET,
					connectTimeout, readTimeout, false);
		} else {
			return doPost(context, url, params, headers, fileParams,
					DEFAULT_CHARSET, connectTimeout, readTimeout);
		}
	}

	/**
	 * 执行带文件上传的HTTP POST请求。
	 * 
	 * @param context
	 *            TODO
	 * @param url
	 *            请求地址
	 * @param fileParams
	 *            文件请求参数
	 * @param charset
	 *            字符集，如UTF-8, GBK, GB2312
	 * @param textParams
	 *            文本请求参数
	 * 
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doPost(Context context, String url,
			Map<String, String> params, Map<String, String> headers,
			Map<String, FileItem> fileParams, String charset,
			int connectTimeout, int readTimeout) throws IOException {
		String boundary = System.currentTimeMillis() + ""; // 随机分隔线
		HttpURLConnection conn = null;
		OutputStream out = null;
		String rsp = null;
		try {
			try {
				String ctype = "multipart/form-data;charset=" + charset
						+ ";boundary=" + boundary;
				conn = getConnection(context, new URL(url), METHOD_POST, ctype);
				if (headers != null) {
					Set<Entry<String, String>> set = headers.entrySet();
					for (Entry<String, String> entry : set) {
						conn.addRequestProperty(entry.getKey(),
								entry.getValue());
					}

				}
				conn.setConnectTimeout(connectTimeout);
				conn.setReadTimeout(readTimeout);
			} catch (IOException e) {
				// Map<String, String> map = getParamsFromUrl(url);
				// TaobaoLogger.logCommError(e,
				// url,map.get("app_key"),map.get("method"), params);
				throw e;
			}

			try {
				out = conn.getOutputStream();

				byte[] entryBoundaryBytes = ("\r\n--" + boundary + "\r\n")
						.getBytes(charset);

				// 组装文本请求参数
				Set<Entry<String, String>> textEntrySet = params.entrySet();
				for (Entry<String, String> textEntry : textEntrySet) {
					byte[] textBytes = getTextEntry(textEntry.getKey(),
							textEntry.getValue(), charset);
					out.write(entryBoundaryBytes);
					out.write(textBytes);
				}

				// 组装文件请求参数
				Set<Entry<String, FileItem>> fileEntrySet = fileParams
						.entrySet();
				for (Entry<String, FileItem> fileEntry : fileEntrySet) {
					FileItem fileItem = fileEntry.getValue();
					byte[] fileBytes = getFileEntry(fileEntry.getKey(),
							fileItem.getFileName(), fileItem.getMimeType(),
							charset);
					out.write(entryBoundaryBytes);
					out.write(fileBytes);
					out.write(fileItem.getContent());
				}

				// 添加请求结束标志
				byte[] endBoundaryBytes = ("\r\n--" + boundary + "--\r\n")
						.getBytes(charset);
				out.write(endBoundaryBytes);
				rsp = getResponseAsString(conn, false);
			} catch (IOException e) {
				// Map<String, String> map = getParamsFromUrl(url);
				// TaobaoLogger.logCommError(e,
				// conn,map.get("app_key"),map.get("method"), params);
				throw e;
			}

		} finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}

	private static byte[] getTextEntry(String fieldName, String fieldValue,
			String charset) throws IOException {
		StringBuilder entry = new StringBuilder();
		entry.append("Content-Disposition:form-data;name=\"");
		entry.append(fieldName);
		entry.append("\"\r\nContent-Type:text/plain\r\n\r\n");
		entry.append(fieldValue);
		return entry.toString().getBytes(charset);
	}

	private static byte[] getFileEntry(String fieldName, String fileName,
			String mimeType, String charset) throws IOException {
		StringBuilder entry = new StringBuilder();
		entry.append("Content-Disposition:form-data;name=\"");
		entry.append(fieldName);
		entry.append("\";filename=\"");
		entry.append(fileName);
		entry.append("\"\r\nContent-Type:");
		entry.append(mimeType);
		entry.append("\r\n\r\n");
		return entry.toString().getBytes(charset);
	}

	/**
	 * 执行HTTP GET请求。
	 * 
	 * @param context
	 *            TODO
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * 
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doGet(Context context, String url,
			Map<String, String> params) throws IOException {
		return doGet(context, url, params, DEFAULT_CHARSET);
	}

	/**
	 * 执行HTTP GET请求。
	 * 
	 * @param context
	 *            TODO
	 * @param url
	 *            请求地址
	 * @param params
	 *            请求参数
	 * @param charset
	 *            字符集，如UTF-8, GBK, GB2312
	 * 
	 * @return 响应字符串
	 * @throws IOException
	 */
	public static String doGet(Context context, String url,
			Map<String, String> params, String charset) throws IOException {
		HttpURLConnection conn = null;
		String rsp = null;

		try {
			String ctype = "application/x-www-form-urlencoded;charset="
					+ charset;
			String query = buildQuery(params, charset);
			try {
				conn = getConnection(context, buildGetUrl(url, query),
						METHOD_GET, ctype);
			} catch (IOException e) {
				// Map<String, String> map = getParamsFromUrl(url);
				// TaobaoLogger.logCommError(e,
				// url,map.get("app_key"),map.get("method"), params);
				throw e;
			}

			try {
				rsp = getResponseAsString(conn, false);
			} catch (IOException e) {
				// Map<String, String> map = getParamsFromUrl(url);
				// TaobaoLogger.logCommError(e,
				// conn,map.get("app_key"),map.get("method"), params);
				throw e;
			}

		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}

	private static HttpURLConnection getConnection(Context context, URL url,
			String method, String ctype) throws IOException {
		Proxy proxy = null;
		WifiManager wifiManager = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (!wifiManager.isWifiEnabled()) {
			// 获取当前正在使用的APN接入点
			Uri uri = Uri.parse("content://telephony/carriers/preferapn");
			Cursor mCursor = context.getContentResolver().query(uri, null,
					null, null, null);
			if (mCursor != null && mCursor.moveToFirst()) {
				// 游标移至第一条记录，当然也只有一条
				String proxyStr = mCursor.getString(mCursor
						.getColumnIndex("proxy"));
				if (proxyStr != null && proxyStr.trim().length() > 0) {
					proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
							proxyStr, 80));
				}
				mCursor.close();
			}
		}

		HttpURLConnection conn = null;
		if ("https".equals(url.getProtocol())) {
			SSLContext ctx = null;
			try {
				ctx = SSLContext.getInstance("TLS");
				ctx.init(new KeyManager[0],
						new TrustManager[] { new DefaultTrustManager() },
						new SecureRandom());
			} catch (Exception e) {
				throw new IOException(e.getMessage());
			}
			HttpsURLConnection connHttps = null;
			if (proxy == null) {
				connHttps = (HttpsURLConnection) url.openConnection();
			} else {
				connHttps = (HttpsURLConnection) url.openConnection(proxy);
			}
			connHttps.setSSLSocketFactory(ctx.getSocketFactory());
			connHttps.setHostnameVerifier(new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;// 默认都认证通过
				}
			});
			conn = connHttps;
		} else {
			if (proxy == null) {
				conn = (HttpURLConnection) url.openConnection();
			} else {
				conn = (HttpURLConnection) url.openConnection(proxy);
			}
		}

		conn.setRequestMethod(method);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Accept", "text/xml,text/javascript,text/html");
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.setRequestProperty("Content-Type", ctype);
		conn.setRequestProperty("Accept-Encoding", "gzip");//support gzip
		return conn;
	}

	public static URL buildGetUrl(String url, Map<String, String> params,
			String charset) throws IOException {
		String queryString = buildQuery(params, charset);
		return buildGetUrl(url, queryString);
	}

	private static URL buildGetUrl(String strUrl, String query)
			throws IOException {
		URL url = new URL(strUrl);
		if (TextUtils.isEmpty(query)) {
			return url;
		}

		if (TextUtils.isEmpty(url.getQuery())) {
			if (strUrl.endsWith("?")) {
				strUrl = strUrl + query;
			} else {
				strUrl = strUrl + "?" + query;
			}
		} else {
			if (strUrl.endsWith("&")) {
				strUrl = strUrl + query;
			} else {
				strUrl = strUrl + "&" + query;
			}
		}

		return new URL(strUrl);
	}

	public static String buildQuery(Map<String, String> params, String charset)
			throws UnsupportedEncodingException {
		if (params == null || params.isEmpty()) {
			return null;
		}
		if (TextUtils.isEmpty(charset)) {
			charset = DEFAULT_CHARSET;
		}

		StringBuilder query = new StringBuilder();
		Set<Entry<String, String>> entries = params.entrySet();
		boolean hasParam = false;

		for (Entry<String, String> entry : entries) {
			String name = entry.getKey();
			String value = entry.getValue();
			// 忽略参数名或参数值为空的参数
			if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
				if (hasParam) {
					query.append("&");
				} else {
					hasParam = true;
				}

				query.append(name).append("=")
						.append(URLEncoder.encode(value, charset));
			}
		}

		return query.toString();
	}
	
	/**
	 * 获取http响应内容<br>
	 * 当响应状态码不等于200时，如果要换取响应的内容responseError参数请输入true。否则会抛出IOException
	 * 
	 * @param conn
	 * @param responseError
	 * @return
	 * @throws IOException
	 */
	protected static String getResponseAsString(HttpURLConnection conn,
			boolean responseError) throws IOException {
		String charset = getResponseCharset(conn.getContentType());
		String header = conn.getHeaderField("Content-Encoding");
		boolean isGzip = false;
		if (header != null && header.toLowerCase().contains("gzip")) {
			isGzip = true;
		}
		InputStream es = conn.getErrorStream();
		if (es == null) {
			InputStream input = conn.getInputStream();
			if (isGzip) {
				input = new GZIPInputStream(input);
			}
			return getStreamAsString(input, charset);
		} else {
			if (isGzip) {
				es = new GZIPInputStream(es);
			}
			String msg = getStreamAsString(es, charset);
			if (TextUtils.isEmpty(msg)) {
				throw new IOException(conn.getResponseCode() + ":"
						+ conn.getResponseMessage());
			} else if (responseError) {
				return msg;
			} else {
				throw new IOException(msg);
			}
		}
	}

	private static String getStreamAsString(InputStream stream, String charset)
			throws IOException {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream, charset));
			StringWriter writer = new StringWriter();

			char[] chars = new char[256];
			int count = 0;
			while ((count = reader.read(chars)) > 0) {
				writer.write(chars, 0, count);
			}

			return writer.toString();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	private static String getResponseCharset(String ctype) {
		String charset = DEFAULT_CHARSET;

		if (!TextUtils.isEmpty(ctype)) {
			String[] params = ctype.split(";");
			for (String param : params) {
				param = param.trim();
				if (param.startsWith("charset")) {
					String[] pair = param.split("=", 2);
					if (pair.length == 2) {
						if (!TextUtils.isEmpty(pair[1])) {
							charset = pair[1].trim();
						}
					}
					break;
				}
			}
		}

		return charset;
	}

	/**
	 * 使用默认的UTF-8字符集反编码请求参数值。
	 * 
	 * @param value
	 *            参数值
	 * @return 反编码后的参数值
	 */
	public static String decode(String value) {
		return decode(value, DEFAULT_CHARSET);
	}

	/**
	 * 使用默认的UTF-8字符集编码请求参数值。
	 * 
	 * @param value
	 *            参数值
	 * @return 编码后的参数值
	 */
	public static String encode(String value) {
		return encode(value, DEFAULT_CHARSET);
	}

	/**
	 * 使用指定的字符集反编码请求参数值。
	 * 
	 * @param value
	 *            参数值
	 * @param charset
	 *            字符集
	 * @return 反编码后的参数值
	 */
	public static String decode(String value, String charset) {
		String result = null;
		if (!TextUtils.isEmpty(value)) {
			try {
				result = URLDecoder.decode(value, charset);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	/**
	 * 使用指定的字符集编码请求参数值。
	 * 
	 * @param value
	 *            参数值
	 * @param charset
	 *            字符集
	 * @return 编码后的参数值
	 */
	public static String encode(String value, String charset) {
		String result = null;
		if (!TextUtils.isEmpty(value)) {
			try {
				result = URLEncoder.encode(value, charset);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	// private static Map<String, String> getParamsFromUrl(String url) {
	// Map<String, String> map = null;
	// if (url != null && url.indexOf('?') != -1) {
	// map = splitUrlQuery(url.substring(url.indexOf('?') + 1));
	// }
	// if (map == null) {
	// map = new HashMap<String, String>();
	// }
	// return map;
	// }

	/**
	 * 从URL中提取所有的参数。
	 * 
	 * @param query
	 *            URL地址
	 * @return 参数映射
	 */
	public static Map<String, String> splitUrlQuery(String query) {
		Map<String, String> result = new HashMap<String, String>();

		String[] pairs = query.split("&");
		if (pairs != null && pairs.length > 0) {
			for (String pair : pairs) {
				String[] param = pair.split("=", 2);
				if (param != null && param.length == 2) {
					result.put(param[0], param[1]);
				}
			}
		}

		return result;
	}

	private static class DefaultTrustManager implements X509TrustManager {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}
	}

}
