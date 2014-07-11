package com.emop.client.fragment;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.SyncStateContract.Columns;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.emop.client.R;
import com.emop.client.TaokeItemDetailActivity;
import com.emop.client.io.FmeiClient;
import com.emop.client.provider.QueryParam;
import com.emop.client.provider.Schema;
import com.emop.client.provider.model.Item;
import com.emop.client.wxapi.DensityUtil;

public class TopicItemListFragment extends Fragment {
	private ViewPager viewPager = null;
	private ArrayList<TopicPage> views = new ArrayList<TopicPage>();
	private ArrayList<ImageView> pageIndicators = new ArrayList<ImageView>();
	private LayoutInflater inflater = null;
	private PagerAdapter adapter = null;
	private int[] resourceIDs = new int[10];
	//private int[] resourceClickIDs = new int[10];
	private FmeiClient client = null; //FmeiClient.getInstance(null);
	//private int indicatorLength = 0;
	//private TextView pageNum = null;
	private LinearLayout indicator = null;
	//private View bar = null;
	private Handler handler = new Handler();
	
	private boolean isRunning = true;

	/**
	 * 定义一个模板列表，数据内容根据模板展示。
	 */
	private ArrayList<TopicPage> pageTemplates = new ArrayList<TopicPage>();
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState){
		Log.d("xx", "onCreateView...");
		client = FmeiClient.getInstance(null);
		
		View v = inflater.inflate(R.layout.topic_item_list_fragment, container, false);
		viewPager = (ViewPager)v.findViewById(R.id.viewPager);
		this.inflater = inflater;
		
		TopicPage page = new TopicPage();
		views.add(page);
		
		indicator = (LinearLayout)v.findViewById(R.id.page_indicator);

		adapter = new TopicItemAdapter();
		viewPager.setOnPageChangeListener(pageListener2);
		viewPager.setAdapter(adapter);
		
		return v;
	}
	
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		
        View title = getActivity().findViewById(R.id.title);
        if(title != null){
        	title.setLongClickable(true);        
        	title.setOnLongClickListener(new RefreshLoader());		
        }else {
        	Log.d("emop", "not founc guang title.");
        }
		
		new DataLoader().start();
		
	}
	
	public void onResume(){
		super.onResume();
		isRunning = true;		
	}
	public void onPause(){
		super.onPause();
		isRunning = false;		
	}
	
	protected void initTemplates(){
		TopicPage page = new TopicPage();
		page.layout = R.layout.topic_template1;
		page.itemCount = 4;
		pageTemplates.add(page);
		
		page = new TopicPage();
		page.layout = R.layout.topic_template2;
		page.itemCount = 4;
		pageTemplates.add(page);		

		page = new TopicPage();
		page.layout = R.layout.topic_template3;
		page.itemCount = 4;		
		pageTemplates.add(page);
		
		page = new TopicPage();
		page.layout = R.layout.topic_template4;
		page.itemCount = 4;		
		pageTemplates.add(page);
		
		//resourceIDs
		Resources res = getActivity().getResources();
		
		for(int i = 0; i < 10; i++){
			resourceIDs[i] = res.getIdentifier("item_" + i, "id", "com.emop.client");
			if(resourceIDs[i] == 0)break;
		}
	}
	
	class RefreshLoader implements OnLongClickListener{
		private ProgressDialog loading = null;
		public boolean onLongClick(View arg0) {
			if(loading != null && loading.isShowing()) {
				Log.d("xxx", "topic item list in loading...");
				return false;
			}else {
				Log.d("xxx", "start topic item list in loading...");
			}
	    	if(loading == null){
	            loading = new ProgressDialog(getActivity());
	            loading.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    	}
	    	loading.setMessage("正在更新专题详情...");    		
	    	loading.show();	
	    	
			new Thread(){
				public void run(){
			    	Uri dataUri = getActivity().getIntent().getData();
			    	dataUri = dataUri.buildUpon().appendQueryParameter("force_refresh", "y")
			    		.appendQueryParameter(QueryParam.PAGE_SIZE, "100")
			    		.build();
					ContentResolver resolver = getActivity().getContentResolver();
					
					Cursor c = resolver.query(dataUri, 					
							new String[] {Columns._ID, Item.WEIBO_ID, Item.PIC_URL}, 
							null, null, null);
					c.close();
					new DataLoader().run();
					loading.dismiss();
				}
			}.start();
			return false;
		}	
	}
	
	
	class TopicItemAdapter extends PagerAdapter{

		@Override
		public void destroyItem(View v, int position, Object arg2) {
			// TODO Auto-generated method stub
			Log.d("xxx", "destroyItem..position:" + position);
			ViewPager vv = (ViewPager)v;
			View sv = vv.findViewById(arg2.hashCode());
			if(sv != null){
				vv.removeView(sv);
			}
			//((ViewPager)v).findViewById(arg2.hashCode());
			//((ViewPager)v).removeView((View)arg2);
		}

		@Override
		public void finishUpdate(View arg0) {
			//Log.d("xxx", "finishUpdate..v:" + arg0);
			
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
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
			//Log.d("xxx", "instantiateItem.." + v.toString() + ", position:" + position);
			
			TopicPage page = views.get(position);
			View newView = null;
			if(page != null){
				newView = inflater.inflate(page.layout, null);
				newView.setId(page.hashCode());
				((ViewPager)v).addView(newView);
			}
			return page;
		}

		@Override
		public boolean isViewFromObject(View v, Object arg1) {
			//Log.d("xxx", "isViewFromObject..v:" + v.toString() + ", arg1:" + arg1);
			return v.getId() == arg1.hashCode();
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
	
	protected OnClickListener clickListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
	    	Uri dataUri = Uri.parse("content://" + Schema.AUTHORITY + "/item/" + v.getId());
			Intent intent = new Intent().setClass(getActivity(), 
					TaokeItemDetailActivity.class);
			intent.setData(dataUri);
			startActivity(intent); 				
		}
		
	};
	
	protected OnPageChangeListener pageListener2 = new OnPageChangeListener(){

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
			//pageIndicators.add(img);
			for(int i = 0; i < pageIndicators.size(); i++){
				if(i == position){
					pageIndicators.get(i).setImageResource(R.drawable.themepage_select);
				}else {
					pageIndicators.get(i).setImageResource(R.drawable.themepage);
				}				
			}
			TopicPage data = views.get(position);
			View v = viewPager.findViewById(data.hashCode());
			if(data != null && v != null && v.getTag() == null){
				v.setTag(data);
				renderPage(v, data);
			}
		}
		
		protected void renderPage(View v, TopicPage  data){
			Resources res = getActivity().getResources();
			//getActivity().getWindow()
			int itemId = 0;
			//.items.size()
			for(int i = 0; i < data.itemCount; i++){
				View sub = v.findViewById(resourceIDs[i]);
				View click_sub = v.findViewById(res.getIdentifier("item_click_" + i, "id", "com.emop.client"));
				if(sub == null) break;
				if(i < data.items.size()){
					String url;
					try {
						url = data.items.get(i).getString(Item.PIC_URL);
						itemId = data.items.get(i).getInt("id");
						if(url != null && sub instanceof ImageView){
							//sub.setBackgroundResource(R.drawable.loading);
							int width = sub.getMeasuredWidth();
							client.tmpImgLoader.loadImage(url, (ImageView)sub, width == 0 ? 300: width, true);
							if(click_sub!=null){
								click_sub.setId(itemId);
								click_sub.setOnClickListener(clickListener);
							}
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else {
					sub.setVisibility(View.INVISIBLE);
					click_sub.setVisibility(View.INVISIBLE);
					try{
						LinearLayout out = (LinearLayout)click_sub.getParent().getParent();
						out.setVisibility(View.INVISIBLE);
					}catch(Throwable e){
						Log.d("emop", "hidden item error:" + e.toString(), e);
					}
				}
			}
		}		
		
	};	
	
	/**
	 * 一个专题页的数据结构。
	 * 1. 页面布局类型。
	 * 2. 布局需要用到的商品列表。	 *
	 */
	class TopicPage {
		public int layout = R.layout.topic_item_loading;
		public boolean loaded = false;
		public int itemCount = 0;
		public List<JSONObject> items = null;
	}
	
	class DataLoader extends Thread{
		public void run(){
			initTemplates();
			
			Intent intent = getActivity().getIntent();
			Uri dataUri = intent.getData();
			dataUri = dataUri.buildUpon().appendQueryParameter(QueryParam.PAGE_SIZE, "100").build();
			ContentResolver resolver = getActivity().getContentResolver();
			
			Cursor c = resolver.query(dataUri, 					
					new String[] {Columns._ID, Item.WEIBO_ID, Item.PIC_URL}, 
					null, null, null);
			
			final ArrayList<JSONObject> result = new ArrayList<JSONObject>(64);
			
			int idIndex = -1, picIndex = -1;
			try{
				boolean hasMore = c.moveToFirst();
				if(hasMore){
					idIndex = c.getColumnIndex(Columns._ID);
					if(idIndex == -1){
						idIndex = c.getColumnIndex("id");
					}
					picIndex = c.getColumnIndex(Item.PIC_URL);
				}
				
				for(; hasMore; hasMore = c.moveToNext()){
					JSONObject obj = new JSONObject();
					int id = c.getInt(idIndex);
					try {
						obj.put("id", id);
						String pic = c.getString(picIndex);
						obj.put(Item.PIC_URL, pic);							
					} catch (JSONException e) {
						Log.w("emop", "load topic error:" + e.toString(), e);
					}
					result.add(obj);
				}
			}finally{
				if(c != null){
					c.close();
				}
			}
			

			if(!isRunning)return;
			
			if(result.size() > 0){
				handler.post(new Runnable(){
					public void run(){
						layoutPage(result);
					}
				});
			}else {
				handler.post(new Runnable(){
					public void run(){
						View v = viewPager.getChildAt(0);
						if(v != null){
							View error = v.findViewById(R.id.error_view);
							error.setVisibility(View.VISIBLE);
							error = v.findViewById(R.id.progressbar_loading);
							error.setVisibility(View.GONE);
						}
						Toast.makeText(getActivity(),"亲，网络出错啦!", Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
		
		protected void layoutPage(ArrayList<JSONObject> itemList){
			int templateCount = pageTemplates.size();
			int templateIndex = 0;
			int startIndex = 0;
			int endIndex = 0;
			views.clear();
			while(startIndex < itemList.size()){
				TopicPage temp = pageTemplates.get(templateIndex++ % templateCount);
				//if(temp.s)
				endIndex = Math.min(startIndex + temp.itemCount, itemList.size());
				List<JSONObject> sub = itemList.subList(startIndex, endIndex);
				startIndex = endIndex;
				TopicPage page  = new TopicPage();
				page.layout = temp.layout;
				page.itemCount = temp.itemCount;
				page.items = sub;
				views.add(page);
			}
			
			indicator.removeAllViews();
			pageIndicators.clear();
			int margin = 10;
			//@todo 不知道为啥，有时间竟然抛出空指针异常。理论上说这个地方context总是有的。前面已经使用过了。			
			try{
				margin = DensityUtil.dip2px(getActivity(), 5);
			}catch(Throwable e){}
			LinearLayout.LayoutParams lp = null;
			for(int i = 0; i < views.size() ; i++){
				ImageView img = new ImageView(getActivity());
				if(views.size() < 10){
					lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
							LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.leftMargin = margin;
					lp.rightMargin = margin;
				}else {
					lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
							LinearLayout.LayoutParams.WRAP_CONTENT);
					lp.weight = 1;
				}
				img.setLayoutParams(lp);
				if(i == 0){
					img.setImageResource(R.drawable.themepage_select);
				}else {
					img.setImageResource(R.drawable.themepage);
				}
				indicator.addView(img);
				pageIndicators.add(img);
			}
			
			handler.post(new Runnable(){
				public void run(){
					Log.d("xx", "cleanup all sub view, add page count:" + views.size());
					adapter.notifyDataSetChanged();
					viewPager.setCurrentItem(0);
				}
			});

			handler.postDelayed(new Runnable(){
				public void run(){
					pageListener2.onPageSelected(0);
				}
			}, 100);
		}
	}
}
