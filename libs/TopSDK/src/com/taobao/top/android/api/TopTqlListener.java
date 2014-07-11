/**
 * 
 */
package com.taobao.top.android.api;

/**
 * @author junyan.hj
 * 
 */
public interface TopTqlListener {
	/**
	 * TQL调用完成后的返回值<br>
	 * 返回值中以"\r\n"分隔多个json字符串
	 * 
	 * @param json
	 */
	void onComplete(String result);

	/**
	 * 出现网络问题等未知异常时会回调此方法
	 * 
	 * @param e
	 */
	void onException(Exception e);
}
