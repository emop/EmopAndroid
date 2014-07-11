package com.emop.client.widget;

import com.tencent.mm.sdk.platformtools.Log;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CateImageView extends ImageView {
	public String name = "";
	public int fontSize = 0;
	private float scaleRate = 0;
	
	private Paint mPaint = new Paint();
	
	public CateImageView(Context context, AttributeSet attr) {
		super(context, attr);
	}	
	
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		if(scaleRate == 0){
			Log.d("ss", "w:" + getMeasuredWidth() + ",h:" + getMeasuredHeight());
			float r1 = getMeasuredWidth() / 300f;
			float r2 = getMeasuredHeight() / 250f;
			scaleRate = Math.min(r1, r2);
		}
		
		drawExtraInfo(canvas);
    }
	
	private void drawExtraInfo(Canvas canvas){
		mPaint.setTypeface(Typeface.DEFAULT);
		//mPaint.setStyle(Style.n)
		mPaint.setTextSize(fontSize);
		mPaint.setAntiAlias(true);
		mPaint.setDither(false);
		
	    int mh = this.getMeasuredHeight();
	    int mw = this.getMeasuredWidth();
	    
	    float minHeight = mPaint.getTextSize() + 4;
	    float maxHeight = mPaint.getTextSize() * 1.5f;
	    float height = (float)(mh * 0.25);
	    	    
	    height = height > minHeight ? height : minHeight;
	    height = height > maxHeight ? maxHeight : height;
	    
	    mPaint.setColor(Color.WHITE);
	    mPaint.setAlpha(255);
	    
	    float top = mh - height;
	    float textWidth = 198 * scaleRate;
	    float leftPadding = (mw - textWidth) / 2;
	    //Log.d("text", "scaleRate:" + scaleRate + ", textWidth:" + textWidth + ", leftPaddig:" + textWidth);
	    
	    canvas.drawRect(leftPadding, top, textWidth + leftPadding, mh - 5, mPaint);

		mPaint.setColor(Color.BLACK);

		float padding = (height - mPaint.getTextSize()) / 2.5f;
	    
	    String text = String.format("%1$1s", this.name);	    
	    float left = (mw -  mPaint.measureText(text)) / 2;
	    left = left > 0 ? left : 0;
	    
	    canvas.drawText(text, left, top + padding + mPaint.getTextSize() - 4, mPaint);
	}		

}
