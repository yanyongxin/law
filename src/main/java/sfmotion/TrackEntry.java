package sfmotion;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.Role;
import core.Phrase;
import utils.Pair;

public class TrackEntry implements Comparable<TrackEntry> {
	public static final String ANSWER = "ANSW";
	public static final String ARBITRATE = "ARBT";
	public static final String CASE_MANAGEMENT_CONFERENCE = "CMGC";
	public static final String CASE_MANAGEMENT_STATEMENT = "CMGS";
	public static final String COMPLAINT = "CPLT";
	public static final String COMPLAINTAC = "CPLTAC";
	public static final String NOTICEOFTRIAL = "NTRL";
	public static final String DECLARATION = "DECL";
	public static final String DEMURRER = "DMRR";
	public static final String DISMISSAL = "DMSL";
	public static final String HEARING = "HRNG";
	public static final String OBJECTION = "OBJN";
	public static final String JUDGMENT = "JGMT";
	public static final String MEMORANDUM = "MMRDM";
	public static final String MIMUTES = "MNTS";
	public static final String MOTION = "MOTN";
	public static final String NOTICE_OF_MOTION = "NMTN";
	public static final String OPPOSITION = "OPPS";
	public static final String ORDER = "ORDR";
	public static final String USELESS = "USLS";
	public static final String PROOFOFSERVICE = "POSV";
	public static final String REPLY = "RPLY";
	public static final String SETTLED = "STTL"; // settlement
	public static final String SETTLECONFERENCE = "STCF"; // settlement conference
	public static final String SUMMONS = "SMMS";
	public static final String TRIAL = "TRAL";

	static final Pattern pSec = Pattern.compile("\\b(filed by|as to|hearing set for|PROOF OF SERVICE)\\b", Pattern.CASE_INSENSITIVE);

	public String raw; // raw docket entry text
	public String text; // useful part of raw text, after remove junk, ignore stuff inside braces
	public String sdate; // filing date as a string "2018-12-24"
	public Date date; // filing date as java.sql.Date
	public String filer; // String after "filed by"
	public String type; // defined type constants above: MOTION = "MOTN", ORDER = "ORDR", etc..
	public String transactionID; // (TRANSACTION ID # 100009372)
	public Map<String, Object> items;
	public Role role; // defined in Role.java: plaintiff, defendant, petitioner, respondant, cross-defendant, cross-complainant, INTERVENOR, court, other or unknown
	// COMPLAINT:CPL; SUMMONS:SMS; MOTION:MTN; OPPOSITION:OPP;MEMORANDUM:MEM;ORDER:ORD;DECLARATION:DCL;REPLY:RPL;
	// CASE MANAGEMENT CONFERENCE:CMC; CASE MANAGEMENT STATEMENT:CMS; DEMURRER:DMR;NOTICE OF MOTION:NMN;
	// HEARING:HRG; ANSWER:ANS;
	public List<Section> sections = new ArrayList<>(); // one docket entry text can broken into multiple sections. See pSec
	List<String> noUse; //SUMMONS use this one; Portions of docket entry not used.
	public Object typeSpecific = null;// motions have motion-specific data structure, Hearings have hearing-specific data, etc..

	public TrackEntry(String _d, String _t) {
		raw = _t;
		text = _t.replaceAll("\\(TRANSA.+?\\)", "").replaceAll("\\(Fee.+?\\)", "").replaceAll("\\(SEALED.+?\\)", "").replaceAll("\\(SLAPP\\)", "");
		text = text.replaceAll("\\\"", "");
		//(TRANSACTION ID # 60057326)(Fee:$900.00)(SEALED DOCUMENT)
		sdate = _d;
		date = Date.valueOf(sdate);
		int len = text.length();
		//		sections.add(s00);
		Matcher m = pSec.matcher(text);
		int start = 0;
		do {
			if (start + 4 < len && m.find(start + 4)) { // 4 < "AS TO".length();
				int offset = m.start();
				Section s0 = new Section(text.substring(start, offset), start);
				sections.add(s0);
				start = offset;
				// Keep only the first, temporary
				break;
			} else {
				sections.add(new Section(text.substring(start), start));
				break;
			}
		} while (true);

	}

	public TrackEntry(String _d, String _t, String _type) {
		text = _t;
		sdate = _d;
		date = Date.valueOf(sdate);
		type = _type;
	}

	@Override
	public int compareTo(TrackEntry o) {
		if (o.date.after(this.date)) {
			return -1;
		} else if (o.date.before(date)) {
			return 1;
		}
		return 0;
	}

	public void storeItem(String key, Object value) {
		if (items == null) {
			items = new HashMap<>();
		}
		items.put(key, value);
	}

	public Object getItem(String key) {
		if (items == null) {
			return null;
		}
		return items.get(key);
	}

	public Date getDate() {
		return date;
	}

	void setTransactionID(String _id) {
		transactionID = _id;
	}

	//	static final Pattern pRoles = Pattern.compile("PLAINTIFF|PETITIONER|DEFENDANT|RESPONDENT|COMPLAINANT", Pattern.CASE_INSENSITIVE);
	static final String regRoles = "(?<role>PLAINTIFF|DEFENDANT|DEFT|CROSS( |-)(DEFENDANT|COMPLAINANT)|RESPONDENT|PETITIONER|INTERVENOR)(S'?|'S)?";
	static final Pattern pRoles = Pattern.compile(regRoles, Pattern.CASE_INSENSITIVE);

	public String toTypeString() {
		if (type != null) {
			//			return type + "\t" + year + "-" + month + "-" + ssdate + "\t" + text;
			return type + "\t" + date + "\t" + text;
		}
		return date + "\t" + text;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.toTypeString() + "\n");
		if (typeSpecific != null) {
			String s = typeSpecific.toString();
			if (s.length() > 0)
				sb.append(s + "\n");
			if (items != null)
				for (String key : items.keySet()) {
					sb.append("\t\t\t" + key + ": " + items.get(key) + "\n");
				}
		}
		return sb.toString();
	}

	public String toPrintString(String indent, int indentPlaintiff, int indentDefendant, int indentOther, int indentNo) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent);
		sb.append(sdate);
		if (role != null) {
			if (role.equals(Role.PLAINTIFF) || role.equals(Role.PETITIONER)) {
				for (int i = 0; i < indentPlaintiff; i++) {
					sb.append("\t");
				}
			} else if (role.equals(Role.DEFENDANT) || role.equals(Role.RESPONDENT)) {
				for (int i = 0; i < indentDefendant; i++) {
					sb.append("\t");
				}
			} else {
				for (int i = 0; i < indentOther; i++) {
					sb.append("\t");
				}
			}
		} else {
			for (int i = 0; i < indentNo; i++) {
				sb.append("\t");
			}
		}
		sb.append(text);
		return sb.toString();
	}

	void setFiler(String _filer) {
		filer = _filer;
		Matcher m = pRoles.matcher(_filer);
		if (m.find()) {
			String filerRole = m.group("role").toUpperCase().replace('-', ' ');
			if (filerRole != null && filerRole.startsWith("DEFT")) {
				filerRole = "DEFENDANT";
			}
			role = new Role(filerRole);
			filer = filer.substring(m.end()).replaceAll("^\\W|\\W$", "").trim();
		}
	}

	public void setType(String _t) {
		type = _t;
	}

	public String getType() {
		return type;
	}

	/**
	 * analyze each entry, generate a Map<String, Object> of properties and values
	 * 
	 * TO CHANGE: Needs multiple tags for some entries.
	 * 
	 * Changing order of the if's can change results.
	 * 
	 * @param entries
	 */
	static final String pos = "^(PROOF OF (\\w+\\s+)*SERVICE|POS)\\s";
	static final Pattern pPos = Pattern.compile(pos, Pattern.CASE_INSENSITIVE);
	static final String objection = "^OBJECTION";
	static final Pattern pobjection = Pattern.compile(objection, Pattern.CASE_INSENSITIVE);

	public static boolean analyze(TrackEntry e) {
		Matcher m = pPos.matcher(e.text);
		if (m.find()) {
			e.type = PROOFOFSERVICE;
			return true;
		}
		m = pobjection.matcher(e.text);
		if (m.find()) {
			e.type = OBJECTION;
			return true;
		}
		if (SummonsEntry.parse(e)) {
			return true;
		}
		if (MotionEntry.parse(e)) {
			return true;
		}
		if (MemorandumEntry.parse(e)) {
			return true;
		}
		if (AnswerEntry.parse(e)) {
			return true;
		}
		if (HearingEntry.parse(e)) {
			return true;
		}
		if (OppositionEntry.parse(e)) {
			return true;
		}
		if (OrderEntry.parse(e)) {
			return true;
		}
		if (DeclarationEntry.parse(e)) {
			return true;
		}
		if (ReplyEntry.parse(e)) {
			return true;
		}
		if (CaseManagementConferenceEntry.parse(e)) {
			return true;
		}
		if (CaseManagementStatementEntry.parse(e)) {
			return true;
		}
		if (CaseSettledEntry.parse(e)) {
			return true;
		}
		if (DismissalEntry.parse(e)) {
			return true;
		}
		if (MinutesEntry.parse(e)) {
			return true;
		}
		if (JudgmentEntry.parse(e)) {
			return true;
		}
		if (UselessEntry.parse(e)) {
			return true;
		}
		if (NoticeOfTrialEntry.parse(e)) {
			return true;
		}
		if (SettlementConferenceEntry.parse(e)) {
			return true;
		}
		if (TrialEntry.parse(e)) {
			return true;
		}
		if (ArbitrateEntry.parse(e)) {
			return true;
		}
		return false;
	}

	public void setTypeSpecific(Object o) {
		typeSpecific = o;
	}

	public Object getTypeSpecific() {
		return typeSpecific;
	}

	public static class DePhrase {
		public String text; // motherText.subString(start, end);
		public int start; // start byte offset
		public int end; // end byte offset
		public Object entity; // Attorney, Judge, Party

		public DePhrase(String _t, int _start, int _end, Object _e) {
			text = _t;
			start = _start;
			end = _end;
			entity = _e;
		}

		public String toString() {
			return text;
		}
	}

	public static class Section {
		public String text;
		public int offset; // offset from the docket entry text
		public List<Pair> doneList = new ArrayList<>();
		public List<DePhrase> dephrases = new ArrayList<>();
		public List<Phrase> plist;
		public Map<Integer, List<Phrase>> rpmap;

		Section(String _t, int _offset) {
			text = _t;
			offset = _offset;
		}

		public String toString() {
			return text;
		}
	}

}
