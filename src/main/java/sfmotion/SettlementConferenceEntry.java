package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettlementConferenceEntry {
	static final String settlementConference = "SETTLEMENT CONFERENCE";
	static final Pattern psettlementConference = Pattern.compile(settlementConference, Pattern.CASE_INSENSITIVE);

	public SettlementConferenceEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = psettlementConference.matcher(e.text);
		if (m.find()) {
			SettlementConferenceEntry entry = new SettlementConferenceEntry();
			e.setType(TrackEntry.SETTLECONFERENCE);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}

}
