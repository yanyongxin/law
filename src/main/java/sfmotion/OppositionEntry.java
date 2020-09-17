package sfmotion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;
import sftrack.TrackCase.MotionLink;

public class OppositionEntry {
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

	public OppositionEntry() {
	}

	static boolean testOppositions(String s) {
		Matcher m = pOpposition1.matcher(s);
		if (m.find())
			return true;
		return false;
	}

	public boolean isToMIL(TrackEntry e) {
		String motionType = (String) e.getItem("motionType");
		if (motionType == null) {
			return false;
		}
		if (motionType.equalsIgnoreCase("MOTION")) {
			String motion = (String) e.getItem("motion");
			if (motion == null)
				return false;
			if (MotionEntry.containsMIL(motion)) {
				return true;
			}
		}
		return false;
	}

	static public boolean parse(TrackEntry e) {
		//		if (_text.startsWith("OPPOSITION TO MTN FOR SUMMARY JUDGMENT FILED BY DEFENDANT BANKS, JAMES D.")) {
		//			System.out.print("");
		//		}
		Matcher m = pOps.matcher(e.text);
		if (!m.find()) {
			return false;
		}
		OppositionEntry entry = new OppositionEntry();
		m = pOpposition1.matcher(e.text);
		if (m.find()) {
			String item;
			item = m.group("filer");
			if (item != null) {
				e.storeItem("filer", item);
				e.setFiler(item);
			}
			item = m.group("opposingParty");
			if (item != null) {
				e.storeItem("opposingParty", item);
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
			item = m.group("filer1");
			if (item != null) {
				e.storeItem("filer1", item);
			}
			e.setType(TrackEntry.OPPOSITION);
			e.setTypeSpecific(entry);
			return true;
		} else {
			String[] splits = e.text.split(sreg);
			for (String s : splits) {
				if (s.startsWith("FILED BY")) {
					String filer = s.substring("FILED BY".length()).trim();
					e.setFiler(filer);
				} else if (s.startsWith("SUBMITTED BY")) {
					String filer = s.substring("SUBMITTED BY".length()).trim();
					e.setFiler(filer);
				} else if (s.startsWith("(TRANS")) {
					Matcher tr = pTrans.matcher(s);
					if (tr.find()) {
						String tid = tr.group();
						e.transactionID = tid;
					}
				} else {
					Matcher mt = pMtn.matcher(s);
					if (mt.find()) {
						String mtn = s.substring(mt.start());
						String[] sp = mtn.split("\\;");
						if (sp.length > 1) {
							mtn = sp[0];
						}
						e.storeItem("motion", mtn);
						e.storeItem("motionType", "MOTION");
					} else {
						Matcher mda = pDemurrApp.matcher(s);
						if (mda.find()) {
							String motion = mda.group("motion");
							String[] sp = motion.split("\\;");
							if (sp.length > 1) {
								motion = sp[0];
							}
							String motionType = mda.group("motionType");
							e.storeItem("motion", motion);
							e.storeItem("motionType", motionType);
						}
					}
				}
			}
		}
		e.setType(TrackEntry.OPPOSITION);
		e.setTypeSpecific(entry);
		return true;
	}

	public List<MotionLink> mlinkCandidates = new ArrayList<>();

	public void addMotionLinkCandidate(MotionLink ml) {
		mlinkCandidates.add(ml);
	}

	public String toString() {// in the items
		return "";
		//		StringBuilder sb = new StringBuilder();
		//		//		if (items != null)
		//		//			for (String key : items.keySet()) {
		//		//				sb.append("\t\t\t" + key + ": " + items.get(key) + "\n");
		//		//			}
		//		return sb.toString();
	}

}
