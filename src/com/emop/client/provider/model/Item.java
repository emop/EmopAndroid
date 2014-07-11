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

/*
 * 围脖数据
 */
public class Item {
	public static final String DB_TABLE_NAME = "item";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.fmei.items";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.fmei.item";

	public static final String LOCAL_CATE = "local_cate";
	public static final String SHORT_KEY = "short_key";
	public static final String WEIBO_ID = "weibo_id";
	public static final String MESSAGE = "message";
	public static final String PIC_URL = "pic_url";
	public static final String ROOT_CATE = "root_cate";
	public static final String ITEM_CONTENT_TYPE = "content_type";
	public static final String NUM_IID = "num_iid";
	public static final String SHOP_ID = "shop_id";
	public static final String PRICE = "price";
	public static final String TITLE = "title";
	public static final String STATUS = "status";
	public static final String UPDATE_TIME = "update_time";
	public static final String LOCAL_UPDATE_TIME = "local_update";
	public static final String WEIGHT = "weight";
	
	
	//长宽比例.
	public static final String RECT_RATE = "rect_rate";
	
	
	public static void buildQuery(SQLiteQueryBuilder builder, Uri uri,
			QueryParam param){
		builder.setTables(DB_TABLE_NAME);
		List<String> seg = uri.getPathSegments();
		String cate = seg.get(0) + "_" + seg.get(1);
		builder.appendWhere(LOCAL_CATE + "='" + cate + "'");
		builder.appendWhere(" and " + STATUS + "='0'");
		param.sortOrder = UPDATE_TIME + " " + "desc";
	}

	public static void buildShopQuery(SQLiteQueryBuilder builder, Uri uri,
			QueryParam param){
		builder.setTables(DB_TABLE_NAME);
		List<String> seg = uri.getPathSegments();
		builder.appendWhere(SHOP_ID + "='" + seg.get(1) + "'");
		builder.appendWhere(" and " + STATUS + "='0'");
		param.sortOrder = UPDATE_TIME + " " + "desc";
	}
	
	public static void buildQueryId(SQLiteQueryBuilder builder, Uri uri,
			QueryParam param){
		builder.setTables(DB_TABLE_NAME);
		List<String> seg = uri.getPathSegments();
		builder.appendWhere(BaseColumns._ID + "='" + seg.get(1) + "'");
	}	
		
	public static Uri insert(SQLiteDatabase db, ContentValues values){
		return null;
	};	
	public static Uri update(SQLiteDatabase db, Uri uri, ContentValues values){
		String taskId = values.getAsString("id");
		if(taskId == null){
			updateItem(db, uri, values);
		}else {
			values.remove("id");
			List<String> seg = uri.getPathSegments(); //.get(1);
			values.put(LOCAL_CATE, seg.get(0) + "_" + seg.get(1));
			values.put(LOCAL_UPDATE_TIME, System.currentTimeMillis());
	        int count = db.update(DB_TABLE_NAME, values, BaseColumns._ID + "=?", new String[]{taskId});
			if(count == 0){
				//Log.d(Constants.TAG_EMOP, String.format("insert new item '%s:%s, shop:%s'", taskId,
				//		values.getAsString(WEIBO_ID), values.getAsString(SHOP_ID)));
				values.put(BaseColumns._ID, taskId);
				db.insert(DB_TABLE_NAME, WEIBO_ID, values);
			}else {
				//Log.d(Constants.TAG_EMOP, String.format("update item '%s:%s, shop:%s'", taskId,
				//		values.getAsString(WEIBO_ID), values.getAsString(SHOP_ID)));
			}		
		}
		return null;		
	};		
	
	public static Uri updateItem(SQLiteDatabase db, Uri uri, ContentValues values){
		String taskId = values.getAsString("num_iid");
		if(taskId != null){
			values.remove("num_iid");
			Log.d(Constants.TAG_EMOP, "Update item by num id:" + taskId);
			List<String> seg = uri.getPathSegments(); //.get(1);
	        db.update(DB_TABLE_NAME, values, NUM_IID + "=?", new String[]{taskId});
		}
		return null;		
	};		
	
	
	
	public static ContentValues convertJson(JSONObject obj){
		ContentValues v = new ContentValues();
		try{
			if(obj.has("id")){
				v.put("id", obj.getInt("id"));
			}
			if(obj.has("short_url_key")){
				v.put(SHORT_KEY, obj.getString("short_url_key"));
			}
			if(obj.has("item_id")){
				v.put(WEIBO_ID, obj.getString("item_id"));
			}
			if(obj.has("text")){
				v.put(MESSAGE, obj.getString("text"));
			}	
			if(obj.has(PIC_URL)){
				v.put(PIC_URL, obj.getString(PIC_URL));
			}
			if(obj.has(ITEM_CONTENT_TYPE)){
				v.put(ITEM_CONTENT_TYPE, obj.getString(ITEM_CONTENT_TYPE));
			}		
			if(obj.has(UPDATE_TIME)){
				v.put(UPDATE_TIME, obj.getString(UPDATE_TIME));
			}	
			if(obj.has(PRICE)){
				v.put(PRICE, obj.getString(PRICE));
			}			
			if(obj.has(NUM_IID)){
				v.put(NUM_IID, obj.getString(NUM_IID));
			}			
			if(obj.has(SHOP_ID)){
				v.put(SHOP_ID, obj.getString(SHOP_ID));
			}		
			if(obj.has(STATUS)){
				v.put(STATUS, obj.getString(STATUS));
			}
			if(obj.has(RECT_RATE)){
				v.put(RECT_RATE, obj.getString(RECT_RATE));
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
