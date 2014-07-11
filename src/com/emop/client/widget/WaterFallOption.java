package com.emop.client.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class WaterFallOption {
	//显示列数
	public int column_count = 3;
	//每次加载的多少张图片
	public int pageCount = 30;
	 //允许加载的最多图片数
	public int pictureTotalCount = 1000;
	//用于handle 通讯的常量
	//消息发送的延迟时间
	public int message_delay = 200;
	//每列的宽度
	public int itemWidth;
	
	public View headLoader = null;
	//public ImageView refreshFlag = null;
	
	//瀑布流图片的相关参数。
	public int itemFontSize = 0;
	public int itemMarginSize = 0;
	
	public ViewGroup waterFallContainer;
	
	public WaterFallOption(ViewGroup container,int itemWidth,int columnCount){
		this.waterFallContainer = container;
		this.itemWidth = itemWidth;
		this.column_count = columnCount;
	}
	
}
