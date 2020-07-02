package sfmotion;

public class CaseManagementStatementEntry extends TrackEntry {
	//CA_SFC_465568	2017-06-12	CASE MANAGEMENT STATEMENT (TRANSACTION ID # 100028006) FILED BY PLAINTIFF WONG, LARRY AN INDIVIDUAL JURY DEMANDED, ESTIMATED TIME FOR TRIAL: 7.0 DAYS
	static final String left = "(?=\\(TRANSACTION\\sID\\s.{6,40}?\\)|JURY DEMAND|FILED\\s+BY|ESTIMATED)";

	public CaseManagementStatementEntry(String _sdate, String _text) {
		super(_sdate, _text, CASE_MANAGEMENT_STATEMENT);
	}

	public static CaseManagementStatementEntry parse(String _sdate, String _text) {
		if (!_text.startsWith("CASE MANAGEMENT STATEMENT")) {
			return null;
		}
		CaseManagementStatementEntry entry = new CaseManagementStatementEntry(_sdate, _text);
		String[] splits = entry.text.split(left);
		for (String s : splits) {
			if (s.startsWith("FILED")) {
				entry.filer = s.trim();
			} else if (s.startsWith("JURY DEMAND")) {
				entry.storeItem("jury", s.trim());
			} else if (s.startsWith("Estimat")) {
				entry.storeItem("estimateTrialDates", s.trim());
			}
		}
		return entry;
	}
}
