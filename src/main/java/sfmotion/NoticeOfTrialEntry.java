package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class NoticeOfTrialEntry {
	static final String noticeOfTimeAndPlaceOfTrial = "^NOTICE OF TIME";
	static final Pattern pnoticeOfTimeAndPlaceOfTrial = Pattern.compile(noticeOfTimeAndPlaceOfTrial, Pattern.CASE_INSENSITIVE);

	public NoticeOfTrialEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = pnoticeOfTimeAndPlaceOfTrial.matcher(e.text);
		if (m.find()) {
			NoticeOfTrialEntry entry = new NoticeOfTrialEntry();
			e.setType(TrackEntry.NOTICEOFTRIAL);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}

}
