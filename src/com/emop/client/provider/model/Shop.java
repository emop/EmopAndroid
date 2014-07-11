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

public class Shop {
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.fmei.shops";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.fmei.shop";
	
	public static final String DB_TABLE_NAME = "shop";
	
	public static final String LOCAL_CATE = "local_cate";
	public static final String SHORT_KEY = "short_url_key";
	public static final String SHOP_ID = "shop_id";

	public static final String ROOT_CATE = "root_tag";
	public static final String USER_NICK = "user_nick";
	public static final String SHOP_TYPE = "shop_type";	
	public static final String GOOD_COMMENT = "good_comment";

	public static final String SHOP_TITLE = "shop_title";
	public static final String SHOP_DESC = "shop_desc";
	public static final String SHOP_LOGO = "pic_path";

	public static final String SHOP_CREDIT = "seller_credit";
	
	public static final String STATUS = "status";
	public static final String UPDATE_TIME = "update_time";
	public static final String LOCAL_UPDATE_TIME = "local_update";
	public static final String WEIGHT = "weight";
	
	public static Uri update(SQLiteDatabase db, Uri uri, ContentValues values){
		
		String taskId = values.getAsString(SHOP_ID);
		//values.remove("id");
		List<String> seg = uri.getPathSegments(); //.get(1);
		values.put(LOCAL_UPDATE_TIME, System.currentTimeMillis());
        int count = db.update(DB_TABLE_NAME, values, BaseColumns._ID + "=?", new String[]{taskId});
		if(count == 0){
			//Log.d(Constants.TAG_EMOP, String.format("insert new item '%s:%s'", taskId,
			//		values.getAsString(WEIBO_ID)));
			values.put(BaseColumns._ID, taskId);
			db.insert(DB_TABLE_NAME, SHOP_ID, values);
		}else {
			//Log.d(Constants.TAG_EMOP, String.format("update shop info'%s:%s'", taskId,
			//		values.getAsString(SHORT_KEY) + ", cate:" + values.getAsString(ROOT_CATE)));
		}		
		
		return null;			
	}
	
	
	public static void buildShopListQuery(SQLiteQueryBuilder builder, 
			String[] fileds, QueryParam param, Uri uri){
		builder.setTables(Shop.DB_TABLE_NAME);	
		String cate = uri.getQueryParameter("cate");
		
		if(cate != null){
			builder.appendWhere(ROOT_CATE + "='" + cate + "'");
		}
	}
	
	public static void buildQueryId(SQLiteQueryBuilder builder, Uri uri,
			QueryParam param){
		builder.setTables(DB_TABLE_NAME);
		List<String> seg = uri.getPathSegments();
		builder.appendWhere("shop_id='" + seg.get(1) + "'");
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
			
			if(obj.has(SHOP_LOGO)){
				v.put(SHOP_LOGO, obj.getString(SHOP_LOGO));
			}
			
			if(obj.has(SHOP_TITLE)){
				v.put(SHOP_TITLE, obj.getString(SHOP_TITLE));
			}
	
			if(obj.has(UPDATE_TIME)){
				v.put(UPDATE_TIME, obj.getString(UPDATE_TIME));
			}	
			
			if(obj.has(USER_NICK)){
				v.put(USER_NICK, obj.getString(USER_NICK));
			}	

			if(obj.has(SHOP_TYPE)){
				v.put(SHOP_TYPE, obj.getString(SHOP_TYPE));
			}			
			
			if(obj.has(SHOP_ID)){
				v.put(SHOP_ID, obj.getString(SHOP_ID));
			}		
			if(obj.has(STATUS)){
				v.put(STATUS, obj.getString(STATUS));
			}
			if(obj.has(ROOT_CATE) && obj.getString(ROOT_CATE) != null){
				v.put(ROOT_CATE, obj.getString(ROOT_CATE));
			}	
			if(obj.has(WEIGHT)){
				v.put(WEIGHT, obj.getString(WEIGHT));
			}	
			if(obj.has(SHOP_DESC)){
				v.put(SHOP_DESC, obj.getString(SHOP_DESC));
			}	
			if(obj.has(SHOP_CREDIT)){
				v.put(SHOP_CREDIT, obj.getString(SHOP_CREDIT));
			}			
		}catch (JSONException e) {
			Log.e(Constants.TAG_EMOP, e.toString());
		}
		
		return v;	
	}	
}
