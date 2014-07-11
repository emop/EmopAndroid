package com.emop.client.widget.item;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import com.emop.client.R;
import com.emop.client.TaokeItemDetailActivity;
import com.emop.client.io.FmeiClient;
import com.emop.client.io.ImageLoader;
import com.emop.client.provider.Schema;
import com.emop.client.widget.Constants;

public class FlowView extends ImageView implements View.OnClickListener,
		View.OnLongClickListener {
	public static ImageLoader imageLoader = null;
	private Paint mPaint = new Paint();

	private Context context;
	public int fontSize = 0;
	public Bitmap bitmap;
	public float rectRate = 0;
	public int topMargin = 0;
	public int bottomMargin = 0;
	private int columnIndex;// 图片属于第几列
	private int rowIndex;// 图片属于第几行
	private String fileName;
	private int ItemWidth;
	private Handler viewHandler;
	private int reloadTimes = 0;
	private long lastLoadTime = 0;
	/**
	 * 商品价格
	 * @todo -- 移到更合理的位置。这个是和瀑布流相关的一个控件。最好不要和特定
	 * 的业务场景相关。
	 */
	public float price;

	public FlowView(Context c, AttributeSet attrs, int defStyle) {
		super(c, attrs, defStyle);
		this.context = c;
		Init();
	}

	public FlowView(Context c, AttributeSet attrs) {
		super(c, attrs);
		this.context = c;
		Init();
	}

	public FlowView(Context c) {
		super(c);
		this.context = c;
		Init();
	}

	private void Init() {
		if(imageLoader == null){
			imageLoader = FmeiClient.getInstance(null).appImgLoader;
		}	
		setBackgroundColor(Color.WHITE);
		setOnClickListener(this);
		setOnLongClickListener(this);
		setAdjustViewBounds(true);
		this.setScaleType(ScaleType.CENTER_CROP);
	}

	@Override
	public void onClick(View v) {
		Log.d("FlowView", "Click");
		//Toast.makeText(context, "单击：" + getId(), Toast.LENGTH_SHORT).show();
    	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/item/" + getId());
		Intent intent = new Intent().setClass(context, 
				TaokeItemDetailActivity.class);
		intent.setData(dataUri);
		context.startActivity(intent); 		
	}

	/**
	 * 加载图片
	 */
	public void LoadImage() {
		//this.getMeasuredWidth()
		//Log.d("xxx", "load image:" + this.getId());
		if(this.rectRate > 0){
			imageLoader.loadToCache(getFileName(), getItemWidth(), true);

			int layoutHeight = (int)(getItemWidth() / rectRate);// 调整高度
			LayoutParams lp = getLayoutParams();
			int showHeight = layoutHeight - topMargin - bottomMargin - bottomMargin;
			if (lp == null) {
				lp = new LayoutParams(getItemWidth(), showHeight);
			}
			lp.height = showHeight;
			//Log.d("xx", "add image rate:" + rectRate + "mw:"+ getMeasuredWidth() + ", width:" + getItemWidth() + ", height:" + layoutHeight + ",topMargin:" + topMargin);
			setLayoutParams(lp);
			setScaleType(ScaleType.CENTER_CROP);
			setBackgroundResource(R.drawable.guang_image_border);

			Handler h = getViewHandler();
			Message m = h.obtainMessage(Constants.HANDLER_WHAT,
					getItemWidth(), layoutHeight, FlowView.this);
			//h.sendMessage(m);
			h.dispatchMessage(m);
		}else {
			imageLoader.runTask(new LoadImageThread());
		}
	}

	/**
	 * 重新加载图片
	 */
	public void Reload() {
		if (this.bitmap == null && (reloadTimes <= 3 || System.currentTimeMillis() - lastLoadTime > 1000 * 4)) {
			lastLoadTime = System.currentTimeMillis();
			reloadTimes++;
			//Log.d("xxxx", "reload img:" + this.fileName);
			setScaleType(ScaleType.FIT_CENTER);
			imageLoader.runTask(new Runnable(){
				@Override
				public void run() {
					bitmap = imageLoader.cache.get(fileName, getItemWidth(), true);
					if(bitmap != null){
						imageLoader.handler.post(new Runnable(){
							public void run(){
								setImageBitmap(bitmap);
								invalidate();
							}
						});
					}
				}
			});
		}
	}

	/**
	 * 回收内存
	 */
	public void recycle() {
		if(this.bitmap == null) return;
		//Log.d("xxxx", "recycle img:" + this.fileName);
		setImageBitmap(null);
		if(!bitmap.isRecycled()){
			bitmap.recycle();
		}
		reloadTimes = 0;
		this.bitmap = null;
	}

	public int getColumnIndex() {
		return columnIndex;
	}

	public void setColumnIndex(int columnIndex) {
		this.columnIndex = columnIndex;
	}

	public int getRowIndex() {
		return rowIndex;
	}

	public void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getItemWidth() {
		return ItemWidth;
	}

	public void setItemWidth(int itemWidth) {
		ItemWidth = itemWidth;
	}

	public Handler getViewHandler() {
		return viewHandler;
	}

	public FlowView setViewHandler(Handler viewHandler) {
		this.viewHandler = viewHandler;
		return this;
	}

	public void onDraw(Canvas canvas){
		super.onDraw(canvas);
		//如果还没有图片，尝试加载图片。
		if(this.bitmap == null){
			this.Reload();
		}
		
		drawExtraInfo(canvas);

	    int mh = this.getMeasuredHeight();
	    int mw = this.getMeasuredWidth();
		
		//Log.d("xx", "show rate:" + rectRate + ", mw:" + mw + ", mh:" + mh);
    }
	
	private void drawExtraInfo(Canvas canvas){
		
		mPaint.setTypeface(Typeface.DEFAULT);
		mPaint.setTextSize(fontSize);
		mPaint.setDither(false);
		mPaint.setAntiAlias(true);
		
	    int mh = this.getMeasuredHeight();
	    int mw = this.getMeasuredWidth();
	    
	    float minHeight = mPaint.getTextSize() + 4;
	    float maxHeight = mPaint.getTextSize() * 1.5f;
	    float height = (float)(mh * 0.25);

	    float margin = fontSize / 14 * 5;
	    height = height > minHeight ? height : minHeight;
	    height = height > maxHeight ? maxHeight : height;
	    
	    mPaint.setColor(Color.BLACK);
	    mPaint.setAlpha(50);
	    
	    float top = mh - height - margin;
	    final float roundPx = fontSize / 14 * 5;
	    
	    String text = String.format("￥%1$1.0f", this.price);	    
	    float left = (mw -  mPaint.measureText(text)); // / 2;
	    left = left > 0 ? left : 0;

	    final RectF rectF = new RectF(left - margin - roundPx * 2, top, mw - margin,
	    		mh - margin);  
	    canvas.drawRoundRect(rectF, roundPx, roundPx, mPaint);   
	    
	    float padding = (height - mPaint.getTextSize()) / 4f;
	    mPaint.setColor(Color.WHITE);
	    canvas.drawText(text, left - margin - roundPx, top + padding + mPaint.getTextSize(), mPaint);
	}
	
	class LoadImageThread extends Thread {
		LoadImageThread() {
		}

		public void run() {
			bitmap = imageLoader.cache.get(getFileName(), ItemWidth, true);
			// if (bitmap != null) {

			// 此处不能直接更新UI，否则会发生异常：
			// CalledFromWrongThreadException: Only the original thread that
			// created a view hierarchy can touch its views.
			// 也可以使用Handler或者Looper发送Message解决这个问题

			((Activity) context).runOnUiThread(new Runnable() {
				public void run() {
					if (bitmap != null) {// 此处在线程过多时可能为null
						int width = bitmap.getWidth();// 获取真实宽高
						int height = bitmap.getHeight();

						LayoutParams lp = getLayoutParams();
						
						// 减20为了尽量让高度，上下不留白边。
						int layoutHeight = (height * getItemWidth()) / width;// 调整高度
						int showHeight = layoutHeight - topMargin - bottomMargin * 2;
						if (lp == null) {
							lp = new LayoutParams(getItemWidth(), showHeight);
						}
						lp.height = showHeight;
						setLayoutParams(lp);
						setScaleType(ScaleType.CENTER_CROP);

						setImageBitmap(bitmap);
						Handler h = getViewHandler();
						Message m = h.obtainMessage(Constants.HANDLER_WHAT,
								getItemWidth(), layoutHeight, FlowView.this);
						h.sendMessage(m);
					}
				}
			});
		}
	}

	@Override
	public boolean onLongClick(View v) {
		return false;
	}
}
