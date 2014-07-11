package com.emop.client.widget;


import android.app.Activity;
import android.app.Dialog;
import android.graphics.Rect;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;

import com.emop.client.R;

public class DialogBuilder {
	public static Dialog showVersionCheck(final Activity context, OnClickListener listener) {
		Dialog dialog = new Dialog(context, R.style.dialog);
		dialog.setContentView(R.layout.dialog_upgrade_version);
		
		Rect displayRectangle = new Rect();
		Window window = context.getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
		dialog.getWindow().setLayout((int)(displayRectangle.width() * 0.9f), 
				LayoutParams.WRAP_CONTENT);
		
		Button b = (Button)dialog.findViewById(R.id.upgrade_now);
		b.setOnClickListener(listener);
		b = (Button)dialog.findViewById(R.id.upgrade_later);
		b.setOnClickListener(listener);
		b = (Button)dialog.findViewById(R.id.no_upgrade);
		b.setOnClickListener(listener);
		
		return dialog;
	}
	
	public static Dialog showInstallAPI(final Activity context, OnClickListener listener) {
		Dialog dialog = new Dialog(context, R.style.dialog);
		dialog.setContentView(R.layout.dialog_download_process);
		
		Rect displayRectangle = new Rect();
		Window window = context.getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
		dialog.getWindow().setLayout((int)(displayRectangle.width() * 0.9f), 
				LayoutParams.WRAP_CONTENT);
		
		return dialog;
	}	
	
	public static Dialog showTaobaoLogin(final Activity context, OnClickListener listener) {
		Dialog dialog = new Dialog(context, R.style.dialog);
		dialog.setContentView(R.layout.dialog_taobao_login);
		
		Rect displayRectangle = new Rect();
		Window window = context.getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
		dialog.getWindow().setLayout((int)(displayRectangle.width() * 0.9f), 
				LayoutParams.WRAP_CONTENT);
		
		Button b = (Button)dialog.findViewById(R.id.login_now);
		b.setOnClickListener(listener);
		b = (Button)dialog.findViewById(R.id.upgrade_later);
		b.setOnClickListener(listener);
		
		return dialog;
	}		

}
