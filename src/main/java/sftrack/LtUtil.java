package sftrack;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LtUtil {
	private static final Logger log = LoggerFactory.getLogger(LtUtil.class);

	boolean flagPrint = true;
	static Map<String, Integer> monthNameToMonthNumber = new HashMap<String, Integer>();

	static {
		//		monthNameToMonthNumber.put("january", 1);
		//		monthNameToMonthNumber.put("february", 2);
		//		monthNameToMonthNumber.put("march", 3);
		//		monthNameToMonthNumber.put("april", 4);
		//		monthNameToMonthNumber.put("may", 5);
		//		monthNameToMonthNumber.put("june", 6);
		//		monthNameToMonthNumber.put("july", 7);
		//		monthNameToMonthNumber.put("august", 8);
		//		monthNameToMonthNumber.put("september", 9);
		//		monthNameToMonthNumber.put("october", 10);
		//		monthNameToMonthNumber.put("november", 11);
		//		monthNameToMonthNumber.put("december", 12);
		monthNameToMonthNumber.put("Month_1", 1);
		monthNameToMonthNumber.put("Month_2", 2);
		monthNameToMonthNumber.put("Month_3", 3);
		monthNameToMonthNumber.put("Month_4", 4);
		monthNameToMonthNumber.put("Month_5", 5);
		monthNameToMonthNumber.put("Month_6", 6);
		monthNameToMonthNumber.put("Month_7", 7);
		monthNameToMonthNumber.put("Month_8", 8);
		monthNameToMonthNumber.put("Month_9", 9);
		monthNameToMonthNumber.put("Month_10", 10);
		monthNameToMonthNumber.put("Month_11", 11);
		monthNameToMonthNumber.put("Month_12", 12);
	}

	public LtUtil() {

	}

	public static String removeBrackets(String s) {
		// for square brackets, leave then contents behind.
		// [SEALED] MOTION ==> SEALED MOTION
		s = s.replaceAll("\\[|\\]", "");
		// for circle brackets, remove contents also.
		// by Zappos.com Inc. (Moore, David) ==> by Zappos.com Inc.
		return s.replaceAll("\\s*\\([\\p{Alnum}\\p{Punct}\\p{Blank}&&[^(]]*?\\)\\s*", " ");
	}

	public void setPrint(boolean b) {
		flagPrint = b;
	}

	public void printPhrase(Phrase ph) {
		if (flagPrint) {
			System.out.println(ph.pprint("", false));
			//			log.debug(ph.pprint("", false));
		}
	}

	public static int getMonthNumber(String monthName) {
		return monthNameToMonthNumber.get(monthName);
	}

	public void print(String s) {
		if (flagPrint) {
			log.debug(s);
		}
	}

}
