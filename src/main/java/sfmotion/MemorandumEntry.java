package sfmotion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class MemorandumEntry {
	static final String P_And_A = "(OF (POINTS? AND AUTHORITIES|P\\&?A)\\s)?";
	static final String C_AND_D = "OF COSTS AND DISBURSEMENTS.+?";
	static final String S_OR_O = "(?<relation>ISO|OPPOSING|-?IN (PARTIAL\\s)?(SUP*ORT? OF|OPPO\\w* TO|REPLY TO))";
	static final String author = "(?<author>.*?\\s)?";
	static final String motions = "(?<motion>(?<motionType>(JOINT|UNOPPOSED\\s*)?MOTION|PRELIMINARY INJUNCTION|NEW TRIAL|DEMU*RRER|MIL\\b|MTN|MSJ|OPPOSITION|STIPULATION|PETITION|EX PARTE(\\sORDER)?|(THE\\s)?JUDGMENT|REPLY|REQUEST|OBJECTION|(EX\\sPARTE\\s)?APP(LICATION)?).+?)";
	static final String paPath = P_And_A + S_OR_O + "\\s" + author + motions;
	static final String transID = "(?<transactionID>\\(TRANSACTION\\s*ID\\s*\\#\\s*\\d+\\))";
	static final String filer = "FILED BY(?<filer>.+?)";
	static final String regMemo1 = "MEMORANDUM\\s(?<path>(" + paPath + "|" + C_AND_D + ")\\s)" + transID + "?" + "\\s*" + filer + "$";
	static final String motionJr = "MOTION|PRELIMINARY INJUNCTION|NEW TRIAL|DEMU*RRER|MIL\\b|MTN|MSJ|(?<!IN\\s)OPPOSITION|STIPULATION|MEMO\\\\w* OF|PETITION|REPLY|REQUEST|OBJECTION|(EX\\sPARTE\\s)?APP(LICATION)?";
	static final Pattern pMotionJr = Pattern.compile(motionJr, Pattern.CASE_INSENSITIVE);
	static final Pattern pMemo1 = Pattern.compile(regMemo1, Pattern.CASE_INSENSITIVE);
	static final String sreg = "(?<=(SUPPORT OF|IN OPPOSITION (AND RESPONSE\\s)?(TO|OF)))|(?=(IN SUPPORT|IN OPPOSITION|FOR SANCTIONS|OF DEFENDANTS?|MEMO\\w* OF|OF PLAINTIFFS?|MOTION|PRELIMINARY INJUNCTION|NEW TRIAL|DEMU*RRER|MIL\\b|MTN|MSJ|(?<!IN\\s)OPPOSITION|STIPULATION|PETITIONS?\\b|REPLY|REQUEST|OBJECTION|EX\\sPARTE\\sAPP(LICATION)?|(?<!EX\\sPARTE\\s)APP(LICATION)?|\\(TRANSACTION\\sID\\s.{6,20}?\\)|FILED\\s+BY))";

	static final String testStrings[] = {
			"MEMORANDUM IN SUPPORT OF PLAINTIFFS MOTION TO SEAL MEMORANDUM FOR MOTION FOR JUDGMENT ON THE PLEADINGS (TRANSACTION ID # 100026359) FILED BY PLAINTIFF LEE, ANTHONY K",
			"MEMORANDUM OF POINTS AND AUTHORITIES IN SUPPORT OF PLAINTIFFS UNOPPOSED MOTION TO SEAL SECOND AMENDED COMPLAINT (TRANSACTION ID # 100023139) FILED BY PLAINTIFF LEE, ANTHONY K",
			"MEMORANDUM OF POINTS AND AUTHORITIES IN SUPPORT OF DEMURRER TO PLAINTIFF'S FIRST AMENDED COMPLAINT (TRANSACTION ID # 100028400) FILED BY DEFENDANT HOSIE, SPENCER AN INDIVIDUAL HOSIE RICE LLP RICE, DIANE",
	};

	List<String> splits = new ArrayList<>();
	List<String> useless = new ArrayList<>();

	public MemorandumEntry() {
	}

	static public boolean parse(TrackEntry e) {
		if (!e.text.startsWith("MEMORANDUM")) {
			return false;
		}
		MemorandumEntry entry = new MemorandumEntry();
		Matcher m = pMemo1.matcher(e.text);
		if (m.find()) {
			String item;
			item = m.group("filer");
			if (item != null) {
				e.storeItem("filer", item);
			}
			item = m.group("relation");
			if (item != null) {
				e.storeItem("relation", item);
			}
			item = m.group("motion");
			if (item != null) {
				e.storeItem("motion", item);
			}
			item = m.group("motionType");
			if (item != null) {
				e.storeItem("motionType", item);
			}
			item = m.group("transactionID");
			if (item != null) {
				e.storeItem("transactionID", item);
			}
			item = m.group("path");
			if (item != null) {
				e.storeItem("path", item);
			}
			e.setType(TrackEntry.MEMORANDUM);
			e.setTypeSpecific(entry);
			return true;
		} else {
			String[] sp = e.text.split(sreg);
			for (String p : sp) {
				p = p.trim();
				if (p.equals("OF")) {
					if (entry.splits.size() > 0) {
						String ppp = entry.splits.remove(entry.splits.size() - 1);
						entry.splits.add(ppp + " OF");
					} else {
						entry.splits.add(p);
					}
				} else {
					entry.splits.add(p);
				}
			}
			boolean bMotion = false;
			for (String p : entry.splits) {
				p = p.trim();
				if (p.length() == 0)
					continue;
				if (p.startsWith("(TRANSACTION")) {
					e.storeItem("transactionID", p);
				} else if (p.startsWith("FILED BY")) {
					String pp = p.substring("FILED BY".length()).trim();
					if (pp.length() > 0) {
						e.storeItem("filer", pp);
					}
				} else if (p.startsWith("IN SUPPORT") || p.startsWith("IN OPPOSITION")) {
					e.storeItem("relation", p);
				} else if (p.startsWith("FOR SANCTIONS")) {
					e.storeItem("relation", "FOR");
					e.storeItem("motion", p.substring(4));
				} else if (p.startsWith("OF DEFENDANT") || p.startsWith("OF PLAINTIFF")) {
					e.storeItem("author", p.substring(3));
				} else if (p.startsWith("DEFENDANT") || p.startsWith("PLAINTIFF")) {
					e.storeItem("author", p);
				} else {
					Matcher mm = pMotionJr.matcher(p);
					if (mm.find()) {
						int start = mm.start();
						if (start < 10) {
							if (!bMotion) {
								bMotion = true;
								e.storeItem("motion", p);
								e.storeItem("motionType", mm.group());
							} else {
								//	MEMORANDUM OF POINTS AND AUTHORITIES TO PLAINTIFFS EX PARTE APPLICATION FOR AN ORDER SHORTENING TIME ON A MOTION FOR LEAVE TO AMEND THE COMPLAINT (TRANSACTION ID # 61510119) FILED BY PLAINTIFF WONG, KINSON
								//		MEMORANDUM OF POINTS AND AUTHORITIES TO PLAINTIFFS 
								//		EX PARTE 
								//		APPLICATION FOR AN ORDER SHORTENING TIME ON A 
								//		MOTION FOR LEAVE TO AMEND THE COMPLAINT 
								//		(TRANSACTION ID # 61510119) 
								//		FILED BY PLAINTIFF WONG, KINSON
								String mtn = (String) e.items.get("motion");
								mtn = mtn + " " + p;
								e.storeItem("motion", mtn);
							}
						} else {
							entry.useless.add(p);
						}
					} else {
						if (!p.startsWith("MEMORANDUM")) {
							entry.useless.add(p);
						}
					}
				}
			}
			e.setTypeSpecific(entry);
			e.setType(TrackEntry.MEMORANDUM);
			return true;
		}
	}

	public String toLongString() {
		StringBuilder sb = new StringBuilder();
		//		sb.append(text + "\n");
		//		for (String key : items.keySet()) {
		//			sb.append("\t" + key + ": " + items.get(key) + "\n");
		//		}
		if (splits != null) {
			sb.append("splits:\n");
			for (String s : splits) {
				s = s.trim();
				if (s.length() == 0)
					continue;
				sb.append("\t" + s + "\n");
			}
		}
		if (useless.size() > 0) {
			sb.append("useless:\n");
			for (String s : useless) {
				s = s.trim();
				if (s.length() == 0)
					continue;
				sb.append("\t" + s + "\n");
			}
		}
		return sb.toString();
	}
}
