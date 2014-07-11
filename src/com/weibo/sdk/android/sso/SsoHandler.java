package com.weibo.sdk.android.sso;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.emop.client.Constants;
import com.emop.client.WebLoginActivity;
import com.emop.client.io.FmeiClient;
import com.emop.client.tasks.GetSinaUserInfoTask;
import com.sina.sso.RemoteSSO;
import com.weibo.net.AccessToken;
import com.weibo.net.Oauth2AccessToken;
import com.weibo.net.Token;
import com.weibo.net.Weibo;

/**
 * 该类用于处理sso 认证功能，通过sso，无需输入用户名、密码即可以通过微博账号访问经过授权的第三方应用，\r\n
 * 使用SSO登录前，请检查手机上是否已经安装新浪微博客户端，目前仅3.0.0及以上微博客户端版本支持SSO；
 * 如果未安装，将自动转为Oauth2.0进行认证
 * 
 * @author xiaowei6@staff.sina.com.cn
 *
 */
public class SsoHandler {
	public static final int START_WEB_LOGIN = 2001;
	public static final int SSO_LOGIN_ERROR = 2002;
	
    private ServiceConnection conn = null;
    private static final int DEFAULT_AUTH_ACTIVITY_CODE = 32973;
    private static final String WEIBO_SIGNATURE = "30820295308201fea00302010202044b4ef1bf300d"
            + "06092a864886f70d010105050030818d310b300906035504061302434e3110300e0603550408130"
            + "74265694a696e673110300e060355040713074265694a696e67312c302a060355040a132353696e"
            + "612e436f6d20546563686e6f6c6f677920284368696e612920436f2e204c7464312c302a0603550"
            + "40b132353696e612e436f6d20546563686e6f6c6f677920284368696e612920436f2e204c746430"
            + "20170d3130303131343130323831355a180f32303630303130323130323831355a30818d310b300"
            + "906035504061302434e3110300e060355040813074265694a696e673110300e0603550407130742"
            + "65694a696e67312c302a060355040a132353696e612e436f6d20546563686e6f6c6f67792028436"
            + "8696e612920436f2e204c7464312c302a060355040b132353696e612e436f6d20546563686e6f6c"
            + "6f677920284368696e612920436f2e204c746430819f300d06092a864886f70d010101050003818"
            + "d00308189028181009d367115bc206c86c237bb56c8e9033111889b5691f051b28d1aa8e42b66b7"
            + "413657635b44786ea7e85d451a12a82a331fced99c48717922170b7fc9bc1040753c0d38b4cf2b2"
            + "2094b1df7c55705b0989441e75913a1a8bd2bc591aa729a1013c277c01c98cbec7da5ad7778b2fa"
            + "d62b85ac29ca28ced588638c98d6b7df5a130203010001300d06092a864886f70d0101050500038"
            + "181000ad4b4c4dec800bd8fd2991adfd70676fce8ba9692ae50475f60ec468d1b758a665e961a3a"
            + "edbece9fd4d7ce9295cd83f5f19dc441a065689d9820faedbb7c4a4c4635f5ba1293f6da4b72ed3"
            + "2fb8795f736a20c95cda776402099054fccefb4a1a558664ab8d637288feceba9508aa907fc1fe2"
            + "b1ae5a0dec954ed831c0bea4";

//    private String[] mAuthPermissions;
    private int mAuthActivityCode;
    private static String ssoPackageName = "";// "com.sina.weibo";
    private static String ssoActivityName = "";// "com.sina.weibo.MainTabActivity";
    //private WeiboAuthListener mAuthDialogListener;
    //private Oauth2AccessToken mAccessToken = null;
    private Activity mAuthActivity;
    private Weibo mWeibo;
    public SsoHandler(Activity activity, Handler handler) {
        mAuthActivity = activity;
        if(handler != null){
        	this.handler = handler;
        }
        
		mWeibo = Weibo.getInstance();
		mWeibo.setupConsumerConfig(Constants.SINA_APPID, Constants.SINA_APPKEY);		
		mWeibo.setRedirectUrl(Constants.SINA_CALLBACK);
        
        //Weibo.isWifi=Utility.isWifi(activity);
        conn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
            	/**
            	 * 为啥在Disconnected的时候启动登陆窗口?
            	 */
                //mWeibo.startAuthDialog(mAuthActivity, mAuthDialogListener);
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                RemoteSSO remoteSSOservice = RemoteSSO.Stub.asInterface(service);
                try {
                    ssoPackageName = remoteSSOservice.getPackageName();
                    ssoActivityName = remoteSSOservice.getActivityName();
                    boolean singleSignOnStarted = startSingleSignOn(
                            mAuthActivity, Constants.SINA_APPID, new String[]{},
                            mAuthActivityCode);
                    if (!singleSignOnStarted) {
                    	startWebLogin();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    /**
     * 进行sso认证
     * 
     * @param activity 发起认证的Activity
     * 
     * @param listener 用于接收认证信息的监听者
    public void authorize( final WeiboAuthListener listener) {
        authorize( DEFAULT_AUTH_ACTIVITY_CODE,
                listener);
    }
     */
    public void authorize() {
        mAuthActivityCode = DEFAULT_AUTH_ACTIVITY_CODE;

        boolean bindSucced = false;
        
        
        //mAuthDialogListener = listener;

        // Prefer single sign-on, where available.
        bindSucced = bindRemoteSSOService(mAuthActivity);
        // Otherwise fall back to traditional dialog.
        if (!bindSucced) {
        	startWebLogin();
        }
    }
    
    public void startWebLogin(){
		Intent intent = new Intent();			
		intent.setClass(mAuthActivity, WebLoginActivity.class);
		
		Message msg = handler.obtainMessage(START_WEB_LOGIN);
		handler.sendMessage(msg);		
		mAuthActivity.startActivity(intent);  
    }

    private boolean bindRemoteSSOService(Activity activity) {
        Context context = activity.getApplicationContext();
        Intent intent = new Intent("com.sina.weibo.remotessoservice");
        return context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private boolean startSingleSignOn(Activity activity, String applicationId,
            String[] permissions, int activityCode) {
        boolean didSucceed = true;
        Intent intent = new Intent();
        intent.setClassName(ssoPackageName, ssoActivityName);
        intent.putExtra("appKey", Constants.SINA_APPID);// applicationId //"2745207810"
        intent.putExtra("redirectUri", Constants.SINA_CALLBACK);

        if (permissions.length > 0) {
            intent.putExtra("scope", TextUtils.join(",", permissions));
        }

        // validate Signature
        if (!validateAppSignatureForIntent(activity, intent)) {
            return false;
        }

        try {
            activity.startActivityForResult(intent, activityCode);
        } catch (ActivityNotFoundException e) {
            didSucceed = false;
        }

        activity.getApplication().unbindService(conn);
        return didSucceed;
    }

    private boolean validateAppSignatureForIntent(Activity activity,
            Intent intent) {
        ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(
                intent, 0);
        if (resolveInfo == null) {
            return false;
        }

        String packageName = resolveInfo.activityInfo.packageName;
        try {
            PackageInfo packageInfo = activity.getPackageManager()
                    .getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            for (Signature signature : packageInfo.signatures) {
                if (WEIBO_SIGNATURE.equals(signature.toCharsString())) {
                    return true;
                }
            }
        } catch (NameNotFoundException e) {
            return false;
        }

        return false;
    }

    /**
     * 重要:发起认证的Activity必须重写onActivityResult， 这个方法必须在onActivityResult 方法内调用，
     * 例如：<br/>
     * 
     * @Override
     * protected void onActivityResult(int requestCode, int resultCode, Intent data) {<br/>
     *    super.onActivityResult(requestCode, resultCode, data);<br/>
     *    if(mSsoHandler!=null){<br/>
     *       mSsoHandler.authorizeCallBack(requestCode, resultCode, data);<br/>
     *   }<br/>
     * }
    */
    
    public void authorizeCallBack(int requestCode, int resultCode, Intent data) {
        if (requestCode == mAuthActivityCode) {
            // Successfully redirected.
            if (resultCode == Activity.RESULT_OK) {
                // Check OAuth 2.0/2.10 error code.
                String error = data.getStringExtra("error");
                if (error == null) {
                    error = data.getStringExtra("error_type");
                }
                // error occurred.
                if (error != null) {
                    if (error.equals("access_denied")
                            || error.equals("OAuthAccessDeniedException")) {
                        Log.d("Weibo-authorize", "Login canceled by user.");
                    } else {
                        String description = data.getStringExtra("error_description");
                        if (description != null) {
                            error = error + ":" + description;
                        }
                        Log.d("Weibo-authorize", "Login failed: " + error);
                    }
                    Message msg = handler.obtainMessage(SSO_LOGIN_ERROR);
                    msg.obj = error;
                    handler.sendMessage(msg);
                } else {                    
                    String token = data.getStringExtra("access_token");
                    String expires_in = data.getStringExtra("expires_in");
        			AccessToken accessToken = new AccessToken(token, Weibo.getAppSecret());
        			
        			long expired = System.currentTimeMillis() + Long.parseLong(expires_in) * 1000L;
        			accessToken.setExpiresIn(expired);
        			if(isSessionValid(accessToken)){
        				FmeiClient client = FmeiClient.getInstance(null);
        				client.saveSettings(Constants.PREFS_SINA_ACCESS_TOKEN, token);
        				client.saveSettings(Constants.PREFS_SINA_EXPIRES_IN, expires_in);	
        				
        				Weibo.getInstance().setAccessToken(accessToken);
        				String userId = data.getStringExtra("uid");
        				
                        Log.d("Weibo-authorize", "user id:" + userId);
        				new GetSinaUserInfoTask(token, userId, mAuthActivity, handler).start();
        			}else {        				
        				//Log.d("weibo", "expires in:" + (System.currentTimeMillis() + Integer.parseInt(expires_in) * 1000L) + ", x:" + (Long.parseLong(expires_in) * 1000L));
        				//Log.d("Weibo-authorize", "error access token:" + token + ", expired in:" + expires_in + ", e:" + accessToken.getExpiresIn() + ", cur:" + System.currentTimeMillis());
        				startWebLogin();
        			}
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (data != null) {
                	String msg = data.getStringExtra("error");
                } else {
                    Log.d("Weibo-authorize", "Login canceled by user.");
                }
                Message msg = handler.obtainMessage(SSO_LOGIN_ERROR);
                handler.sendMessage(msg);
            }
        }
    }
   
    public boolean isSessionValid(Token mAccessToken) {
        if (mAccessToken != null) {
            return (!TextUtils.isEmpty(mAccessToken.getToken()) && (mAccessToken.getExpiresIn() == 0 || (System
                    .currentTimeMillis() < mAccessToken.getExpiresIn())));
        }
        return false;
    }
    
    private Handler handler = new Handler(){
    	
    	public void handleMessage(final Message msg) {
    		String message = null;
    		if(msg.obj != null){
    			message = msg.obj.toString();
    			if(message != null){
    				Toast.makeText(mAuthActivity, message, Toast.LENGTH_LONG).show();
    			}
    		}
    		if(msg.what == GetSinaUserInfoTask.LOGIN_DONE){
    		}
    	}

    };    

}
