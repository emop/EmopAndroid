/**
 * 
 */
package com.taobao.top.android.api;

import org.json.JSONObject;

/**
 * API调用的事件监听器
 * @author junyan.hj
 * 
 */
public interface TopApiListener {
	/**
	 * API调用成功后返回值以json对象方式通知监听器
	 * @param json
	 */
	void onComplete(JSONObject json);
	/**
	 * 出现业务错误时通知监听器错误码及字错误码等信息
	 * @param error
	 */
	void onError(ApiError error);
	/**
	 * 出现网络问题等未知异常时会回调此方法
	 * @param e
	 */
	void onException(Exception e);
}
