package com.taobao.top.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.taobao.top.android.api.FileItem;

/**
 * TOP API请求业务参数
 *
 */
public class TopParameters {

	private String method;
	private List<String> fields=new ArrayList<String>();
	private Map<String,String> params=new HashMap<String,String>();
	private Map<String,FileItem> attachments=new HashMap<String,FileItem>();
	
	/**
	 * 添加返回数据字段
	 * 
	 * @param value
	 */
	public void addFields(String... value){
		if(value!=null){
			for(String v:value){
				fields.add(v);
			}
		}
	}
	/**
	 * 添加业务参数
	 * @param key
	 * @param value
	 */
	public void addParam(String key,String value){
		params.put(key, value);
	}
	/**
	 * 获取已添加的业务参数
	 * @param key
	 * @return
	 */
	public String getParam(String key){
		return params.get(key);
	}
	/**
	 * 删除已添加的业务参数
	 * @param key
	 */
	public void removeParam(String key){
		params.remove(key);
	}
	/**
	 * 添加附件
	 * @param key
	 * @param file
	 */
	public void addAttachment(String key,FileItem file){
		if(file==null){
			return;
		}
		attachments.put(key, file);
	}
	/**
	 * 获取已添加的附件
	 * @param key
	 * @return
	 */
	public FileItem getAttachment(String key){
		return attachments.get(key);
	}
	/**
	 * 删除已添加的附件
	 * @param key
	 */
	public void removeAttachment(String key){
		attachments.remove(key);
	}
	
	public String getMethod() {
		return method;
	}
	/**
	 * 设置准备调用的api名字
	 * @param method
	 */
	public void setMethod(String method) {
		this.method = method;
	}
	public List<String> getFields() {
		return fields;
	}
	public void setFields(List<String> fields) {
		this.fields = fields;
	}
	public Map<String, String> getParams() {
		return params;
	}
	public void setParams(Map<String, String> params) {
		this.params = params;
	}
	public Map<String, FileItem> getAttachments() {
		return attachments;
	}
	public void setAttachments(Map<String, FileItem> attachment) {
		this.attachments = attachment;
	}
	
}
