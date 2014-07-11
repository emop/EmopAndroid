package com.emop.client.provider;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class JSONCursor implements Cursor {
	public static final Cursor EMPTY = new JSONCursor(new JSONArray(), new String[]{});
	private JSONArray data = null;
	private JSONObject curObj = null;
	private int curIndex = 0;
	private ArrayList<String> keys = new ArrayList<String>();
	private boolean isClosed = false;
	
	public JSONCursor(JSONArray data, String[] keys){
		this.data = data;
		if(data.length() > 0){
			try {
				curObj = data.getJSONObject(0);
				for(String k: keys){
					this.keys.add(k);
				}
				if(curObj.has("id")){
					this.keys.add("id");
				}
				//this.keys = keys;
			} catch (JSONException e) {
				Log.w("emop", "error:" + e.toString(), e);
			}
		}
	}

	@Override
	public void close() {
		isClosed = true;
	}

	@Override
	public void copyStringToBuffer(int arg0, CharArrayBuffer arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deactivate() {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] getBlob(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getColumnCount() {
		return keys.size();
	}

	@Override
	public int getColumnIndex(String name) {
		if(name.equals("_id")){
			name = "id";
		}
		if(curObj.has(name)){
			return keys.indexOf(name);
		}else if(name.equals("id")){
			return 0;
		}else {
			return -1;
		}
	}

	@Override
	public int getColumnIndexOrThrow(String arg0)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getColumnName(int index) {
		return keys.get(index);
	}

	@Override
	public String[] getColumnNames() {
		return keys.toArray(new String[]{});
	}

	@Override
	public int getCount() {
		return data.length();
	}

	@Override
	public double getDouble(int index) {
		try{
			index = index == 0 ? 1 : index;
			return curObj.getDouble(keys.get(index));
		}catch(Exception e){
			Log.w("emop", "error:" + e.toString() + ", index:" + index, e);
		}
		return 0;
	}

	@Override
	public Bundle getExtras() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getFloat(int index) {
		return (float)getDouble(index);
	}

	@Override
	public int getInt(int index) {
		return (int)getLong(index);
	}

	@Override
	public long getLong(int index) {
		try{
			if(index == 0){
				index = this.getColumnIndex("_id");
			}
			return curObj.getLong(keys.get(index));
		}catch(Exception e){
			Log.w("emop", "error:" + e.toString() + ", index:" + index +".\n obj:" + curObj.toString(),
					e);
		}
		return 0;
	}

	@Override
	public int getPosition() {
		return curIndex;
	}

	@Override
	public short getShort(int index) {
		return (short)getLong(index);
	}

	@Override
	public String getString(int index) {
		try{
			index = index == 0 ? 1 : index;
			return curObj.getString(keys.get(index));
		}catch(Exception e){
			Log.w("emop", "error:" + e.toString() + ", index:" + index, e);
		}
		return null;
	}

	@Override
	public boolean getWantsAllOnMoveCalls() {
		return false;
	}

	@Override
	public boolean isAfterLast() {
		return curIndex > data.length();
	}

	@Override
	public boolean isBeforeFirst() {
		return curIndex < 0;
	}

	@Override
	public boolean isClosed() {
		return isClosed;
	}

	@Override
	public boolean isFirst() {
		return curIndex == 0;
	}

	@Override
	public boolean isLast() {
		return curIndex == data.length() - 1;
	}

	@Override
	public boolean isNull(int arg0) {
		return false;
	}

	@Override
	public boolean move(int index) {
		return moveToPosition(curIndex + index);
	}

	@Override
	public boolean moveToFirst() {
		return moveToPosition(0);
	}

	@Override
	public boolean moveToLast() {
		return moveToPosition(data.length() - 1);
	}

	@Override
	public boolean moveToNext() {
		return moveToPosition(curIndex + 1);
	}

	@Override
	public boolean moveToPosition(int index) {
		if(index < 0 || index >= data.length()){
			return false;
		}
		this.curIndex = index;
		try{
			curObj = data.getJSONObject(index);
		}catch(Exception e){
			Log.w("emop", "move to position error:" + index, e);
		}
		return true;
	}

	@Override
	public boolean moveToPrevious() {
		return moveToPosition(curIndex - 1);
	}

	@Override
	public void registerContentObserver(ContentObserver arg0) {

	}

	@Override
	public void registerDataSetObserver(DataSetObserver arg0) {

	}

	@Override
	public boolean requery() {
		return false;
	}

	@Override
	public Bundle respond(Bundle arg0) {
		return null;
	}

	@Override
	public void setNotificationUri(ContentResolver arg0, Uri arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregisterContentObserver(ContentObserver arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
		// TODO Auto-generated method stub

	}

}
