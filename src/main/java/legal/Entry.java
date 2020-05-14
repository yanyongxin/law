package legal;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import utils.Pair;

public class Entry implements Comparable<Entry> {
	static final String ANSWER = "ANSW";
	static final String ARBITRATE = "ARBT";
	static final String CASE_MANAGEMENT_CONFERENCE = "CMGC";
	static final String CASE_MANAGEMENT_STATEMENT = "CMGS";
	static final String COMPLAINT = "CPLT";
	static final String COMPLAINTAC = "CPLTAC";
	static final String NOTICEOFTRIAL = "NTRL";
	static final String DECLARATION = "DECL";
	static final String DEMURRER = "DMRR";
	static final String DISMISSAL = "DMSL";
	static final String HEARING = "HRNG";
	static final String OBJECTION = "OBJN";
	static final String JUDGMENT = "JGMT";
	static final String MEMORANDUM = "MMRDM";
	static final String MIMUTES = "MNTS";
	static final String MOTION = "MOTN";
	static final String NOTICE_OF_MOTION = "NMTN";
	static final String OPPOSITION = "OPPS";
	static final String ORDER = "ORDR";
	static final String USELESS = "USLS";
	static final String PROOFOFSERVICE = "POSV";
	static final String REPLY = "RPLY";
	static final String SETTLED = "STTL"; // settlement
	static final String SETTLECONFERENCE = "STCF"; // settlement conference
	static final String SUMMONS = "SMMS";
	static final String TRIAL = "TRAL";

	public String text;
	public String raw;
	String sdate;
	Date date;
	String filer;
	String type;
	String transactionID;
	Map<String, Object> items;
	// COMPLAINT:CPL; SUMMONS:SMS; MOTION:MTN; OPPOSITION:OPP;MEMORANDUM:MEM;ORDER:ORD;DECLARATION:DCL;REPLY:RPL;
	// CASE MANAGEMENT CONFERENCE:CMC; CASE MANAGEMENT STATEMENT:CMS; DEMURRER:DMR;NOTICE OF MOTION:NMN;
	// HEARING:HRG; ANSWER:ANS;
	public List<Section> sections = new ArrayList<>();

	public Entry(String _d, String _t) {
		raw = _t;
		text = _t.replaceAll("\\(TRANSA.+?\\)", "").replaceAll("\\(Fee.+?\\)", "").replaceAll("\\(SEALED.+?\\)", "");
		//(TRANSACTION ID # 60057326)(Fee:$900.00)(SEALED DOCUMENT)
		sdate = _d;
		date = Date.valueOf(sdate);
		int offset = text.toLowerCase().lastIndexOf("filed by");
		if (offset > 0) {
			Section s0 = new Section(text.substring(0, offset), 0);
			Section s1 = new Section(text.substring(offset), offset);
			sections.add(s0);
			sections.add(s1);
		} else {
			sections.add(new Section(text, 0));
		}
	}

	public Entry(String _d, String _t, String _type) {
		text = _t;
		sdate = _d;
		date = Date.valueOf(sdate);
		type = _type;
	}

	@Override
	public int compareTo(Entry o) {
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

	Date getDate() {
		return date;
	}

	void setTransactionID(String _id) {
		transactionID = _id;
	}

	//	static final Pattern pRoles = Pattern.compile("PLAINTIFF|PETITIONER|DEFENDANT|RESPONDENT|COMPLAINANT", Pattern.CASE_INSENSITIVE);
	static final String regRoles = "(?<role>PLAINTIFF|DEFENDANT|DEFT|CROSS( |-)(DEFENDANT|COMPLAINANT)|RESPONDENT|PETITIONER|INTERVENOR)(S'?|'S)?";
	static final Pattern pRoles = Pattern.compile(regRoles, Pattern.CASE_INSENSITIVE);

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
		return year + "-" + month + "-" + ssdate + "\t" + text;
	}

	public void setType(String _t) {
		type = _t;
	}

	public String getType() {
		return type;
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
		int offset; // offset from the docket entry text
		public List<Pair> doneList = new ArrayList<>();
		public List<DePhrase> dephrases = new ArrayList<>();

		Section(String _t, int _offset) {
			text = _t;
			offset = _offset;
		}
	}
}
