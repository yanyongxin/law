package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArbitrateEntry {
	static final String arbitrate = "\\bARBITRAT";
	static final Pattern parbitrate = Pattern.compile(arbitrate, Pattern.CASE_INSENSITIVE);

	public ArbitrateEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = parbitrate.matcher(e.text);
		if (m.find()) {
			ArbitrateEntry entry = new ArbitrateEntry();
			e.setType(TrackEntry.ARBITRATE);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}
}
