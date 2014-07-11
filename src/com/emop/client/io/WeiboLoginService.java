package com.emop.client.io;

import static com.emop.client.Constants.TAG_EMOP;

import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.weibo.net.DialogError;
import com.weibo.net.Oauth2AccessTokenHeader;
import com.weibo.net.Token;
import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboDialogListener;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboParameters;

public class WeiboLoginService {
	
	private WebView web = null;
	private Weibo mWeibo = null;
	private Context ctx = null;
	private WeiboDialogListener mAuthDialogListener;
	private ProgressBar bar;
	
	public WeiboLoginService(Context ctx, WebView web, Weibo weibo, WeiboDialogListener webListener, ProgressBar bar){
		web.setWebViewClient(new WeiboWebViewClient());
		//web.loadUrl(mUrl);
		this.web = web;
		this.mWeibo = weibo;
		this.mAuthDialogListener = webListener;
		this.bar = bar;
		this.ctx = ctx;
	}
		
	public void startLogin(){
		
		WeiboParameters params = new WeiboParameters();
		
		params.add("client_id", mWeibo.getAppKey());
		params.add("response_type", "token");
        params.add("redirect_uri", mWeibo.getRedirectUrl());
        params.add("display", "mobile");
		
		String url = Weibo.URL_OAUTH2_ACCESS_AUTHORIZE + "?" + Utility.encodeUrl(params);
		Utility.setAuthorization(new Oauth2AccessTokenHeader());
		CookieSyncManager.createInstance(ctx);

		web.loadUrl(url);
	}
	
    private class WeiboWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG_EMOP, "Redirect URL: " + url);
            // 待后台增加对默认重定向地址的支持后修改下面的逻辑
            if (url.startsWith(mWeibo.getRedirectUrl())) {
                handleRedirectUrl(view, url);
               // WeiboDialog.this.dismiss();
                return true;
            }
            // launch non-dialog URLs in a full browser
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            mAuthDialogListener.onError(new DialogError(description, errorCode, failingUrl));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG_EMOP, "onPageStarted URL: " + url);
            // google issue. shouldOverrideUrlLoading not executed
            if (url.startsWith(mWeibo.getRedirectUrl())) {
                handleRedirectUrl(view, url);
                view.stopLoading();
                return;
            }else {
            	super.onPageStarted(view, url, favicon);
            }
            //mSpinner.show();
            bar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG_EMOP, "onPageFinished URL: " + url);
            super.onPageFinished(view, url);
            
            bar.setVisibility(View.INVISIBLE);
        }

        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        	Log.d(TAG_EMOP, "onReceivedSslError: ...");
            handler.proceed();
        }

    }

    private void handleRedirectUrl(WebView view, String url) {
    	Log.d(TAG_EMOP, "redirect to:" + url);
        Bundle values = Utility.parseUrl(url);

		Uri uri = Uri.parse(url);
		String ret = uri.getFragment();
		if(ret != null){
			String[] kv = ret.split("&");
			for (String each : kv) {
				String[] ss = each.split("=");
				if (ss != null && ss.length == 2) {
					values.putString(ss[0], ss[1]);
				}
			}
		}
        Log.d(TAG_EMOP, "redirect to:" + values.getString("uid") + ",token:" + values.getString("access_token") + ", expired:" + values.getString(Weibo.EXPIRES));

        String error = values.getString("error");
        String error_code = values.getString("error_code");
        
        Log.d(TAG_EMOP, "error:" + error + ", error_code:" + error_code);
        Token mAccessToken = new Token();
        mAccessToken.setToken(values.getString(Weibo.TOKEN));
        mAccessToken.setExpiresIn(values.getString(Weibo.EXPIRES));
        
        Date d = new Date(mAccessToken.getExpiresIn());
        
        Log.d(TAG_EMOP, "expirs In:" + d.toString() + ", now:" + new Date().toString());
        
        if (error == null && error_code == null) {
        	mAuthDialogListener.onComplete(values);
        } else if (error.equals("access_denied")) {
            // 用户或授权服务器拒绝授予数据访问权限
        	mAuthDialogListener.onCancel();
        } else {
        	mAuthDialogListener.onWeiboException(new WeiboException(error, Integer.parseInt(error_code)));
        }
    }
    
    public boolean isSessionValid(Token mAccessToken) {
        if (mAccessToken != null) {
            return (!TextUtils.isEmpty(mAccessToken.getToken()) && (mAccessToken.getExpiresIn() == 0 || (System
                    .currentTimeMillis() < mAccessToken.getExpiresIn())));
        }
        return false;
    }    
 
}
