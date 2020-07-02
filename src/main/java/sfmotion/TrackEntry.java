package sfmotion;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.Role;
import sftrack.Phrase;
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

	static final Pattern pSec = Pattern.compile("\\b(filed by|as to)\\b", Pattern.CASE_INSENSITIVE);

	public String text;
	public String raw;
	public String sdate;
	public Date date;
	public String filer;
	public String type;
	public String transactionID;
	public Map<String, Object> items;
	public Role role; // plaintiff, defendant, petitioner, respondant, cross-defendant, cross-complainant, INTERVENOR, court, other or unknown
	// COMPLAINT:CPL; SUMMONS:SMS; MOTION:MTN; OPPOSITION:OPP;MEMORANDUM:MEM;ORDER:ORD;DECLARATION:DCL;REPLY:RPL;
	// CASE MANAGEMENT CONFERENCE:CMC; CASE MANAGEMENT STATEMENT:CMS; DEMURRER:DMR;NOTICE OF MOTION:NMN;
	// HEARING:HRG; ANSWER:ANS;
	public List<Section> sections = new ArrayList<>();
	List<String> noUse; //SUMMONS use this one;

	public TrackEntry(String _d, String _t) {
		raw = _t;
		text = _t.replaceAll("\\(TRANSA.+?\\)", "").replaceAll("\\(Fee.+?\\)", "").replaceAll("\\(SEALED.+?\\)", "").replaceAll("\\(SLAPP\\)", "");
		text = text.replaceAll("\\\"", "");
		//(TRANSACTION ID # 60057326)(Fee:$900.00)(SEALED DOCUMENT)
		sdate = _d;
		date = Date.valueOf(sdate);
		//		Section s00 = new Section(text, 0);
		//		sections.add(s00);
		Matcher m = pSec.matcher(text);
		int start = 0;
		do {
			if (m.find(start + 4)) { // 4 < "AS TO".length();
				int offset = m.start();
				Section s0 = new Section(text.substring(start, offset), start);
				sections.add(s0);
				start = offset;
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
		return date + "\t" + text;
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
	//SUMMONS ON COMPLAINT (TRANSACTION ID # 60057326), PROOF OF SERVICE ONLY, FILED BY PLAINTIFF BARRIENTOS, CLAUDIA CRUZ SERVED JAN-06-2017, PERSONAL SERVICE AS TO DEFENDANT YOUNG, LILLIE PEARL
	//SUMMONS ON COMPLAINT FILED BY PLAINTIFF STRONG, PAUL SERVED MAR-14-2017, POSTING AND MAILING AS TO DEFENDANT TAYLOR, DAVID
	static final String regSummons1 = "^SUMMONS\\s*ON\\s*COMPLAINT\\,?\\s*(?<transactionID>\\(TRANSACTION\\s*ID\\s*\\#\\s*\\d+\\))?\\,?\\s*(?<prejudgmentClaim>PREJUDGMENT\\sCLAIM\\sOF\\sRIGHT\\sOF\\sPOSSESSION\\,?\\s?)?(?<pos>PROOF\\s*OF\\s*SERVICE\\s*(ONLY)?)?.+?FILED\\s*BY\\s*(?<filer>.+?)"
			+ "\\s(SERVED\\s*(ON\\s)?(?<serveDate>\\w{3,10}-\\d\\d-\\d{2,4})\\,\\s*)" + "(?<serviceMethod>.+?\\s*)AS\\s*TO\\s(?<receiver>.+?)$";
	static final Pattern pSummons1 = Pattern.compile(regSummons1, Pattern.CASE_INSENSITIVE);
	static final String regSummons2 = "^SUMMONS\\s*ISSUED\\s*(ON\\s*(?<document>.+?))?TO(?<receiver>.+?)$";
	static final String sreg = "\\;|\\s/\\s|(?=(PROOF\\s+OF\\s+SERVICE|\\(Fee.{3,10}?\\)|\\[ORIGINALLY\\sFILED|\\(TRANSACTION\\sID\\s.{6,20}?\\)|SERVED\\s(ON\\s+)?\\w{3,10}-\\d\\d-\\d{2,4}|(PERSONAL|SUBSTITUTE)\\s*SERVICE|\\bAS\\sTO\\b|(?<!AS\\s)TO\\sDEFENDANT|PREJUDGMENT\\sCLAIM|MAIL\\sAND\\sACK|FILED\\s+BY))";

	static final Pattern pSummons2 = Pattern.compile(regSummons2, Pattern.CASE_INSENSITIVE);

	public boolean analyze() {
		Matcher m = pPos.matcher(text);
		if (m.find()) {
			type = PROOFOFSERVICE;
			return true;
		}
		m = pobjection.matcher(text);
		if (m.find()) {
			type = OBJECTION;
			return true;
		}
		if (!text.startsWith("SUMMONS")) {
			return false;
		}
		Map<String, Object> _items = new TreeMap<>();
		m = pSummons1.matcher(text);
		if (m.find()) {
			String item;
			_items.put("document", "COMPLAINT");
			item = m.group("filer");
			if (item != null) {
				_items.put("filer", item);
			}
			item = m.group("receiver");
			if (item != null) {
				_items.put("receiver", item);
			}
			item = m.group("serveDate");
			if (item != null) {
				_items.put("serveDate", item);
			}
			item = m.group("transactionID");
			if (item != null) {
				_items.put("transactionID", item);
			}
			item = m.group("prejudgmentClaim");
			if (item != null) {
				_items.put("prejudgmentClaim", item);
			}
			item = m.group("pos");
			if (item != null) {
				_items.put("pos", item);
			}
			item = m.group("serviceMethod");
			if (item != null) {
				_items.put("serviceMethod", item);
			}
			type = SUMMONS;
			items = _items;
			return true;
		} else {
			m = pSummons2.matcher(text);
			if (m.find()) {
				String item = m.group("document");
				if (item != null) {
					_items.put("document", item);
				}
				item = m.group("receiver");
				if (item != null) {
					_items.put("receiver", item);
				}
				type = SUMMONS;
				items = _items;
				return true;
			} else {
				List<String> _noUse = new ArrayList<>();
				String[] splits = text.split(sreg);
				boolean bready = false;
				if (splits.length > 1)
					bready = true;
				for (String p : splits) {
					p = p.trim();
					if (p.startsWith("SUMMONS")) {// SUMMONS ON COMPLAINT,
						int id = p.indexOf(" ON ");
						if (id > 0) {
							_items.put("document", p.substring(id + 4));
						}
					} else if (p.startsWith("PROOF OF SERVICE")) {//PROOF OF SERVICE ONLY,
						_items.put("pos", p);
					} else if (p.startsWith("PERSONAL") || p.startsWith("SUBSTITUTE") || p.startsWith("MAIL") || p.startsWith("POSTING")) {//SERVED OCT-31-2016, 
						_items.put("serviceMethod", p);
					} else if (p.startsWith("SERVED")) {
						_items.put("serveDate", p);
					} else if (p.startsWith("[ORIGINALLY")) {//[ORIGINALLY FILED NOV-02-16] 
						_items.put("originalFile", p);
					} else if (p.startsWith("FILED BY")) {//FILED BY PLAINTIFF WILSON, MICHAEL GEARY 
						_items.put("filer", p.substring(1 + "FILED BY".length()).trim());
					} else if (p.startsWith("AS TO")) {// DEFENDANT HAYNES, BRIAN
						_items.put("receiver", p.substring(5).trim());
					} else if (p.startsWith("(TRANSACT")) {//(TRANSACTION ID # 60107832),
						_items.put("transactionID", p);
					} else if (p.startsWith("TO ")) {
						_items.put("receiver", p.substring(3).trim());
					} else {
						_noUse.add(p);
					}
				}
				if (bready) {
					type = SUMMONS;
					items = _items;
					noUse = _noUse;
					return true;
				}
			}
		}
		return false;

		if ((e = MotionEntry.parse(e)) != null) {
			return e;
		}
		if ((e = MemorandumEntry.parse(e)) != null) {
			return e;
		}
		if ((e = AnswerEntry.parse(e)) != null) {
			return e;
		}
		if ((e = HearingEntry.parse(e)) != null) {
			return e;
		}
		if ((e = OppositionEntry.parse(e)) != null) {
			return e;
		}
		if ((e = OrderEntry.parse(e)) != null) {
			return e;
		}
		if ((e = DeclarationEntry.parse(e)) != null) {
			return e;
		}
		if ((e = ReplyEntry.parse(e)) != null) {
			return e;
		}
		if ((e = CaseManagementConferenceEntry.parse(e)) != null) {
			return e;
		}
		if ((e = CaseManagementStatementEntry.parse(e)) != null) {
			return e;
		}
		if ((e = CaseSettledEntry.parse(e)) != null) {
			return e;
		}
		if ((e = DismissalEntry.parse(e)) != null) {
			return e;
		}
		if ((e = MinutesEntry.parse(e)) != null) {
			return e;
		}
		if ((e = JudgmentEntry.parse(e)) != null) {
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
		return new TrackEntry(_sdate, _text);
		//		String[] split = entries.get(i).text.split("FILED BY");
		//		if (split.length >= 2) {
		//			names.add(split[1].replaceAll("^\\W+|\\W+$", ""));
		//		}

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

		Section(String _t, int _offset) {
			text = _t;
			offset = _offset;
		}
	}

}
