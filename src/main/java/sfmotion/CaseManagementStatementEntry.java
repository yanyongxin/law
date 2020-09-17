package sfmotion;

import legal.TrackEntry;

public class CaseManagementStatementEntry {
	//CA_SFC_465568	2017-06-12	CASE MANAGEMENT STATEMENT (TRANSACTION ID # 100028006) FILED BY PLAINTIFF WONG, LARRY AN INDIVIDUAL JURY DEMANDED, ESTIMATED TIME FOR TRIAL: 7.0 DAYS
	static final String left = "(?=\\(TRANSACTION\\sID\\s.{6,40}?\\)|JURY DEMAND|FILED\\s+BY|ESTIMATED)";

	public CaseManagementStatementEntry() {
	}

	public static boolean parse(TrackEntry e) {
		if (!e.text.startsWith("CASE MANAGEMENT STATEMENT")) {
			return false;
		}
		CaseManagementStatementEntry entry = new CaseManagementStatementEntry();
		String[] splits = e.text.split(left);
		for (String s : splits) {
			if (s.startsWith("FILED")) {
				e.filer = s.trim();
			} else if (s.startsWith("JURY DEMAND")) {
				e.storeItem("jury", s.trim());
			} else if (s.startsWith("Estimat")) {
				e.storeItem("estimateTrialDates", s.trim());
			}
		}
		e.setType(TrackEntry.CASE_MANAGEMENT_STATEMENT);
		e.setTypeSpecific(entry);
		return true;
	}
}
