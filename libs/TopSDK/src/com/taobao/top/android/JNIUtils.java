package com.taobao.top.android;

import android.content.Context;

public abstract class JNIUtils {

	static {
		System.loadLibrary("top-sdk");
	}
	
	public native static String getTrackId(Context context, String appkey,
			String appsecret);

	public native static String getSDKVersion();

}
