/**
 * 
 */
package com.taobao.top.android.auth;

/**
 * 授权过程中一些未知异常，例如网络访问时出现的异常
 * @author junyan.hj
 * 
 */
public class AuthException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6991536303345890311L;

	public AuthException() {
		super();
	}

	public AuthException(String msg) {
		super(msg);
	}

	public AuthException(String msg, Throwable throwable) {
		super(msg, throwable);
	}

	public AuthException(Throwable throwable) {
		super(throwable);
	}
}
