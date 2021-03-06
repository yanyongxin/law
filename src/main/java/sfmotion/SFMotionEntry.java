package sfmotion;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.Role;

public class SFMotionEntry implements Comparable<SFMotionEntry> {
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

	public String text;
	public String sdate;
	public Date date;
	public String filer;
	public Role role; // plaintiff, defendant, petitioner, respondant, cross-defendant, cross-complainant, INTERVENOR, court, other or unknown
	public String type;
	public String transactionID;
	public Map<String, Object> items;
	// COMPLAINT:CPL; SUMMONS:SMS; MOTION:MTN; OPPOSITION:OPP;MEMORANDUM:MEM;ORDER:ORD;DECLARATION:DCL;REPLY:RPL;
	// CASE MANAGEMENT CONFERENCE:CMC; CASE MANAGEMENT STATEMENT:CMS; DEMURRER:DMR;NOTICE OF MOTION:NMN;
	// HEARING:HRG; ANSWER:ANS;

	public SFMotionEntry(String _d, String _t) {
		text = _t;
		sdate = _d;
		date = Date.valueOf(sdate);
	}

	public SFMotionEntry(String _d, String _t, String _type) {
		text = _t;
		sdate = _d;
		date = Date.valueOf(sdate);
		type = _type;
	}

	@Override
	public int compareTo(SFMotionEntry o) {
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

	/*
	
	 */
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

	public String toTypeString() {
		String year = "" + (1900 + date.getYear());
		String month = "" + (1 + date.getMonth());
		if (month.length() == 1) {
			month = "0" + month;
		}
		String ssdate = "" + date.getDate();
		if (ssdate.length() == 1) {
			ssdate = "0" + ssdate;
		}
		if (type != null) {
			return type + "\t" + year + "-" + month + "-" + ssdate + "\t" + text;
		}
		return "\t\t" + year + "-" + month + "-" + ssdate + "\t" + text;
	}

	public String toString() {
		String year = "" + (1900 + date.getYear());
		String month = "" + (1 + date.getMonth());
		if (month.length() == 1) {
			month = "0" + month;
		}
		String ssdate = "" + date.getDate();
		if (ssdate.length() == 1) {
			ssdate = "0" + ssdate;
		}
		//		return year + "-" + month + "-" + ssdate + "\t" + text;
		StringBuilder sb = new StringBuilder();
		sb.append(type + "\t\t" + date + "\t" + text);
		return sb.toString();
	}

	public void setType(String _t) {
		type = _t;
	}

	public String getType() {
		return type;
	}

	static class EntryType {

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
	public static SFMotionEntry analyze(String _sdate, String _text) {
		SFMotionEntry e;
		if ((e = ProofOfServiceEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = ObjectionEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = SummonsEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = MotionEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = MemorandumEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = AnswerEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = HearingEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = OppositionEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = OrderEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = DeclarationEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = ReplyEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = CaseManagementConferenceEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = CaseManagementStatementEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = CaseSettledEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = DismissalEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = MinutesEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = JudgmentEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = UselessEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = NoticeOfTrialEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = SettlementConferenceEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = TrialEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		if ((e = ArbitrateEntry.parse(_sdate, _text)) != null) {
			return e;
		}
		return new SFMotionEntry(_sdate, _text);
		//		String[] split = entries.get(i).text.split("FILED BY");
		//		if (split.length >= 2) {
		//			names.add(split[1].replaceAll("^\\W+|\\W+$", ""));
		//		}

	}

}
