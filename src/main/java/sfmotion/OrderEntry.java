package sfmotion;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sftrack.TrackCase.MotionLink;
import utils.Pair;

public class OrderEntry {
	/*
	 * Order type 1: grant/deny motion, application, demurrer:
	 * 
	 *	ORDER DENYING PLAINTIFF'S MOTION FOR PRELIMINARY INJUNCTION (SEE SCANNED ORDER FOR DETAILS)
	 *	ORDER GRANTING DEFENDANTS' HOSIE RICE LLP, SPENCER HOSIE, AND DIANE S. RICE'S MOTION TO SEAL DEFENDANTS' OPPOSITION TO PLAINTIFF'S MOTION FOR A PRELIMINARY INJUNCTION, AND SUPPORTING PAPERS
	 *	ORDER GRANTING APPLICATION TO APPEAR AS COUNSEL PRO HAC VICE FOR ATTORNEY ALEXANDER G. TIEVSKY (TRANSACTION ID #60457108) FILED BY COUNSEL FOR PLAINTIFF PARKER, JASON INDIVIDUALLY AND ON BEHALF OF ALL OTHERS SIMILARLY SITUATED
	 * 	NOTICE OF ENTRY OF ORDER/NOTICE OF RULING FILED GRANTING PLAINTIFF'S UNOPPOSED MOTION TO SEAL SECOND AMENDED COMPLAINT; CONTINUING CASE MANAGEMENT CONFERENCE (TRANSACTION ID # 100025214) (TRANSACTION ID # 100025214) FILED BY PLAINTIFF LEE, ANTHONY K
	 *	NOTICE OF ENTRY OF ORDER/NOTICE OF RULING FILED DENYING PLAINTIFF'S MOTION FOR JUDGMENT ON THE PLEADINGS WITHOUT PREJUDICE (TRANSACTION ID # 100028134) FILED BY PLAINTIFF LEE, ANTHONY K
	 *
	 * Order type 2: 
	 *	ORDER SUSTAINING DEMURRERS WITH LEAVE TO AMEND FILED BY DEFENDANTS MILLENNIUM PARTNERS MANAGEMENT INC., ET AL., AND CHRISTOPHER JEFFRIES ET AL. (TRANSACTION #60913678)
	 *	ORDER OVERRULING IN PART AND IN PART SUSTAINING WITHOUT LEAVE DEMURRERS OF PEETS COFFEE (TRANSACTION #60890940)
	 *	ORDER RULING ON DEMURRERS ISSUED.
	 *
	 * Order type 3:
	 * 	ORDER TO SHOW CAUSE SET FOR MAY-08-2018 IN DEPARTMENT 610 AT 10:30 AM FOR FAILURE TO FILE PROOF(S) OF SERVICE ON DEFENDANT(S) J. HARMOND HUGHEY; OBTAIN ANSWER(S) OR ENTER DEFAULT(S) AGAINST DEFENDANT(S) AS TO 1ST AMENDED COMPLAINT. THE MAR-28-2018 CASE MANAGEMENT CONFERENCE IS OFF CALENDAR. NOTICE SENT BY COURT.
	 *	ORDER TO SHOW CAUSE SET FOR OCT-17-2017 IN DEPARTMENT 610 AT 10:30 AM FOR FAILURE TO FILE PROOF OF SERVICE AND OBTAIN ANSWER(S) OR ENTER DEFAULT(S) AGAINST DEFENDANT(S) RAHUL KUMAR. THE AUG-30-2017 CASE MANAGEMENT CONFERENCE IS OFF CALENDAR. NOTICE SENT BY COURT.
	 * 
	 * other orders
	 * 	ORDER AND STIPULATION REGARDING FILING OF SECOND AMENDED COMPLAINT
	 *	ORDER CLARIFYING TIME TO RESPOND TO CROSS-COMPLAINT
	 *ORDER TO CONTINUE TRIAL DATE
	 *ORDER SHORTENING TIME RE: MOTION TO CONTINUE TRIAL
	 *ORDER CONSOLIDATING APPEALS [SEE SCANNED ORDER FOR DETAILS] A155610 DIV 1 AND A15512 DIV 1
	 *ORDER CONTINUING MARCH 21, 2017 INITIAL CASE MANAGEMENT CONFERENCE (TRANSACTION ID #60306413)
	 *ORDER RE JURISDICTIONAL DISCOVERY (TRANSACTION #60529768)
	 *ORDER ON MEDIA COVERAGE (TRANSACTION #60957853)
	 *ORDER DISCOVERY REFEREE RECOMMENDATION 6 AND ORDER (TRANSACTION #62198089)
	 *
	 *
	 *ORDER FOR EXAMINATION AS TO DEFENDANT CAREY GRANT, JR. MAR-05-2019 - CONTINUED TO MAY-07-2019 AT 2:00 PM IN DEPT. 514 FOR LETTER PURSUANT TO THE AGREEMENT OF THE PARTIES AND COUNSEL, AND AT THE WRITTEN REQUEST OF ROBET E. WHITE, ESQ., COUNSEL FOR PLAINTIFF CLEARPOINT INVESTMENT MANAGEMENT, LP, ON 3/4/19. BENCH LETTER REISSUED. NO FURTHER NOTICE REQUIRED. THE PARTIES AND COUNSEL WAIVE NOTICE OF THE 5/7/19 EXAMINATION DATE. NO APPEARANCE. NOT REPORTED. JUDGE: SUZANNE BOLANOS. CLERK: K. DOUGHERTY. (D514).
	 *
	 */
	static final int ORDER_GRANTDENY_UNKNOWN = 0;
	static final int ORDER_GRANTDENY_MOTION = 1;
	static final int ORDER_OVERSUST_DEMURRER = 2;
	static final int ORDER_ING_APPLICATION = 3;
	static final int ORDER_TO_SHOW_CAUSE = 4;
	static final int ORDER_OTHER = 5;
	static final Pattern pOrder = Pattern.compile("^(ORDER|NOTICE OF ENTRY OF ORDER)", Pattern.CASE_INSENSITIVE);
	static final Pattern pMotionTitle = Pattern.compile("APPLICATION|MOTIONS?\\s+(TO|FOR|IN\\sLIMINE)|DEMURRERS?\\sTO.+?(COMPLAINT|ANSWER)", Pattern.CASE_INSENSITIVE);//|(EX\\\\sPARTE\\\\s)?APPLICATION
	static final Pattern pGrantDeny = Pattern.compile("((is|are)\\s)?(?<gd>Grant|den(y|i)|SUSTAIN|OVERRUL)(ed|ing)", Pattern.CASE_INSENSITIVE);// sustain for demurrer
	// order to show cause:
	static final Pattern pOrderToShowCause = Pattern.compile("^ORDER TO SHOW CAUSE", Pattern.CASE_INSENSITIVE);
	static final Pattern pOrderBackup = Pattern.compile("ORDER (?<gd>(GRANT|DENY)ING) (?<pd>PLAINTIFF|DEFENDANT)'?S (?<content>.+?MOTION)", Pattern.CASE_INSENSITIVE);
	public int subtype; // ORDER_GRANTDENY_MOTION, ORDER_OVERSUST_DEMURRER, ...
	public String content; // for subtype = ORDER_GRANTDENY_MOTION
	public List<Pair> gds = new ArrayList<>(); // grant, deny, sustain, overrule

	public OrderEntry() {
	}

	public static boolean parse(TrackEntry e) {
		//		if (e.text.startsWith("NOTICE OF ENTRY OF ORDER/NOTICE OF RULING FILED GRANTING PLAINTIFF'S UNOPPOSED MOTION TO SEAL SECOND AMENDED COMPLAINT;")) {
		//			System.out.print("");
		//		}
		Matcher m = pOrder.matcher(e.text);
		if (!m.find()) {
			return false;
		}
		OrderEntry entry = new OrderEntry();
		String[] proper = e.raw.split("\\(TRANS");
		String text = proper[0];
		m = pOrderToShowCause.matcher(text);
		if (m.find()) {
			entry.subtype = ORDER_TO_SHOW_CAUSE;
			entry.content = text.substring(m.end()).trim();
			e.setType(TrackEntry.ORDER);
			e.setTypeSpecific(entry);
			return true;
		}
		m = pMotionTitle.matcher(text);
		if (m.find()) {
			String body = text.substring(0, m.start()).trim();
			entry.content = text.substring(m.start()).trim();
			if (entry.content.startsWith("DEMURR")) {
				entry.subtype = ORDER_OVERSUST_DEMURRER;
			} else {
				int idx = entry.content.indexOf("MOTION");
				if (idx >= 0 && idx <= 5) {
					entry.subtype = ORDER_GRANTDENY_MOTION;
				} else {
					int idx1 = entry.content.indexOf("APPL");
					if (idx1 >= 0 && idx1 <= 5) {
						entry.subtype = ORDER_ING_APPLICATION;
					}
				}
			}
			Matcher mgd = pGrantDeny.matcher(body);
			int start = 0;
			while (mgd.find(start)) {
				String gd = mgd.group("gd");
				if (gd.startsWith("G")) {
					entry.gds.add(new Pair(Integer.valueOf(mgd.start()), "G"));
				} else if (gd.startsWith("D")) {
					entry.gds.add(new Pair(Integer.valueOf(mgd.start()), "D"));
				} else if (gd.startsWith("S")) {
					entry.gds.add(new Pair(Integer.valueOf(mgd.start()), "S"));
				} else if (gd.startsWith("O")) {
					entry.gds.add(new Pair(Integer.valueOf(mgd.start()), "OVER"));
				}
				start = mgd.start() + 4;
			}
			e.setType(TrackEntry.ORDER);
			e.setTypeSpecific(entry);
			return true;
		} else {
			Matcher mback = pOrderBackup.matcher(e.text);
			if (mback.find()) {
				String gd = mback.group("gd");
				if (gd != null) {
					if (gd.startsWith("G")) {
						entry.gds.add(new Pair(Integer.valueOf(mback.start("gd")), "G"));
					} else if (gd.startsWith("D")) {
						entry.gds.add(new Pair(Integer.valueOf(mback.start("gd")), "D"));
					} else if (gd.startsWith("S")) {
						entry.gds.add(new Pair(Integer.valueOf(mback.start("gd")), "S"));
					} else if (gd.startsWith("O")) {
						entry.gds.add(new Pair(Integer.valueOf(mback.start("gd")), "OVER"));
					}
				}
				String pd = mback.group("pd");
				String ct = mback.group("content");
				if (ct != null) {
					entry.content = ct;
				}
				entry.subtype = ORDER_GRANTDENY_MOTION;
			} else {
				int idx = e.text.indexOf("ORDER");
				if (idx >= 0) {
					entry.content = e.text.substring(idx + "ORDER".length()).trim();
				}
				entry.subtype = ORDER_OTHER;
			}
		}
		e.setType(TrackEntry.ORDER);
		e.setTypeSpecific(entry);
		return true;
	}

	public List<MotionLink> mlinkCandidates = new ArrayList<>();

	public void addMotionLinkCandidate(MotionLink ml) {
		mlinkCandidates.add(ml);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\t\t\tMotion:\t" + content + "\n");
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
}
