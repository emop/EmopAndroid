package com.emop.client;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class AboutMeActivity extends BaseActivity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_me);
        
	    TextView version = (TextView) findViewById(R.id.setting_verison_name);
		PackageManager packageManager = getPackageManager();
		try {
			PackageInfo curVersion = packageManager.getPackageInfo(getPackageName(), 0);
			version.setText(curVersion.versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} 
		
		TextView copyright = (TextView) findViewById(R.id.copy_right);
		copyright.setText("All Rights Reserved.");
		if(client.trackUserId != null && client.trackUserId.trim().length() > 0){
			String s = "All Rights Reserved.";
			s += "(" + client.trackUserId.trim() + ")";
			copyright.setText(s);
		}
	}
	
    public void onFinish(View v){
    	finish();
    }	
}
