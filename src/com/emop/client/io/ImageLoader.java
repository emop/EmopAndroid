package com.emop.client.io;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.widget.ImageView;

import com.emop.client.R;
import com.emop.client.cache.ImageCache;
import com.tencent.mm.sdk.platformtools.Log;

public class ImageLoader {
	public static final int MAX_QUEUE_SIZE = 20;
	public ImageCache cache = null;
	public ThreadPoolExecutor theadPool = null;
	public ThreadPoolExecutor lowPirorTheadPool = null;
	public BlockingQueue<Runnable> queue = null;
	public BlockingQueue<Runnable> lowqueue = null;
	public Handler handler = null; //new Handler();

	//private Context ctx = null;
	
	private static ImageLoader ins = null; 
	public ImageLoader(Context ctx){
		this(ctx, ctx.getCacheDir(), 0, 10);
	}
	
	public ImageLoader(Context ctx, File cacheDir, int cacheSize, int minThread){
		if(cacheSize == 0){
			int memClass = ((ActivityManager) ctx.getSystemService(
					Context.ACTIVITY_SERVICE)).getMemoryClass();	
			cacheSize = 1024 * 1024 * memClass / 8;
		}
		
		//ctx.gete
		cache = new ImageCache(cacheDir, cacheSize);
		/**
		 * 最多只能有20个图片在队列中，等待加载。主要是考虑在瀑布流中，超过20
		 * 个的图片，可能已经进入下一屏幕。加载来也没有意义了。
		 */
		queue = new ArrayBlockingQueue<Runnable>(MAX_QUEUE_SIZE);
		lowqueue = new ArrayBlockingQueue<Runnable>(MAX_QUEUE_SIZE * 20);
		theadPool = new ThreadPoolExecutor(minThread, minThread + 5, 5, TimeUnit.SECONDS, queue);
		lowPirorTheadPool = new ThreadPoolExecutor(3, 8, 5, TimeUnit.SECONDS, lowqueue);
		
		handler = new Handler();
	}
	
	/**
	 * 清除图片加载队列, 比如在Activity切换的时候. 需要把之前加载中的图片清空.
	 */
	public void clear(){
		if(lowqueue.remainingCapacity() > queue.size()){
			lowqueue.addAll(queue);
		}
		queue.clear();
	}
	
	
	public void loadImage(String url, ImageView img){
		loadImage(url, img, true);
	}
	
	public void loadImage(String url, ImageView img, boolean autoScale){
		int width = img.getMeasuredWidth();
		width = Math.max(width, 32);
		loadImage(url, img, width, autoScale);
	}
	
	public void loadImage(String url, ImageView img, int width, boolean autoScale, int loading){
		if(url != null && url.trim().length() > 3){
			Bitmap bitmap = cache.get(url, width, autoScale, false);
			if(bitmap != null){
				img.setImageBitmap(bitmap);
			}else {
				if(loading > 0){
					img.setImageResource(loading);
				}
				runTask(new LoadingImage(img, width, url));
			}	
		}else if(loading > 0){
			img.setImageResource(loading);			
		}
	}	
	
	public void loadImage(String url, ImageView img, int width, boolean autoScale){
		loadImage(url, img, width, autoScale, R.drawable.loading);
	}

	public void loadToCache(final String url, final int width, boolean lowPrioir){
		if(lowPrioir){
			this.lowPirorTheadPool.execute(new Runnable(){
				@Override
				public void run() {
					cache.get(url, width, true);
				}				
			});
		}else {
			runTask(new Runnable(){
				@Override
				public void run() {
					cache.get(url, width, true);
				}
			});
		}
	}
	
	public void loadToCache(final String url, final int width){
		loadToCache(url, width, false);
	}
	
	public void runTask(Runnable command){
		if(queue.size() >= MAX_QUEUE_SIZE){
			queue.poll();
		}
		theadPool.execute(command);
	}
	
	
    class LoadingImage implements Runnable{
    	private ImageView img = null;
    	private int width = 0;
    	private String url = null;
    	LoadingImage(ImageView v, int width, String url){
    		img = v;
    		this.width = width;
    		this.url = url;
    	}
    	
		@Override
		public void run() {
			final Bitmap bm = cache.get(url, width, true);
			if(bm != null){
				handler.post(new Runnable(){
					@Override
					public void run() {
						//Log.d("xxx", "iv:" + img.getId() + "xx:" + url);
						img.setImageBitmap(bm);
					}
				});
			}
		}
    }
    
}
