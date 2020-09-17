package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class TrialEntry {
	static final String trial = "^TRIAL\\s";
	static final Pattern ptrial = Pattern.compile(trial, Pattern.CASE_INSENSITIVE);

	public TrialEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = ptrial.matcher(e.text);
		if (m.find()) {
			TrialEntry entry = new TrialEntry();
			e.setType(TrackEntry.TRIAL);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}
}
