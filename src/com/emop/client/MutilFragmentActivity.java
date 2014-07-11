package com.emop.client;

import android.content.Intent;
import android.os.Bundle;

public class MutilFragmentActivity extends BaseActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        int[] view = intent.getIntArrayExtra("view_id");
        if(view != null){
        	//R.layout.top
        	setContentView(view[0]);
        }
    }	
}
