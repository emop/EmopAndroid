package com.emop.client.io;

import org.json.JSONObject;

public class WeiboUser extends ApiResult{

	public WeiboUser(JSONObject d) {
		super();
		this.json = d;
	}
	
	public String toString(){
		return this.getString("screen_name");
	}
}
