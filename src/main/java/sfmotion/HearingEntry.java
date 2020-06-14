package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.Role;
import utils.Pair;

public class HearingEntry extends Entry {
	/**
	 * SF Hearing types:
	 * 
	 * 1. LAW AND MOTION:
	 * 2. DISCOVERY 302
	 * 3. REAL PROPERTY/HOUSING MOTION 501
	 * 4. TRIAL MOTION IN DEPT. 306
	 * 5. CASE MANAGEMENT CONFERENCE (not tracked)
	 * 
	 * Calendar events:
	 * 1. MASTER (MOTION|COURT|JURY) CALENDAR: 
	 * 2. UNCONTESTED CALENDAR
	 */
	static final Pattern pHearingEntries = Pattern.compile(
			"^(LAW.{1,6}MOTION|MOTION)|^DISCOVERY\\,?\\s*302|^(MASTER|UNCONTESTED).+?CALENDAR|REAL\\s*PROPERTY\\/HOUSING\\s*MOTION\\,?\\s*501|^TRIAL\\s*MOTION|^NOT REPORTED\\.|^HEARING",
			Pattern.CASE_INSENSITIVE);
	static final Pattern pMotionTitle = Pattern.compile("APPLICATION|MOTIONS?\\s+(TO|FOR|IN\\sLIMINE)|DEMURRERS?\\sTO.+?(COMPLAINT|ANSWER)", Pattern.CASE_INSENSITIVE);//|(EX\\\\sPARTE\\\\s)?APPLICATION
	static final Pattern pGrantDeny = Pattern.compile("((is|are)\\s)?(?<gd1>Granted|den(ying|ied)|MOOT|SUSTAIN(ED|ING)|OVERRUL(ED|ING))|(THE\\s*COURT\\s+(?<gd2>GRANTS|DENIES))",
			Pattern.CASE_INSENSITIVE);// sustain for demurrer
	static final Pattern pOffCalendar = Pattern.compile("(IS\\s)?OFF(-|\\s*)CALENDAR", Pattern.CASE_INSENSITIVE);
	//	static final Map<String, String> monthLookup = new HashMap<>();

	String motion;
	MotionEntry ms;
	List<Pair> gds = new ArrayList<>();
	// track changes in calendar:
	Date oldDate = null;
	Date newDate = null;
	boolean offCalendar = false;
	int subtype = MotionEntry.TYPE_UNKNOWN;
	Role authorRole = null;
	String authors = null;

	public HearingEntry(String _sdate, String _text) {
		super(_sdate, _text, HEARING);
		analyze();
		if (motion != null) {
			if (motion.startsWith("MOTION") || motion.startsWith("MTN")) {
				subtype = MotionEntry.TYPE_MOTION;
			} else if (motion.startsWith("APPL")) {
				subtype = MotionEntry.TYPE_APPLIC;
			} else if (motion.startsWith("DEMU")) {
				subtype = MotionEntry.TYPE_DEMURR;
			}
		}
		verifyOffCalendar();
	}

	private void verifyOffCalendar() {
		// sometimes "OFF CALENDAR" captured refers to something else other than motion.
		// that happens when the motion is already GRANTED/DENIED/SUSTAINED/OVERRULED
		// So we check if "OFF CALENDAR" is after one of these rules. If so, that's irrelevant
		boolean hasGD = false;
		Pair pOff = null;
		int offset = 0;
		int start = 10000;
		for (Pair p : gds) {
			Integer n = (Integer) p.o1;
			String gd = (String) p.o2;
			if (gd.startsWith("OFF")) {
				offset = n;
				pOff = p;
			} else {
				if (gd.startsWith("M")) // moot and off calendar usually coexist
					continue;
				hasGD = true;
				start = Math.min(start, n);
			}
		}
		if (hasGD && pOff != null) {
			if (offset > start) {
				gds.remove(pOff);
				this.offCalendar = false;
			}
		}
	}

	public static HearingEntry parse(String _sdate, String _text) {
		Matcher m = pHearingEntries.matcher(_text);
		if (m.find()) {
			HearingEntry entry = new HearingEntry(_sdate, _text);
			return entry;
		}
		return null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Hearing\t\t" + date + "\t");
		sb.append(text + "\n");
		sb.append("\t\t\tmotion:\t" + motion + "\n");
		//		if (offCalendar) {
		//			sb.append("\t\t\tOff Calendar\n");
		//		}
		if (newDate != null) {
			sb.append("\t\t\tContinued to: " + newDate + "\n");
		}
		for (Pair p : gds) {
			Integer ii = (Integer) p.o1;
			String gd = (String) p.o2;
			if (gd.startsWith("G")) {
				sb.append("\t\t\t" + ii + " Granted\n");
			} else if (gd.startsWith("D")) {
				sb.append("\t\t\t" + ii + " Denied\n");
			} else if (gd.startsWith("S")) {
				sb.append("\t\t\t" + ii + " Sustained\n");
			} else if (gd.startsWith("OVER")) {
				sb.append("\t\t\t" + ii + " Overruled\n");
			} else if (gd.startsWith("OFF")) {
				sb.append("\t\t\t" + ii + " OFF CALENDAR\n");
			} else if (gd.startsWith("M")) {
				sb.append("\t\t\t" + ii + " Moot\n");
			}
		}
		return sb.toString();
	}

	static final String regDate = "((\\w+)(?:-|\\s)(\\d+)(?:ST|ND|RD|TH)?(?:\\,|-|\\s)\\s*(\\d+)|(\\d+)/(\\d+)/(\\d+))";
	static final Pattern pContinuedTo = Pattern.compile("(?:(?:IS\\s)?CONTINUED.+?TO|BE HEARD.{0,45} ON)\\s.*?" + regDate,
			Pattern.CASE_INSENSITIVE);
	//	static final Pattern pContinuedTo = Pattern.compile("(?:(?:IS\\s)?CONTINUED.+?TO|BE HEARD ON)\\s.*?(\\w+)(?:-|\\s)(\\d+)(?:\\,|-)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
	static final Pattern pContinueTrial = Pattern.compile("MOTION\\s*(TO|FOR).{1,4}(CONTINU.{2,6}TRIAL|TRIAL.{1,4}CONTINU)");
	static final Pattern pNotMotion = Pattern.compile("^(MEMORANDUM|DECLARATION)", Pattern.CASE_INSENSITIVE);
	static final Pattern pCourtRule = Pattern.compile("(NO APPEARANCES|ARGUED AND|THIS MATTER IS NOT ARGUED)?\\.?\\s*THE COURT ADOPTS");

	//BE HEARD.{0,30} ON ((\w+)(?:-|\s)(\d+)(?:\,|-)\s*(\d+)|(\d+/\d+/\d+))
	void setAuthor(String _author) {
		Matcher m = pRoles.matcher(_author);
		if (m.find()) {
			String authRole = m.group("role").toUpperCase().replace('-', ' ');
			if (authRole != null && authRole.startsWith("DEFT")) {
				authRole = "DEFENDANT";
			}
			authorRole = new Role(authRole);
			authors = _author.substring(m.end()).replaceAll("^\\W|\\W$", "").trim();
			Matcher mmm = pRoles.matcher(authors);
			if (mmm.find()) {// do it twice to handle "PLAINTIFF/CROSS-DEFENDANTS","DEFENDANT/CROSS COMPLAINANT", "DEFENDANT/CROSS-DEFENDANT"
				authors = authors.substring(mmm.end()).replaceAll("^\\W|\\W$", "").trim();// ignore the second role
			}
		}
	}

	static final Pattern ppdate = Pattern.compile(regDate);
	static final Pattern ppmaster = Pattern.compile("MASTER (?:\\w+\\s){1,4}ON\\s");
	static final String regMasterDate = "MASTER (?:\\w+\\s){1,4}ON\\s" + regDate;
	static final Pattern pdate = Pattern.compile(regMasterDate, Pattern.CASE_INSENSITIVE);

	void analyze() {
		if (text.startsWith("MASTER CALENDAR MOTION CALENDAR ON AUG-30-2018 IN DEPT. 206, DEFENDANT DELANCEY STREET FOUNDATION DBA DELANCEY STREET MOVING'S")) {
			System.out.print("");
		}
		Matcher m = pMotionTitle.matcher(text);
		String body = "";
		String head = text;
		List<String> list = new ArrayList<>();
		if (m.find()) {
			head = text.substring(0, m.start());
			body = text.substring(m.start());
			motion = text.substring(m.start());
			if (head.startsWith("MASTER")) {
				Matcher mh = pdate.matcher(head);
				if (mh.find()) {
					int n1 = mh.groupCount();
					for (int i = 2; i <= n1; i++) {
						String g = mh.group(i);
						list.add(g);
					}
					oldDate = utils.DateTime.getSqlDate(list);
				}
			}
			Matcher mm = pRoles.matcher(head);
			if (mm.find()) {
				String s = head.substring(mm.start());
				setAuthor(s);
			}
		}

		int motionEndOffset = body.length();
		boolean motionEndFound = false;
		Matcher courtRule = pCourtRule.matcher(body);
		if (courtRule.find()) {
			//			body = body.substring(courtRule.start());
			//			motion = motion.substring(0, courtRule.start());
			motionEndOffset = Math.min(motionEndOffset, courtRule.start());
			//			motionEndFound = true;

		}
		Matcher moff = pOffCalendar.matcher(body);
		if (moff.find()) {
			offCalendar = true;
			gds.add(new Pair(Integer.valueOf(moff.start()), "OFF"));
			motionEndOffset = Math.min(motionEndOffset, moff.start());
			motionEndFound = true;
			// there will not be an order on this motion anymore:
			// set order field to non-null so it won't be erroneously assigned later
		}
		//		if (!motionEndFound && motionEndOffset > 0) {
		//			motion = body.substring(0, motionEndOffset);
		//			return;
		//		}
		Matcher mgd = pGrantDeny.matcher(body);
		while (mgd.find()) {
			String gd = mgd.group("gd1");
			if (gd == null)
				gd = mgd.group("gd2");
			if (gd.startsWith("G")) {
				gds.add(new Pair(Integer.valueOf(mgd.start()), "G"));
			} else if (gd.startsWith("D")) {
				gds.add(new Pair(Integer.valueOf(mgd.start()), "D"));
			} else if (gd.startsWith("S")) {
				gds.add(new Pair(Integer.valueOf(mgd.start()), "S"));
			} else if (gd.startsWith("O")) {
				gds.add(new Pair(Integer.valueOf(mgd.start()), "OVER"));
			} else if (gd.startsWith("M")) {
				gds.add(new Pair(Integer.valueOf(mgd.start()), "M"));
			}
			motionEndOffset = Math.min(motionEndOffset, mgd.start());
			motionEndFound = true;
			//			if (motionEndOffset == 0) {
			//				motionEndOffset = mgd.start();
			//			}
		}
		//		if (!motionEndFound && motionEndOffset > 0) {
		//			motion = body.substring(0, motionEndOffset);
		//			return;
		//		}
		if (motionEndFound) {
			motion = body.substring(0, motionEndOffset);
			return;
		}
		Matcher mcd = pContinuedTo.matcher(body);
		if (mcd.find()) {
			List<String> dlist = new ArrayList<>();
			//			dlist.add(mcd.group());
			dlist.add(mcd.group(2));
			dlist.add(mcd.group(3));
			dlist.add(mcd.group(4));
			dlist.add(mcd.group(5));
			dlist.add(mcd.group(6));
			dlist.add(mcd.group(7));
			//			String g5 = mcd.group(5);
			//			String month = mcd.group(2);// JANUARY, JAN, FEBRUARY, FEB, etc.
			//			String date = mcd.group(3);
			//			String year = mcd.group(4);
			//			if (g5 == null) {
			//				month = month.substring(0, 3);
			//				month = monthLookup.get(month);
			//			} else {
			//				month = mcd.group(6);
			//				date = mcd.group(7);
			//				year = mcd.group(8);
			//				if (year.length() == 2) {
			//					year = "20" + year;
			//				}
			//			}
			//			String dateNew = year + "-" + month + "-" + date;
			//			newDate = Date.valueOf(dateNew);
			newDate = utils.DateTime.getSqlDate(dlist);
			motionEndOffset = Math.min(motionEndOffset, mcd.start());
		}
		if (motionEndOffset < body.length()) {
			motion = body.substring(0, motionEndOffset);
			return;
		}
	}

	static String[] p = {
			"MASTER CALENDAR MOTION CALENDAR ON AUG-30-2018 IN DEPT. 206, DEFENDANT DELANCEY STREET FOUNDATION DBA DELANCEY STREET MOVING'S MOTION TO CONTINUE TRIAL DATE, AND ALL RELATED DEADLINES, INCLUDING DISCOVERY WITH A HEARING DATE OF 8/30/18 AT 9:30AM IN DEPT. 206 IS OFF CALENDAR PER MOVING PARTY. (206)",
			"MASTER CALENDAR MOTION CALENDAR ON 9/11/18 IN DEPT. 206, DEFENDANT DELANCEY STREET FOUNDATION DBA DELANCEY STREET MOVING'S MOTION TO CONTINUE TRIAL DATE, AND ALL RELATED DEADLINES, INCLUDING DISCOVERY WITH A HEARING DATE OF 9/11/18 AT 9:30AM IN DEPT. 206 IS OFF CALENDAR PER MOVING PARTY. (206)",
			"ORDER GRANTING EX PARTE APPLICATION FOR ORDER SHORTENING TIME ON A MOTION TO VACATE JUDGMENT; MOTION TO BE HEARD ON 6/20/17 IN DEPT. 501 AT 9:30AM. PAPERS TO BE FILED AND SERVED BY PERSONAL SERVICE ON 6/13/17. WRITTEN OPPOS FILED AND SERVED BY 6/16/17. NO WRITTEN REPLY (SEE SCANNED ORDER FOR DETAILS)",
			"DISCOVERY 302,DEFENDANT ANASTASIA DAVIS' MOTION TO COMPEL ANSWERS AT DEPOSITION AND REQUEST FOR MONETARY SANCTIONS. CONTINUED TO JULY 19, 2018 PER THE AGREEMENT OF THE PARTIES. JUDGE PRO TEM: STEPHEN ZOLLMAN, CLERK: M.GOODMAN, NOT REPORTED. =(302/JPT)" };

	static void test1() {
		for (int j = 0; j < p.length; j++) {
			System.out.println(p[j]);
			Matcher mcd = pContinuedTo.matcher(p[j]);
			if (mcd.find()) {
				int n = mcd.groupCount();
				for (int i = 0; i <= n; i++) {
					System.out.println("i=" + i + ", " + mcd.group(i));
				}
			}
		}
	}

	static void test2() {
		for (int j = 0; j < p.length; j++) {
			System.out.println(p[j]);
			HearingEntry he = parse("2018-08-23", p[j]);
			System.out.println(he);
		}
	}

	//HearingEntry parse(String _sdate, String _text)
	public static void main(String[] args) throws IOException {
		test2();
		if (args.length != 2) {
			System.out.println("args: infile outfile");
			System.exit(-1);
		}
		String infile = args[0];
		BufferedReader br = new BufferedReader(new FileReader(infile));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(args[1]));
		String line;
		int count1 = 0;
		while ((line = br.readLine()) != null) {
			String[] splits = line.split("\\t");
			if (splits.length != 3)
				continue;
			HearingEntry d = HearingEntry.parse(splits[1], splits[2]);
			if (d != null) {
				wr1.write(line + "\n");
				count1++;
				continue;
			}
		}
		br.close();
		wr1.write("count: " + count1 + "\n");
		wr1.close();
		System.out.println("Hearing : " + count1);
	}
}
