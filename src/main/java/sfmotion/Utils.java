package sfmotion;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.simmetrics.metrics.DamerauLevenshtein;

public class Utils {
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

	static String test1 = "DELANCEY STREET FOUNDATION DBA DELANCEY STREET MOVING";
	static String test2 = "DELANCY STREET FOUNDATION DBA DELANCY STREET MOVING";

	public static void main(String[] args) {
		String[] ss1 = test1.split("\\s+");
		String[] ss2 = test2.split("\\s+");
		List<String> ls1 = new ArrayList<>();
		for (String s : ss1) {
			ls1.add(s);
		}
		List<String> ls2 = new ArrayList<>();
		for (String s : ss2) {
			ls2.add(s);
		}
		double score = stringListDistance(ls1, ls2, 0.73);
		System.out.println("Score: " + score);
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

	public static double stringListDistance(List<String> ls1, List<String> ls2, double threshold) {
		List<String> list1 = new ArrayList<>(ls1); // copy them so we do not alter the inputs
		List<String> list2 = new ArrayList<>(ls2);
		//		List<String>leftOver = new ArrayList<>();
		List<String> matched = new ArrayList<>();
		int i = 0;
		while (i < list1.size()) {
			String s = list1.get(i);
			if (list2.remove(s)) {
				matched.add(s);
				list1.remove(i);
			} else {
				i++;
			}
		}
		if (list1.size() > 0 && list2.size() > 0) {
			List<Triple> tri = new ArrayList<>();
			DamerauLevenshtein dl = new DamerauLevenshtein();
			for (String s : list1) {
				for (String p : list2) {
					double d = dl.compare(s, p);
					if (d >= threshold) {
						Triple t = new Triple(s, p, Double.valueOf(d));
						tri.add(t);
					}
				}
			}
			Collections.sort(tri);
			while (!tri.isEmpty()) {
				Triple t = tri.remove(0);
				String s = (String) t.o1;
				String p = (String) t.o2;
				matched.add(s);
				list1.remove(s);
				list2.remove(p);
				int j = 0;
				while (j < tri.size()) {
					Triple tt = tri.get(j);
					String ss = (String) tt.o1;
					String pp = (String) tt.o2;
					if (ss == s || pp == p) {
						tri.remove(j);
						continue;
					} else {
						j++;
					}
				}
			}
		}
		double distance = (double) (list1.size() + list2.size()) / (double) (ls1.size() + ls2.size());
		double score = 1.0 - distance;
		return score;
	}

	public static class Triple implements Comparable<Triple> {
		public Object o1;
		public Object o2;
		public Object o3;

		public Triple(Object _o1, Object _o2, Object _o3) {
			o1 = _o1;
			o2 = _o2;
			o3 = _o3;
		}

		@Override
		// descending order
		public int compareTo(Triple p) {
			if (p.o3 instanceof Integer) {
				Integer i1 = (Integer) p.o3;
				Integer i2 = (Integer) this.o3;
				return i1 - i2;
			}
			if (p.o3 instanceof Double) {
				Double i1 = (Double) p.o3;
				Double i2 = (Double) this.o3;
				double d = i1 - i2;
				if (d > 0.0)
					return 1;
				if (d == 0.0)
					return 0;
				if (d < 0.0)
					return -1;
			}
			return 0;
		}
	}

	public static List<Object> listAND(List<? extends Object> list1, List<? extends Object> list2) {
		List<Object> rlist = new ArrayList<>();
		for (Object o : list1) {
			if (list2.contains(o)) {
				rlist.add(o);
			}
		}
		return rlist;
	}

	public static List<Object> listXOR(List<? extends Object> list1, List<? extends Object> list2) {
		List<Object> rlist = new ArrayList<>();
		if (list1 == null && list2 == null) {
			return rlist;
		}
		if (list1 != null && list2 == null) {
			rlist.addAll(list1);
		} else if (list2 != null && list1 == null) {
			rlist.addAll(list2);
		} else {
			for (Object o : list1) {
				if (!list2.contains(o)) {
					rlist.add(o);
				}
			}
			for (Object o : list2) {
				if (!list1.contains(o)) {
					rlist.add(o);
				}
			}
		}
		return rlist;
	}
}
