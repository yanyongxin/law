package sfmotion;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComplaintEntry extends TrackEntry {
	static final String regComplaint = "^(?<caseType>.+?)" + "(COMPLAINT\\s*.+?FILED\\s*BY\\s*PLAINTIFFS?)(?<plaintiffs>.+?)"
			+ "(AS\\s*TO\\s*DEFENDANTS?(?<defendants>.+?))"
			+ "(?<summons>SUMMONS\\s*ISSUED\\,?\\s*)" + "(?<coverSheet>JUDICIAL\\s*COUNCIL\\s*CIVIL\\s*CASE\\s*COVER\\s*SHEETS?\\s*FILED\\s*)?"
			+ "(CASE\\s*MANAGEMENT\\s*CONFERENCE\\s*SCHEDULED\\s*FOR\\s*(?<caseManagementConferenceDate>\\w{3,10}-\\d\\d-\\d{2,4})\\s*)?"
			+ "(PROOF\\s*OF\\s*SERVICE\\s*DUE\\s*ON\\s*(?<posDate>\\w{3,10}-\\d\\d-\\d{2,4})\\s*)?"
			+ "(CASE\\s*MANAGEMENT\\s*STATEMENT\\s*DUE\\s*\\s*ON\\s*(?<caseManageStatementDate>\\w{3,10}-\\d\\d-\\d{2,4})\\s*)?" + "(\\(Fee\\:(?<fee>.+?)\\))?";
	static final String regPetition = "^((VERIFIED )?PETITION\\s*(?<caseType>.+?)?\\sFILED\\s*BY\\s*(PETITIONER|PLAINTIFF)S?)(?<plaintiffs>.+?)"
			+ "(AS\\s*TO\\s*(RESPONDENT|DEFENDANT|CLAIMANT|TRUSTOR|CONSERVATEE|MINOR|DECEDENT)S?(?<defendants>.+?))?"
			+ "(?<coverSheet>JUDICIAL\\s*COUNCIL\\s*CIVIL\\s*CASE\\s*COVER\\s*SHEETS?\\s*FILED\\s*)?"
			+ "(?<hearingSetFor>HEARING\\sSET\\sFOR\\s(?<month>\\w+)-(?<date>\\d\\d)-(?<year>\\d+)\\s+AT.+?IN DEP.+?\\d+\\s+)?"
			+ "(\\(Fee\\:(?<fee>.+?)\\))?$";
	static final Pattern pComplaint = Pattern.compile(regComplaint, Pattern.CASE_INSENSITIVE);
	static final Pattern pPetition = Pattern.compile(regPetition, Pattern.CASE_INSENSITIVE);
	String caseType; // some SF cases specify case type at the beginning of complaint docket entry 
	public String plaintiffs; // complaint are always filed by plaintiffs or petitioner
	public String defendants; // complaint entry in SF usually include list of defendants or respondents
	String summons; // whether summons have been issued. Sometimes have multiple separate docket entries on summons issued, one for each defendant
	String coverSheet; // civil case coversheet accompany complaint. SF docket usually mentions this fact in the complaint entry
	String caseManagementConferenceDate; // scheduled case management date. Should happen on this date, if not, an entry indicating change should appear before this date.
	String posDate; // required proof of service date. Must be after this entry date. 
	String caseManageStatementDate; // scheduled case management statement filing date
	String fee; // a fee of $450 is charged for filing complaint, and mentioned at the end of complaint docket entry
	String filer;
	String role;

	Date hearingdate;

	static String[] testcases = {
			"PETITION FOR CHANGE OF NAME, RESIDENCY VERIFIED, FILED BY PETITIONER WORCESTER, ALISON HILARY JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED (Fee:450.00)",
			"PETITION FILED BY PETITIONER DOUGLAS, ETHAN AS TO RESPONDENT STATE FARM MUTUAL AUTOMOBILE INSURANCE COMPANY (Fee:$450.00)",
			"PETITION FOR INJUNCTION PROHIBITING HARASSMENT FILED BY PLAINTIFF TONG, KONG MIN AS TO DEFENDANT SHEN, CHUN YAN LIANG, JUN JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED (Fee:IFP)",
			"PETITION FOR WRIT OF MANDATE FILED BY PETITIONER O'DORISIO MD, JAMES EDWARD AS TO RESPONDENT MEDICAL BOARD OF CALIFORNIA JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED (Fee:450.00)",
	};

	public static void main(String[] args) throws IOException {
		for (String s : testcases) {
			ComplaintEntry ce = new ComplaintEntry("2010-09-11", s);
			System.out.print(ce);
		}
	}

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
		m = pPetition.matcher(text);
		if (m.find()) {
			caseType = m.group("caseType");
			if (caseType != null)
				caseType = caseType.trim();
			plaintiffs = m.group("plaintiffs").trim();
			filer = plaintiffs; // filer is a field in Entry, parent class
			role = "PLAINTIFF"; // role is a field in Entry, parent class
			defendants = m.group("defendants");
			if (defendants != null)
				defendants = defendants.trim();
			coverSheet = m.group("coverSheet");
			String hearing = m.group("hearingSetFor");
			if (hearing != null) {
				String month = m.group("month");
				String sdate = m.group("date");
				String year = m.group("year");
				List<String> dlist = new ArrayList<>();
				dlist.add(month);
				dlist.add(sdate);
				dlist.add(year);
				hearingdate = Utils.getSqlDate(dlist);
			}
			fee = m.group("fee");
			return true;
		}
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(text + "\n");
		if (caseType != null)
			sb.append("\t" + "caseType: " + caseType + "\n");
		if (plaintiffs != null)
			sb.append("\t" + "plaintiffs: " + plaintiffs + "\n");
		if (defendants != null)
			sb.append("\t" + "defendants: " + defendants + "\n");
		if (summons != null)
			sb.append("\t" + "summons: " + summons + "\n");
		if (coverSheet != null)
			sb.append("\t" + "coverSheet: " + coverSheet + "\n");
		if (caseManagementConferenceDate != null)
			sb.append("\t" + "caseManagementConferenceDate: " + caseManagementConferenceDate + "\n");
		if (hearingdate != null)
			sb.append("\t" + "hearingdate: " + hearingdate + "\n");
		if (posDate != null)
			sb.append("\t" + "posDate: " + posDate + "\n");
		if (caseManageStatementDate != null)
			sb.append("\t" + "caseManageStatementDate: " + caseManageStatementDate + "\n");
		if (fee != null)
			sb.append("\t" + "fee: " + fee + "\n");
		return sb.toString();
	}

}
