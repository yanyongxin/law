package sftrack;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseNumber {
	private static final Logger log = LoggerFactory.getLogger(CaseNumber.class);
	// 1:10-cv-00667-rk-sle
	// month:year-cv-serial[-initials]
	int month;
	int year;
	int serial;
	List<String> initials;
	int courtID;
	String raw;
	static Pattern ptn = Pattern.compile("(\\d\\d?)\\:(\\d\\d?)-?[a-z][a-z]-?(\\d{2,5})((-[a-z]{2,})*)", Pattern.CASE_INSENSITIVE);

	public static void main(String[] args) throws Exception {
		String test = args[0];
		CaseNumber cn = new CaseNumber(test);
		String tst = cn.toString();
		log.info(tst);
	}

	public CaseNumber(String casenumber) {
		raw = casenumber;
		Matcher m = ptn.matcher(casenumber);
		if (m.matches()) {
			month = Integer.parseInt(m.group(1));
			year = Integer.parseInt(m.group(2));
			serial = Integer.parseInt(m.group(3));
			if (m.groupCount() > 4) {
				initials = new ArrayList<String>();
				String initls = m.group(4);
				String[] split = initls.split("-");
				for (String s : split) {
					if (s.trim().length() > 1) {
						initials.add(s.trim());
					}
				}
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			CaseNumber cn = (CaseNumber) o;
			return (year == cn.year && serial == cn.serial);
		} else if (o.getClass().equals(String.class)) {
			String csnumful = (String) o;
			CaseNumber cno = new CaseNumber(csnumful);
			return equals(cno);
		}
		return false;
	}

}
