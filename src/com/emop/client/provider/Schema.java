package com.emop.client.provider;

import android.content.UriMatcher;
import android.net.Uri;

public class Schema {
	public static final UriMatcher FmeiUriMatcher;

	public static final String AUTHORITY = "com.emop.client.provider.Fmei";
	
	/*
	 * 专题列表。
	 */
    public static final Uri TOPIC_LIST = Uri.parse("content://" + AUTHORITY + "/topics/");
	
    /*
	 * 我的收藏。
	 */
    public static final Uri MYFAV_LIST = Uri.parse("content://" + AUTHORITY + "/myfav/");

    /*
     * 分类列表
     */
    public static final Uri CATE_LIST = Uri.parse("content://" + AUTHORITY + "/cates/");

    public static final Uri REBATE_CATE_LIST = Uri.parse("content://" + AUTHORITY + "/rebate_cates/");
    
    

    /*
     * 店铺列表
     */
    public static final Uri SHOP_LIST = Uri.parse("content://" + AUTHORITY + "/shops/");
    
    /*
     * 热门分类列表
     */
    public static final Uri HOT_CATE_LIST = Uri.parse("content://" + AUTHORITY + "/hots/");

    /*
     * 外链活动列表
     */
    public static final Uri ACTIVITY_LIST = Uri.parse("content://" + AUTHORITY + "/acts/");
    
    
    /*
     * 内容列表
     */
    public static final Uri ITME_LIST = Uri.parse("content://" + AUTHORITY + "/items/");

    /*
     * 折扣列表
     */
    public static final Uri REBATE_LIST = Uri.parse("content://" + AUTHORITY + "/rebates/");

    
    public static final int TYPE_TOPICS = 1001;
    public static final int TYPE_TOPIC_ID = 1002;
    public static final int TYPE_TOPIC_ITEM_LIST = 1003;
    
    public static final int TYPE_CATES = 2001;
    public static final int TYPE_CATE_ID = 2002;
    public static final int TYPE_CATE_ITEM_LIST = 2003;

    public static final int TYPE_HOTS = 3001;
    //public static final int TYPE_CATES_ID = 4;
    public static final int TYPE_HOT_ITEM_LIST = 3003;

    public static final int TYPE_ACTS = 4001;    
    public static final int TYPE_ACT_ITEM_LIST = 4003;

    public static final int TYPE_MYFAVS = 5001;    
    public static final int TYPE_MYFAV_ITEM_LIST = 5003;
    
    
    public static final int TYPE_ITEMS = 5001;
    public static final int TYPE_ITEM_ID = 5002;
    
    public static final int TYPE_SHOPS = 6001;
    public static final int TYPE_SHOPS_CATE = 6002;
    public static final int TYPE_SHOP_ID = 6003;
    public static final int TYPE_SHOP_TAOKE_LSIT = 6004;
    
    public static final int TYPE_REBATES = 7001;
    public static final int TYPE_REBATES_CATE = 7002;

    public static final int TYPE_REBATE_CATES = 8001;
    public static final int TYPE_REBATE_CATE_ID = 8002;
    public static final int TYPE_REBATE_CATE_ITEM_LIST = 8002;
    
    
    static {
    	FmeiUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "topics", Schema.TYPE_TOPICS);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "topic/#",  Schema.TYPE_TOPIC_ID);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "topic/#/list",  Schema.TYPE_TOPIC_ITEM_LIST);
        
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "cates", Schema.TYPE_CATES);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "cate/#", Schema.TYPE_CATE_ID);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "cate/#/list", Schema.TYPE_CATE_ITEM_LIST);

        FmeiUriMatcher.addURI(Schema.AUTHORITY, "hots", Schema.TYPE_HOTS);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "hot/#/list", Schema.TYPE_HOT_ITEM_LIST);
        
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "acts", Schema.TYPE_ACTS);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "act/#/list", Schema.TYPE_ACT_ITEM_LIST);

        FmeiUriMatcher.addURI(Schema.AUTHORITY, "rebate_cates", Schema.TYPE_REBATE_CATES);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "rebate_cates/#", Schema.TYPE_REBATE_CATE_ID);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "rebate_cates/#/list", Schema.TYPE_REBATE_CATE_ITEM_LIST);        
        
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "myfavs", Schema.TYPE_MYFAVS);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "myfav/#/list", Schema.TYPE_MYFAV_ITEM_LIST);        
        
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "items", Schema.TYPE_ITEMS);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "item/#", Schema.TYPE_ITEM_ID);

        FmeiUriMatcher.addURI(Schema.AUTHORITY, "shops", Schema.TYPE_SHOPS);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "shops/cate/#/list", Schema.TYPE_SHOPS_CATE);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "shop/#", Schema.TYPE_SHOP_ID);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "shop/#/taoke_list", Schema.TYPE_SHOP_TAOKE_LSIT);

        FmeiUriMatcher.addURI(Schema.AUTHORITY, "rebates", Schema.TYPE_REBATES);
        FmeiUriMatcher.addURI(Schema.AUTHORITY, "rebates/cate/#/list", Schema.TYPE_REBATES_CATE);        
    }    
    
}
