package utils;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DateTime {
	static final Map<String, String> monthLookup = new HashMap<>();
	static {
		monthLookup.put("JAN", "01");
		monthLookup.put("FEB", "02");
		monthLookup.put("MAR", "03");
		monthLookup.put("APR", "04");
		monthLookup.put("MAY", "05");
		monthLookup.put("JUN", "06");
		monthLookup.put("JUL", "07");
		monthLookup.put("AUG", "08");
		monthLookup.put("SEP", "09");
		monthLookup.put("OCT", "10");
		monthLookup.put("NOV", "11");
		monthLookup.put("DEC", "12");
		monthLookup.put("JANUARY", "01");
		monthLookup.put("FEBRUARY", "02");
		monthLookup.put("MARCH", "03");
		monthLookup.put("APRIL", "04");
		monthLookup.put("JUNE", "06");
		monthLookup.put("JULY", "07");
		monthLookup.put("AUGUST", "08");
		monthLookup.put("SEPTEMBER ", "09");
		monthLookup.put("OCTOBER ", "10");
		monthLookup.put("NOVEMBER ", "11");
		monthLookup.put("DECEMBER ", "12");
	}

	public static Date getSqlDate(List<String> datefields) {
		String sMonth = datefields.get(0);
		String sdate = datefields.get(1);
		String syear = datefields.get(2);
		if (sMonth != null && sdate != null && syear != null) {
			sMonth = sMonth.substring(0, 3);
			sMonth = monthLookup.get(sMonth);
		} else {
			sMonth = datefields.get(3);
			sdate = datefields.get(4);
			syear = datefields.get(5);
			if (syear.length() == 2) {
				syear = "20" + syear;
			}
		}
		String dateNew = syear + "-" + sMonth + "-" + sdate;
		return Date.valueOf(dateNew);
	}

	public static int daysInBetween(Date d1, Date d2) {
		long lms1 = d1.getTime();
		long lms2 = d2.getTime();
		long diff = (lms2 - lms1) / 1000; // seconds
		long days = diff / (24 * 3600);
		return (int) days;
	}

}
