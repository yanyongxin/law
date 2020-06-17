package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OppositionEntry extends SFMotionEntry {
	static final String regOpposition1 = "^OPPOSITION\\s((AND\\s)?(RESPONSE|OPPOSITION|OBJECTION)\\s)?(OF\\s(?<filer1>.+?))?TO\\s*(?<opposingParty>.+?)??(?<motion>(?<motionType>MOTION|DEMU*RRER|MIL|MTN|PETITION|EX PARTE ORDER|(THE\\s)?JUDGMENT|REQUEST|OBJECTION|NOTICE|(EX\\s*PARTE\\s)?APP(LICATION)?).+?)(?<transactionID>\\(TRANSACTION\\s*ID\\s*\\#\\s*\\d+\\))?\\,?\\s*FILED\\s*BY\\s*(?<filer>.+?)$";
	static final Pattern pOpposition1 = Pattern.compile(regOpposition1, Pattern.CASE_INSENSITIVE);
	static final Pattern pOps = Pattern.compile(
			"^(?!PROOF OF .{0,20}?SERVICE|JOINDER|EXHIBIT|ERRATA|MEMORANDUM|STATEMENT|DECLARATION|POS|REQUEST).{0,40}(?<!( ISO | I?N (SUPPORT OF )?|REPLY).{0,40}?)\\bOPPOS",
			Pattern.CASE_INSENSITIVE);
	static final Pattern pTrans = Pattern.compile("\\d+");
	static final Pattern pMtn = Pattern.compile("\\bMIL\\b|(MOTIONS?|MTN)\\s(TO|FOR|IN|OF|\\w+ING)\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern pDemurrApp = Pattern
			.compile("(?<motion>(?<motionType>MOTION|DEMU*RRER|MIL|MTN|PETITION|EX PARTE ORDER|(THE\\s)?JUDGMENT|REQUEST|OBJECTION|NOTICE|(EX\\s*PARTE\\s)?APP(LICATION)?))", Pattern.CASE_INSENSITIVE);
	static final String sreg = "(?=\\(TRANSA|FILED\\sBY)";
	static final String testStrings[] = {
			"OPPOSITION TO DEFENDANT NYRENE HOWARDS MOTIONS IN LIMINE NOS 118 (TRANSACTION ID # 100044287) FILED BY PLAINTIFF HOWARD, SCOTT AN INDIVIDUAL",
	};

	public OppositionEntry(String _sdate, String _text) {
		super(_sdate, _text, OPPOSITION);
	}

	public static void main(String[] args) throws IOException {
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
			OppositionEntry ct = OppositionEntry.parse(splits[1], splits[2]);
			if (ct != null) {
				wr.write(ct.toString() + "\n");
			} else {
				wr2.write(line + "\n");
			}
		}
		br.close();
		wr.close();
		wr2.close();
	}

	static boolean testOppositions(String s) {
		Matcher m = pOpposition1.matcher(s);
		if (m.find())
			return true;
		return false;
	}

	public boolean isToMIL() {
		String motionType = (String) this.getItem("motionType");
		if (motionType == null) {
			return false;
		}
		if (motionType.equalsIgnoreCase("MOTION")) {
			String motion = (String) this.getItem("motion");
			if (motion == null)
				return false;
			if (MotionEntry.containsMIL(motion)) {
				return true;
			}
		}
		return false;
	}

	static public OppositionEntry parse(String _sdate, String _text) {
		//		if (_text.startsWith("OPPOSITION TO MTN FOR SUMMARY JUDGMENT FILED BY DEFENDANT BANKS, JAMES D.")) {
		//			System.out.print("");
		//		}
		Matcher m = pOps.matcher(_text);
		if (!m.find()) {
			return null;
		}
		OppositionEntry entry = new OppositionEntry(_sdate, _text);
		m = pOpposition1.matcher(_text);
		if (m.find()) {
			String item;
			item = m.group("filer");
			if (item != null) {
				entry.storeItem("filer", item);
				entry.setFiler(item);
			}
			item = m.group("opposingParty");
			if (item != null) {
				entry.storeItem("opposingParty", item);
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
			item = m.group("filer1");
			if (item != null) {
				entry.storeItem("filer1", item);
			}
			return entry;
		} else {
			String[] splits = _text.split(sreg);
			for (String s : splits) {
				if (s.startsWith("FILED BY")) {
					String filer = s.substring("FILED BY".length()).trim();
					entry.setFiler(filer);
				} else if (s.startsWith("SUBMITTED BY")) {
					String filer = s.substring("SUBMITTED BY".length()).trim();
					entry.setFiler(filer);
				} else if (s.startsWith("(TRANS")) {
					Matcher tr = pTrans.matcher(s);
					if (tr.find()) {
						String tid = tr.group();
						entry.transactionID = tid;
					}
				} else {
					Matcher mt = pMtn.matcher(s);
					if (mt.find()) {
						String mtn = s.substring(mt.start());
						String[] sp = mtn.split("\\;");
						if (sp.length > 1) {
							mtn = sp[0];
						}
						entry.storeItem("motion", mtn);
						entry.storeItem("motionType", "MOTION");
					} else {
						Matcher mda = pDemurrApp.matcher(s);
						if (mda.find()) {
							String motion = mda.group("motion");
							String[] sp = motion.split("\\;");
							if (sp.length > 1) {
								motion = sp[0];
							}
							String motionType = mda.group("motionType");
							entry.storeItem("motion", motion);
							entry.storeItem("motionType", motionType);
						}
					}
				}
			}
		}
		return entry;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Opposition\t" + date + "\t");
		sb.append(text + "\n");
		if (items != null)
			for (String key : items.keySet()) {
				sb.append("\t\t\t" + key + ": " + items.get(key) + "\n");
			}
		return sb.toString();
	}

}
