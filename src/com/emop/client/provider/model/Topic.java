package com.emop.client.provider.model;

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
 * 专题
 */
public class Topic {
	public static final String DB_TABLE_NAME = "topic";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.fmei.topics";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.fmei.topic";
	
    public static final String GROUP_NAME = "group_name";
    public static final String TAGS = "tags";    
	public static final String TITLE = "topic_name";
	public static final String DESC = "description";
	public static final String ITEM_COUNT = "item_count";
	public static final String FRONT_PIC = "front_pic";	
	public static final String START_TIME = "start_time";
	public static final String END_TIME = "end_time";
	public static final String UPDATE_TIME = "update_time";
	public static final String VIEW_ORDER = "view_order";
	public static final String STATUS = "status";
	public static final String LOCAL_UPDATE_TIME = "local_update";
	
	public static void buildTopicListQuery(SQLiteQueryBuilder builder, 
			String[] fileds, QueryParam param){
		builder.setTables(DB_TABLE_NAME);		
		builder.appendWhere(Topic.GROUP_NAME + "='topic'");
		builder.appendWhere(" and " + Topic.STATUS + "='1001'");
		param.sortOrder = "view_order desc, update_time desc";
	}
	
	public static Uri insert(SQLiteDatabase db, ContentValues values){
		return null;
	};
	
	public static Uri update(SQLiteDatabase db, ContentValues values){
		return update(db, values, "topic");		
	}
	
	public static Uri update(SQLiteDatabase db, ContentValues values, String groupName){
		String taskId = values.getAsString("id");
		values.remove("id");
		values.put(Topic.GROUP_NAME, groupName);
		values.put(LOCAL_UPDATE_TIME, System.currentTimeMillis());
        int count = db.update(DB_TABLE_NAME, values, BaseColumns._ID + "=?", new String[]{taskId});
		if(count == 0){
			//Log.d(Constants.TAG_EMOP, String.format("insert new topic '%s:%s'", taskId,
			//		values.getAsString(TITLE)));
			values.put(BaseColumns._ID, taskId);
			db.insert(DB_TABLE_NAME, TITLE, values);
		}else {
			//Log.d(Constants.TAG_EMOP, String.format("update topic '%s:%s, order:%s' + tags:", taskId,
			//		values.getAsString(TITLE), values.getAsString(VIEW_ORDER), values.getAsString(TAGS)));
		}		
		return null;
	};	
	
	public static ContentValues convertJson(JSONObject obj){
		ContentValues v = new ContentValues();
		try{
			if(obj.has("id")){
				v.put("id", obj.getInt("id"));
			}
			if(obj.has(TITLE)){
				v.put(TITLE, obj.getString(TITLE));
			}
			if(obj.has(DESC)){
				v.put(DESC, obj.getString(DESC));
			}
			if(obj.has("create_time")){
				v.put(START_TIME, obj.getString("create_time"));
			}	
			if(obj.has(UPDATE_TIME)){
				v.put(UPDATE_TIME, obj.getString(UPDATE_TIME));
			}
			if(obj.has(ITEM_COUNT)){
				v.put(ITEM_COUNT, obj.getString(ITEM_COUNT));
			}	
			if(obj.has(FRONT_PIC)){
				v.put(FRONT_PIC, obj.getString(FRONT_PIC));
			}			
			if(obj.has(VIEW_ORDER)){
				v.put(VIEW_ORDER, obj.getString(VIEW_ORDER));
			}	
			if(obj.has(STATUS)){
				v.put(STATUS, obj.getString(STATUS));
			}
			if(obj.has(TAGS)){
				v.put(TAGS, obj.getString(TAGS));
			}			
		}catch (JSONException e) {
			Log.e(Constants.TAG_EMOP, e.toString());
		}
		
		return v;
	}
}
