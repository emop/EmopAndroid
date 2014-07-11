/**
 * 
 */
package com.taobao.top.android;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

import com.taobao.top.android.auth.AccessToken;
import com.taobao.top.android.auth.RefreshToken;

public class TOPUtils {

	public static String getDeviceId(Context context) {
		String deviceId = null;
		TelephonyManager manager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (manager != null) {
			deviceId = manager.getDeviceId();
		}
		if (deviceId != null) {
			return "IMEI:" + deviceId;
		}
		deviceId = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);
		if (deviceId != null) {
			return "ANDROID:" + deviceId;
		}
		deviceId = Installation.id(context);
		return "UUID:" + deviceId;
	}
	
	public static AccessToken convertToAccessToken(JSONObject json) {
		AccessToken token = new AccessToken();
		token.setValue(json.optString(AccessToken.KEY_ACCESS_TOKEN));
		json.remove(AccessToken.KEY_ACCESS_TOKEN);

		token.setExpiresIn(json.optLong(AccessToken.KEY_EXPIRES_IN));
		json.remove(AccessToken.KEY_EXPIRES_IN);

		token.setTokenType(json.optString(AccessToken.KEY_TOKEN_TYPE));
		json.remove(AccessToken.KEY_TOKEN_TYPE);

		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setValue(json.optString(AccessToken.KEY_REFRESH_TOKEN));
		json.remove(AccessToken.KEY_REFRESH_TOKEN);

		refreshToken.setReExpiresIn(json.optLong(AccessToken.KEY_RE_EXPIRES_IN));
		json.remove(AccessToken.KEY_RE_EXPIRES_IN);

		token.setRefreshToken(refreshToken);

		@SuppressWarnings("unchecked")
		Iterator<String> keys = json.keys();
		if (keys != null) {
			Map<String, String> additionalInformation = new HashMap<String, String>();
			while(keys.hasNext()){
				String key=keys.next();
				additionalInformation.put(key, json.optString(key));
			}
			token.setAdditionalInformation(additionalInformation);
		}

		return token;
	}
	
	public static AccessToken convertToAccessToken(Bundle values) {
		AccessToken token = new AccessToken();
		token.setValue(values.getString(AccessToken.KEY_ACCESS_TOKEN));
		values.remove(AccessToken.KEY_ACCESS_TOKEN);

		token.setExpiresIn(Long.valueOf(values
				.getString(AccessToken.KEY_EXPIRES_IN)));
		values.remove(AccessToken.KEY_EXPIRES_IN);

		token.setTokenType(values.getString(AccessToken.KEY_TOKEN_TYPE));
		values.remove(AccessToken.KEY_TOKEN_TYPE);

		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setValue(values.getString(AccessToken.KEY_REFRESH_TOKEN));
		values.remove(AccessToken.KEY_REFRESH_TOKEN);

		refreshToken.setReExpiresIn(Long.valueOf(values
				.getString(AccessToken.KEY_RE_EXPIRES_IN)));
		values.remove(AccessToken.KEY_RE_EXPIRES_IN);

		token.setRefreshToken(refreshToken);

		Set<String> keys = values.keySet();
		if (keys != null) {
			Map<String, String> additionalInformation = new HashMap<String, String>();
			for (String key : keys) {
				additionalInformation.put(key, values.getString(key));
			}
			token.setAdditionalInformation(additionalInformation);
		}

		return token;
	}


}
