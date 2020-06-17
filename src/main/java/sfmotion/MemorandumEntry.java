package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MemorandumEntry extends SFMotionEntry {
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

	public static void main(String[] args) throws IOException {
		if (!testMemos(testStrings[0])) {
			System.exit(1);
		}
		if (args.length != 3) {
			System.out.println("args: infile outfile failfile");
			System.exit(-1);
		}
		String infile = args[0];
		String outfile = args[1];
		BufferedReader br = new BufferedReader(new FileReader(infile));
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(args[2]));
		String line;
		while ((line = br.readLine()) != null) {
			String[] splits = line.split("\\t");
			if (splits.length != 3) {
				continue;
			}
			MemorandumEntry ct = MemorandumEntry.parse(splits[1], splits[2]);
			if (ct != null) {
				wr.write(ct.toString() + "\n");
			} else {
				wr2.write(ct.toString() + "\n");
			}
		}
		br.close();
		wr.close();
		wr2.close();
	}

	public MemorandumEntry(String _sdate, String _text) {
		super(_sdate, _text, MEMORANDUM);
	}

	static boolean testMemos(String s) {
		Matcher m = pMemo1.matcher(s);
		if (m.find()) {
			String path = m.group("path");
			String relation = m.group("relation");
			String motion = m.group("motion");
			String motionType = m.group("motionType");
			String transactionID = m.group("transactionID");
			String filer = m.group("filer");
			return true;
		}
		return false;
	}

	static public MemorandumEntry parse(String _sdate, String _text) {
		if (!_text.startsWith("MEMORANDUM")) {
			return null;
		}
		MemorandumEntry entry = new MemorandumEntry(_sdate, _text);
		Matcher m = pMemo1.matcher(_text);
		if (m.find()) {
			String item;
			item = m.group("filer");
			if (item != null) {
				entry.storeItem("filer", item);
			}
			item = m.group("relation");
			if (item != null) {
				entry.storeItem("relation", item);
			}
			item = m.group("motion");
			if (item != null) {
				entry.storeItem("motion", item);
			}
			item = m.group("motionType");
			if (item != null) {
				entry.storeItem("motionType", item);
			}
			item = m.group("transactionID");
			if (item != null) {
				entry.storeItem("transactionID", item);
			}
			item = m.group("path");
			if (item != null) {
				entry.storeItem("path", item);
			}
			return entry;
		} else {
			String[] sp = _text.split(sreg);
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
					entry.storeItem("transactionID", p);
				} else if (p.startsWith("FILED BY")) {
					String pp = p.substring("FILED BY".length()).trim();
					if (pp.length() > 0) {
						entry.storeItem("filer", pp);
					}
				} else if (p.startsWith("IN SUPPORT") || p.startsWith("IN OPPOSITION")) {
					entry.storeItem("relation", p);
				} else if (p.startsWith("FOR SANCTIONS")) {
					entry.storeItem("relation", "FOR");
					entry.storeItem("motion", p.substring(4));
				} else if (p.startsWith("OF DEFENDANT") || p.startsWith("OF PLAINTIFF")) {
					entry.storeItem("author", p.substring(3));
				} else if (p.startsWith("DEFENDANT") || p.startsWith("PLAINTIFF")) {
					entry.storeItem("author", p);
				} else {
					Matcher mm = pMotionJr.matcher(p);
					if (mm.find()) {
						int start = mm.start();
						if (start < 10) {
							if (!bMotion) {
								bMotion = true;
								entry.storeItem("motion", p);
								entry.storeItem("motionType", mm.group());
							} else {
								//	MEMORANDUM OF POINTS AND AUTHORITIES TO PLAINTIFFS EX PARTE APPLICATION FOR AN ORDER SHORTENING TIME ON A MOTION FOR LEAVE TO AMEND THE COMPLAINT (TRANSACTION ID # 61510119) FILED BY PLAINTIFF WONG, KINSON
								//		MEMORANDUM OF POINTS AND AUTHORITIES TO PLAINTIFFS 
								//		EX PARTE 
								//		APPLICATION FOR AN ORDER SHORTENING TIME ON A 
								//		MOTION FOR LEAVE TO AMEND THE COMPLAINT 
								//		(TRANSACTION ID # 61510119) 
								//		FILED BY PLAINTIFF WONG, KINSON
								String mtn = (String) entry.items.get("motion");
								mtn = mtn + " " + p;
								entry.storeItem("motion", mtn);
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
			return entry;
		}
	}

	public String toLongString() {
		StringBuilder sb = new StringBuilder();
		sb.append(text + "\n");
		for (String key : items.keySet()) {
			sb.append("\t" + key + ": " + items.get(key) + "\n");
		}
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
