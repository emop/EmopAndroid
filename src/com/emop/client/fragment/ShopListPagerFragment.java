package com.emop.client.fragment;

import java.util.ArrayList;

import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import com.emop.client.R;
import com.emop.client.fragment.TopicItemListFragment.TopicPage;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Topic;
import com.emop.client.wxapi.DensityUtil;


public class ShopListPagerFragment extends Fragment {
	private ViewPager viewPager = null;
	private ViewGroup navigationGroup = null;
	private int picMarginDp = 2;
	private Handler handler = new Handler();

	private ArrayList<CateData> views = new ArrayList<CateData>();
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState){		
		Log.d("xx", "onCreateView...");
				
		View v = inflater.inflate(R.layout.shop_list_viewpager_fragment, container, false);
		viewPager = (ViewPager)v.findViewById(R.id.viewPager);		
		navigationGroup = (ViewGroup)v.findViewById(R.id.navigationGroup);
		
		viewPager.setAdapter(new TopicItemAdapter());
		viewPager.setOnPageChangeListener(pageListener);
		
		CateData EMPTY = new CateData();
		EMPTY.id = -1;
		views.add(EMPTY);
		
		return v;
	}
	
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		
		getLoaderManager().initLoader(0, null, 
				new LoaderCallbacks<Cursor>() {
					@Override
					public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
						return new CursorLoader(getActivity(), Schema.CATE_LIST,
								new String[] { BaseColumns._ID, Topic.TITLE}, 
								null, null, null);
					}
					
					@Override
					public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
						Log.d("xx", "onLoad finishied, count:" + cursor.getCount());
						loadCateList(cursor);
					}

					@Override
					public void onLoaderReset(Loader<Cursor> arg0) {
						//adapter.swapCursor(null);
					}
				}
			);	
	}
	
	/**
	 * 加载分类列表.
	 * @param c
	 */
	protected void loadCateList(Cursor c){
		if(c == null && c.getCount() == 0){
			return;
		}
		
		int idIndex = 0, nameIndex = 0;
		boolean hasMore = c.moveToFirst();
		
		idIndex = c.getColumnIndex(BaseColumns._ID);
		nameIndex = c.getColumnIndex(Topic.TITLE);

		views.clear();
		
		CateData cate = new CateData();
		cate.id = CateData.ALL_CATE;
		cate.name = "全部";
		views.add(cate);
		
		for(;hasMore; hasMore = c.moveToNext()){
			cate = new CateData();
			cate.id = c.getInt(idIndex);
			cate.name = c.getString(nameIndex);
			views.add(cate);
		}
		
		loadNavigation();
		
		handler.post(new Runnable(){
			public void run(){		
				viewPager.getAdapter().notifyDataSetChanged();
			}
		});
	}
	
	protected void loadNavigation(){
		if(navigationGroup != null){
			navigationGroup.removeAllViews();
		}
		
		TextView text = null;
		int margin = DensityUtil.dip2px(getActivity(), picMarginDp);
		LayoutInflater inflater = getLayoutInflater(null);
		for(CateData c: views){
			text = (TextView)inflater.inflate(R.layout.shop_nav_tab_item, null);
			text.setClickable(true);
			text.setText(c.name);
			text.setId(c.id);
			//text.setEnabled(enabled)
			text.setEnabled(c.id != CateData.ALL_CATE);
			
			text.setOnClickListener(clickListener);
			
			navigationGroup.addView(text);
		}
	}
	
	private OnClickListener clickListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			for(int i = 0; i < views.size(); i++){
				if(views.get(i).id == v.getId()){
					viewPager.setCurrentItem(i);
				}
			}
		}
		
	};
	
	protected OnPageChangeListener pageListener = new OnPageChangeListener(){

		@Override
		public void onPageScrollStateChanged(int arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onPageSelected(int position) {
			Log.d("xx", "onPage selected:" + position);
			TextView text = null;
			
			for(int i = 0; i < views.size(); i++){
				text = (TextView)navigationGroup.getChildAt(i);
				text.setEnabled(i != position);
			}
			
			HorizontalScrollView scroll = (HorizontalScrollView)navigationGroup.getParent();
			text = (TextView)navigationGroup.getChildAt(position);
			int[] location = new int[2];
			//text.getl
			text.getLocationOnScreen(location);
			scroll.smoothScrollTo(text.getMeasuredWidth() * position, location[1]);			
		}
	};
	
	static class CateData {
		public static int ALL_CATE = Integer.MAX_VALUE;
		public int id = 0;
		public String name = "";	
		public Fragment fragment = null;
		
		public int hashCode(){
			return id;
		}
	}
	
	class TopicItemAdapter extends PagerAdapter{

		@Override
		public void destroyItem(View v, int position, Object arg2) {
			ViewPager vv = (ViewPager)v;
			if(arg2 instanceof CateData){
				CateData data = (CateData)arg2;
				View sv = vv.findViewWithTag(data.id);
				Log.d("xxx", "destroyItem..position:" + position + ", v:" + sv);
				if(sv != null){
					vv.removeView(sv);
				}
				if(data.fragment != null){
					FragmentTransaction ft = getFragmentManager().beginTransaction();
					ft.remove(data.fragment);
					ft.commit();
					data.fragment = null;
				}
			}else {
				Log.w("xxx", "not found view by object:" + arg2);
			}
		}

		@Override
		public void finishUpdate(View arg0) {
			//Log.d("xxx", "finishUpdate..v:" + arg0);
			
		}

		@Override
		public int getCount() {
			return views.size();
		}
		
		public int getItemPosition (Object object){
			if(views.contains(object)){
				return views.indexOf(object);
			}else {
				return POSITION_NONE;
			}
		}

		@Override
		public Object instantiateItem(View v, int position) {
			CateData data = views.get(position);
			
			String tag = "shop_" + data.id;
			if(data.id > 0){
				
				Builder uriBuilder = Schema.SHOP_LIST.buildUpon();
				if(data.name != null && data.id != CateData.ALL_CATE){
					uriBuilder = Uri.parse("content://" + Schema.AUTHORITY + "/shops/cate/" + data.id + "/list").buildUpon();
					uriBuilder.appendQueryParameter("cate", data.name);
				}
				
				Bundle arg = new Bundle();
				arg.putString("uri", uriBuilder.build().toString());
				
				Log.d("xxx", "instantiateItem.." + v.toString() + ", position:" + position);
				
				ShopListFragment fragment = new ShopListFragment(); //.instantiate(getActivity(), tag, null); 
				fragment.cateId = data.id;
				
				fragment.setArguments(arg);
				FragmentTransaction ft = getFragmentManager().beginTransaction();
				ft.add(v.getId(), fragment, tag);
				
				//ft.setTransition(FragmentTransaction.TRANSIT_NONE);
				ft.commit();
			}else {
				View v2 = getLayoutInflater(null).inflate(R.layout.topic_item_loading, null);
				v2.setTag(data.id);
				((ViewPager)v).addView(v2);
			}			
			
			return data;
		}

		@Override
		public boolean isViewFromObject(View v, Object arg1) {
			//Log.d("xxx", "isViewFromObject.." + v.toString() + ", tag:" + v.getTag());
			if(arg1 instanceof CateData){
				CateData data = (CateData)arg1;
				View v2 = v.findViewWithTag(data.id);
				//Log.d("xxx", "tags object:" + v2 + ", id:" + data.id);
				
				return v2 != null;
			}
			
			return false;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
		}

		@Override
		public Parcelable saveState() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void startUpdate(View arg0) {
			//Log.d("xxx", "startUpdate..v:" + arg0);
			// TODO Auto-generated method stub
			//this.
		}		
	}	
}
