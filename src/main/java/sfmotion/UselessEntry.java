package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UselessEntry {
	static final String useless = "^(PAYMENT|COURT REPORTING SERVICE|JURY FEES|NOTICE TO PLAINTIFF|NOTICE OF CHANGE OF (ADDRESS|FIRM NAME|HANDLING ATTORNEY|NOTICE OF INTENT)|.{0,6}\\bFEES? PAID|SUBSTITUTION OF ATTORNEY)";
	static final Pattern puseless = Pattern.compile(useless, Pattern.CASE_INSENSITIVE);

	public UselessEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = puseless.matcher(e.text);
		if (m.find()) {
			UselessEntry entry = new UselessEntry();
			e.setType(TrackEntry.USELESS);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}

}
