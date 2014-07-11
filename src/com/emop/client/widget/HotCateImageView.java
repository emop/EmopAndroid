package com.emop.client.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageView;

public class HotCateImageView extends ImageView {
	public int id = 0;	
	public int fontSize = 0;
	public String pic = null;
	public String name = null;
	public String tags = null;
	//private ImageCache cache = null;
	private Paint mPaint = new Paint();

	public HotCateImageView(Context context, AttributeSet attr) {
		super(context, attr);
	}
	
	public HotCateImageView(Context context, String name, String pic, int id) {
		super(context);
		this.name = name;
		this.pic = pic;
		this.id = id;
		//this.cache = cache;
	}
	
	
	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		
		drawExtraInfo(canvas);
    }
	
	private void drawExtraInfo(Canvas canvas){
		String familyName = "宋体";
		//mPaint.setTypeface(Typeface.create(familyName, Typeface.NORMAL));
		mPaint.setTypeface(Typeface.DEFAULT);
		mPaint.setTextSize(fontSize);
		mPaint.setDither(false);
		mPaint.setAntiAlias(true);
		//FontMetrics mm = mPaint.getFontMetrics();
		//mm.
		
	    int mh = this.getMeasuredHeight();
	    int mw = this.getMeasuredWidth();
	    
	    float minHeight = mPaint.getTextSize() + 4;
	    float maxHeight = mPaint.getTextSize() * 1.5f;
	    float height = (float)(mh * 0.25);
	    	    
	    height = height > minHeight ? height : minHeight;
	    height = height > maxHeight ? maxHeight : height;
	    
	    mPaint.setColor(Color.BLACK);
	    mPaint.setAlpha(50);
	    
	    float top = mh - height;	    
	    canvas.drawRect(0, top, mw, mh, mPaint);

		mPaint.setColor(Color.WHITE);

		float padding = (height - mPaint.getTextSize()) / 2.5f;
	    
	    String text = String.format("%1$1s", this.name);	    
	    float left = (mw - mPaint.measureText(text)) / 2;
	    left = left > 0 ? left : 0;
	    
	    canvas.drawText(text, left, top + padding + mPaint.getTextSize(), mPaint);
	}	
	
}
