package com.emop.client.fragment.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ListAdapter;

import com.emop.client.R;

public class CreditAdapter implements ListAdapter{
	private int creditLevel = 0;
	private int imageId = R.drawable.shop_level1;
	private Context context;
	//private LayoutInflater inflater = getLayoutInflater(null);
	public CreditAdapter(Context context, int c){
		this.context = context;
		creditLevel = c - 1;
		int level = creditLevel / 5 + 1;
		if(context != null && context.getResources() != null){
			imageId = context.getResources().getIdentifier("shop_level" + level, "drawable", "com.emop.client");
		}
	}
	@Override
	public int getCount() {
		return creditLevel % 5 + 1;
	}
	@Override
	public Object getItem(int arg0) {
		return arg0;
	}
	@Override
	public long getItemId(int arg0) {
		return creditLevel;
	}
	@Override
	public int getItemViewType(int arg0) {
		return 0;
	}
	@Override
	public View getView(int arg0, View arg1, ViewGroup arg2) {
		//GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.LayoutParams.FILL_PARENT,
		//		GridLayout.LayoutParams.FILL_PARENT);
		ImageView im = new ImageView(context);
		//ImageView im = (ImageView)inflater.inflate(R.layout.credit_level_image, null);
		//im.setLayoutParams(lp);
		im.setScaleType(ScaleType.FIT_CENTER);
		//Log.d("emop", "lp:" + im.getLayoutParams());
		im.setImageResource(imageId);			
		return im;
	}
	@Override
	public int getViewTypeCount() {
		return 1;
	}
	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void registerDataSetObserver(DataSetObserver arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean areAllItemsEnabled() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isEnabled(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}
}