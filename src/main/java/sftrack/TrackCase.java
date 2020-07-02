package sftrack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.attribute.standard.MediaSize.Other;

import common.Role;
import sfmotion.CaseEntity;
import sfmotion.CaseLink;
import sfmotion.ComplaintEntry;
import sfmotion.HearingEntry;
import sfmotion.MotionEntry;
import sfmotion.OppositionEntry;
import sfmotion.OrderEntry;
import sfmotion.PersonName;
import sfmotion.ReplyEntry;
import sfmotion.TrackEntry;
import utils.Pair;

public class TrackCase {
	static final String LLCP = "A (\\w+\\s){0,6}(COMPANY|PARTNERSHIP|CORP(ORATION))";
	static final String leftBreak = "(?<=AN INDIVIDUAL\\,?|INCLUSIVE|AN ENTITY\\,?|\\bINC\\b\\.?+\\,?|LLC|LLP|\\bLP\\b)";
	static final String rightBreak = "(?=(AND )?AS ((AN|A)\\s)?\\S+ OF|DOES\\b)";
	static final String regexPartyTypeBreak = leftBreak + "|" + LLCP + "|" + rightBreak;
	static final String regexPartyType = "AN INDIVIDUAL\\,?|AN ENTITY\\,?|A (PUBLIC|GOVERNMENTAL) ENTITY\\,?";
	static final Pattern pParty = Pattern.compile(regexPartyType, Pattern.CASE_INSENSITIVE);
	static final Pattern pIndividual = Pattern.compile("(AS\\s)?AN INDIVIDUAL", Pattern.CASE_INSENSITIVE);
	static final Pattern pEntity = Pattern.compile("AN ENTITY|A (PUBLIC|GOVERNMENTAL) ENTITY", Pattern.CASE_INSENSITIVE);
	static final Pattern pInc = Pattern.compile("\\bINC\\b\\.?+\\,?|LLC\\,?|LLP|\\bLP\\b|CORP(ORATION|COMPANY|TRUST|PARTNERSHIP)?", Pattern.CASE_INSENSITIVE);
	static final Pattern pAsA = Pattern.compile("(AND )?AS ((AN|A)\\s)?(?<relation>\\S+) OF", Pattern.CASE_INSENSITIVE);
	static final Pattern pSuedAs = Pattern.compile("SUED HEREIN AS", Pattern.CASE_INSENSITIVE);
	static final Pattern pErrSuedAs = Pattern.compile("ERRONEOUSLY SUED", Pattern.CASE_INSENSITIVE);

	//	static final String regSep = "AS TO|FILED BY|\\(TRANSACTION.+?\\)|\\(FEE.+?\\|HEARING SET|)";
	public static Pattern ptransactionID = Pattern.compile("\\(TRANSACTION\\sID\\s\\#\\s+(\\d+)\\)", Pattern.CASE_INSENSITIVE);
	static final String[] ROLENAMES = { "APPELLANT", "CROSS DEFENDANT", "CROSS COMPLAINANT", "DEFENDANT", "PLAINTIFF", "CLAIMANT", "OTHER", };
	static int MAX_ALLOWED_DAYS_WITH_GD = 4;
	static int MAX_ALLOWED_DAYS_NO_GD = 14;

	Map<String, List<TrackEntry>> transactions = new TreeMap<>();
	public List<TrackEntry> entries;
	public List<TrackEntry> motionEntries = new ArrayList<>();
	List<List<TrackEntry>> daily; // entries of the same day
	List<Other> others; // others, that cannot organized into anything
	// Motions In limine grouped together:

	String casetype; // PERSONAL INJURY/PROPERTY DAMAGE - VEHICLE RELATED
	String caseSubtype; // VEHICLE RELATED
	String id;
	List<String> names = new ArrayList<>();
	List<Pair> namep = new ArrayList<>();
	ComplaintEntry complaint = null;
	List<CaseEntity> gel = new ArrayList<>();
	List<CaseLink> glk = new ArrayList<>();
	List<PartyCluster> clusters = new ArrayList<>();
	List<PersonName> judges = new ArrayList<>();
	public Date lastDate; // date of the last entry;
	List<Party> partylist;

	List<MotionEntry> motionlist = new ArrayList<>();
	List<HearingEntry> hrlist = new ArrayList<>();
	List<OrderEntry> orlist = new ArrayList<>();
	public List<OppositionEntry> oppositions = new ArrayList<>(); //  
	public List<ReplyEntry> replies = new ArrayList<>(); //  
	// Motions In limine grouped together:
	public Map<Role, List<MotionEntry>> mlnlists;
	public Map<Role, List<OppositionEntry>> oplists;
	List<MotionEntry> miplist; // plaintiff motion in limine, 
	List<MotionEntry> miulist; // unknown motion in limine, 
	List<OppositionEntry> opsToMiFromDefendant; // Oppositions to plaintiff's motion in limine from defendant
	List<OppositionEntry> opsToMiFromPlaintiff; // Oppositions to defendant's motion in limine from plaintiff
	List<OppositionEntry> opsToMiFromUnknown; // Oppositions to motion in limine from unknown party roles
	List<TrackEntry> otherMilist; // declarations, proof of services, from both parties lumped together

	public String getID() {
		return id;
	}

	static String readEntries(String infile, List<TrackEntry> entries) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(infile));
		String caseID = null;
		String line;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (caseID == null) {
				caseID = items[0];
			}
			TrackEntry en = new TrackEntry(items[1], items[2]);
			entries.add(en);
		}
		br.close();
		return caseID;
	}

	public TrackCase(String _id) {
		id = _id;
	}

	CaseEntity addToGel(CaseEntity e) {
		for (CaseEntity t : gel) {
			if (t.equals(e)) {
				t.combine(e);
				return t;
			}
		}
		gel.add(e);
		return e;
	}

	public void findlastDate() {
		TrackEntry e = motionEntries.get(motionEntries.size() - 1);
		lastDate = e.date;
	}

	public TrackCase(String _id, List<TrackEntry> _es) {
		id = _id;
		entries = _es;
	}

	public void addEntry(TrackEntry _e) {
		entries.add(_e);
	}

	public void convertToPairs() {
		int count = 0;
		String s = null;
		for (String n : names) {
			if (!n.equals(s)) {
				if (count > 0) {
					Pair p = new Pair(s, Integer.valueOf(count));
					namep.add(p);
				}
				count = 1;
				s = n;
			} else {
				count++;
			}
		}
		if (count > 0) {
			Pair p = new Pair(s, Integer.valueOf(count));
			namep.add(p);
		}
	}

	private void writePartyClusters(BufferedWriter wr) throws IOException {
		wr.write("============================= Party Clusters: ===============================\n\n");
		for (PartyCluster pc : clusters) {
			wr.write(pc.toString() + "\n");
		}
	}

	public void writeNames(BufferedWriter wr) throws IOException {
		wr.write(complaint.toString() + "\n");
		for (String s : names) {
			wr.write(s + "\n");
		}
	}

	public void writePairs(BufferedWriter wr) throws IOException {
		wr.write(complaint.toString() + "\n");
		for (Pair p : namep) {
			String s = (String) p.o1;
			Integer c = (Integer) p.o2;
			wr.write(s + "\t" + c + "\n");
		}
	}

	static class PartyCluster {
		String text;
		String role;
		String caseID;
		int count;
		List<CaseEntity> gel; // global entity list
		List<CaseEntity> list; // global entity list
		List<CaseLink> lklist = new ArrayList<>();

		public PartyCluster(String _cid, String _text, String _role, int _count, List<CaseEntity> _gel) {
			caseID = _cid;
			if (_text.startsWith(" ")) {
				System.out.print("");
			}
			text = _text;
			role = _role;
			count = _count;
			gel = _gel;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(text + "\n\n");
			//			sb.append("\tRole: " + role);
			//			sb.append("\tCount: " + count);
			for (CaseEntity e : list) {
				sb.append("\t" + e.toString() + "\n");
			}
			if (lklist.size() > 0) {
				sb.append("Links:\n");
				for (CaseLink lk : lklist) {
					sb.append("\t" + lk.toString() + "\n");
				}
			}
			return sb.toString();
		}

		public void useLeafEntities() {
			List<CaseEntity> nlist = new ArrayList<>();
			for (CaseEntity e : list) {
				if (e.children != null && e.children.size() > 1) {
					for (CaseEntity c : e.children) {
						if (!gel.contains(c))
							continue;
						if (!nlist.contains(c)) {
							nlist.add(c);
						}
					}
				} else {
					if (!gel.contains(e))
						continue;
					if (!nlist.contains(e)) {
						nlist.add(e);
					}
				}
			}
			list = nlist;
			if (lklist.size() > 0) {
				for (CaseLink lk : lklist) {
					lk.relink();
				}
			}
		}

		CaseEntity dba(String ss) {
			CaseEntity currentEntity = null;
			String[] dbas = ss.split("\\b(AND )?DBA\\b");
			if (dbas.length > 1) {
				CaseEntity e = new CaseEntity(dbas[0].trim(), role, "INDIVIDUAL", count);
				e = addToGel(e);
				if (!list.contains(e))
					list.add(e);
				currentEntity = e;
				for (int i = 1; i < dbas.length; i++) {
					String ds = dbas[i].trim();
					if (ds.length() < 4)
						continue;
					e = new CaseEntity(ds, role, "ENTITY", count);
					e = addToGel(e);
					if (!list.contains(e))
						list.add(e);
					CaseLink lk = new CaseLink(currentEntity, e, "DBA");
					lklist.add(lk);
				}
			}
			return currentEntity;
		}

		private CaseEntity findCurrentEntity() {
			for (int i = list.size() - 1; i >= 0; i--) {
				CaseEntity e = list.get(i);
				if (e.type.equalsIgnoreCase("INDIVIDUAL"))
					return e;
			}
			return null;
		}

		CaseEntity addToGel(CaseEntity e) {
			for (CaseEntity t : gel) {
				if (t.equals(e)) {
					t.combine(e);
					return t;
				}
			}
			gel.add(e);
			return e;
		}
	}

	static class CaseNames {
		String id;
		List<Party> parties = new ArrayList<>();

		public CaseNames(String _id) {
			id = _id;
		}

		void addParty(Party _p) {
			parties.add(_p);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(id);
			for (Party p : parties) {
				sb.append("\n\t" + p);
			}
			return sb.toString();
		}

		List<Party> getParties() {
			return parties;
		}
	}

	public void addMotionEntry(TrackEntry _e) {
		motionEntries.add(_e);
	}

	public void addOpposition(OppositionEntry e) {
		if (e.isToMIL()) {
			if (oplists == null) {
				oplists = new HashMap<>();
			}
			List<OppositionEntry> oplist;
			//			if (e.role == null) {
			//				e.role = "UNKNOWN";
			//			}
			oplist = oplists.get(e.role);
			if (oplist == null) {
				oplist = new ArrayList<>();
				oplists.put(e.role, oplist);
			}
			oplist.add(e);
		} else
			oppositions.add(e);
	}

	public void addReply(ReplyEntry e) {
		replies.add(e);
	}

	public void generateLists() {
		for (TrackEntry e : motionEntries) {
			if (e instanceof MotionEntry) {
				addMotion((MotionEntry) e);
			} else if (e instanceof HearingEntry) {
				addHearingEntry((HearingEntry) e);
			} else if (e instanceof OrderEntry) {
				addOrder((OrderEntry) e);
			} else if (e instanceof OppositionEntry) {
				addOpposition((OppositionEntry) e);
			} else if (e instanceof ReplyEntry) {
				addReply((ReplyEntry) e);
			}
		}
	}

	/**
	 * Remove duplications in the same transaction group of the same type (motion, demurrer, application)
	 * The one having no hearing date is the one to be removed.
	 * 
	 * Sometimes, two separate motions can be grouped into one transaction group. This method can do harm
	 * in this situation.
	 *  
	 * @return
	 */
	int removeDuplicateMotions() {
		int count = 0;
		for (int i = 0; i < motionlist.size(); i++) {
			MotionEntry ms = motionlist.get(i);
			String tid = ms.transactionID;
			if (tid != null) {
				List<TrackEntry> list = transactions.get(tid);
				if (list != null) {
					for (TrackEntry e : list) {
						if (e.type == null) {
							motionlist.remove(e);
						} else if (ms != e && e.type.equals(TrackEntry.MOTION)) {
							MotionEntry me = (MotionEntry) e;
							if (me.subtype == ms.subtype) {
								// need to remove one, remove the one does not have hearing date:
								if (ms.hearingDate != null && me.hearingDate == null) {
									motionlist.remove(e);
								} else if (ms.hearingDate == null && me.hearingDate != null) {
									motionlist.remove(ms);
									MotionEntry mt = me;
									e = ms;
									ms = mt;
									i--;
								}
								count++;
							}
						}
						if (ms != e)
							ms.addToGroup(e);
					}
				}
			}
		}
		return count;
	}

	int removeDuplicateMotions1() {
		int count = 0;
		for (String tid : transactions.keySet()) {
			List<TrackEntry> list = transactions.get(tid);
			List<MotionEntry> mlist = new ArrayList<>();
			for (TrackEntry e : list) {
				boolean be = false;
				if (e.type != null && e.type.equals(TrackEntry.MOTION)) {
					MotionEntry me = (MotionEntry) e;
					for (int i = 0; i < mlist.size(); i++) {
						MotionEntry ms = mlist.get(i);
						if (me.subtype == ms.subtype) {
							// need to remove one, remove the one does not have hearing date:
							if (ms.hearingDate == null && me.hearingDate != null) {
								motionlist.remove(ms);
								me.addToGroup(ms);
								mlist.remove(i);
								mlist.add(me);
							} else {
								motionlist.remove(me);
								ms.addToGroup(me);
							}
							be = true;
							count++;
						}
					}
					if (!be) {// not combined with any existing
						mlist.add(me);
					}
				}
			}
			for (TrackEntry e : list) {
				if (e.type == null || !e.type.equals(TrackEntry.MOTION)) {
					if (mlist.size() == 1) {
						MotionEntry ms = mlist.get(0);
						ms.addToGroup(e);
					} else if (mlist.size() > 1) {
						//						if (e.text.startsWith("MEMORANDUM OF POINTS AND AUTHORITIES IN SUPPORT OF MOTION TO STRIKE (TRANSACTION ID # 61722537)")) {
						//							System.out.print("");
						//						}
						for (MotionEntry ms : mlist) {
							if (ms.isCompatible(e)) {
								ms.addToGroup(e);
							}
						}
					}
				}
			}
		}
		return count;
	}

	int groupTransactions() {
		int count = 0;
		for (MotionEntry ms : motionlist) {
			//			if (ms.text.contains("MOTION TO COMPEL DISCOVERY RESPONSES, ATTENDANCE AND TESTIMONY OF DEFENDANTS AT DEPOSITION, AND FOR MONETARY SANCTIONS (TRANSACTION ID # 61415847)")) {
			//				System.out.println("Captured");
			//			}
			String tid = ms.transactionID;
			if (tid != null) {
				List<TrackEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (TrackEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		for (OppositionEntry ms : oppositions) {
			String tid = ms.transactionID;
			if (tid != null) {
				List<TrackEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (TrackEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		for (OrderEntry ms : orlist) {
			String tid = ms.transactionID;
			if (tid != null) {
				List<TrackEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (TrackEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		for (HearingEntry ms : hrlist) {
			String tid = ms.transactionID;
			if (tid != null) {
				List<TrackEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (TrackEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		for (ReplyEntry ms : replies) {
			String tid = ms.transactionID;
			if (tid != null) {
				List<TrackEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (TrackEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		return count;
	}

	Pattern pPrep = Pattern.compile("^(OF|FOR|ALSO|IN|FEE)\\s", Pattern.CASE_INSENSITIVE);

	private void cleanGel() {
		int i = 0;

		while (i < gel.size()) {
			CaseEntity e = gel.get(i);
			Matcher m = pPrep.matcher(e.nameNormalized);
			if (m.find()) {
				gel.remove(i);
				continue;
			}
			i++;
		}
	}

	public void writeGlobalEntities(BufferedWriter wr) throws IOException {
		wr.write("\n============== Global Entities:===================\n\n");
		for (CaseEntity e : gel) {
			wr.write(e + "\n");
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id + "\n");
		for (TrackEntry e : entries) {
			sb.append(e.toString() + "\n");
		}
		for (TrackEntry e : motionEntries) {
			sb.append(e.toPrintString("\t", 1, 2, 3, 4) + "\n");
		}
		return sb.toString();
	}

	public String toMotionString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id + "\n");
		if (motionlist.size() > 0) {
			for (MotionEntry ms : motionlist) {
				sb.append("\n" + ms + "\n");
			}
		}
		return sb.toString();
	}

	public List<TrackEntry> getEntries() {
		return motionEntries;
	}

	private void relinkParyClusters() {
		for (PartyCluster pc : clusters) {
			pc.useLeafEntities();
		}
	}

	// MANCIA, SUSANA AKA SUSY GARCIA AKA SUSANA GARCIA AKA SUSY GARCIA MANCIA, AS AN INDIVIDUAL, AS AN AGENT OF TERRA NOVA REAL ESTATE SERVICES, INC., AS AN AGENT/OWNER OF MGM INVESTMENTS 2015, LLC, AND AS AN AGENT/OWNER OF LAW OFFICE/DEFENSA LATINA
	List<Pair> decomposePartyList(String pl) {
		String[] splits = pl.split(regexPartyTypeBreak);
		List<Pair> list = new ArrayList<Pair>();
		for (String s : splits) {
			String ss = s;
			Matcher m = pIndividual.matcher(s);
			if (m.find()) {
				ss = s.substring(0, m.start()).replaceAll("\\,$", "").trim();
				Pair p = new Pair(ss, "INDIVIDUAL");
				list.add(p);
				continue;
			}
			m = pEntity.matcher(s);
			if (m.find()) {
				ss = s.substring(0, m.start()).replaceAll("\\,$", "").trim();
				Pair p = new Pair(ss, "ENTITY");
				list.add(p);
				continue;
			}
			Pair p = new Pair(ss, "UNKNOWN");
			list.add(p);
		}
		return list;
	}

	private void addToPartyList(String pName, String _type, String role, Integer count, List<Party> parties) {
		for (Party p : parties) {
			if (p.addParty(pName, role, _type, count))
				return;
		}
		Party p = new Party(pName, role, _type, count);
		parties.add(p);
	}

	public void writeParties(BufferedWriter wr) throws IOException {
		wr.write(complaint.toString() + "\n");
		for (Party p : partylist) {
			wr.write(p.toString() + "\n");
		}
	}

	public List<MotionEntry> getMotionList() {
		return motionlist;
	}

	public List<HearingEntry> getHearingEntries() {
		return hrlist;
	}

	public List<OrderEntry> getOrderEntries() {
		return orlist;
	}

	void addHearingEntry(HearingEntry _lm) {
		hrlist.add(_lm);
	}

	void addMotion(List<MotionEntry> _mslist) {
		motionlist.addAll(_mslist);
	}

	void addMotion(MotionEntry me) {
		if (me.b_motionInLimine) {
			if (mlnlists == null) {
				mlnlists = new HashMap<>();
			}
			//			if (me.role == null)
			//				me.role = "UNKNOWN";
			List<MotionEntry> mlnlist = mlnlists.get(me.role);
			if (mlnlist == null) {
				mlnlist = new ArrayList<>();
				mlnlists.put(me.role, mlnlist);
			}
			mlnlist.add(me);
		} else
			motionlist.add(me);
	}

	void addOrder(OrderEntry _or) {
		orlist.add(_or);
	}

	public void trackMotionSequences() {
		for (MotionEntry ms : motionlist) {
			findMatchingHearings(ms);
			findMatchingOrders(ms);
			findMatchingOppositions(ms);
			findMatchingReplies(ms);
			ms.organizeSequence();
		}
	}

	// Find hearing entry from a given date
	List<HearingEntry> findHearing(Date _hdate) {
		List<HearingEntry> hrs = new ArrayList<HearingEntry>();
		for (HearingEntry e : hrlist) {
			if (e.motion == null)
				continue;
			if (_hdate.before(e.date)) { // because lmlist is date ordered, no more can be there
				break;
			}
			if (_hdate.after(e.date))
				continue;
			// this must be the date:
			hrs.add(e);
		}
		return hrs;
	}

	// Find order entry with n days from a given date
	List<OrderEntry> findOrder(Date _hdate, int ndays, int subtype) {
		List<OrderEntry> hrs = new ArrayList<>();
		for (OrderEntry e : orlist) {
			if (e.content == null)
				continue;
			if (_hdate.before(e.date)) { // because orlist is date ordered, no more can be there
				break;
			}
			if (utils.DateTime.daysInBetween(e.date, _hdate) > ndays)
				continue;
			if (subtype != MotionEntry.TYPE_UNKNOWN && e.subtype != MotionEntry.TYPE_UNKNOWN && subtype != e.subtype)
				continue;
			hrs.add(e);
		}
		return hrs;
	}

	boolean findMatchingOppositions(MotionEntry ms) {
		//		if (ms.text.startsWith("MOTION FOR SUMMARY JUDGMENT, PROOF OF SERVICE FILED BY PLAINTIFF COURTHOUSE VENTURES INC. HEARING SET FOR SEP-12-2018")) {
		//			System.out.print("");
		//		}
		Date hearingDate = ms.finalHearingDate;
		//		List<HearingEntry> hrs = ms.hearingEntries;
		//		if (hrs != null && hrs.size() > 0) {
		//			HearingEntry hr = hrs.get(hrs.size() - 1);
		//			if (hr.hearingDate != null)
		//				hearingDate = hr.hearingDate;
		//			else
		//				hearingDate = hr.getDate();
		//		}
		int i = 0;
		while (i < oppositions.size()) {
			OppositionEntry lm = oppositions.get(i);
			if (hearingDate != null && lm.date.after(hearingDate)) {
				break;
			}
			if (lm.items == null) {
				i++;
				continue;
			}
			String motion = (String) lm.items.get("motion");
			if (motion == null) {
				i++;
				continue;
			}
			if (lm.date.before(ms.date)) {
				i++;
				continue;
			}
			//			HearingEntry h = ms.hearingEntries.get(ms.hearingEntries.size()-1);
			//			Date day = h.hearingDate;
			//			if(day==null) {
			//				day = h.date;
			//			}
			if (ms.matchMotion(motion)) {
				ms.addOppositionEntry(lm);
				oppositions.remove(i);
				continue;
			}
			i++;
		}
		if (ms.oppositions.size() > 0)
			return true;
		return false;
	}

	boolean findMatchingHearings(MotionEntry ms) {
		//		if (ms.text.startsWith("NOTICE OF MOTION AND MOTION TO CONTINUE TRIAL DATE, ALL RELATED DEADLINES, INCLUDING DISCOVERY (TRANSACTION ID # 100045042)")) {
		//			System.out.print("");
		//		}
		Date hearingDate = ms.hearingDate;
		int i = 0;
		boolean ret = false;
		while (i < hrlist.size() && hearingDate != null) {
			HearingEntry lm = hrlist.get(i);
			if (lm.date.before(ms.date)) {
				i++;
				continue;
			}
			if (lm.oldDate != null) {
				if (!lm.oldDate.equals(hearingDate)) {
					i++;
					continue;
				}
			}
			int daysAfter = utils.DateTime.daysInBetween(hearingDate, lm.date);
			if (daysAfter >= MAX_ALLOWED_DAYS_WITH_GD) {
				i++;
				break;
			}
			if (lm.subtype != MotionEntry.TYPE_UNKNOWN && lm.subtype != ms.subtype) {
				i++;
				continue;
			}
			if (ms.matchMotion(lm.motion)) {
				if (ms.filer != null && ms.filer.length() > 4 && lm.authors != null && lm.authors.length() > 4) {
					if (!ms.matchMotion(ms.filer, lm.authors)) {
						i++; // does not match, skip
						ms.matchMotion(ms.filer, lm.authors);// this is for debugging
						continue;
					}
					// reaches here means good match 
				} else if (ms.role != null && lm.authorRole != null) {
					if (!ms.role.equals(lm.authorRole)) {
						i++; // does not match, skip
						ms.role.equals(lm.authorRole);// this is for debugging
						continue;
					}
					// reaches here means good match 
				}
				// we check before and after the hearingDate differently:
				if (daysAfter <= 0) {
					if (lm.offCalendar) {
						ms.setOffCalendar(lm.date);
						ms.addHearingEntry(lm);
						hrlist.remove(i);
						return true;
					}
					if (lm.newDate != null) {// moved to a new date
						hearingDate = lm.newDate;
						ms.addHearingEntry(lm);
						hrlist.remove(i);
						continue;
					}
				}
				if (daysAfter >= 0) {
					if (lm.gds.size() > 0) {
						for (Pair p : lm.gds) {
							String s = (String) p.o2;
							if (s.startsWith("G")) {//granted
								ms.setGranted();
							} else if (s.startsWith("D")) {// denied
								ms.setDenied();
							} else if (s.startsWith("S")) {// sustained
								ms.setSustained();
							} else if (s.startsWith("OVER")) {// overruled
								ms.setOverruled();
							} else if (s.startsWith("OFF")) {
								ms.setOffCalendar(lm.date);
							} else if (s.startsWith("M")) {
								ms.setMoot();
							}
						}
						ret = true;
						//					break;
					}
					ms.addHearingEntry(lm);
					hrlist.remove(i);
					continue;
				}
				// Otherwise, it's not for this Motion, ignore it:
				i++;
				continue;
			} else if (!ret && lm.date.equals(hearingDate)) {
				HearingEntry hr = lm;
				int ml = 0;
				if (hr.motion != null) {
					ml = ms.fuzzyMatchMotion(hr.motion);
				}
				List<HearingEntry> hrs = findHearing(hearingDate);
				if (hrs != null) {
					for (int k = 0; k < hrs.size(); k++) {
						HearingEntry he = hrs.get(k);
						if (he == hr)
							continue;
						if (he.motion == null)
							continue;
						int kl = ms.fuzzyMatchMotion(he.motion);
						if (kl > ml) {
							ml = kl;
							hr = he;
						}
					}
				}
				ms.addHearingEntry(hr);
				hrlist.remove(hr);
				if (hr.offCalendar) {
					ms.setOffCalendar(hr.date);
					return true;
				}
				Date oldhearingdate = hearingDate;
				if (hr.newDate != null) {// moved to a new date
					hearingDate = hr.newDate;
				} else {
					if (hr.gds.size() > 0) {
						for (Pair p : hr.gds) {
							String s = (String) p.o2;
							if (s.startsWith("G")) {
								ms.setGranted();
							} else if (s.startsWith("D")) {
								ms.setDenied();
							} else if (s.startsWith("S")) {
								ms.setSustained();
							} else if (s.startsWith("OVER")) {
								ms.setOverruled();
							} else if (s.startsWith("OFF")) {
								ms.setOffCalendar(lm.date);
							} else if (s.startsWith("M")) {
								ms.setMoot();
							}
						}
						ret = true;
					}
				}
				while (i < hrlist.size()) {
					HearingEntry he = hrlist.get(i);
					if (he.date.after(oldhearingdate)) {
						break;
					}
					i++;
				}
				continue;
			}
			i++;
			// more sophisticated similarity measurement
		}
		if (!ret && hearingDate != null) {// not found among hearings, check that date:
			List<HearingEntry> hrs = findHearing(hearingDate);
			if (hrs.size() > 0) {
				HearingEntry hr = hrs.get(0);
				if (hrs.size() > 1) {
					int ml = ms.fuzzyMatchMotion(hr.motion);
					for (int k = 1; k < hrs.size(); k++) {
						HearingEntry he = hrs.get(k);
						if (he.motion == null)
							continue;
						int kl = ms.fuzzyMatchMotion(he.motion);
						if (kl > ml) {
							ml = kl;
							hr = he;
						}
					}
				}
				ms.addHearingEntry(hr);
				hrlist.remove(hr);
				if (hr.offCalendar) {
					ms.setOffCalendar(hr.date);
					return true;
				}
				//						if (hr.hearingDate != null) {// moved to a new date
				//							hearingDate = hr.hearingDate;
				//							continue;
				//						}
				for (Pair p : hr.gds) {
					String s = (String) p.o2;
					if (s.startsWith("G")) {
						ms.setGranted();
					} else if (s.startsWith("D")) {
						ms.setDenied();
					} else if (s.startsWith("S")) {
						ms.setSustained();
					} else if (s.startsWith("OVER")) {
						ms.setOverruled();
					} else if (s.startsWith("OFF")) {
						ms.setOffCalendar(hr.date);
					} else if (s.startsWith("M")) {
						ms.setMoot();
					}
					ret = true;
				}

				//					}
			}
		}
		ms.finalHearingDate = hearingDate;
		return ret;
	}

	boolean findMatchingOrders(MotionEntry ms) {
		//		if (ms.text.startsWith("EX PARTE APPLICATION FOR ORDER TO FILE CROSS COMPLAINT AGAINST VIKING")) {
		//			System.out.print("");
		//		}
		Date hearingDate = ms.finalHearingDate;
		boolean ret = false;
		int i = 0;
		while (i < orlist.size()) {
			OrderEntry or = orlist.get(i);
			if (or.date.before(ms.date)) {
				i++;
				continue;
			}
			if (hearingDate != null) {
				int maxDays = MAX_ALLOWED_DAYS_NO_GD;
				if (ms.isTracked())
					maxDays = MAX_ALLOWED_DAYS_WITH_GD;
				int daysAfter = utils.DateTime.daysInBetween(hearingDate, or.date);
				if (daysAfter < 0 || daysAfter >= maxDays) {
					i++;
					continue;
				}
			}
			if (ms.matchMotion(or.content)) {
				ms.addOrder(or);
				orlist.remove(i);
				for (Pair p : or.gds) {
					String s = (String) p.o2;
					if (s.startsWith("G")) {
						ms.setGranted();
					} else if (s.startsWith("D")) {
						ms.setDenied();
					} else if (s.startsWith("S")) {
						ms.setSustained();
					} else if (s.startsWith("OVER")) {
						ms.setOverruled();
					} else if (s.startsWith("OFF")) {
						ms.setOffCalendar(or.date);
					} else if (s.startsWith("M")) {
						ms.setMoot();
					}
				}
				ret = true;
				break;
			}
			i++;
			// else if((lm.date.equals(hearingDate))
			// more sophisticated similarity measurement
		}
		if (!ret && hearingDate != null) {// not found among hearings, check that date:
			List<OrderEntry> ors = findOrder(hearingDate, 2, ms.subtype);
			if (ors.size() > 0) {
				OrderEntry or = ors.get(0);
				if (ors.size() > 1) {
					int ml = ms.fuzzyMatchMotion(or.content);
					for (int k = 1; k < ors.size(); k++) {
						OrderEntry he = ors.get(k);
						int kl = ms.fuzzyMatchMotion(he.content);
						if (kl > ml) {
							ml = kl;
							or = he;
						}
					}
				}
				ms.addOrder(or);
				orlist.remove(or);
				for (Pair p : or.gds) {
					String s = (String) p.o2;
					if (s.startsWith("G")) {
						ms.setGranted();
					} else if (s.startsWith("D")) {
						ms.setDenied();
					} else if (s.startsWith("S")) {
						ms.setSustained();
					} else if (s.startsWith("OVER")) {
						ms.setOverruled();
					} else if (s.startsWith("OFF")) {
						ms.setOffCalendar(or.date);
					} else if (s.startsWith("M")) {
						ms.setMoot();
					}
				}
				ret = true;
			}
		}
		return ret;
	}

	boolean findMatchingReplies(MotionEntry ms) {
		Date hearingDate = ms.finalHearingDate;
		for (int i = 0; i < replies.size(); i++) {
			ReplyEntry lm = replies.get(i);
			if (hearingDate != null && lm.date.after(hearingDate)) {
				break;
			}
			if (lm.items == null) {
				continue;
			}
			String motion = (String) lm.items.get("motion");
			if (motion == null) {
				continue;
			}
			if (lm.date.before(ms.date)) {
				continue;
			}
			if (ms.matchMotion(motion)) {
				ms.addReplyEntry(lm);
				replies.remove(i);
				i--;
				continue;
			}
		}
		if (ms.replies.size() > 0)
			return true;
		return false;
	}

	public void findTransactions() {
		for (TrackEntry e : motionEntries) {
			Matcher mm = ptransactionID.matcher(e.text);
			if (mm.find()) {
				String transactionID = mm.group(1);
				List<TrackEntry> list = transactions.get(transactionID);
				if (list == null) {
					list = new ArrayList<>();
					transactions.put(transactionID, list);
				}
				list.add(e);
			}
		}
	}

	static class Party {
		List<String> roles = new ArrayList<>();// defendant, plaintiff, cross defendant, petitioner, respondent, appellant, CROSS COMPLAINANT, 
		String name;
		List<String> aka;
		List<String> dba;
		List<Pair> asRoleOf;// AS AN AGENT, OWNER, TRUSTEE OF
		String type; // INDIVIDUAL, ENTITY, UNKNOWN
		int count = 0;

		public Party(String _name, String _role, String _type, Integer cnt) {
			name = _name;
			type = _type;
			if (_role != null) {
				roles.add(_role);
			}
			count = cnt;
		}

		void addDBA(String _dba) {
			if (dba == null) {
				dba = new ArrayList<>();
			}
			if (dba.contains(_dba)) {
				return;
			}
			dba.add(_dba);
		}

		void addAKA(String _aka) {
			if (aka == null) {
				aka = new ArrayList<>();
			}
			if (aka.contains(_aka)) {
				return;
			}
			aka.add(_aka);
		}

		void addRole(String _role) {
			if (roles.contains(_role)) {
				return;
			}
			roles.add(_role);
		}

		boolean isThisParty(String pName) {
			return name.equalsIgnoreCase(pName);
		}

		boolean addParty(String pName, String _role, String _type, Integer cnt) {
			if (name.equalsIgnoreCase(pName)) {
				this.count += cnt;
				if (!roles.contains(_role)) {
					roles.add(_role);
				}
				if (type == null || type == "UNKNOWN") {
					type = _type;
				}
				return true;
			}
			return false;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(name + "\n");
			sb.append("\tCount:" + count + "\n");
			if (type == null)
				type = "UNKNOWN";
			sb.append("\tType:" + type + "\n");
			sb.append("\tRoles: ");
			for (String r : roles) {
				sb.append(r + ";");
			}
			if (aka != null) {
				sb.append("\nAKA: ");
				for (String s : aka) {
					sb.append(s + "; ");
				}
			}
			if (dba != null) {
				sb.append("\nDBA: ");
				for (String s : dba) {
					sb.append(s + "; ");
				}
			}
			if (asRoleOf != null) {
				sb.append("\nAs roles of: ");
				for (Pair p : asRoleOf) {
					sb.append("(" + (String) p.o1 + ", " + (String) p.o2 + "); ");
				}
			}
			return sb.toString();
		}
	}

}
