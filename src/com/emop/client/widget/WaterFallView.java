package com.emop.client.widget;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.emop.client.BuildConfig;
import com.emop.client.R;
import com.emop.client.widget.item.FlowView;
import com.emop.client.widget.item.FlowViewHandler;

public class WaterFallView extends ScrollView {
	
    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;
	

	public static final String TAG = "LazyScrollView";
	// 图片显示相关的图片
	public int columnCount = 3;
	public int itemWidth;
	//public int pageCount = 10;
	//public int loadedCount = 0;
	public ArrayList<LinearLayout> waterfall_items;
	public Handler handler;
	public Handler itemHandler;
	public WaterFallUtils waterFallUtils;
	public View view;
	public ViewGroup waterfallContainer;
	public WaterFallOption options = null;

	public int current_page = 0;// 当前页数
	public int pictureTotalCount = 1000;

	public int[] topIndex;
	public int[] bottomIndex;
	public int[] lineIndex;		//每列一共有多少个图片。
	public int[] column_height;// 每列的高度

	public SparseArray<String> pins;

	public int loaded_count = 0;// 已加载数量
	public SparseIntArray[] pin_mark; //每列里面，每行所对应的高度。

	public SparseArray<FlowView> iviews;

	public WaterFallView(Context context) {
		super(context);

	}

	public WaterFallView(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	public WaterFallView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		super.onScrollChanged(l, t, oldl, oldt);
		onScrollListener.onAutoScroll(l, t, oldl, oldt);
		waterFallUtils.autoReload(l, t, oldl, oldt);

	}

	public void setOptions() {

	}
	
	/**
	 * 显示开始加载栏。
	 */
	public void showHeadLoader(){
		if(this.options.headLoader != null){
			ProgressBar bar = (ProgressBar)this.options.headLoader.findViewById(R.id.loading_bar);
			bar.setIndeterminate(true);
			this.refreshManager.flag.setVisibility(GONE);
			options.headLoader.setVisibility(VISIBLE);
		}
	}
	
	public void load(){
		this.refreshManager.onRefreshing();
	}
	
	public boolean isLoading(){
		return refreshManager.isLoading;		
	}
	
	public boolean isHeadLoading(){
		if(options.headLoader != null){
			return options.headLoader.getVisibility() == VISIBLE;
		}else {
			return false;
		}
	}
	
	//private
	public void addImage(String filename, int rowIndex, int id, float price) {
		addImage(filename, rowIndex, id, price, 0);
	}
	public void addImage(String filename, int rowIndex, int id, float price, float rectRate) {
		this.loaded_count++;

		final FlowView item = new FlowView(getContext());
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT);
		lp.setMargins(options.itemMarginSize, 
				options.itemMarginSize, 
				options.itemMarginSize, 
				options.itemMarginSize);
		item.setLayoutParams(lp);
		
		item.setPadding(0, 0, 0, 0);
		item.setRowIndex(rowIndex);
		item.setId(id);
		item.setViewHandler(itemHandler);
		item.setFileName(filename);
		item.setItemWidth(options.itemWidth);
		item.fontSize = options.itemFontSize;
		item.topMargin = options.itemMarginSize;
		item.bottomMargin = options.itemMarginSize;
		
		item.price = price;
		item.rectRate = rectRate;
		this.handler.post(new Runnable(){
			public void run(){
				item.LoadImage();
			}
		});
		//Log.d("xxx", "added image count:" + this.loaded_count);
	}	

	private void init() {
		this.setOnTouchListener(onTouchListener);
		column_height = new int[columnCount];
		iviews = new SparseArray<FlowView>();
		pins = new SparseArray<String>();
		pin_mark = new SparseIntArray[columnCount];
		this.lineIndex = new int[columnCount];
		this.bottomIndex = new int[columnCount];
		this.topIndex = new int[columnCount];
		for (int i = 0; i < columnCount; i++) {
			lineIndex[i] = -1;
			bottomIndex[i] = -1;
		}
		// 初始化话waterfall_items 用于加载图片
		waterfall_items = new ArrayList<LinearLayout>();
		for (int i = 0; i < columnCount; i++) {
			LinearLayout itemLayout = new LinearLayout(getContext());
			LinearLayout.LayoutParams itemParam = new LinearLayout.LayoutParams(
					itemWidth, LayoutParams.WRAP_CONTENT);
			itemLayout.setPadding(0, 0, 0, 0);
			itemLayout.setOrientation(LinearLayout.VERTICAL);
			itemLayout.setLayoutParams(itemParam);
			waterfall_items.add(itemLayout);
			// 用于加载单列的显示
			waterfallContainer.addView(itemLayout);
			pin_mark[i] = new SparseIntArray();
		}

		//this.setv
        //setVerticalScrollBarEnabled(true);
        /*
        if (isVerticalFadingEdgeEnabled()) {
        }*/		
	}
	
	/**
	 * 初始化两个动画运行方式。
	 */
	private void initAnimation(){
		//动画变化的开始状态，和结束状态。
        mFlipAnimation = new RotateAnimation(0, 180,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        
        //动画运行速度。 直线平均速度。
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        //动画的运行时间。
        mFlipAnimation.setDuration(250);
        //动画结束后，是否保留结束状态。
        mFlipAnimation.setFillAfter(true);
        
        mReverseFlipAnimation = new RotateAnimation(180, 0,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);		
		
		
	}
	

	/**
	 * 获得参考的View，主要是为了获得它的MeasuredHeight，然后和滚动条的ScrollY+getHeight作比较。
	 */
	public void commitWaterFall(WaterFallOption options) {
		this.options = options;
		this.columnCount = options.column_count;
		this.itemWidth = options.itemWidth;
		this.waterfallContainer = options.waterFallContainer;
		// 一共加载的图片
		this.pictureTotalCount = options.pictureTotalCount;
		waterFallUtils = new WaterFallUtils(this);
		this.view = getChildAt(0);
		if (view != null) {
			handler = new WaterFallHandler(view, this, (int)(itemWidth * 2));
			init();
		}
		
		initAnimation();
		refreshManager = new RefreshManager();
		itemHandler = new FlowViewHandler(this);
	
	}
	

	OnTouchListener onTouchListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				Log.d(TAG,
						"ACTION_DOWN" + "Y->" + event.getY() + "X->"
								+ event.getX());
				refreshManager.actionDown(event);
				break;
			case MotionEvent.ACTION_UP:
				if (view != null && onScrollListener != null) {
					handler.sendMessageDelayed(
							handler.obtainMessage(Constants.HANDLER_WHAT),
							Constants.MESSAGE_DELAY);
				}
				refreshManager.actionUp(event);
				break;
			case MotionEvent.ACTION_MOVE:
				refreshManager.actionMove(event);
				break;
			default:
				break;
			}
			return false;
		}

	};
	


	/**
	 * 定义接口
	 * 
	 * @author admin
	 * 
	 */
	public interface OnScrollListener {
		void onBottom();

		void onTop();

		void onScroll();

		void onAutoScroll(int l, int t, int oldl, int oldt);
	}
	
	public interface OnRefreshListener {
		void onRefresh();
	}

	protected OnScrollListener onScrollListener;
	protected OnRefreshListener onRefreshListener;
	protected RefreshManager refreshManager = null;

	public void setOnScrollListener(OnScrollListener onScrollListener) {
		this.onScrollListener = onScrollListener;
	}
	
	public void setOnRefrreshListener(OnRefreshListener onRefreshListener) {
		this.onRefreshListener = onRefreshListener;
	}	

	public void deleteItems(FlowView v) {
		int rowIndex = v.getRowIndex();
		int columnIndex = v.getColumnIndex();

		int height = v.getHeight();
		waterfall_items.get(columnIndex).removeView(v);
		this.pin_mark[columnIndex].removeAt(rowIndex);
		for (int i = rowIndex; i < pin_mark[columnIndex].size(); i++) {
			this.pin_mark[columnIndex].put(i,
					this.pin_mark[columnIndex].get(i + 1) - height);
			this.pin_mark[columnIndex].removeAt(i + 1);
			((FlowView) this.waterfall_items.get(columnIndex).getChildAt(i))
					.setRowIndex(i);
		}

		lineIndex[columnIndex]--;
		column_height[columnIndex] -= height;
		if (this.bottomIndex[columnIndex] > this.lineIndex[columnIndex]) {
			bottomIndex[columnIndex]--;
		}
	}
	
	@Override
	public void fling(int velocityY) {
		super.fling(velocityY / 2);
		//Debug("velocity-->" + velocityY);
	}

	public static void Debug(String message) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, message);
		}
	}
	
	class RefreshManager{
		static final int NO_START_PULL  = 1;
		static final int START_TO_PULL  = 2;
		static final int RELEASE_TO_REFRESH = 3;
		static final int REFRESHING = 4;	
		
		public boolean isLoading = false;
		
		//开始下拉的位置。
		private float startPullMotionY = 0;
		private int mState = 1;
		private View loader = null; //options.headLoader;
		private ProgressBar loading = null; //options.headLoader;
		private ImageView flag = null; //options.headLoader;
		private TextView info = null;
		
		
		private int mRefreshViewHeight = 0;
		private int pullHeight = 0;
		private int oldLoaderPadding = 0;
		private int downMotionY = 0;
		
		public RefreshManager(){
			loader = options.headLoader;
			flag = (ImageView)loader.findViewById(R.id.refresh_flag);
			loading = (ProgressBar)loader.findViewById(R.id.loading_bar);
			info = (TextView)loader.findViewById(R.id.loading_info);	
		}
		
		public void actionDown(MotionEvent event){
			downMotionY = (int)event.getY();			
		}
		
		public void actionUp(MotionEvent event){
            if (!isVerticalScrollBarEnabled()) {
                setVerticalScrollBarEnabled(true);
            }			
			if(mState != NO_START_PULL){
				loader.setPadding(
						loader.getPaddingLeft(),
						oldLoaderPadding,
	                    loader.getPaddingRight(),
	                    loader.getPaddingBottom());
				if(mState == RELEASE_TO_REFRESH){
					onRefreshing();
				}else {
					onReset();
				}
			}
			mState = NO_START_PULL;
			downMotionY = 0;
		}
		
		public void actionMove(MotionEvent event){
			if(isLoading) return;
			if(downMotionY == 0){
				downMotionY = (int)event.getY();
				return;
			}
			
			float prePull = event.getY() - downMotionY;
			Log.d("tag", "pre pull size:" + prePull + ", downMotionY:" + downMotionY + ", now:" + event.getY());
			
			if(getScrollY() <= 0 && mState == NO_START_PULL && prePull > 30){
				mState = START_TO_PULL;
				pullHeight = 0;
				startPullMotionY = 0; //event.getY();
				if(mRefreshViewHeight == 0){
					mRefreshViewHeight = loader.getMeasuredHeight();
					oldLoaderPadding = loader.getPaddingTop();
				}
				onStart();
			}else if(mState == START_TO_PULL || mState == RELEASE_TO_REFRESH){
				applyHeaderPadding(event);
			}else {				
				Log.d("tag", "scroll y:" + getScrollY() + ", st:" + mState);
			}
			
			if(mState == START_TO_PULL && pullHeight > mRefreshViewHeight * 1){
				mState = RELEASE_TO_REFRESH;
				this.onReleaseToRefresh();
			} else if(mState == RELEASE_TO_REFRESH && pullHeight > 0 && pullHeight < mRefreshViewHeight * 1){
				mState = START_TO_PULL;
				this.onStartToPull();
			}
			
		}
		
		private void onReset(){
			loader.setVisibility(GONE);
			//flag.clearAnimation();
			//flag.setImageResource(R.drawable.ic_pulltorefresh_arrow);
			scrollTo(0, 0);			
		}

		private void onRefreshing(){
			flag.setVisibility(GONE);
			loading.setVisibility(VISIBLE);
			info.setText("努力加载中");
			Log.d(com.emop.client.Constants.TAG_EMOP, "on refreshing...");
			if(onRefreshListener != null){
				new Thread(){
					public void run(){
						isLoading = true;
						try{
							onRefreshListener.onRefresh();
						}finally{
							isLoading = false;
						}
						handler.post(new Runnable() {
							@Override
							public void run() {
								onReset();
							}});
					}
				}.start();
			}else {
				Log.e(com.emop.client.Constants.TAG_EMOP, "no listener....");
			}
		}		
		
		private void onStart(){
			loading.setVisibility(GONE);
			flag.setVisibility(VISIBLE);
			flag.clearAnimation();
			flag.setImageResource(R.drawable.ic_pulltorefresh_arrow);
			info.setText("下拉刷新");
			loader.setVisibility(VISIBLE);
            if (isVerticalFadingEdgeEnabled()) {
                setVerticalScrollBarEnabled(false);
            }
		}
		
		private void onReleaseToRefresh(){
			info.setText("松开即可刷新");
			flag.clearAnimation();
			flag.startAnimation(mFlipAnimation);	
		}

		private void onStartToPull(){
			info.setText("下拉刷新");
			flag.clearAnimation();
			flag.startAnimation(mReverseFlipAnimation);	
		}
		
	    private void applyHeaderPadding(MotionEvent ev) {
            int historicalY = (int) ev.getY();
            if(startPullMotionY == 0){
            	startPullMotionY = historicalY;
            }
            pullHeight = (int) (historicalY - startPullMotionY);
            
			//Log.d("xxx", "historicalY:" + historicalY + ",pull height:" + pullHeight + ", loader height:" + mRefreshViewHeight);
            
			//pullHeight = (int) ((event.getY() - startPullMotionY)); 
			loader.setPadding(
					loader.getPaddingLeft(),
					pullHeight + oldLoaderPadding,
                    loader.getPaddingRight(),
                    loader.getPaddingBottom());  			
	    }
	}
}
