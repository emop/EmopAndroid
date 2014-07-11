package com.emop.client.provider.model;

import com.emop.client.provider.QueryParam;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class Cate {
	public static final String DB_TABLE_NAME = "cate";
    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.fmei.cates";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.fmei.cate";

    
	public static void buildCateListQuery(SQLiteQueryBuilder builder, 
			String[] fileds, QueryParam param){
		builder.setTables(Topic.DB_TABLE_NAME);	
		builder.appendWhere(Topic.GROUP_NAME + "='cate'");
		builder.appendWhere(" and " + Topic.STATUS + "='1001'");
		param.sortOrder = "view_order asc, update_time desc";
		
	}
	
	public static void buildHotCateListQuery(SQLiteQueryBuilder builder, 
			String[] fileds, QueryParam param){
		builder.setTables(Topic.DB_TABLE_NAME);	
		builder.appendWhere(Topic.GROUP_NAME + "='hot_cate'");
		builder.appendWhere(" and " + Topic.STATUS + "='1001'");
		param.sortOrder = "view_order asc, update_time desc";
	}
	
	public static void buildActListQuery(SQLiteQueryBuilder builder, 
			String[] fileds, QueryParam param){
		builder.setTables(Topic.DB_TABLE_NAME);	
		builder.appendWhere(Topic.GROUP_NAME + "='act'");
		builder.appendWhere(" and " + Topic.STATUS + "='1001'");
		param.sortOrder = "view_order asc, update_time desc";
	}
	
	public static void buildRebateCateListQuery(SQLiteQueryBuilder builder, 
			String[] fileds, QueryParam param){
		builder.setTables(Topic.DB_TABLE_NAME);	
		builder.appendWhere(Topic.GROUP_NAME + "='rebate'");
		builder.appendWhere(" and " + Topic.STATUS + "='1001'");
		param.sortOrder = "view_order asc, update_time desc";
	}	
	
	public static Uri insert(SQLiteDatabase db, ContentValues values){
		return null;
	};	
	public static Uri update(SQLiteDatabase db, ContentValues values){
		return null;
	};		
}
