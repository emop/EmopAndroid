package com.emop.client.provider;

public class QueryParam {
	public final static String PAGE_SIZE = "pageSize";
	public final static String PAGE_NO = "pageNo";
	
	public String selection;
	public String[] selectionArgs;
	public String sortOrder;
	public String groupBy;
	public String having;
	public QueryParam(String sel, String[] args, String order){
		this.selection = sel;
		this.selectionArgs = args;
		this.sortOrder = order;
	}
}
