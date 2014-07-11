package com.emop.client.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeHelper {
	public static DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
	public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public static DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static String formatTime(Date time){
		return timeFormat.format(time);
	}

	public static String formatDate(Date time){
		return dateFormat.format(time);
	}
	
	public static String formatDateTime(Date time){
		return dateTimeFormat.format(time);
	}

	public static Date parseDateTime(String time){
		try {
			return dateTimeFormat.parse(time);
		} catch (ParseException e) {
			return null;
		}
	}

	public static Date parseDate(String time){
		try {
			return dateFormat.parse(time);
		} catch (ParseException e) {
			return null;
		}
	}

	public static Date parseTime(String time){
		try {
			return timeFormat.parse(time);
		} catch (ParseException e) {
			return null;
		}
	}
	
	public static String formatRemainHour(String end, int maxDate){
		Date date = parseDateTime(end);
		if(date != null && date.getTime() > System.currentTimeMillis()){
			long time = date.getTime() - System.currentTimeMillis();
			long minutes = time / 1000 / 60;
			
			long reDay = (minutes / (60 * 24)) % maxDate;
			
			long reHour = (minutes % (60 * 24)) / 60;
			long reMin = minutes % 60;
			return String.format("%s天%s小时%s分", reDay, reHour, reMin);
			
		}
		return "0小时0分";
	}
}
