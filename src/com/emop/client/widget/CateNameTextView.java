package com.emop.client.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

public class CateNameTextView extends TextView {
	private static float scaleRate = 0;
	
	public CateNameTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		//this.onAttachToWindow()
	}
	
	protected void onAttachedToWindow(){
		super.onAttachedToWindow();
		
		Log.e("xxx", "scale with getMeasuredWidth:" + this.getMeasuredWidth());
		if(this.getMeasuredWidth() > 0){
			if(scaleRate == 0){
				initScaleRate();
			}
			FrameLayout.LayoutParams lp = (LayoutParams) this.getLayoutParams();
			if(lp != null){
				lp.width = (int)(getMeasuredWidth() * scaleRate);
				setLayoutParams(lp);
				Log.e("xxx", "scale with rate:" + scaleRate);
			}else {
				Log.e("xxx", "Not found layout param for item_text.");
			}
		}
	}

	private void initScaleRate(){
		ViewParent p = this.getParent();
		if(p instanceof FrameLayout){
			FrameLayout fl = (FrameLayout)p;
			ImageView v = (ImageView)fl.findViewById(com.emop.client.R.id.item_pic);
			float r1 =  300 / v.getMeasuredWidth();
			float r2 = 250 / v.getMeasuredHeight();
			scaleRate = Math.min(r1, r2);				
		}
	}
}
