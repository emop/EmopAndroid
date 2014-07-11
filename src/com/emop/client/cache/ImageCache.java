package com.emop.client.cache;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.emop.client.Constants;
import com.jakewharton.DiskLruCache;
import com.jakewharton.MD5;

public class ImageCache {
	public static DiskLruCache diskCache = null;
	private File cacheRoot = null;
	private LruCache<String, Bitmap> memCache;
	private LruCache<String, Lock> lockCache;
	
	public ImageCache(File root, final int cacheSize){
		this.cacheRoot = root;
		
		memCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	        	int size = bitmap.getHeight() * bitmap.getWidth() * 4;
	            return size;
	        }
	    };	
	    
	    lockCache = new LruCache<String, Lock>(200) {
	        @Override
	        protected int sizeOf(String key, Lock obj) {
	            return 1;
	        }
	    };
	    
	    if(diskCache == null && root != null){
	    	try {
	    		Log.d(Constants.TAG_EMOP, "create LRU cache in:" + root.getAbsolutePath());
				diskCache = DiskLruCache.open(root, 1, 1, 1024 * 1024 * 64);
			} catch (IOException e) {
				Log.w(Constants.TAG_EMOP, "open disk cache error:" + e.toString(), e);
			}
	    }
	}
	
	public void cleanUpDiskCache(){
		if(diskCache != null){
			File f = diskCache.getDirectory();
			try {
				memCache.evictAll();
				diskCache.delete();
				diskCache = DiskLruCache.open(f, 1, 1, 1024 * 1024 * 64);
			} catch (IOException e) {
				Log.w(Constants.TAG_EMOP, "open disk cache error:" + e.toString(), e);
			}
		}
	}
	//public ImageCache(Context )
	public Bitmap get(String url, int minWdith, boolean scaled){
		return get(url, minWdith, scaled, true);
	}
	
	public Bitmap get(String url, int minWdith, boolean scaled, boolean autoLoad){
		if(scaled){
			url = getScaledUrl(url, minWdith);
		}
		
		try {
			return get(new URL(url), autoLoad, minWdith);
		} catch (MalformedURLException e) {
			Log.e("tag", "error url:" + url.toString(), e);
		}	
		return null;
	}
	
	public Bitmap get(URL url, boolean autoLoad, int minWidth){
		
		String ck = url.toString() + "!" + minWidth;		
		//避免多个线程重复的加载一个文件。
		Lock o = getLock(ck);
		
		
		Bitmap img = null;
		img = memCache.get(ck);
		if(img != null && !img.isRecycled()){
			//Log.d("xxx", "load from memcache:" + ck);
		}else if(autoLoad){
			synchronized(o){
				img = memCache.get(ck);
				if(img == null || img.isRecycled()){
					img = loadFormURL(url, minWidth);
					if(img != null){
						memCache.put(ck, img);
					}
				}
			}
		}else {
			img = null;
		}
		
		return img;
	}
	
	public Bitmap loadFormURL(URL url, int minWidth){
		
		Bitmap img = null;
		try{
			img = readFromDiskCache(url);
			if(img == null){
				File cacheFile = getCached(url, minWidth);
				if(cacheFile != null && cacheFile.isFile()){
					//Log.d("tag", "load image from disk cache:" + cacheFile.getAbsolutePath());
					img = BitmapFactory.decodeFile(cacheFile.getAbsolutePath());
				}else {
					//Log.d("tag", "from web:" + url.toString());
					img = loadUrl(url, minWidth);
					boolean isWrited = false;
					if(img != null){
						isWrited = writeToDiskCache(url, img);
					}
					if(!isWrited && cacheFile != null && img != null){
						saveAsFile(img, cacheFile);
					}
				}
			}
		}catch(OutOfMemoryError e){
			Log.w("error", "OutOfMemoryError in load image:" + url.toString());
			memCache.evictAll();
			if(diskCache != null){
				try {
					diskCache.flush();
				} catch (IOException e1) {
				}
			}
		}
		
		return img;
	}
	
	protected Bitmap readFromDiskCache(URL url){
		Bitmap bm = null;
		if(diskCache != null){
			//diskCache.flush()
			String cacheKey = MD5.encode(url.toString());
			try {
				DiskLruCache.Snapshot snapshot = diskCache.get(cacheKey);
				if(snapshot != null){
					InputStream in = snapshot.getInputStream(0);
					bm = BitmapFactory.decodeStream(in);
					in.close();
					snapshot.close();
					snapshot = null;
					//Log.d(Constants.TAG_EMOP, "read from disk key:" + cacheKey + ", url:" + url.toString());
				}
			} catch (IOException e) {
				Log.d(Constants.TAG_EMOP, "read disk lru cache error:" + e.toString(), e);
			} 
		}else {
			Log.w(Constants.TAG_EMOP, "read disk lru cache is null");
		}
		
		return bm;
	}
	
	protected boolean writeToDiskCache(URL url, Bitmap bitmap){
		boolean isOk = false;
		if(diskCache != null){
			String cacheKey = MD5.encode(url.toString());
			try {
				DiskLruCache.Editor editor = diskCache.edit(cacheKey);
				if(editor != null){
					OutputStream out = editor.newOutputStream(0);
					bitmap.compress(CompressFormat.PNG, 95, out);
					out.close();
					editor.commit();
					//Log.d(Constants.TAG_EMOP, "write to disk key:" + cacheKey + ", url:" + url.toString());
					isOk = true;
					editor = null;
				}
			} catch (IOException e) {
				isOk = false;
				Log.d(Constants.TAG_EMOP, "write disk lru cache error:" + e.toString(), e);
			}
		}else {
			Log.w(Constants.TAG_EMOP, "read disk lru cache is null");
		}
		return isOk;
	}
	
	
	public File getCachedFile(String picPath, int minWidth){
		Bitmap img = null;
		File cacheFile = null;
		String path = getScaledUrl(picPath, minWidth);
		try{
			URL url = new URL(path);
			cacheFile = getCached(url, minWidth);
			if(cacheFile != null && cacheFile.isFile()){
			}else {
				Log.d("tag", "from web:" + url.toString());
				img = loadUrl(url, minWidth);
				if(cacheFile != null && img != null){
					saveAsFile(img, cacheFile);
				}
			}
		}catch(Exception e){
			Log.e("error", "Error in load image:" + picPath, e);
		}
		return cacheFile;
	}
	
	public synchronized Lock getLock(String url){
		Lock o = lockCache.get(url);
		if(o == null){
			o = new ReentrantLock();
			lockCache.put(url, o);
		}
		return o;
	}
	
    protected File getCached(URL url, int minWidth){
    	File cache = null;
    	if(url.getProtocol().toLowerCase().startsWith("http")){
    		cache = new File(cacheRoot, url.getPath() + "_" + minWidth);
    	}
    	return cache;
    }	
    
    protected void saveAsFile(Bitmap bitmap, File path){
    	File dir = path.getParentFile();
    	if(!dir.isDirectory()){
    		dir.mkdirs();
    	}
    	FileOutputStream fOut = null; 
    	try{
    		Log.d("tag", "save to cache:" + path.getAbsolutePath());
        	if(path.createNewFile()){
        		fOut = new FileOutputStream(path); 
        		bitmap.compress(CompressFormat.PNG, 95, fOut);
        	}
    	}catch (IOException e) { 
    		Log.e("tag", e.toString());
    	}finally{
    		if(fOut != null){
    			try {
					fOut.close();
				} catch (IOException e) {
					Log.e("tag", e.toString());
				}
    		}
    	}
    }
    
    protected Bitmap loadFromFile(String path, int width){
    	BitmapFactory.Options options = new BitmapFactory.Options(); 
    	options.inJustDecodeBounds = true; 
    	Bitmap bitmap = BitmapFactory.decodeFile(path, options);
    	
    	options.inSampleSize = computeSampleSize(options, width,
    			width * width * 3);
    	
    	Log.i("tag", "load picture, orgWidth:" + options.outWidth + ", orgHeight:" + options.outHeight + ", sample:" + options.inSampleSize);
    	options.inJustDecodeBounds = false;
    	options.inInputShareable = true;
    	options.inPurgeable = true;
    	
    	bitmap = BitmapFactory.decodeFile(path, options);
    	return bitmap;
    }
    
    protected Bitmap loadUrl(URL url, int width){
    	Bitmap bitmap = null;
    	try{
    		byte[] buffer = loadUrlRaw(url);
    		if(buffer != null && buffer.length > 100){
		    	BitmapFactory.Options options = new BitmapFactory.Options(); 
		    	options.inJustDecodeBounds = true; 
		    	bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
		    	options.inSampleSize = computeSampleSize(options, width,
		    			width * width * 3);
		    	
		    	Log.i("tag", "load picture, orgWidth:" + options.outWidth + ", orgHeight:" + options.outHeight + ", sample:" + options.inSampleSize);
		    	options.inJustDecodeBounds = false;
		    	options.inInputShareable = true;
		    	options.inPurgeable = true;
		    	
		    	bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
    		}
    	}catch(Throwable e){
    		Log.e("tag", "error log image:" + url.toString(), e);
    	}finally{
    	}
    	
    	return bitmap;
    }
    
    protected byte[] loadUrlRaw(URL url){
    	ByteArrayOutputStream out = new ByteArrayOutputStream(1024 * 100);
    	BufferedInputStream bis = null;
    	byte[] isBuffer = new byte[1024 * 4];
    	int length = 0;
    	int contentLength = 0;
    	URLConnection conn = null;
    	try{
    		conn = url.openConnection(); 
    		contentLength = conn.getContentLength();
    		conn.setReadTimeout(1000 * 5);
    		bis = new BufferedInputStream(conn.getInputStream(), 1024 * 4);
    		for(int len = bis.read(isBuffer); len != -1; len = bis.read(isBuffer)){
    			out.write(isBuffer, 0, len);
    			length += len;
    			if(length > 1024 * 512){
    				Log.w("tag", "The file is too large, It be dropped. url:" + url.toString());
    				break;
    			}
    		}
    		if(contentLength <= 0){
    			Log.w("tag", "The URL have not content length header");
    		}
    		if(contentLength <= 0 || contentLength == length){
    			isBuffer = out.toByteArray();
    		}else {
    			isBuffer = null;
    		}
    	}catch(Exception e){
    		Log.e("tag", "load url error:" + url.toString(), e);
    	}finally{
    		if(bis != null){
    			try {
					bis.close();
				} catch (IOException e) {
				}
    		}
    		if(out != null){
    			try {
					out.close();
				} catch (IOException e) {
				}
    		}
    	}
    	return isBuffer;
    }
    
    /**
     * 根据需要显示的图片大小，选择一个宽度对接近的缩放尺寸。节约网络带宽。
     * @param u
     * @param minWidth
     * @return
     */
	private String getScaledUrl(String u, int minWidth){
		if(u.indexOf('!')> 0){
			return u;
		}
		
		String scaleType = "";
		if(u.indexOf("mobile01") > 0){
			scaleType = getMobile01Scale(minWidth);
		}else if(u.indexOf("tdcms") > 0){
			scaleType = getTdcmsScale(minWidth);
		}
		if(scaleType != null && scaleType.length() > 0){
			return u + "!" + scaleType;
		}
		
		return u;
	}  
	
	private String getTdcmsScale(int w){
		if(w <= 165){
			return "small";
		}else if(w <= 250){
			return "190";
		}else if(w <= 440){
			return "weibo";
		}
		return "weibo";
	}

	private String getMobile01Scale(int w){
		if(w <= 120){
			return "90";
		}else if(w <= 165){
			return "small";
		}else if(w <= 230){
			return "190";
		}else if(w <= 540){
			return "weibo";
		}else if(w <= 640){
			return "weibo2";
		}
		
		return "xhdpi";
	}	
	
	public static int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
				.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
			return 1;
		} else if (minSideLength == -1) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}	
	
}
