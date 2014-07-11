package com.emop.client;

import static com.emop.client.Constants.TAG_EMOP;

import java.util.HashMap;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.emop.client.cache.ImageCache;
import com.emop.client.io.ApiResult;
import com.emop.client.io.FmeiClient;
import com.emop.client.io.ImageLoader;
import com.emop.client.io.WeiBoClient;
import com.emop.client.io.WeiboUser;
import com.weibo.net.Weibo;

public class RegisterActivity extends BaseActivity {
	//private WeiBoClient weibo = null;
	
	private EditText nick = null;
	private EditText email = null;
	private EditText password = null;
	private WeiBoClient weibo = null;
	private ImageView icon = null;
	
	private ProgressDialog loading = null;
	private ImageCache cache = null;
	private WeiboUser weiboUser = null;
	private String userId = null;
	
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_view);
                
        loading = new ProgressDialog(this);
        loading.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loading.setMessage("注册中...");
        
        email = (EditText)findViewById(R.id.register_email_text);
        nick = (EditText)findViewById(R.id.register_nickname_text);
        password = (EditText)findViewById(R.id.register_passward_text);
        
        icon = (ImageView)findViewById(R.id.register_icon_icon);
        
        weibo = new WeiBoClient(this);//WeiBoClient.getInstance();
        if(cache == null){
        	cache = FmeiClient.getInstance(null).appImgLoader.cache;
        }
    }
    
    protected void onResume (){
    	super.onResume();
    	 
    	String sinaId = this.getIntent().getStringExtra("uid");
    	userId = this.getIntent().getStringExtra("userId");
    	Log.d(TAG_EMOP, "login sinaID:" + sinaId);
    	
    	if(sinaId != null){
    		new GetUserInfoTask().execute(sinaId);
    	}
    	
    }    
    
    public void doSaveRegister(View v){
    	String emailStr = email.getText().toString();
    	String nickStr = nick.getText().toString();
    	String passStr = password.getText().toString();

		loading.show();
    	new RegisterRequest().execute(emailStr, nickStr, passStr);
    }
    
    
    private class GetUserInfoTask extends AsyncTask<String, Void, WeiboUser> {
    	private Bitmap bitIcon = null;
    	
        protected void onPostExecute(WeiboUser u) {
        	if(nick != null && u != null && u.getString("screen_name") != null){
        		nick.setText(u.getString("screen_name"));
        	}
        	if(bitIcon != null){
        		icon.setImageBitmap(bitIcon);
        	}
        }

		@Override
		protected WeiboUser doInBackground(String... uid) {
			//if(uid)
			//WeiboUser u = null;
			if(uid.length > 0){
				weiboUser = weibo.getProfile(uid[0]);
			}
			
			if(weiboUser != null){
				bitIcon = cache.get(weiboUser.getString("profile_image_url"), 0, false);
			}
			
			return weiboUser;
		}
	}  
    
    private class RegisterRequest extends AsyncTask<String, Void, ApiResult> {

        protected void onPostExecute(ApiResult u) {
        	loading.dismiss();
        	if(u != null && u.isOK){
        		client.saveLoginUser(RegisterActivity.this, userId);
        		finish();
        	}else if(u != null){
        		Toast.makeText(RegisterActivity.this, u.getString("msg"), Toast.LENGTH_LONG).show();
        	}
        }

		@Override
		protected ApiResult doInBackground(String... uid) {
			ApiResult u = null;
			
			Map<String, Object> p = new HashMap<String, Object>();
			p.put("email", uid[0]);
			p.put("user_name", uid[1]);
			p.put("password", uid[2]);
			p.put("user_status", "2001");
			p.put("user_id", userId);
			
			if(weiboUser != null){
				Weibo weibo = Weibo.getInstance();
				
				p.put("profile_pic_url", weiboUser.getString("profile_image_url"));
				p.put("ref_uid", weiboUser.getString("id"));
				p.put("ref_token", weibo.getAccessToken().getToken());
				p.put("ref_type", "sina");
			}
			
			u = client.registerUser(p);
			
			return u;
		}
	}    
    
    

    
 
    

}
