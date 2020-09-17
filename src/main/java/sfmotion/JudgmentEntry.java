package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class JudgmentEntry {
	//	Line 10794: CA_SFC_468543	2019-06-27	NOTICE OF ENTRY OF JUDGMENT (TRANSACTION ID # 100075969)
	//	Line 6733: CA_SFC_466097	2017-11-21	JUDGMENT, DISSOLUTION OF MARRIAGE - STATUS ONLY, MARITAL STATUS ENDS SEP-19-2017
	static final String judgment = "^(NOTICE OF ENTRY OF )?JUDGMENT";
	static final Pattern pjudgment = Pattern.compile(judgment, Pattern.CASE_INSENSITIVE);

	public JudgmentEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = pjudgment.matcher(e.text);
		if (m.find()) {
			JudgmentEntry entry = new JudgmentEntry();
			e.setType(TrackEntry.JUDGMENT);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}

}
