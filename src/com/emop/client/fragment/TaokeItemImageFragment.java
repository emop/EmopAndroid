package com.emop.client.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.emop.client.R;

public class TaokeItemImageFragment extends Fragment {
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState){
		Log.d("xx", "onCreateView...");
		View v = inflater.inflate(R.layout.taoke_item_detail_img, container, false);
		
		return v;
	}
	
	public void onActivityCreated(Bundle savedState){
		super.onActivityCreated(savedState);
		//new DataLoader().start();
		getLoaderManager().initLoader(0, null, 
			new LoaderCallbacks<Cursor>() {

				@Override
				public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onLoaderReset(Loader<Cursor> arg0) {
					// TODO Auto-generated method stub
					
				}
			}
		);
	}	

}
