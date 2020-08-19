package sfmotion;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.Role;
import sftrack.TrackCase.MotionLink;
import utils.Pair;

public class HearingEntry {
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
			"^(LAW.{1,6}MOTION|MOTION)|^DISCOVERY\\,?\\s*302|^(MASTER|UNCONTESTED)\\s+((MOTION|COURT)\\s*)?CALENDAR|REAL\\s*PROPERTY\\/HOUSING\\s*MOTION\\,?\\s*501|^TRIAL\\s*MOTION|^NOT REPORTED\\.|^HEARING",
			Pattern.CASE_INSENSITIVE);
	static final Pattern pMotionTitle = Pattern.compile("APPLICATION|MOTIONS?\\s+(TO|FOR|IN\\sLIMINE)|DEMURRERS?\\sTO.+?(COMPLAINT|ANSWER)", Pattern.CASE_INSENSITIVE);//|(EX\\\\sPARTE\\\\s)?APPLICATION
	static final Pattern pGrantDeny = Pattern.compile("((is|are)\\s)?(?<gd1>Granted|den(ying|ied)|MOOT|SUSTAIN(ED|ING)|OVERRUL(ED|ING))|(THE\\s*COURT\\s+(?<gd2>GRANTS|DENIES))",
			Pattern.CASE_INSENSITIVE);// sustain for demurrer
	static final Pattern pOffCalendar = Pattern.compile("(IS\\s)?OFF(-|\\s*)CALENDAR", Pattern.CASE_INSENSITIVE);
	//	static final Map<String, String> monthLookup = new HashMap<>();

	public String motion;
	MotionEntry ms;
	public List<Pair> gds = new ArrayList<>();
	// track changes in calendar:
	// CA_SFC_464545	2019-07-03	MASTER CALENDAR MOTION CALENDAR ON JUL-09-2019 IN DEPT. 206, DEFENDANT NORMAN CHARLES CONTRUCTION, INC.'S MOTION FOR TRIAL CONTINUANCE WITH A HEARING DATE OF 7/9/19 AT 9:30AM IN DEPT. 206 IS OFF CALENDAR PER REQUEST OF MOVING PARTY. (206)
	public Date oldDate = null;
	public Date newDate = null;
	public boolean offCalendar = false;
	public int subtype = MotionEntry.TYPE_UNKNOWN;
	public Role authorRole = null;
	public String authors = null;
	public List<MotionLink> mlinkCandidates = new ArrayList<>();

	public void addMotionLinkCandidate(MotionLink ml) {
		mlinkCandidates.add(ml);
	}

	public HearingEntry(String _text) {
		analyze(_text);
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

	public static boolean parse(TrackEntry e) {
		Matcher m = pHearingEntries.matcher(e.text);
		if (m.find()) {
			HearingEntry entry = new HearingEntry(e.text);
			e.setType(TrackEntry.HEARING);
			e.setTypeSpecific(entry);
			return true;
		}
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\t\t\tmotion:\t" + motion + "\n");
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
		Matcher m = TrackEntry.pRoles.matcher(_author);
		if (m.find()) {
			String authRole = m.group("role").toUpperCase().replace('-', ' ');
			if (authRole != null && authRole.startsWith("DEFT")) {
				authRole = "DEFENDANT";
			}
			authorRole = new Role(authRole);
			authors = _author.substring(m.end()).replaceAll("^\\W|\\W$", "").trim();
			Matcher mmm = TrackEntry.pRoles.matcher(authors);
			if (mmm.find()) {// do it twice to handle "PLAINTIFF/CROSS-DEFENDANTS","DEFENDANT/CROSS COMPLAINANT", "DEFENDANT/CROSS-DEFENDANT"
				authors = authors.substring(mmm.end()).replaceAll("^\\W|\\W$", "").trim();// ignore the second role
			}
		}
	}

	static final Pattern ppdate = Pattern.compile(regDate);
	static final Pattern ppmaster = Pattern.compile("MASTER (?:\\w+\\s){1,4}ON\\s");
	static final String regMasterDate = "MASTER (?:\\w+\\s){1,4}ON\\s" + regDate;
	static final Pattern pdate = Pattern.compile(regMasterDate, Pattern.CASE_INSENSITIVE);

	void analyze(String text) {
		//		if (text.startsWith("MASTER CALENDAR MOTION CALENDAR ON AUG-30-2018 IN DEPT. 206, DEFENDANT DELANCEY STREET FOUNDATION DBA DELANCEY STREET MOVING'S")) {
		//			System.out.print("");
		//		}
		Matcher m = pMotionTitle.matcher(text);
		String body = "";
		String head = text;
		List<String> list = new ArrayList<>();
		if (m.find()) {
			head = text.substring(0, m.start()).trim();
			body = text.substring(m.start()).trim();
			motion = text.substring(m.start()).trim();
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
			Matcher mm = TrackEntry.pRoles.matcher(head);
			if (mm.find()) {
				String s = head.substring(mm.start()).trim();
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
		int start = 0;
		while (mgd.find(start)) {
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
			start = mgd.start() + 4;
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
			motion = body.substring(0, motionEndOffset).trim();
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
			motion = body.substring(0, motionEndOffset).trim();
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

}
