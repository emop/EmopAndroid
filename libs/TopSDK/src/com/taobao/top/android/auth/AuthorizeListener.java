/**
 * 
 */
package com.taobao.top.android.auth;


/**
 * 授权操作监听器
 * 
 * @author junyan.hj
 * 
 */
public interface AuthorizeListener {
	/**
	 * 授权完成时回调此方法
	 * 
	 * @param accessToken
	 */
	void onComplete(AccessToken accessToken);

	/**
	 * 授权失败时回调此方法<br>
	 * 例如：用户拒绝授权等
	 * 
	 * @param e
	 */
	void onError(AuthError e);

//	/**
//	 * 用户中断了授权操作回调此方法
//	 * 
//	 */
//	void onCancel();

	/**
	 * 出现一些系统异常是回调此方法<br>
	 * 例如：因为网络问题，页面无法打开等
	 * 
	 * @param e
	 */
	void onAuthException(AuthException e);

}
