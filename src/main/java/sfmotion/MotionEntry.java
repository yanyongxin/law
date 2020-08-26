package sfmotion;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.simmetrics.metrics.BlockDistance;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import sftrack.TrackCase.MotionLink;
import utils.Pair;

public class MotionEntry {
	// For subtypes:
	public static final int TYPE_UNKNOWN = 0;
	public static final int TYPE_MOTION = 1;
	public static final int TYPE_DEMURR = 2;
	public static final int TYPE_APPLIC = 3;
	public static final int CONSECUTIVE_WORD_MATCH = 4; // 
	// Patterns:
	static final String head = "^";
	static final String motionSlash = "(MOTION (/|-) )?";
	static final String motionInLimine = "(?<motionProper>MOTIONS? IN LIMINE\\s.+?)";
	static final String motionInLimineOld = "(?<motionProper>MOTION IN LIMINE\\s(NO\\.?\\s|#)(\\d+|ONE|TWO|THREE|FOUR|FIVE)\\:?\\s(MOTION\\s)?(TO|FOR).+?)";
	static final String noticeOfMotion = "(NOTICE OF( MOTION\\s+AND)?\\s+)";
	static final String plaintiffDefendant = "(PLAINTIFF|DEFENDANT)\\'?S?";
	static final String partys = "(?<parties>.+?S\\s)?";
	static final String motionProper = "(?<motionProper>((EX PARTE|UNOPPOSED|JOINT|RENEWED)\\s)?MOTIONS?\\s+(TO|FOR|IN\\sLIMINE).+?)";
	static final String regTransactionID = "(?<transactionID>\\(TRANSAC.+?\\)\\s)";
	static final String regTransID = "\\(TRANSAC.+?(?<transactionID>\\d+)\\)";
	static final String filedby = "(FILED|SUBMITTED) BY\\s(?<filer>.+?)";
	static final String hearingSetFor = "(?<hearingSetFor>HEARING\\sSET\\sFOR\\s(?<month>\\w+)-(?<date>\\d\\d)-(?<year>\\d+)\\s+AT.+?IN DEP.+?\\d+\\s+)";
	static final String regfee = "\\(FEE.+?\\)";
	static final String regMotion1 = head + motionSlash + noticeOfMotion + "?" + partys + motionProper + regTransactionID + "?" + filedby + "\\s" + hearingSetFor + regfee + "?$";
	static final String regMotion2 = head + "(" + plaintiffDefendant + "\\s)?" + noticeOfMotion + "?" + motionInLimine + regTransactionID + "?" + filedby + "$";
	static final Pattern pTransID = Pattern.compile(regTransID, Pattern.CASE_INSENSITIVE);
	static final Pattern pMotion1 = Pattern.compile(regMotion1, Pattern.CASE_INSENSITIVE);
	static final Pattern pMotion2 = Pattern.compile(regMotion2, Pattern.CASE_INSENSITIVE);
	static final Pattern pNotMotion = Pattern.compile("^(MEMORANDUM|DECLARATION)", Pattern.CASE_INSENSITIVE);
	static final Pattern pHearingSet = Pattern.compile("HEARING\\sSET\\sFOR\\s(\\w+)-(\\d\\d)-(\\d+)", Pattern.CASE_INSENSITIVE);
	//NOTICE OF MOTION AND PLAINTIFF/CROSS-DEFENDANTS MOTION TO SEAL OPENING MEMORANDUM FOR DEMURRER TO CROSS-COMPLAINT (TRANSACTION ID # 100038076) FILED BY PLAINTIFF LEE, ANTHONY K HEARING SET FOR JUN-20-2018 AT 09:30 AM IN DEPT 302 (Fee:$60.00)
	//NOTICE OF MOTION AND MOTION TO BE RELIEVED AS COUNSEL (TRANSACTION ID # 18050012) FILED BY PLAINTIFF SASSANI, THOMAS JOSEPH AN INDIVIDUAL, ON HIS OWN BEHALF AND DERIVATIVELY ON BEHALF OF EKOVENTURE, INC., A DELAWARE CORPORATION HEARING SET FOR MAR-01-2018 AT 09:30 AM IN DEPT 302 (Fee:$60.00)
	//MOTION TO STRIKE 2ND AMENDED COMPLAINT (TRANSACTION ID # 61633166) FILED BY DEFENDANT BARTH, BRIAN AN INDIVIDUAL HEARING SET FOR APR-11-2018 AT 09:30 AM IN DEPT 302 (Fee:NO FEE)
	// Extract Motion title in an entry text:
	static final Pattern pMotionTitle = Pattern.compile("^((NOTICE\\sOF\\s)?EX PARTE\\s*)?APPLICATION|MOTIONS?\\s+(OF .{2,50}?)?(TO|FOR|IN\\sLIMINE)|\\bMIL\\b|DEMURRERS?\\sTO.+?(COMPLAINT|ANSWER)",
			Pattern.CASE_INSENSITIVE);//|(EX\\\\sPARTE\\\\s)?APPLICATION
	static final Pattern pMotionEntry = Pattern.compile("^(((NOTICE\\sOF\\s)?((PLAINTIFF|DEFENDANT)\\'?S?\\s)?((EX PARTE|UNOPPOSED|JOINT)\\s)?(MOTION|\\bMIL\\b|DEMURRER|APPLICATION)))",
			Pattern.CASE_INSENSITIVE);//|(EX\\sPARTE\\s)?APPLICATION
	static final String left = "";
	static final String right = "";
	//	static final Pattern pComplaint = Pattern.compile(regComplaint, Pattern.CASE_INSENSITIVE);
	static final String sreg = "\\;|\\s/\\s|(?=(PROOF\\s+OF\\s+SERVICE|\\(Fee.{3,10}?\\)|\\(TRANSACTION\\sID\\s.{6,20}?\\)|HEARING\\s*SET\\s*FOR|MEMORANDUM\\s+(OF\\s+POINTS\\s+(\\&|AND)\\s+AUTHORITIES)|\\,\\s+POINTS\\s+AND\\s+AUTHORITIES|DECLARATION|FILED\\s+BY))";
	static final Pattern pContinueTrial = Pattern.compile("^.{0,15}MOTION\\s*(TO|FOR).{1,4}(CONTINU.{2,6}((JURY|BENCH|COURT)\\s)?TRIAL|((JURY|BENCH|COURT)\\s)?TRIAL.{1,4}CONTINU)");
	//	static final Map<String, String> monthLookup = new HashMap<>();

	static final String test1 = "MOTION FOR DETERMINATION OF GOOD FAITH SETTLEMENT";
	static final String test2 = "MOTION FOR GOOD FAITH SETTLEMENT DETERMINATION";

	TrackEntry owner;
	Object motionString;// "MOTION TO COMPEL ...", "MOTION IN LIMINE NO.3: TO EXCLUDE ..."
	int motionOffset;
	public Date hearingDate;
	public Date finalHearingDate;
	public String partyRaw;
	// often motion in limine appear in series, "MOTION IN LIMINE NO.3: TO EXCLUDE ..."
	// But this cannot handle "MOTIONS IN LIMINE I - XXIV"
	// There are other dates set at the same entry for various 
	// other milestones, such Proof of service, opposition, memorandom, declarations, etc.
	// We'll add them later.		
	public List<MotionLink> hrCandidates = new ArrayList<>();// hearing candidates
	public List<MotionLink> orCandidates = new ArrayList<>();// order candidates
	public List<MotionLink> opCandidates = new ArrayList<>();// opposition candidates
	public List<MotionLink> repCandidates = new ArrayList<>();// reply candidates
	public List<TrackEntry> hearings = new ArrayList<>();// in SF court, it's called Law and Motion.
	public List<TrackEntry> orders = new ArrayList<>();
	public List<TrackEntry> oppositions = new ArrayList<>();
	public List<TrackEntry> replies = new ArrayList<>();
	public List<TrackEntry> others = new ArrayList<>();
	//	List<TrackEntry> group = new ArrayList<>();
	List<TrackEntry> sequence;
	// It can be more complicated than this, such as moot, partial grant partial deny. grant cause 1 and 3, deny cause 2 and 4.
	boolean grant = false;
	boolean deny = false;
	boolean sustain = false;
	boolean overrule = false;
	boolean moot = false;
	boolean offCalendar = false;
	public boolean b_motionInLimine = false;
	public int subtype = TYPE_UNKNOWN;
	public List<Pair> hearDates = new ArrayList<>();
	//	List<TrackEntry> 

	static void testing() {
		MotionEntry me = new MotionEntry(test1, null);
		boolean b = me._matchMotion(test1, test2);
		System.out.print(b);
	}

	public boolean combine(TrackEntry t) {
		if (others.contains(t))// already in
			return false;
		MotionEntry m = (MotionEntry) t.getTypeSpecific();
		if (!t.getType().equals(owner.getType())) {
			return false;
		}
		MotionEntry mm = (MotionEntry) owner.getTypeSpecific();
		if (!(m.subtype == mm.subtype)) {
			return false;
		}
		others.add(0, t);
		for (TrackEntry h : m.hearings) {
			this.addHearingEntry(h);
		}
		m.hearings.clear();
		for (TrackEntry h : m.orders) {
			this.addOrderEntry(h);
		}
		m.orders.clear();
		for (TrackEntry h : m.oppositions) {
			this.addOppositionEntry(h);
		}
		m.oppositions.clear();
		for (TrackEntry h : m.replies) {
			this.addReplyEntry(h);
		}
		m.replies.clear();
		for (TrackEntry h : m.others) {
			this.addOtherEntry(h);
		}
		m.others.clear();
		m.others.add(owner);
		for (MotionLink lk : m.orCandidates) {
			if (!orCandidates.contains(lk)) {
				orCandidates.add(lk);
			}
		}
		m.orCandidates.clear();
		for (MotionLink lk : m.opCandidates) {
			if (!opCandidates.contains(lk)) {
				opCandidates.add(lk);
			}
		}
		m.opCandidates.clear();
		for (MotionLink lk : m.repCandidates) {
			if (!repCandidates.contains(lk)) {
				repCandidates.add(lk);
			}
		}
		m.repCandidates.clear();

		if (m.finalHearingDate != null) {
			if (finalHearingDate == null) {
				finalHearingDate = m.finalHearingDate;
			} else {
				if (m.finalHearingDate.after(finalHearingDate)) {
					finalHearingDate = m.finalHearingDate;
				}
			}
		}
		for (Pair p : m.hearDates) {
			addHearDate(p);
		}
		grant |= m.grant;
		deny |= m.deny;
		sustain |= m.sustain;
		overrule |= m.overrule;
		moot |= m.moot;
		offCalendar |= m.offCalendar;
		b_motionInLimine |= m.b_motionInLimine;
		return true;
	}

	public void addHearDate(Pair p) {
		Date d = (Date) p.o2;
		int y = d.getYear();
		int m = d.getMonth();
		int d1 = d.getDate();
		boolean b = false;
		for (int i = 0; i < hearDates.size(); i++) {
			Pair pp = hearDates.get(i);
			Date dd = (Date) pp.o2;
			int yy = dd.getYear();
			int mm = dd.getMonth();
			int d2 = dd.getDate();
			if (yy == y && mm == m && d1 == d2) {
				// same date, do not add
				b = true;
				break;
			}
			if (dd.after(d)) {
				hearDates.add(i, p);
				b = true;
				break;
			}
		}
		if (!b) {
			hearDates.add(p);
		}
	}

	public boolean isTracked() {
		return (grant | deny | sustain | overrule | moot | offCalendar);
	}

	public boolean isOff() {
		return offCalendar;
	}

	boolean isMotionInLimine() {
		return b_motionInLimine;
	}

	static final Pattern pMil = Pattern.compile("MOTIONS? IN LIMINE|\\bMIL\\b", Pattern.CASE_INSENSITIVE);

	public MotionEntry(String _m, TrackEntry _owner) {
		owner = _owner;
		motionString = _m.trim();
		Matcher mt = pMil.matcher(_m);
		if (mt.find()) {
			b_motionInLimine = true;
			subtype = TYPE_MOTION;
		} else {
			subtype = findSubtype(_m);
		}
	}

	private void setMotionString(String _m) {
		motionString = _m.trim();
		Matcher mt = pMil.matcher(_m);
		if (mt.find()) {
			b_motionInLimine = true;
			subtype = TYPE_MOTION;
		} else {
			subtype = findSubtype(_m);
		}
	}

	//	public void addToGroup(TrackEntry e) {
	//		if (e.type != null && e.type.equals(TrackEntry.MOTION)) {
	//			MotionEntry me = (MotionEntry) e.typeSpecific;
	//			if (!me.group.isEmpty()) {
	//				boolean b = me.group.remove(this);
	//				//				if (b) {
	//				//					System.out.print("");
	//				//				}
	//				group.addAll(me.group);
	//			}
	//			me.group.clear();
	//		}
	//		if (!group.contains(e))
	//			group.add(e);
	//	}

	public static boolean containsMIL(String s) {
		Matcher mt = pMil.matcher(s);
		return mt.find();
	}

	public MotionEntry(List<String> _m) {
		motionString = _m;
		String m = _m.get(0);
		Matcher mt = pMil.matcher(m);
		if (mt.find()) {
			b_motionInLimine = true;
			subtype = TYPE_MOTION;
		} else {
			subtype = findSubtype(m);
		}
	}

	int findSubtype(String _m) {
		List<Pair> plist = new ArrayList<>();
		int dm = _m.indexOf("MOTION");
		if (dm >= 0) {
			plist.add(new Pair(Integer.valueOf(dm), TYPE_MOTION));
		}
		int da = _m.indexOf("APPLIC");
		if (da >= 0) {
			plist.add(new Pair(Integer.valueOf(da), TYPE_APPLIC));
		}
		int dd = _m.indexOf("DEMURR");
		if (dd >= 0) {
			plist.add(new Pair(Integer.valueOf(dd), TYPE_DEMURR));
		}
		if (plist.size() == 0)
			return TYPE_UNKNOWN;
		Collections.sort(plist);// decending order
		Pair winner = plist.get(plist.size() - 1);
		return ((Integer) (winner.o2)).intValue();
	}

	static boolean parse1(TrackEntry e) {
		String txt = e.text;
		Matcher m = pMotion1.matcher(txt);
		if (m.find()) {// change to while(m.find()). Some have multiple motions in one docket entry
			String parties = m.group("parties");
			String motionProper = m.group("motionProper").trim();
			String transactionID = m.group("transactionID");
			String filer = m.group("filer");
			String month = m.group("month");
			String sdate = m.group("date");
			String year = m.group("year");
			List<String> dlist = new ArrayList<>();
			dlist.add(month);
			dlist.add(sdate);
			dlist.add(year);
			//			String mon = monthLookup.get(month);
			//			Date hearingdate = Date.valueOf(year + "-" + mon + "-" + sdate);
			Date hearingdate = utils.DateTime.getSqlDate(dlist);
			MotionEntry ms = new MotionEntry(motionProper, e);
			e.setType(TrackEntry.MOTION);
			e.setTypeSpecific(ms);
			ms.setHearingDate(hearingdate);
			e.setTransactionID(transactionID);
			e.setFiler(filer);
			ms.setParty(parties);
			return true;
		} else {
			Matcher mm = pMotion2.matcher(txt);
			if (mm.find()) {
				String motionProper = mm.group("motionProper").trim();
				String transactionID = mm.group("transactionID");
				String filer = mm.group("filer");
				MotionEntry ms = new MotionEntry(motionProper, e);
				e.setType(TrackEntry.MOTION);
				e.setTypeSpecific(ms);
				e.setTransactionID(transactionID);
				e.setFiler(filer);
				return true;
			}
			return false;
		}
	}

	void setParty(String _party) {
		partyRaw = _party;
	}

	public static boolean parse(TrackEntry e) {
		//		if (txt.startsWith("MOTION TO CONTINUE COURT TRIAL / NOTICE OF MOTION & JOINT MOTION TO CONTINUE TRIAL DATE;")) {
		//			System.out.print("");
		//		}
		String txt = e.text;
		Matcher m = pMotionEntry.matcher(txt);
		if (m.find()) {// change to while(m.find()). Some have multiple motions in one docket entry
			Date hearingDate = null;
			String transactionID = null;
			String filer = null;
			String[] splits = txt.split(sreg);
			int index = 0;
			List<Pair> plist = new ArrayList<>();
			for (String s : splits) {
				s = s.trim();
				if (s.length() < 4)
					continue;
				index++;
				Matcher mn = pNotMotion.matcher(s);
				if (mn.find()) {
					continue;
				}
				Matcher mm = pMotionTitle.matcher(s);
				if (mm.find()) {// change : allow demurrer, application
					String motionTitle = s.substring(mm.start()).trim().replaceAll("^\\p{Punct}|\\p{Punct}$", "").trim();
					motionTitle = motionTitle.replaceFirst("^EX PARTE APP", "APP");
					Pair p = new Pair(motionTitle, Integer.valueOf(index));
					plist.add(p);
				} else if (s.startsWith("HEARING")) {// hearing set for JAN-14-2017 AT 9:30 am in Dept. 302
					Matcher mh = pHearingSet.matcher(s);
					if (mh.find()) {
						String month = mh.group(1);// JANUARY, JAN, FEBRUARY, FEB, etc.
						String sdate = mh.group(2);
						String year = mh.group(3);
						List<String> dlist = new ArrayList<>();
						dlist.add(month);
						dlist.add(sdate);
						dlist.add(year);
						//						month = month.substring(0, 3);
						//						String monthNew = monthLookup.get(month);
						//						String dateNew = year + "-" + monthNew + "-" + sdate;
						//						hearingDate = Date.valueOf(dateNew);
						hearingDate = utils.DateTime.getSqlDate(dlist);
					}
				} else if (s.startsWith("(TRANSAC")) {
					Matcher mt = pTransID.matcher(s);
					if (mt.find()) {
						transactionID = mt.group("transactionID");
					}
				} else if (s.startsWith("FILED BY")) {
					filer = s.substring("FILED BY".length()).trim();
				} else if (s.startsWith("SUBMITTED BY")) {
					filer = s.substring("SUBMITTED BY".length()).trim();
				}
			}
			if (plist.size() > 0) {
				MotionEntry ms = null;
				Pair p = plist.get(0);
				String mtn = (String) (p.o1);
				if (plist.size() == 1) {
					ms = new MotionEntry(mtn, e);
				} else {
					Integer i0 = (Integer) (p.o2);
					Pair p1 = plist.get(0);
					String mtn1 = (String) (p1.o1);
					Integer i1 = (Integer) (p1.o2);
					if (i1 - i0 == 1) {
						List<String> mlist = new ArrayList<>();
						mlist.add(mtn);
						mlist.add(mtn1);
						ms = new MotionEntry(mlist);
					} else {
						ms = new MotionEntry(mtn, e);
					}
				}
				if (ms != null) {
					ms.setHearingDate(hearingDate);
					if (transactionID != null) {
						e.setTransactionID(transactionID);
					}
					if (filer != null) {
						e.setFiler(filer);
					}
				}
				if (ms != null) {
					e.setType(TrackEntry.MOTION);
					e.setTypeSpecific(ms);
					return true;
				}
				return false;
			}
		} else {
			boolean b = parse1(e);
			//			if (b != null) {
			//				System.out.print("");
			//			}
			return b;
		}
		boolean b = parse1(e);
		return b;
	}

	Object getMotion() {
		return motionString;
	}

	// how many leading words match:
	public int fuzzyMatchMotion(String s) {
		if (motionString instanceof String) {
			String mts = (String) motionString;
			String[] myList = mts.split("\\s+");
			String[] heList = s.split("\\s+");
			int len = Math.min(myList.length, heList.length);
			int count;
			for (count = 0; count < len; count++) {
				if (!myList[count].equalsIgnoreCase(heList[count]))
					break;
			}
			return count;
		}
		List<String> mtsl = (List<String>) motionString;
		int countMax = 0;
		for (String mts : mtsl) {
			String[] myList = mts.split("\\s+");
			String[] heList = s.split("\\s+");
			int len = Math.min(myList.length, heList.length);
			int count;
			for (count = 0; count < len; count++) {
				if (!myList[count].equalsIgnoreCase(heList[count]))
					break;
			}
			if (count > countMax) {
				countMax = count;
			}
		}
		return countMax;
	}

	public double matchMotionScore(String s) {
		if (motionString instanceof String) {
			String mtn = (String) motionString;
			//			if (this.subtype == MotionEntry.TYPE_APPLIC) {
			//				mtn = orderReplace(mtn);
			//				s = orderReplace(s);
			//			}
			double score = matchMotionScore(s, mtn);
			return score;
		} else {
			List<String> list = (List<String>) motionString;
			double maxScore = 0.0;
			for (String mtn : list) {
				//				mtn = orderReplace(mtn);
				double score = matchMotionScore(s, mtn);
				if (score > maxScore)
					maxScore = score;
			}
			return maxScore;
		}
	}

	public boolean matchMotion(String s) {
		if (motionString instanceof String) {
			String mtn = (String) motionString;
			//			if (this.subtype == MotionEntry.TYPE_APPLIC) {
			//				mtn = orderReplace(mtn);
			//				s = orderReplace(s);
			//			}
			boolean b = matchMotion(s, mtn);
			if (b)
				return b;
		} else {
			List<String> list = (List<String>) motionString;
			//			s = orderReplace(s);
			for (String mtn : list) {
				//				mtn = orderReplace(mtn);
				boolean b = matchMotion(s, mtn);
				if (b)
					return b;
			}
		}
		return false;
	}

	private String orderReplace(String s) {
		//		s = s.replaceAll("FOR ORDER", "").replaceAll("ENTRY OF", "").replaceAll("\\b(TO|FOR|THE|OF|AS|A|AN)\\b", "").replaceAll("\\p{Punct}", "").replaceAll("\\s+", " ").trim();
		s = s.replaceAll("FOR ORDER", "").replaceAll("ENTRY OF", "").replaceAll("\\b(THE|OF|AS|A|AN)\\b", "").replaceAll("\\p{Punct}", "").replaceAll("\\s+", " ").trim();
		s = s.replaceAll("COMPLT", "COMPLAINT").replaceAll("JUDGMNT", "JUDGMENT");
		s = s.replaceAll("\\bMTN\\b", "MOTION");
		return s;
	}

	/**
	 * This is a simple match. A better version should return a matching measure. The calling program can compare the measure
	 * from competing matches to make a best choice.
	 * 
	 * @param s
	 * @param mtn
	 * @return
	 */
	public boolean matchMotion(String s, String mtn) {
		if (s == null)
			return false;
		if (mtn == null)
			return false;
		s = s.replaceAll("\\(.+?(\\)|$)", "").replaceAll("\\,|\\.|\\bETC\\b|\\\"", " ").replaceAll("NOTICE OF MOTION AND", "").trim().replaceAll("'S$", "");
		mtn = mtn.replaceAll("\\(.+?(\\)|$)", "").replaceAll("\\,|\\.|\\bETC\\b|\\\"", " ").replaceAll("NOTICE OF MOTION AND", "").trim().replaceAll("'S$", "");
		String s1 = s.replaceAll("\\s+|-", " ");
		String s2 = mtn.replaceAll("\\s+|-", " ");
		String[] s1s = s1.split("'S\\b");
		s1 = s1s[0];
		String[] s2s = s2.split("'S\\b");
		s2 = s2s[0];

		String ss1 = orderReplace(s1);
		String ss2 = orderReplace(s2);
		boolean bb = _matchMotion(ss1, ss2);
		if (!bb) {
			s1 = s.replaceAll("\\s+", " ").replaceAll("-", "").trim();
			s2 = mtn.replaceAll("\\s+", " ").replaceAll("-", "").trim();
			ss1 = orderReplace(s1);
			ss2 = orderReplace(s2);
			bb = _matchMotion(ss1, ss2);
		}
		if (bb)
			return true;
		Matcher m = pContinueTrial.matcher(mtn);
		if (m.find()) {
			Matcher mm = pContinueTrial.matcher(s);
			if (mm.find()) {
				// "MOTION FOR A TRIAL CONTINUANCE" matches "MOTION TO CONTINUE TRIAL DATE"
				return true;
			}
		}
		// more sophisticated matching here:
		return false;
	}

	/**
	 * This is a simple match. A better version should return a matching measure. The calling program can compare the measure
	 * from competing matches to make a best choice.
	 * 
	 * @param s
	 * @param mtn
	 * @return
	 */
	public double matchMotionScore(String s, String mtn) {
		if (s == null)
			return 0.0;
		if (mtn == null)
			return 0.0;
		Matcher m = pContinueTrial.matcher(mtn);
		if (m.find()) {
			Matcher mm = pContinueTrial.matcher(s);
			if (mm.find()) {
				// "MOTION FOR A TRIAL CONTINUANCE" matches "MOTION TO CONTINUE TRIAL DATE"
				return 1.0;
			}
		}
		s = s.replaceAll("\\(.+?(\\)|$)", "").replaceAll("\\,|\\.|\\bETC\\b|\\\"", " ").replaceAll("NOTICE OF MOTION AND", "").trim().replaceAll("'S$", "");
		mtn = mtn.replaceAll("\\(.+?(\\)|$)", "").replaceAll("\\,|\\.|\\bETC\\b|\\\"", " ").replaceAll("NOTICE OF MOTION AND", "").trim().replaceAll("'S$", "");
		s = s.replaceAll("\\b1ST", "FIRST").replaceAll("\\b2ND", "SECOND").replaceAll("\\b3RD", "THIRD");
		mtn = mtn.replaceAll("\\b1ST", "FIRST").replaceAll("\\b2ND", "SECOND").replaceAll("\\b3RD", "THIRD");
		String s1 = s.replaceAll("\\s+|-", " ");
		String s2 = mtn.replaceAll("\\s+|-", " ");
		//		String[] s1s = s1.split("'S\\b");
		//		s1 = s1s[0];
		//		String[] s2s = s2.split("'S\\b");
		//		s2 = s2s[0];
		s1 = s1.replaceAll("'S\\b", "S"); // PLAINTIFF'S ==> PLAINTIFFS
		s2 = s2.replaceAll("'S\\b", "S");

		String ss1 = orderReplace(s1);
		String ss2 = orderReplace(s2);
		double score1 = _matchMotionScore(ss1, ss2);
		s1 = s.replaceAll("\\s+", " ").replaceAll("-", "").trim();
		s2 = mtn.replaceAll("\\s+", " ").replaceAll("-", "").trim();
		ss1 = orderReplace(s1);
		ss2 = orderReplace(s2);
		double score2 = _matchMotionScore(ss1, ss2);
		double score = Math.max(score1, score2);
		return score;
	}

	private boolean _matchMotion(String s1, String s2) {
		if (s1.startsWith(s2)) {
			return true;
		} else if (s2.startsWith(s1)) {
			return true;
		}
		String[] ss = s1.split("\\s+");
		String[] sm = s2.split("\\s+");
		String[] s11;
		String[] s22;
		if (ss.length <= sm.length) {
			s11 = ss;
			s22 = sm;
		} else {
			s11 = sm;
			s22 = ss;
		}
		int len = s11.length;
		int i = 0;
		for (i = 0; i < len; i++) {
			if (!s11[i].equals(s22[i])) {
				break;
			}
		}
		if (i >= CONSECUTIVE_WORD_MATCH) {// this causes several matching errors
			return true;
		}
		// more sophisticated matching here:
		double score = _matchScore1(s11, s22);
		if (score > 0.73)
			return true;
		return false;
	}

	public void addHearingCandidate(MotionLink ml) {
		hrCandidates.add(ml);
	}

	public void addOrderCandidate(MotionLink ml) {
		orCandidates.add(ml);
	}

	public void addOppositionCandidate(MotionLink ml) {
		opCandidates.add(ml);
	}

	public void addReplyCandidate(MotionLink ml) {
		repCandidates.add(ml);
	}

	private double _matchMotionScore(String s1, String s2) {
		if (s1.startsWith(s2)) {
			return 1.0;
		} else if (s2.startsWith(s1)) {
			return 1.0;
		}
		if (s1.startsWith("MOTION TO") && s2.startsWith("MOTION FOR")) {
			return 0.0;
		}
		if (s1.startsWith("MOTION FOR") && s2.startsWith("MOTION TO")) {
			return 0.0;
		}
		String[] ss = s1.split("\\s+");
		String[] sm = s2.split("\\s+");
		String[] s11;
		String[] s22;
		if (ss.length <= sm.length) {
			s11 = ss;
			s22 = sm;
		} else {
			s11 = sm;
			s22 = ss;
		}
		// more sophisticated matching here:
		return (double) _matchScore1(s11, s22);
	}

	private float _matchScore(String[] shortlist, String[] longlist) {
		int len1 = shortlist.length;
		int len2 = Math.min(len1 * 2, longlist.length);
		float scoreMax = 0;
		BlockDistance<String> bd = new BlockDistance<String>();
		Multiset<String> ms1 = HashMultiset.create();
		for (String s : shortlist) {
			ms1.add(s);
		}
		for (int i = len1 - 1; i <= len2; i++) {
			Multiset<String> ms2 = HashMultiset.create();
			for (int j = 0; j < i; j++) {
				String s = longlist[j];
				ms2.add(s);
			}
			float blockScore = bd.compare(ms1, ms2);
			if (blockScore > scoreMax) {
				scoreMax = blockScore;
			}
		}
		return scoreMax;
	}

	private double _matchScore1(String[] shortlist, String[] longlist) {
		int len1 = shortlist.length;
		int len2 = Math.min(len1 * 2, longlist.length);
		double scoreMax = 0;
		List<String> ms1 = new ArrayList<>();
		for (String s : shortlist) {
			ms1.add(s);
		}
		for (int i = len1 - 1; i <= len2; i++) {
			List<String> ms2 = new ArrayList<>();
			for (int j = 0; j < i; j++) {
				ms2.add(longlist[j]);
			}
			double blockScore = utils.MyStringUtils.stringListDistance(ms1, ms2, 0.73);
			if (blockScore > scoreMax) {
				scoreMax = blockScore;
			}
		}
		return scoreMax;
	}

	public boolean isCompatible(TrackEntry e) {
		String s = e.text;
		int idxMotion = s.indexOf("MOTION");
		int idxDemurr = s.indexOf("DEMURR");
		if (idxMotion < 0 && idxDemurr < 0) {
			return false;
		}
		int stype = TYPE_MOTION;
		if (idxMotion < 0 || (idxDemurr >= 0 && idxDemurr < idxMotion)) {
			stype = TYPE_DEMURR;
		}
		return stype == subtype;
	}

	void setHearingDate(Date _d) {
		hearingDate = _d;
	}

	public void addHearingEntry(TrackEntry _lm) {
		if (!hearings.contains(_lm))
			hearings.add(_lm);
	}

	public void addOppositionEntry(TrackEntry _lm) {
		if (!oppositions.contains(_lm))
			oppositions.add(_lm);
	}

	public void addReplyEntry(TrackEntry _lm) {
		if (!replies.contains(_lm))
			replies.add(_lm);
	}

	public void addOtherEntry(TrackEntry o) {
		if (!others.contains(o))
			others.add(o);
	}

	public void addOrderEntry(TrackEntry or) {
		if (!orders.contains(or))
			orders.add(or);
	}

	public void setGranted() {
		grant = true;
	}

	public void setDenied() {
		deny = true;
	}

	public void setSustained() {
		sustain = true;
	}

	public void setOverruled() {
		overrule = true;
	}

	public void setMoot() {
		moot = true;
	}

	public void setOffCalendar(Date dt) {
		offCalendar = true;
		this.finalHearingDate = dt;
	}

	public void setFinalHearingDate() {
		if (finalHearingDate != null)
			return;
		if (hearDates != null && hearDates.size() > 0) {
			Pair p = hearDates.get(hearDates.size() - 1);
			finalHearingDate = (Date) p.o2;
		}
	}

	public void setOrderFlagsFromHearings() {
		for (TrackEntry e : hearings) {
			HearingEntry he = (HearingEntry) e.getTypeSpecific();
			for (Pair p : he.gds) {
				String gd = (String) p.o2;
				if (gd.startsWith("G")) {
					grant = true;
				} else if (gd.startsWith("D")) {
					deny = true;
				} else if (gd.startsWith("S")) {
					sustain = true;
				} else if (gd.startsWith("OVER")) {
					overrule = true;
				} else if (gd.startsWith("OFF")) {
					offCalendar = true;
				} else if (gd.startsWith("M")) {
					moot = true;
				}
			}
		}
	}

	public void setOrderFlagsFromOrders() {
		for (TrackEntry e : orders) {
			OrderEntry he = (OrderEntry) e.getTypeSpecific();
			for (Pair p : he.gds) {
				String gd = (String) p.o2;
				if (gd.startsWith("G")) {
					grant = true;
				} else if (gd.startsWith("D")) {
					deny = true;
				} else if (gd.startsWith("S")) {
					sustain = true;
				} else if (gd.startsWith("OVER")) {
					overrule = true;
				} else if (gd.startsWith("OFF")) {
					offCalendar = true;
				} else if (gd.startsWith("M")) {
					moot = true;
				}
			}
		}
	}

	public void organizeSequence() {
		sequence = new ArrayList<>();
		sequence.addAll(oppositions);
		sequence.addAll(replies);
		sequence.addAll(hearings);
		sequence.addAll(orders);
		sequence.addAll(others);
		Collections.sort(sequence);
		setOrderFlagsFromOrders();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\t\t\tMotion\t" + motionString + "\n");
		if (hearingDate != null)
			sb.append("\t\t\tHSet\t" + hearingDate + "\n");
		if (sequence != null)
			for (TrackEntry od : sequence) {
				sb.append(od);
			}
		if (grant && deny) {
			sb.append("Ruling\t\tPARTIAL GRANTED AND PARTIAL DENIED");
		} else if (grant) {
			sb.append("Ruling\t\tGRANTED ");
		} else if (deny) {
			sb.append("Ruling\t\tDENIED ");
		}
		if (sustain && overrule) {
			sb.append("Ruling\t\tPARTIAL SUSTAIN AND PARTIAL OVERRULE");
		} else if (sustain) {
			sb.append("Ruling\t\tSUSTAINED ");
		} else if (overrule) {
			sb.append("Ruling\t\tOVERRULED ");
		} else if (moot) {
			sb.append("Ruling\t\tMOOT ");
		}
		if (offCalendar) {
			sb.append("Ruling\t\tOFF CALENDAR");
		}
		//		if (!group.isEmpty()) {
		//			sb.append("\nGroup members:\n");
		//			for (TrackEntry e : group) {
		//				if (e.type != null && e.type.equals(TrackEntry.MOTION)) {
		//					sb.append("Extra: " + e + "\n");
		//				} else
		//					sb.append(e + "\n");
		//			}
		//		}
		return sb.toString();
	}

	static void findMotion(String s, String line, BufferedWriter wr1, BufferedWriter wr2) throws IOException {
		Matcher m = pMotionEntry.matcher(s);
		if (m.find()) {// change to while(m.find()). Some have multiple motions in one docket entry
			wr1.write(line + "\n");
		} else {
			wr2.write(line + "\n");
		}
	}

}
