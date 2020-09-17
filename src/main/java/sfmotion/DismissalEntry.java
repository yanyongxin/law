package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class DismissalEntry {//
	static final String entire = "DISMISSAL.{3,50}ENTIRE ACTION";
	static final Pattern pEntire = Pattern.compile(entire, Pattern.CASE_INSENSITIVE);
	static final String partial = "DISMISSAL.+?\\bAS TO (?<dismissedParty>.+?$)";
	static final Pattern pPartial = Pattern.compile(partial, Pattern.CASE_INSENSITIVE);
	static final String onfile = "DISMISSALS? ON FILE|\\.\\s*DISMISSALS? FILED";
	static final Pattern pOnfile = Pattern.compile(onfile, Pattern.CASE_INSENSITIVE);
	static final String complaint = "DISMISSAL (?<with>WITH(OUT)?) PREJUDICE (?<claim>(\\w+\\s+)+(COMPLAINT|CLAIM))?";
	static final Pattern pComplaint = Pattern.compile(complaint, Pattern.CASE_INSENSITIVE);
	static final String notice = "NOTICE OF ENTRY OF DISMISSAL";
	static final Pattern pNotice = Pattern.compile(notice, Pattern.CASE_INSENSITIVE);
	static final String left = "(?=\\(TRANSACTION\\sID\\s.{6,40}?\\)|JURY DEMAND|FILED\\s+BY|ESTIMATED)";

	public DismissalEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = pEntire.matcher(e.text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry();
			e.setType(TrackEntry.DISMISSAL);
			e.setTypeSpecific(entry);
			return true;
		}
		m = pPartial.matcher(e.text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry();
			e.setType(TrackEntry.DISMISSAL);
			e.setTypeSpecific(entry);
			return true;
		}
		m = pOnfile.matcher(e.text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry();
			e.setType(TrackEntry.DISMISSAL);
			e.setTypeSpecific(entry);
			return true;
		}
		m = pComplaint.matcher(e.text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry();
			e.setType(TrackEntry.DISMISSAL);
			e.setTypeSpecific(entry);
			return true;
		}
		m = pNotice.matcher(e.text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry();
			e.setType(TrackEntry.DISMISSAL);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}

}
