package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinutesEntry extends TrackEntry {
	//	Line 22913: CA_SFC_479414	2017-05-17	MINI MINUTES FOR MAY-17-2017 01:30 PM FOR DEPT 501
	//	Line 22920: CA_SFC_479414	2017-05-22	MINUTES FOR MAY-22-2017 10:45 AM
	static final String minutes = "^(MINI )MINUTES";
	static final Pattern pMinutes = Pattern.compile(minutes, Pattern.CASE_INSENSITIVE);

	public MinutesEntry(String _sdate, String _text) {
		super(_sdate, _text, MIMUTES);
	}

	public static MinutesEntry parse(String _sdate, String _text) {
		Matcher m = pMinutes.matcher(_text);
		if (m.find()) {
			MinutesEntry entry = new MinutesEntry(_sdate, _text);
			return entry;
		}
		return null;
	}
}
