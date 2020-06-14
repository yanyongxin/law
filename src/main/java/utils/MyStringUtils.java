package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simmetrics.metrics.DamerauLevenshtein;

public class MyStringUtils {
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

	// generate a regular expression from a string: Alan J. Smith => Alan\s*J\.?\s*Smith
	public static String regConvert(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isAlphabetic(c) || Character.isDigit(c)) {
				sb.append(c);
			} else if (Character.isSpaceChar(c)) {
				sb.append("\\s*");
			} else if (c == '.') {
				sb.append("\\.?");
			} else {
				sb.append("\\" + c);
			}
		}
		return sb.toString();
	}

}
