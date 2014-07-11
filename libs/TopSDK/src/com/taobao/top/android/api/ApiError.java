/**
 * 
 */
package com.taobao.top.android.api;

/**
 * API调用的业务错误信息
 * @author junyan.hj
 * 
 */
public class ApiError {
	private String errorCode;
	private String msg;
	private String subCode;
	private String subMsg;

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getSubCode() {
		return subCode;
	}

	public void setSubCode(String subCode) {
		this.subCode = subCode;
	}

	public String getSubMsg() {
		return subMsg;
	}

	public void setSubMsg(String subMsg) {
		this.subMsg = subMsg;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("errorCode:").append(this.errorCode).append(" msg:")
				.append(this.msg).append(" subCode:").append(subCode)
				.append(" subMsg:").append(this.subMsg);
		return builder.toString();
	}
}
