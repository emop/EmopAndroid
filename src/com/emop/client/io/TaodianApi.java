package com.emop.client.io;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

/**
 * Taodian API接口。
 * @author deonwu
 *
 */
public class TaodianApi {
	public static final int STATUS_NORMAL = 1001;
	public static final int STATUS_ALL = -1;
	
	private String appSecret = "298cd2d9700b08f7bab6f6a28647c8eb";
	private String appKey = "11";
	private HttpTransport http = null;
	public Context ctx = null;
	
	public TaodianApi(){
		//http = new HttpTransport(appKey, appSecret);
	}
	
	public ApiResult connect(Context ctx){
		http = new HttpTransport(ctx, appKey, appSecret);
		this.ctx = ctx;
		return http.ping();		
	}
	
	/*
	 * 直接调用底层Taodian API.
	 */
	public ApiResult call(String api, Map<String, Object> param){
		if(this.http == null){
			this.connect(ctx);
		}
		return this.http.call(api, param);
	}
	
	public ApiResult getTopicList(int size, int status, String noCache){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("no_cache", noCache);		
		param.put("user_id", 2);
		param.put("item_head_count", 0);
		param.put("cate", 3);
		param.put("status", status + "");
		param.put("scope", "site");
		param.put("fields", "topic_name,description,create_time,update_time,item_count,front_pic,view_order,status");
		
		return 	call("tuji_user_topic_list", param);
	}
	
	public ApiResult getCateList(int size, int status){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("user_id", 2);
		param.put("item_head_count", 0);
		param.put("cate", 4);
		param.put("status", status + "");
		param.put("scope", "site");
		
		param.put("fields", "topic_name,description,create_time,update_time,item_count,front_pic,view_order,status");
		
		return 	call("tuji_user_topic_list", param);
	}
	
	public ApiResult getHotCateList(int size, int status){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("user_id", 2);
		param.put("item_head_count", 0);
		param.put("cate", 5);
		param.put("status", status + "");
		param.put("scope", "site");
		
		param.put("fields", "topic_name,tags,description,create_time,update_time,item_count,front_pic,view_order,status");
		
		return 	call("tuji_user_topic_list", param);
	}	
	
	public ApiResult getActList(int size, int status){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("user_id", 2);
		param.put("item_head_count", 0);
		param.put("cate", 6);
		param.put("status", status + "");
		param.put("scope", "site");
		
		param.put("fields", "topic_name,description,create_time,update_time,item_count,front_pic,view_order,status");
		
		return 	call("tuji_user_topic_list", param);
	}		
	
	public ApiResult getTopicPidItemList(int topic, int page_size, String pageNo, String uid, String noCache){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("topic_id", topic);
		param.put("page_size", page_size);
		param.put("page_no", pageNo);
		param.put("user_id", 2);
		param.put("track_user_id", uid);
		param.put("content_type", "taoke");
		param.put("no_cache", noCache);		
		param.put("fields", "id,pic_url,price,content_type,num_iid,shop_id,short_url_key,item_id,update_time,status");
		
		return 	call("tuji_topic_convert_item_click_url", param);
	}

	public ApiResult getShopItemList(int shopId, int page_size, String pageNo, String uid, String noCache){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("shop_id", shopId);
		param.put("page_size", page_size);
		param.put("page_no", pageNo);
		param.put("user_id", 2);
		param.put("track_user_id", uid);
		param.put("content_type", "taoke");
		param.put("no_cache", noCache);		
		param.put("fields", "id,pic_url,price,content_type,num_iid,shop_id,short_url_key,item_id,update_time,status");
		
		return 	call("tuji_topic_convert_item_click_url", param);
	}
	
	public ApiResult getTopicItemList(int topic, int page_size, String pageNo){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("topic_id", topic);
		param.put("page_size", page_size);
		param.put("page_no", pageNo);
		param.put("user_id", 2);
		param.put("fields", "id,text,pic_url,price,content_type,num_iid,shop_id,short_url_key,item_id,update_time,status");
		
		return 	call("tuji_topic_item_list", param);
	}
	
	public ApiResult getShopList(String uid, String cate, int page_size, int pageNo){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("track_user_id", uid);
		param.put("cate", cate);
		
		param.put("page_size", page_size + "");
		param.put("page_no", pageNo + "");
		param.put("fields", "shop_id,root_tag,user_nick,pic_path,shop_title,shop_url,short_url_key,shop_type,shop_desc,seller_credit");
		
		return 	call("shop_taoke_link_get", param);
	}	
	
	public ApiResult getOneShop(String uid, String shopId, int page_size, int pageNo){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("track_user_id", uid);
		param.put("cate", "11");
		param.put("shop_ids", shopId);
		
		param.put("page_size", page_size + "");
		param.put("page_no", pageNo + "");
		param.put("fields", "shop_id,root_tag,user_nick,pic_path,shop_title,shop_url,short_url_key,shop_type,shop_desc,seller_credit");
		
		return 	call("shop_taoke_link_get", param);
	}	
	
	public ApiResult getRebateList(String uid, String cate, int page_size, int pageNo){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("track_user_id", uid);
		param.put("cate", cate);
		
		param.put("page_size", page_size + "");
		param.put("page_no", pageNo + "");
		param.put("fields", "num_iid,root_tag,nick,title,price,pic_url,coupon_rate,coupon_price,coupon_start_time,coupon_end_time,taoke_click_url,short_url_key");
		
		return 	call("rebate_taoke_link_get", param);
	}
	
	public ApiResult getRebateCateList(int size, int status){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();

		param.put("fields", "topic_name,description,create_time,update_time,item_count,front_pic,view_order,status");
		
		return 	call("rebate_cate_list_get", param);
	}	
	
	public ApiResult getMyFavoriteItemList(String topic, String userId){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("topic_id", topic);
		param.put("page_size", 40);
		param.put("user_id", userId);
		
		param.put("fields", "id,text,pic_url,price,content_type,num_iid,shop_id,short_url_key,item_id,update_time,status");
		
		return 	call("tuji_topic_item_list", param);
	}	

	public ApiResult getMyFavoriteShopList(String topic, String userId){
		ApiResult r = null;
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("topic_id", topic);
		param.put("page_size", 40);
		param.put("user_id", userId);
		
		param.put("fields", "id,text,pic_url,price,content_type,num_iid,shop_id,short_url_key,item_id,update_time,status");
		
		return 	call("tuji_topic_item_list", param);
	}	

}
