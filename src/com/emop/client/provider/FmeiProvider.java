package com.emop.client.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.emop.client.provider.model.Cate;
import com.emop.client.provider.model.Item;
import com.emop.client.provider.model.Rebate;
import com.emop.client.provider.model.Shop;
import com.emop.client.provider.model.Topic;

public class FmeiProvider extends ContentProvider{
    private static final String TAG = "areaci_provider";

    private static final String DATABASE_NAME = "fmei_android.db";
    private static final int DATABASE_VERSION = 24;
    
    //private static final UriMatcher sUriMatcher;
    private DatabaseHelper mOpenHelper;	   
    private DataUpdateService updateService = null;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);            
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	Log.i(TAG, "Create new table...");
            db.execSQL("CREATE TABLE " + Topic.DB_TABLE_NAME + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY,"
                    + Topic.GROUP_NAME + " TEXT," 
                    + Topic.TAGS + " TEXT,"
                    + Topic.TITLE + " TEXT,"
                    + Topic.DESC + " TEXT,"
                    + Topic.ITEM_COUNT + " INTEGER,"
                    + Topic.FRONT_PIC + " TEXT,"
                    + Topic.START_TIME + " TEXT,"
                    + Topic.END_TIME + " TEXT,"
                    + Topic.UPDATE_TIME + " TEXT," 
                    + Topic.VIEW_ORDER + " INTEGER,"
                    + Topic.STATUS + " INTEGER,"
                    + Topic.LOCAL_UPDATE_TIME + " INTEGER"
                    + ");");   

            db.execSQL("CREATE TABLE " + Item.DB_TABLE_NAME + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY,"            		
                    + Item.LOCAL_CATE + " TEXT,"
                    + Item.SHORT_KEY + " TEXT,"
                    + Item.WEIBO_ID + " INTEGER,"  
                    + Item.STATUS + " INTEGER,"
                    + Item.MESSAGE + " TEXT,"
                    + Item.PIC_URL + " TEXT,"
                    + Item.ROOT_CATE + " TEXT,"
                    + Item.ITEM_CONTENT_TYPE + " TEXT,"                    
                    + Item.SHOP_ID + " TEXT,"
                    + Item.NUM_IID + " TEXT,"
                    + Item.PRICE + " INTEGER,"
                    + Item.TITLE + " TEXT,"
                    + Item.UPDATE_TIME + " INTEGER,"
                    + Item.LOCAL_UPDATE_TIME + " INTEGER,"
                    + Item.RECT_RATE + " INTEGER,"
                    + Item.WEIGHT + " INTEGER"                    
                    + ");");
            
            db.execSQL("CREATE TABLE " + Shop.DB_TABLE_NAME + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY,"            		
                    + Shop.LOCAL_CATE + " TEXT,"
                    + Shop.SHORT_KEY + " TEXT,"
                    + Shop.SHOP_ID + " INTEGER,"  
                    + Shop.ROOT_CATE + " TEXT,"
                    + Shop.USER_NICK + " TEXT,"
                    + Shop.SHOP_CREDIT + " INTEGER,"
                    + Shop.SHOP_TYPE + " TEXT,"
                    + Shop.GOOD_COMMENT + " INTEGER,"
                    + Shop.SHOP_TITLE + " TEXT,"
                    + Shop.SHOP_DESC + " TEXT,"
                    + Shop.SHOP_LOGO + " TEXT,"
                    + Shop.STATUS + " INTEGER,"
                    + Shop.UPDATE_TIME + " INTEGER,"
                    + Shop.LOCAL_UPDATE_TIME + " INTEGER,"
                    + Shop.WEIGHT + " INTEGER"                    
                    + ");");
            
            db.execSQL("CREATE TABLE " + Rebate.DB_TABLE_NAME + " ("
                    + BaseColumns._ID + " INTEGER PRIMARY KEY,"            		
                    + Rebate.LOCAL_CATE + " TEXT,"
                    + Rebate.SHORT_KEY + " TEXT,"
                    + Rebate.SHOP_ID + " INTEGER,"  
                    + Rebate.NUM_IID + " INTEGER,"
                    + Rebate.TITLE + " TEXT,"
                    + Rebate.PIC_URL + " TEXT,"
                    + Rebate.PRICE + " INTEGER,"
                    + Rebate.COUPON_PRICE + " INTEGER,"
                    + Rebate.COUPON_RATE + " TEXT,"
                    + Rebate.COUPON_START_TIME + " TEXT,"
                    + Rebate.COUPON_END_TIME + " TEXT,"
                    + Rebate.ROOT_CATE + " TEXT,"                                        
                    + Rebate.LOCAL_UPDATE_TIME + " INTEGER,"
                    + Rebate.WEIGHT + " INTEGER"                    
                    + ");");            
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + Topic.DB_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Cate.DB_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Item.DB_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Shop.DB_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + Rebate.DB_TABLE_NAME);
            
            onCreate(db);
        }
    }    
    
	/*
	 * 删除URI 相关的内容。
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String table = "";
        switch (Schema.FmeiUriMatcher.match(uri)) {
	    	case Schema.TYPE_TOPICS:
	    	case Schema.TYPE_CATES:
	    	case Schema.TYPE_HOTS:
	    	case Schema.TYPE_ACTS:
	    		table = Topic.DB_TABLE_NAME;
	    		break;    	
	    	case Schema.TYPE_ACT_ITEM_LIST:
	    	case Schema.TYPE_TOPIC_ITEM_LIST:
	    	case Schema.TYPE_HOT_ITEM_LIST:
	    	case Schema.TYPE_CATE_ITEM_LIST:
	    	case Schema.TYPE_MYFAV_ITEM_LIST:
	    	case Schema.TYPE_ITEM_ID:
	    	case Schema.TYPE_ITEMS:	
	    		table = Item.DB_TABLE_NAME;
	    		break;
	    	case Schema.TYPE_REBATES:
	    		table = Rebate.DB_TABLE_NAME;
	    		break;
	    	case Schema.TYPE_SHOPS:
	    		table = Shop.DB_TABLE_NAME;
	    		break;
	    		
	    	default:
	    		Log.e(TAG, "unkown uri:" + uri.toString());            
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        return db.delete(table, selection, selectionArgs);
	}

	@Override
	public String getType(Uri uri) {
		String t = null;
        switch (Schema.FmeiUriMatcher.match(uri)) {
        	case Schema.TYPE_TOPICS:
        		t = Topic.CONTENT_TYPE; break;
        	case Schema.TYPE_TOPIC_ID:
        		t = Topic.CONTENT_ITEM_TYPE; break;
        	case Schema.TYPE_CATES:
        		t = Cate.CONTENT_TYPE; break;
        	case Schema.TYPE_CATE_ID:
        		t = Cate.CONTENT_ITEM_TYPE; break;
        	case Schema.TYPE_ITEMS:
        		t = Item.CONTENT_TYPE; break;
        	case Schema.TYPE_ITEM_ID:
        		t = Item.CONTENT_ITEM_TYPE; break;
        	case Schema.TYPE_SHOPS:
        		t = Shop.CONTENT_TYPE; break;
        	case Schema.TYPE_SHOPS_CATE:
        		t = Shop.CONTENT_TYPE; break;
        	case Schema.TYPE_REBATES:
        		t = Rebate.CONTENT_TYPE; break;

        }
		return t;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri newUri = null;
        switch (Schema.FmeiUriMatcher.match(uri)) {
        	case Schema.TYPE_TOPICS:
        		newUri = Topic.insert(db, values);
        		break;
        	case Schema.TYPE_ITEMS:
        		newUri = Item.insert(db, values);
        		break;
        	case Schema.TYPE_CATES:
        		newUri = Cate.insert(db, values);
        		break; 		
        }
        
        if(newUri == null){
        	throw new SQLException("Failed to insert row into " + uri);	
        }
        return newUri;        
	}

	@Override
	public boolean onCreate() {
		updateService = new DataUpdateService(getContext(), this);
		mOpenHelper = new DatabaseHelper(getContext());
		
		mOpenHelper.getWritableDatabase();

		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		QueryParam param = new QueryParam(selection, selectionArgs, sortOrder);
		
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		int type = Schema.FmeiUriMatcher.match(uri);
        switch (type) {
        	case Schema.TYPE_TOPICS:
        		Topic.buildTopicListQuery(qb, projection, param);
        		break;
        	case Schema.TYPE_CATES:
        		Cate.buildCateListQuery(qb, projection, param);
        		break;
        	case Schema.TYPE_REBATE_CATES:
        		Cate.buildRebateCateListQuery(qb, projection, param);
        		break;        		
        	case Schema.TYPE_HOTS:
        		Cate.buildHotCateListQuery(qb, projection, param);
        		break;
        	case Schema.TYPE_ACTS:
        		Cate.buildActListQuery(qb, projection, param);
        		break;
        	case Schema.TYPE_SHOP_ID:
        		Shop.buildQueryId(qb, uri, param);
        		break;
        	case Schema.TYPE_SHOPS_CATE:
        	case Schema.TYPE_SHOPS:
        		Shop.buildShopListQuery(qb, projection, param, uri);
        		break;
        	case Schema.TYPE_SHOP_TAOKE_LSIT:
        		Item.buildShopQuery(qb, uri, param);
        		break;
        	case Schema.TYPE_REBATES_CATE:
        	case Schema.TYPE_REBATES:
        		Rebate.buildRebateListQuery(qb, projection, param, uri);
        		break;
        		
        	case Schema.TYPE_ACT_ITEM_LIST:
        	case Schema.TYPE_TOPIC_ITEM_LIST:
        	case Schema.TYPE_HOT_ITEM_LIST:
        	case Schema.TYPE_CATE_ITEM_LIST:
        	case Schema.TYPE_MYFAV_ITEM_LIST:
        		Item.buildQuery(qb, uri, param);
        		break;
        	case Schema.TYPE_ITEM_ID:
        		Item.buildQueryId(qb, uri, param);
        		break;            
	    	default:
	    		Log.e(TAG, "unkown query uri:" + uri.toString() + ", type:" + type);            
        }
        
        String size = uri.getQueryParameter(QueryParam.PAGE_SIZE);
        String pageNo = uri.getQueryParameter(QueryParam.PAGE_NO);
        int iSize = 30, iPageNo = 0;
        try{
        	iSize = Integer.parseInt(size);
        	iPageNo = Integer.parseInt(pageNo);
        }catch(Throwable e){}
        finally{
        	iSize = iSize > 1 ? iSize : 30;
        	iPageNo = iPageNo >= 0 ? iPageNo: 0;
        }
        
        String limit = iPageNo * iSize + "," + iSize;
        //qb.query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder, limit)
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        String q = qb.buildQuery(projection, param.selection, param.selectionArgs, param.groupBy, param.having, param.sortOrder, limit);
        //+ limit;
        Log.d(TAG, "query sql:" + q);        
        Cursor c = null;
        
        try{
        	c = db.rawQuery(q, new String[]{});
            /**
             * 由更新服务检查是否需要更新数据。
             */
            if(updateService != null){
            	Cursor newc = updateService.syncCheck(uri, c);
            	if(newc != c){
            		c.close();
            		c = newc;
            	}
            } 
        }catch(Exception e){
        	Log.e(TAG, "Query error:" + e.toString(), e);
        }
        
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (Schema.FmeiUriMatcher.match(uri)) {
	    	case Schema.TYPE_TOPICS:
	    		Topic.update(db, values, "topic");
	    		break;
	    	case Schema.TYPE_CATES:
	    		Topic.update(db, values, "cate");
	    		break;
	    	case Schema.TYPE_HOTS:
	    		Topic.update(db, values, "hot_cate");
	    		break;	
	    	case Schema.TYPE_ACTS:
	    		Topic.update(db, values, "act");
	    		break;	
	    	case Schema.TYPE_REBATE_CATES:
	    		Topic.update(db, values, "rebate");
	    		break;		    		
	    	case Schema.TYPE_SHOPS:
	    		Shop.update(db, uri, values);
	    		break;
	    	case Schema.TYPE_REBATES:
	    		Rebate.update(db, uri, values);
	    		break;
	    	case Schema.TYPE_ACT_ITEM_LIST:
	    	case Schema.TYPE_HOT_ITEM_LIST:
	    	case Schema.TYPE_CATE_ITEM_LIST:
	    	case Schema.TYPE_TOPIC_ITEM_LIST:
	    	case Schema.TYPE_MYFAV_ITEM_LIST:
	    	case Schema.TYPE_SHOP_TAOKE_LSIT:
	    	case Schema.TYPE_ITEMS:
	    		Item.update(db, uri, values);
	    		break;
	    	default:
	    		Log.e(TAG, "update unkown uri:" + uri.toString());
        }
        //getContext().getContentResolver().notifyChange(uri, null);
        return 0;
	}
	
}
