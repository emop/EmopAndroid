package com.emop.client.provider.model;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.emop.client.Constants;
import com.emop.client.provider.QueryParam;

public class Rebate {
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.fmei.rebates";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.fmei.rebate";
	
	public static final String DB_TABLE_NAME = "rebate";
	
	public static final String LOCAL_CATE = "local_cate";
	public static final String SHORT_KEY = "short_url_key";
	public static final String NUM_IID = "num_iid";
	public static final String SHOP_ID = "shop_id";
	public static final String TITLE = "title";
	public static final String PIC_URL = "pic_url";

	public static final String PRICE = "price";
	public static final String COUPON_PRICE = "coupon_price";
	public static final String COUPON_RATE = "coupon_rate";
	public static final String COUPON_START_TIME = "coupon_start_time";
	
	
	public static final String COUPON_END_TIME = "coupon_end_time";
	public static final String ROOT_CATE = "root_tag";

	public static final String LOCAL_UPDATE_TIME = "local_update";
	public static final String WEIGHT = "weight";
	
	public static Uri update(SQLiteDatabase db, Uri uri, ContentValues values){
		
		String taskId = values.getAsString(NUM_IID);
		//values.remove("id");
		List<String> seg = uri.getPathSegments(); //.get(1);
		values.put(LOCAL_UPDATE_TIME, System.currentTimeMillis());
        int count = db.update(DB_TABLE_NAME, values, BaseColumns._ID + "=?", new String[]{taskId});
		if(count == 0){
			//Log.d(Constants.TAG_EMOP, String.format("insert new item '%s:%s'", taskId,
			//		values.getAsString(WEIBO_ID)));
			values.put(BaseColumns._ID, taskId);
			db.insert(DB_TABLE_NAME, NUM_IID, values);
		}else {
			//Log.d(Constants.TAG_EMOP, String.format("update rebate info'%s:%s'", taskId,
			//		values.getAsString(SHORT_KEY) + ", cate:" + values.getAsString(ROOT_CATE)));
		}		
		
		return null;			
	}
	
	
	public static void buildRebateListQuery(SQLiteQueryBuilder builder, 
			String[] fileds, QueryParam param, Uri uri){
		builder.setTables(DB_TABLE_NAME);	
		String cate = uri.getQueryParameter("cate");
		
		if(cate != null){
			builder.appendWhere(ROOT_CATE + "='" + cate + "'");
		}
		
		param.sortOrder = COUPON_START_TIME + " " + "desc, local_update desc";
	}
	
	public static ContentValues convertJson(JSONObject obj){
		ContentValues v = new ContentValues();
		try{
			if(obj.has("id")){
				v.put("id", obj.getInt("id"));
			}
			if(obj.has(SHORT_KEY)){
				v.put(SHORT_KEY, obj.getString(SHORT_KEY));
			}
			
			if(obj.has(NUM_IID)){
				v.put(NUM_IID, obj.getString(NUM_IID));
			}
			
			if(obj.has(SHOP_ID)){
				v.put(SHOP_ID, obj.getString(SHOP_ID));
			}
	
			if(obj.has(TITLE)){
				v.put(TITLE, obj.getString(TITLE));
			}	
			
			if(obj.has(PIC_URL)){
				v.put(PIC_URL, obj.getString(PIC_URL));
			}	

			if(obj.has(PRICE)){
				v.put(PRICE, obj.getString(PRICE));
			}			
			
			if(obj.has(COUPON_PRICE)){
				v.put(COUPON_PRICE, obj.getString(COUPON_PRICE));
			}		
			if(obj.has(COUPON_RATE)){
				v.put(COUPON_RATE, obj.getString(COUPON_RATE));
			}
			if(obj.has(COUPON_END_TIME) && obj.getString(COUPON_END_TIME) != null){
				v.put(COUPON_END_TIME, obj.getString(COUPON_END_TIME));
			}
			if(obj.has(COUPON_START_TIME) && obj.getString(COUPON_START_TIME) != null){
				v.put(COUPON_START_TIME, obj.getString(COUPON_START_TIME));
			}
			
			if(obj.has(ROOT_CATE)){
				v.put(ROOT_CATE, obj.getString(ROOT_CATE));
			}
			
			if(obj.has(LOCAL_UPDATE_TIME)){
				v.put(LOCAL_UPDATE_TIME, obj.getString(LOCAL_UPDATE_TIME));
			}	
			if(obj.has(WEIGHT)){
				v.put(WEIGHT, obj.getString(WEIGHT));
			}			
		}catch (JSONException e) {
			Log.e(Constants.TAG_EMOP, e.toString());
		}
		
		return v;	
	}

}
