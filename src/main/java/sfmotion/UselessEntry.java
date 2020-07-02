package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UselessEntry extends TrackEntry {
	static final String useless = "^(PAYMENT|COURT REPORTING SERVICE|JURY FEES|NOTICE TO PLAINTIFF|NOTICE OF CHANGE OF (ADDRESS|FIRM NAME|HANDLING ATTORNEY|NOTICE OF INTENT)|.{0,6}\\bFEES? PAID|SUBSTITUTION OF ATTORNEY)";
	static final Pattern puseless = Pattern.compile(useless, Pattern.CASE_INSENSITIVE);

	public UselessEntry(String _sdate, String _text) {
		super(_sdate, _text, USELESS);
	}

	public static UselessEntry parse(String _sdate, String _text) {
		Matcher m = puseless.matcher(_text);
		if (m.find()) {
			UselessEntry entry = new UselessEntry(_sdate, _text);
			return entry;
		}
		return null;
	}

}
