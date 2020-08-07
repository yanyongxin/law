package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//
public class ReplyEntry {
	//	static String regReplyInSupportMotion = "^REPLY\\s((MEMORANDUM|DECLARATION|BRIEF)\\s)?(OF\\s((DEFENDANT|PLAINTIFF)S?\\s)?.+?)?IN\\sSUPPORT\\sOF\\s((PLAINTIFF|DEFENDANT)\\'?S\\s)?(\\w+\\s)?(MOTION|DEMURRER|APPLICATION|REQUEST)";
	//	static final String sreg = "\\b(?=(MOTION\\s+(FOR|TO)|PROOF\\s+OF\\s+SERVICE|Fee.{3,10}?\\)|TRANSACTION\\sID\\s.{6,20}?\\)|HEARING\\s*SET\\s*FOR|MEMORANDUM\\s+(OF\\s+POINTS\\s+AND\\s+AUTHORITIES\\s+)IN\\s+SUPPORT|\\,\\s+POINTS\\s+AND\\s+AUTHORITIES|DECLARATION|FILED\\s+BY))";
	static final String sreg = "(?=\\(TRANSACTION|FILED\\s+BY)";
	static String regReplyInSupportMotion = "^REPLY\\s(?<document>(MEMORANDUM|DECLARATION|BRIEF)\\s)?(OF\\s(?<docuparty>(DEFENDANT|PLAINTIFF)S?\\s)?.+?)?(IN\\s(FURTHER\\s)?SUPPORT\\sOF|\\bISO)\\s(PLAINTIFF|DEFENDANT)?.*?(?<motion>MOTION|DEMURRER|APPLICATION|REQUEST|MTN)";
	static Pattern pReplyInSupportMotion = Pattern.compile(regReplyInSupportMotion, Pattern.CASE_INSENSITIVE);
	static String regReplyToOppo = "^REPLY\\sTO\\s(?<opposer>\\w+(\\'S)?\\s)*(NON-?)?(OPPOSITION|OPPOS?|RESPONSES?)\\s(?:TO|OF)\\s(?<motioner>.*)?(?<motion>MOTION|DEMUR+ER|APPLICATION|MTN)";
	static Pattern pReplyToOppo = Pattern.compile(regReplyToOppo, Pattern.CASE_INSENSITIVE);
	static String text1 = "REPLY MEMORANDUM IN SUPPORT OF PLAINTIFF/CROSS-DEFENDANTS SPECIAL MOTION";
	static String text2 = "REPLY TO PLAINTIFFS OPPOSITION TO DEFENDANT LYFT, INC.S MOTION TO STRIKE (TRANSACTION ID # 60540562) FILED BY DEFENDANT LYFT, INC.";
	static String text3 = "REPLY BRIEF IN SUPPORT OF REQUEST FOR TREBLE DAMAGES UNDER SFRO 37.10B";
	static String text4 = "REPLY TO OPPOSITION OF MOTION OF COE1 FOR PROTECTIVE ORDER (TRANSACTION ID # 100068424) FILED BY OTHER COE 1";

	// Entries contain "REPLY" are saved into a file
	//REPLY BRIEF TO DEFENDANT'S OPPOSITION TO PLAINTIFF'S MOTION
	// 
	static void test() {
		//Matcher m = pReplyInSupportMotion.matcher(text3);
		Matcher m = pReplyToOppo.matcher(text4);
		if (m.find()) {
			for (int i = 0; i <= m.groupCount(); i++) {
				System.out.println(i + "\t" + m.group(i));
			}
		}
		System.out.println(text2);
	}

	public ReplyEntry() {
	}

	public String toString() {// in the items
		return "";
		//		StringBuilder sb = new StringBuilder();
		//		sb.append("Reply\t\t" + date + "\t" + text + "\n");
		//		if (items != null) {
		//			String mt = (String) items.get("motion");
		//			if (mt != null) {
		//				sb.append("\t\t\tMotion\t" + mt + "\n");
		//			}
		//		}
		//		return sb.toString();
	}

	public static void main(String[] args) throws IOException {
		//		test();
		if (args.length != 4) {
			System.out.println("args: infile replyInSupportfile replyToOppofile notCapturedfile");
			System.exit(-1);
		}
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(args[1]));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(args[2]));
		BufferedWriter wr3 = new BufferedWriter(new FileWriter(args[3]));
		String line;
		while ((line = br.readLine()) != null) {
			Matcher m = pReplyInSupportMotion.matcher(line);
			if (m.find()) {
				wr1.write(line + "\n\n");
				String document = m.group("document");
				String docuparty = m.group("docuparty");
				int motionStart = m.start("motion");
				String motion = line.substring(motionStart);
				String[] mots = motion.split("\\(");
				wr1.write("document=" + document + "\n");
				wr1.write("partyType=" + docuparty + "\n");
				wr1.write("motion=" + mots[0] + "\n\n");
			} else {
				Matcher mm = pReplyToOppo.matcher(line);
				if (mm.find()) {
					wr2.write(line + "\n\n");
					String opposer = mm.group("opposer");
					String motioner = mm.group("motioner");
					int motionStart = mm.start("motion");
					String motion = line.substring(motionStart);
					String[] mots = motion.split("\\(");
					wr2.write("opposer=" + opposer + "\n");
					wr2.write("motioner=" + motioner + "\n");
					wr2.write("motion=" + mots[0] + "\n\n");
				} else {
					wr3.write(line + "\n");
				}
			}
		}
		wr1.close();
		wr2.close();
		wr3.close();
		br.close();
	}

	public static boolean parse(TrackEntry e) {
		if (!e.text.startsWith("REPLY")) {
			return false;
		}
		ReplyEntry entry = new ReplyEntry();
		Matcher m = pReplyInSupportMotion.matcher(e.text);
		if (m.find()) {
			String document = m.group("document");
			String docuparty = m.group("docuparty");
			int motionStart = m.start("motion");
			String motion = e.text.substring(motionStart);
			String[] mots = motion.split("\\(");
			e.storeItem("document", document);
			e.storeItem("docuparty", docuparty);
			e.storeItem("motion", mots[0]);
		} else {
			Matcher mm = pReplyToOppo.matcher(e.text);
			if (mm.find()) {
				String opposer = mm.group("opposer");
				String motioner = mm.group("motioner");
				int motionStart = mm.start("motion");
				String motion = e.text.substring(motionStart);
				String[] mots = motion.split("\\(");
				e.storeItem("opposer", opposer);
				e.storeItem("motioner", motioner);
				e.storeItem("motion", mots[0]);
			} else {
				// there are 84 not parsed
				String[] splits = e.text.split(sreg);
				int idx = splits[0].indexOf("MOTION");
				if (idx >= 0) {
					e.storeItem("motion", splits[0].substring(idx));
				}
				for (String s : splits) {
					if (s.startsWith("FILED BY")) {
						String filedby = s.substring("FILED BY".length() + 1);
						e.filer = filedby;
						break;
					}
					if (s.startsWith("SUBMITTED BY")) {
						String filedby = s.substring("SUBMITTED BY".length() + 1);
						e.filer = filedby;
						break;
					}
				}
			}
		}
		e.setType(TrackEntry.REPLY);
		e.setTypeSpecific(entry);
		return true;
	}
}
