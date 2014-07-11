/**
 * 
 */
package com.taobao.top.android.auth;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * OAuth2.0授权成功后得到的AccessToken
 * 
 * @author junyan.hj
 * 
 */
public class AccessToken implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7322593516469872908L;

//	private static final float EXPIRES_THRESHOLD=0.8f;
	
	public static final String KEY_ACCESS_TOKEN = "access_token";
	public static final String KEY_REFRESH_TOKEN = "refresh_token";
	public static final String KEY_EXPIRES_IN = "expires_in";
	public static final String KEY_TOKEN_TYPE = "token_type";
	public static final String KEY_RE_EXPIRES_IN = "re_expires_in";
	public static final String KEY_R1_EXPIRES_IN = "r1_expires_in";
	public static final String KEY_R2_EXPIRES_IN = "r2_expires_in";
	public static final String KEY_W1_EXPIRES_IN = "w1_expires_in";
	public static final String KEY_W2_EXPIRES_IN = "w2_expires_in";
	public static final String KEY_TAOBAO_USER_ID = "taobao_user_id";
	public static final String KEY_TAOBAO_USER_NICK = "taobao_user_nick";
	public static final String KEY_SUB_TAOBAO_USER_ID = "sub_taobao_user_id";
	public static final String KEY_SUB_TAOBAO_USER_NICK = "sub_taobao_user_nick";
	public static final String KEY_MOBILE_TOKEN = "mobile_token";

	private String value;
	private Long expiresIn;
	private String tokenType;
	private RefreshToken refreshToken;
	private Set<String> scope;
	private Map<String, String> additionalInformation;
	private Date startDate;//失效时间开始计时时间点

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Long expiresIn) {
		this.expiresIn = expiresIn;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public RefreshToken getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(RefreshToken refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Set<String> getScope() {
		return scope;
	}

	public void setScope(Set<String> scope) {
		this.scope = scope;
	}

	public Map<String, String> getAdditionalInformation() {
		return additionalInformation;
	}

	public void setAdditionalInformation(
			Map<String, String> additionalInformation) {
		this.additionalInformation = additionalInformation;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
}
