package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class AnswerEntry {
	//	Line 958: CA_SFC_464264	2017-12-12	ANSWER TO CROSS COMPLAINT (TRANSACTION ID # 17345056) FILED BY CROSS DEFENDANT LAMBERT/O'CONNOR DEVELOPMENT 450RI LLC (Fee:$450.00)
	//	Line 7220: CA_SFC_466231	2017-05-10	ANSWER TO 1ST AMENDED COMPLAINT (TRANSACTION ID # 100015495) FILED BY DEFENDANT BUCKLEY, CASSANDRA (Fee:$450.00)
	static final String answer = "^(AMENDED )?ANSWER";
	static final Pattern panswer = Pattern.compile(answer, Pattern.CASE_INSENSITIVE);

	public AnswerEntry() {
	}

	public static boolean parse(TrackEntry e) {
		Matcher m = panswer.matcher(e.text);
		if (m.find()) {
			AnswerEntry entry = new AnswerEntry();
			e.setTypeSpecific(entry);
			e.setType(TrackEntry.ANSWER);
			return true;
		}
		return false;
	}
}
