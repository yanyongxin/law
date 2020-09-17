
package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class CaseSettledEntry {
	static final String settle = "CASE SETTLED|NOTICE OF SETTLEMENT(?!\\s+CONFERENCE)";
	static final Pattern pSettle = Pattern.compile(settle, Pattern.CASE_INSENSITIVE);

	public CaseSettledEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = pSettle.matcher(e.text);
		if (m.find()) {
			CaseSettledEntry entry = new CaseSettledEntry();
			e.setType(TrackEntry.SETTLED);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}

}
