package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComplaintEntry extends SFMotionEntry {
	static final String regComplaint = "^(?<caseType>.+?)" + "(COMPLAINT\\s*.+?FILED\\s*BY\\s*PLAINTIFFS?)(?<plaintiffs>.+?)" + "(AS\\s*TO\\s*DEFENDANTS?(?<defendants>.+?))"
			+ "(?<summons>SUMMONS\\s*ISSUED\\,?\\s*)" + "(?<coverSheet>JUDICIAL\\s*COUNCIL\\s*CIVIL\\s*CASE\\s*COVER\\s*SHEETS?\\s*FILED\\s*)?"
			+ "(CASE\\s*MANAGEMENT\\s*CONFERENCE\\s*SCHEDULED\\s*FOR\\s*(?<caseManagementConferenceDate>\\w{3,10}-\\d\\d-\\d{2,4})\\s*)?"
			+ "(PROOF\\s*OF\\s*SERVICE\\s*DUE\\s*ON\\s*(?<posDate>\\w{3,10}-\\d\\d-\\d{2,4})\\s*)?"
			+ "(CASE\\s*MANAGEMENT\\s*STATEMENT\\s*DUE\\s*\\s*ON\\s*(?<caseManageStatementDate>\\w{3,10}-\\d\\d-\\d{2,4})\\s*)?" + "(\\(Fee\\:(?<fee>.+?)\\))?";
	static final Pattern pComplaint = Pattern.compile(regComplaint, Pattern.CASE_INSENSITIVE);
	String caseType; // some SF cases specify case type at the beginning of complaint docket entry 
	String plaintiffs; // complaint are always filed by plaintiffs
	String defendants; // complaint entry in SF usually include list of defendants
	String summons; // whether summons have been issued. Sometimes have multiple separate docket entries on summons issued, one for each defendant
	String coverSheet; // civil case coversheet accompany complaint. SF docket usually mentions this fact in the complaint entry
	String caseManagementConferenceDate; // scheduled case management date. Should happen on this date, if not, an entry indicating change should appear before this date.
	String posDate; // required proof of service date. Must be after this entry date. 
	String caseManageStatementDate; // scheduled case management statement filing date
	String fee; // a fee of $450 is charged for filing complaint, and mentioned at the end of complaint docket entry
	String filer;
	String role;

	public ComplaintEntry(String _sdate, String _text) {
		super(_sdate, _text, COMPLAINT);
		parse();
	}

	boolean parse() {
		Matcher m = pComplaint.matcher(text);
		if (m.find()) {
			caseType = m.group("caseType").trim();
			plaintiffs = m.group("plaintiffs").trim();
			filer = plaintiffs; // filer is a field in Entry, parent class
			role = "PLAINTIFF"; // role is a field in Entry, parent class
			defendants = m.group("defendants").trim();
			summons = m.group("summons");
			coverSheet = m.group("coverSheet");
			caseManagementConferenceDate = m.group("caseManagementConferenceDate");
			posDate = m.group("posDate");
			caseManageStatementDate = m.group("caseManageStatementDate");
			fee = m.group("fee");
			return true;
		}
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(text + "\n");
		sb.append("\t" + "caseType: " + caseType + "\n");
		sb.append("\t" + "plaintiffs: " + plaintiffs + "\n");
		sb.append("\t" + "defendants: " + defendants + "\n");
		sb.append("\t" + "summons: " + summons + "\n");
		sb.append("\t" + "coverSheet: " + coverSheet + "\n");
		sb.append("\t" + "caseManagementConferenceDate: " + caseManagementConferenceDate + "\n");
		sb.append("\t" + "posDate: " + posDate + "\n");
		sb.append("\t" + "caseManageStatementDate: " + caseManageStatementDate + "\n");
		sb.append("\t" + "fee: " + fee + "\n");
		return sb.toString();
	}

}
