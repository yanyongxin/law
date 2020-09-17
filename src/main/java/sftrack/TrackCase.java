package sftrack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.attribute.standard.MediaSize.Other;

import apps.SFcomplex;
import apps.SFcomplex.ComparePhrases;
import common.Role;
import core.Phrase;
import legal.CaseEntity;
import legal.TrackEntry;
import legal.TrackEntry.Section;
import sfmotion.CaseLink;
import sfmotion.ComplaintEntry;
import sfmotion.HearingEntry;
import sfmotion.MotionEntry;
import sfmotion.OppositionEntry;
import sfmotion.OrderEntry;
import sfmotion.PersonName;
import sfmotion.ReplyEntry;
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
	static int MAX_ALLOWED_DAYS_WITH_GD = 3;
	static int MAX_ALLOWED_DAYS_NO_GD = 14;
	static double IGNORE_THRESHOLD = 0.6; // ignore any match below this
	static double TRUST_THRESHOLD = 0.94; // accept any match above this
	static double RANGE_THRESHOLD = 0.04; // any match as score in this range are considered same

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

	List<TrackEntry> motionlist = new ArrayList<>();
	List<TrackEntry> caseHearingsList = new ArrayList<>();
	List<TrackEntry> caseOrderList = new ArrayList<>();
	public List<TrackEntry> caseOppositionList = new ArrayList<>(); //  
	public List<TrackEntry> caseReplyList = new ArrayList<>(); //  
	// Motions In limine grouped together:
	public Map<Role, List<TrackEntry>> mlnlists;
	public Map<Role, List<TrackEntry>> oplists;
	List<TrackEntry> miplist; // plaintiff motion in limine, 
	List<TrackEntry> miulist; // unknown motion in limine, 
	List<TrackEntry> opsToMiFromDefendant; // Oppositions to plaintiff's motion in limine from defendant
	List<TrackEntry> opsToMiFromPlaintiff; // Oppositions to defendant's motion in limine from plaintiff
	List<TrackEntry> opsToMiFromUnknown; // Oppositions to motion in limine from unknown party roles
	List<TrackEntry> otherMilist; // declarations, proof of services, from both parties lumped together
	List<MotionLink> mHearingLinks; // similarity links between motion-related entries
	List<MotionLink> mOrderLinks; // similarity links between motion-related entries
	List<MotionLink> mOppositionLinks; // similarity links between motion-related entries
	List<MotionLink> mReplyLinks; // similarity links between motion-related entries
	List<MotionLink> maxHearingLinks = new ArrayList<>(); // surviving maximum motion-hearing links
	List<MotionLink> maxOrderLinks = new ArrayList<>(); // surviving maximum motion-order links
	List<MotionLink> maxOppositionLinks = new ArrayList<>(); // surviving maximum motion-opposition links
	List<MotionLink> maxReplyLinks = new ArrayList<>(); // surviving maximum motion-reply links

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
		if (entries.size() > 1) {
			TrackEntry e = entries.get(entries.size() - 1);
			lastDate = e.date;
		}
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

	public void addOpposition(TrackEntry e) {
		OppositionEntry oe = (OppositionEntry) e.getTypeSpecific();
		if (oe.isToMIL(e)) {
			if (oplists == null) {
				oplists = new HashMap<>();
			}
			List<TrackEntry> oplist;
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
			caseOppositionList.add(e);
	}

	public void addReply(TrackEntry e) {
		caseReplyList.add(e);
	}

	public void generateLists() {
		for (TrackEntry te : entries) {
			String t = te.getType();
			if (t == null)
				continue;
			if (t.equals(TrackEntry.MOTION)) {
				addMotion(te);
			} else if (t.equals(TrackEntry.HEARING)) {
				addHearingEntry(te);
			} else if (t.equals(TrackEntry.ORDER)) {
				addOrder(te);
			} else if (t.equals(TrackEntry.OPPOSITION)) {
				addOpposition(te);
			} else if (t.equals(TrackEntry.REPLY)) {
				addReply(te);
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
	//	int removeDuplicateMotions() {
	//		int count = 0;
	//		for (int i = 0; i < motionlist.size(); i++) {
	//			TrackEntry s = motionlist.get(i);
	//			MotionEntry ms = (MotionEntry) s.getTypeSpecific();
	//			String tid = s.transactionID;
	//			if (tid != null) {
	//				List<TrackEntry> list = transactions.get(tid);
	//				if (list != null) {
	//					for (TrackEntry e : list) {
	//						if (e.type == null) {
	//							motionlist.remove(e);
	//						} else if (s != e && e.type.equals(TrackEntry.MOTION)) {
	//							MotionEntry me = (MotionEntry) e.getTypeSpecific();
	//							if (me.subtype == ms.subtype) {
	//								// need to remove one, remove the one does not have hearing date:
	//								if (ms.hearingDate != null && me.hearingDate == null) {
	//									motionlist.remove(e);
	//								} else if (ms.hearingDate == null && me.hearingDate != null) {
	//									motionlist.remove(s);
	//									MotionEntry mt = me;
	//									s = e;
	//									ms = mt;
	//									i--;
	//								}
	//								count++;
	//							}
	//						}
	//						if (s != e)
	//							ms.addToGroup(e);
	//					}
	//				}
	//			}
	//		}
	//		return count;
	//	}

	int groupTransactions() {
		int count = 0;
		for (TrackEntry ms : motionlist) {
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
		for (TrackEntry ms : caseOppositionList) {
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
		for (TrackEntry ms : caseOrderList) {
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
		for (TrackEntry ms : caseHearingsList) {
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
		for (TrackEntry ms : caseReplyList) {
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
			for (TrackEntry ms : motionlist) {
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

	public List<TrackEntry> getMotionList() {
		return motionlist;
	}

	public List<TrackEntry> getHearingEntries() {
		return caseHearingsList;
	}

	public List<TrackEntry> getOrderEntries() {
		return caseOrderList;
	}

	void addHearingEntry(TrackEntry _lm) {
		caseHearingsList.add(_lm);
	}

	void addMotion(List<TrackEntry> _mslist) {
		motionlist.addAll(_mslist);
	}

	void addMotion(TrackEntry te) {
		MotionEntry me = (MotionEntry) te.getTypeSpecific();
		if (me.b_motionInLimine) {
			if (mlnlists == null) {
				mlnlists = new HashMap<>();
			}
			//			if (me.role == null)
			//				me.role = "UNKNOWN";
			List<TrackEntry> mlnlist = mlnlists.get(te.role);
			if (mlnlist == null) {
				mlnlist = new ArrayList<>();
				mlnlists.put(te.role, mlnlist);
			}
			mlnlist.add(te);
		} else
			motionlist.add(te);
	}

	void addOrder(TrackEntry _or) {
		caseOrderList.add(_or);
	}

	public void trackMotionSequences() {
		for (TrackEntry ms : motionlist) {
			findHearingLinks(ms);
		}
		// register links on each TrackEntry:
		addMotionHearingCandidates();
		iterateHearings();
		// iterate until find:
		for (TrackEntry ms : motionlist) {
			findOrderLinks(ms);
			findOppositionLinks(ms);
			findReplieLinks(ms);
		}
		// register links on each TrackEntry:
		addMotionOrderCandidates();
		addMotionOppositionCandidates();
		addMotionReplyCandidates();
		// iterate until find:
		iterateOrders();
		iterateOppositions();
		iterateReplies();

		organizeSequences();
	}

	public void organizeSequences() {
		for (TrackEntry ms : motionlist) {
			MotionEntry me = (MotionEntry) ms.getTypeSpecific();
			me.organizeSequence();
		}
	}

	/**
	 * decide which Motion-Hearing Link should be used. Eliminate those not appropriate ones.
	 */
	public void iterateHearings() {
		/*
		 *  1. 回答：有没有必要递推？
		 *  2. 不递推是什么做法？
		 *  3. 挑最大的
		 *  4. 挑唯一的
		 *  5. 然后看 相互抵触的。
		 *  6. 多个hearing: 是否满足“不断推迟”的要求 
		*/

		for (TrackEntry e : caseHearingsList) {
			HearingEntry he = (HearingEntry) e.getTypeSpecific();
			double scoreMax = 0.0;
			MotionLink lkMax = null;
			for (MotionLink lk : he.mlinkCandidates) {
				double v = lk.getValue();
				if (v > scoreMax) {
					scoreMax = v;
					lkMax = lk;
				}
			}
			if (lkMax != null) {
				maxHearingLinks.add(lkMax);
			}
			he.mlinkCandidates.clear();
		}
		for (TrackEntry e : motionlist) {
			MotionEntry me = (MotionEntry) e.getTypeSpecific();
			double scoreMax = 0.0;
			MotionLink lkMax = null;
			for (MotionLink lk : me.hrCandidates) {
				double v = lk.getValue();
				if (v > scoreMax) {
					scoreMax = v;
					lkMax = lk;
				}
			}
			if (lkMax != null) {
				for (MotionLink lk : me.hrCandidates) {
					double v = lk.getValue();
					if (v >= TRUST_THRESHOLD || v >= scoreMax - RANGE_THRESHOLD) {
						//						me.addHearingEntry(lk.t2);
						if (!maxHearingLinks.contains(lk))
							maxHearingLinks.add(lk);
					}
				}
			}
			me.hrCandidates.clear();
			if (me.hearings.size() > 1)
				Collections.sort(me.hearings);
			if (me.hearings.size() > 0) {
				me.finalHearingDate = me.hearings.get(me.hearings.size() - 1).getDate();
			}
		}
		for (MotionLink lk : maxHearingLinks) {
			MotionEntry me = (MotionEntry) lk.t1.getTypeSpecific();
			HearingEntry he = (HearingEntry) lk.t2.getTypeSpecific();
			me.hrCandidates.add(lk);
			he.mlinkCandidates.add(lk);
			me.addHearingEntry(lk.t2);
			caseHearingsList.remove(lk.t2);
		}

		for (TrackEntry e : motionlist) {
			MotionEntry me = (MotionEntry) e.getTypeSpecific();
			me.setOrderFlagsFromHearings();
			me.setFinalHearingDate();
		}

		for (TrackEntry ht : caseHearingsList) {
			HearingEntry he = (HearingEntry) ht.getTypeSpecific();
			if (he.mlinkCandidates.size() > 1) {
				/** this means one hearing is linked to two or more motions
				 * case 1:
				 * 	these motions are really the same motion. The first is "NOTICE OF MOTION" the second is the real motion
				 * or, the second is the "redacted" version.
				 * it satisfy 2 conditions:
				 * (1) happen within 2 days, most likely same day.
				 * (2) motion text match each other
				 * case 2:
				 * 		only one link is correct
				 * signs that one link is incorrect:
				 * (1) the motion has other hearings
				 * (2) the other hearing matches planned hearing date
				 * (3) the motion is off calendar
				 * (4) this link is either too close to the motion date (<30 days) or too far (>60 days)
				 * Solution:
				 * 	score: (1) date score (2) match score (3) motion has other hearing score
				 * after scoring all links, keep the highest one.
				*/
				// treating them as the same motion, combine them:
				MotionLink lk0 = he.mlinkCandidates.get(0);
				TrackEntry t0 = lk0.t1;
				MotionEntry m0 = (MotionEntry) t0.getTypeSpecific();
				for (int i = 1; i < he.mlinkCandidates.size(); i++) {
					MotionLink lk = he.mlinkCandidates.get(i);
					if (m0.combine(lk.t1))
						this.motionlist.remove(lk.t1);
				}
				he.mlinkCandidates.clear();
				he.mlinkCandidates.add(lk0);
			}
		}
	}

	/**
	 * decide which Motion-Order Link should be used. Eliminate those not appropriate ones.
	 */
	public void iterateOrders() {
		for (TrackEntry e : caseOrderList) {
			OrderEntry or = (OrderEntry) e.getTypeSpecific();
			double scoreMax = 0.0;
			MotionLink lkMax = null;
			for (MotionLink lk : or.mlinkCandidates) {
				double v = lk.getValue();
				if (v > scoreMax) {
					scoreMax = v;
					lkMax = lk;
				}
			}
			if (lkMax != null) {
				MotionEntry me = (MotionEntry) lkMax.t1.getTypeSpecific();
				me.addOrderEntry(e);
				maxOrderLinks.add(lkMax);
			}
		}
		for (TrackEntry e : motionlist) {
			MotionEntry me = (MotionEntry) e.getTypeSpecific();
			double scoreMax = 0.0;
			MotionLink lkMax = null;
			for (MotionLink lk : me.orCandidates) {
				double v = lk.getValue();
				if (v > scoreMax) {
					scoreMax = v;
					lkMax = lk;
				}
			}
			if (lkMax != null) {
				for (MotionLink lk : me.orCandidates) {
					double v = lk.getValue();
					if (v >= TRUST_THRESHOLD || v >= scoreMax - RANGE_THRESHOLD) {
						me.addOrderEntry(lk.t2);
						if (!maxOrderLinks.contains(lk))
							maxOrderLinks.add(lk);
					}
				}
			}
		}
		for (MotionLink lk : maxOrderLinks) {
			caseOrderList.remove(lk.t2);
		}
	}

	/**
	 * decide which Motion-Opposition Link should be used. Eliminate those not appropriate ones.
	 */
	public void iterateOppositions() {
		for (TrackEntry e : caseOppositionList) {
			OppositionEntry op = (OppositionEntry) e.getTypeSpecific();
			double scoreMax = 0.0;
			MotionLink lkMax = null;
			for (MotionLink lk : op.mlinkCandidates) {
				double v = lk.getValue();
				if (v > scoreMax) {
					scoreMax = v;
					lkMax = lk;
				}
			}
			if (lkMax != null) {
				MotionEntry me = (MotionEntry) lkMax.t1.getTypeSpecific();
				me.addOppositionEntry(e);
				maxOppositionLinks.add(lkMax);
			}
		}
		for (TrackEntry e : motionlist) {
			MotionEntry me = (MotionEntry) e.getTypeSpecific();
			double scoreMax = 0.0;
			MotionLink lkMax = null;
			for (MotionLink lk : me.opCandidates) {
				double v = lk.getValue();
				if (v > scoreMax) {
					scoreMax = v;
					lkMax = lk;
				}
			}
			if (lkMax != null) {
				for (MotionLink lk : me.opCandidates) {
					double v = lk.getValue();
					if (v >= TRUST_THRESHOLD || v >= scoreMax - RANGE_THRESHOLD) {
						me.addOppositionEntry(lk.t2);
						if (!maxOppositionLinks.contains(lk))
							maxOppositionLinks.add(lk);
					}
				}
			}
		}
		for (MotionLink lk : maxOppositionLinks) {
			caseOppositionList.remove(lk.t2);
		}
	}

	/**
	 * decide which Motion-Reply Link should be used. Eliminate those not appropriate ones.
	 */
	public void iterateReplies() {
		// for every reply, keep only the top motion links in maxReplyLinks
		for (TrackEntry e : caseReplyList) {
			ReplyEntry re = (ReplyEntry) e.getTypeSpecific();
			double scoreMax = 0.0;
			MotionLink lkMax = null;
			for (MotionLink lk : re.mlinkCandidates) {
				double v = lk.getValue();
				if (v > scoreMax) {
					scoreMax = v;
					lkMax = lk;
				}
			}
			if (lkMax != null) {
				for (MotionLink lk : re.mlinkCandidates) {
					double v = lk.getValue();
					if (v >= TRUST_THRESHOLD || v >= scoreMax - RANGE_THRESHOLD) {
						if (!maxReplyLinks.contains(lk))
							maxReplyLinks.add(lk);
					}
				}
			}
			re.mlinkCandidates.clear();
		}
		// for every motion, keep only the top reply links in maxReplyLinks
		for (TrackEntry e : motionlist) {
			MotionEntry me = (MotionEntry) e.getTypeSpecific();
			double scoreMax = 0.0;
			MotionLink lkMax = null;
			for (MotionLink lk : me.repCandidates) {
				double v = lk.getValue();
				if (v > scoreMax) {
					scoreMax = v;
					lkMax = lk;
				}
			}
			if (lkMax != null) {
				for (MotionLink lk : me.repCandidates) {
					double v = lk.getValue();
					if (v >= TRUST_THRESHOLD || v >= scoreMax - RANGE_THRESHOLD) {
						if (!maxReplyLinks.contains(lk))
							maxReplyLinks.add(lk);
					}
				}
			}
			me.repCandidates.clear();
		}
		// rebuild the caseReplyList and reply candidate list for each reply from maxReplyLinks:
		caseReplyList.clear();
		for (MotionLink lk : maxReplyLinks) {
			ReplyEntry re = (ReplyEntry) lk.t2.getTypeSpecific();
			re.mlinkCandidates.add(lk);
			if (!caseReplyList.contains(lk.t2)) {
				caseReplyList.add(lk.t2);
			}
		}
		// add reply to motion: and combine motions that link to the same reply
		for (TrackEntry ht : caseReplyList) {
			ReplyEntry re = (ReplyEntry) ht.getTypeSpecific();
			if (re.mlinkCandidates.size() > 0) {
				// treating them as the same motion, combine them:
				// (1) find one that has not be combined into another:
				MotionLink lk0 = null;
				TrackEntry t0 = null;
				for (MotionLink lk : re.mlinkCandidates) {
					if (!motionlist.contains(lk.t1)) {
						MotionEntry me = (MotionEntry) lk.t1.getTypeSpecific();
						lk0 = lk;
						t0 = me.others.get(0);
						break;
					}
				}
				if (t0 == null) {
					lk0 = re.mlinkCandidates.get(0);
					t0 = lk0.t1;
				}
				MotionEntry m0 = (MotionEntry) t0.getTypeSpecific();
				// now we have the motion t0 in hand.
				// next, combine everything into it.

				while (!re.mlinkCandidates.isEmpty()) {
					MotionLink lk = re.mlinkCandidates.remove(0);
					if (lk == lk0)
						continue;
					if (motionlist.contains(lk.t1) && lk.t1 != t0) {
						if (m0.combine(lk.t1))
							motionlist.remove(lk.t1);
					}
				}
				m0.addReplyEntry(ht);
				re.mlinkCandidates.clear();
				re.mlinkCandidates.add(lk0);
			}
		}
		// maxReplyList.clear();
		// caseReplyList.clear();
	}

	public void trackMotionSequencesOld() {
		for (TrackEntry ms : motionlist) {
			findMatchingHearings(ms);
			findMatchingOrders(ms);
			findMatchingOppositions(ms);
			findMatchingReplies(ms);
			MotionEntry me = (MotionEntry) ms.getTypeSpecific();
			me.organizeSequence();
		}
	}

	// Find hearing entry from a given date
	List<TrackEntry> findHearing(Date _hdate) {
		List<TrackEntry> hrs = new ArrayList<TrackEntry>();
		for (TrackEntry e : caseHearingsList) {
			HearingEntry he = (HearingEntry) e.getTypeSpecific();
			if (he.motion == null)
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
	List<TrackEntry> findOrder(Date _hdate, int ndays, int subtype) {
		List<TrackEntry> hrs = new ArrayList<>();
		for (TrackEntry e : caseOrderList) {
			OrderEntry oe = (OrderEntry) e.getTypeSpecific();
			if (oe.content == null)
				continue;
			if (_hdate.before(e.date)) { // because orlist is date ordered, no more can be there
				break;
			}
			if (utils.DateTime.daysInBetween(e.date, _hdate) > ndays)
				continue;
			if (subtype != MotionEntry.TYPE_UNKNOWN && oe.subtype != MotionEntry.TYPE_UNKNOWN && subtype != oe.subtype)
				continue;
			hrs.add(e);
		}
		return hrs;
	}

	void findOppositionLinks(TrackEntry te) {
		MotionEntry ms = (MotionEntry) te.getTypeSpecific();
		Date hearingDate = ms.finalHearingDate;
		if (hearingDate == null) {
			hearingDate = addMonths(te.date, 1);
		}
		for (TrackEntry le : caseOppositionList) {
			if (le.date.before(te.date)) {
				continue;
			}
			if (hearingDate != null && le.date.after(hearingDate)) {
				break;
			}
			if (le.items == null) {
				continue;
			}
			String motion = (String) le.items.get("motion");
			if (motion == null) {
				continue;
			}
			double score = ms.matchMotionScore(motion);
			if (score <= IGNORE_THRESHOLD)
				continue;
			MotionLink ml = new MotionLink(te, le, score);
			addOppositionLink(ml);
		}
	}

	boolean findMatchingOppositions(TrackEntry te) {
		MotionEntry ms = (MotionEntry) te.getTypeSpecific();
		Date hearingDate = ms.finalHearingDate;
		int i = 0;
		while (i < caseOppositionList.size()) {
			TrackEntry le = caseOppositionList.get(i);
			OppositionEntry lm = (OppositionEntry) le.getTypeSpecific();
			if (hearingDate != null && le.date.after(hearingDate)) {
				break;
			}
			if (le.items == null) {
				i++;
				continue;
			}
			String motion = (String) le.items.get("motion");
			if (motion == null) {
				i++;
				continue;
			}
			if (le.date.before(te.date)) {
				i++;
				continue;
			}
			if (ms.matchMotion(motion)) {
				ms.addOppositionEntry(le);
				caseOppositionList.remove(i);
				continue;
			}
			i++;
		}
		if (ms.oppositions.size() > 0)
			return true;
		return false;
	}

	public void addMLink(MotionLink ml) {
		if (mHearingLinks == null) {
			mHearingLinks = new ArrayList<>();
		}
		mHearingLinks.add(ml);
	}

	public void addOrderLink(MotionLink ml) {
		if (mOrderLinks == null) {
			mOrderLinks = new ArrayList<>();
		}
		mOrderLinks.add(ml);
	}

	public void addOppositionLink(MotionLink ml) {
		if (mOppositionLinks == null) {
			mOppositionLinks = new ArrayList<>();
		}
		mOppositionLinks.add(ml);
	}

	public void addReplyLink(MotionLink ml) {
		if (mReplyLinks == null) {
			mReplyLinks = new ArrayList<>();
		}
		mReplyLinks.add(ml);
	}

	public void findHearingLinks(TrackEntry mte) {
		MotionEntry mm = (MotionEntry) mte.getTypeSpecific();
		Date lastDate = null;
		if (mm.hearingDate != null) {
			Pair p = new Pair(new Double(1.0), mm.hearingDate);
			mm.hearDates.add(p);
			lastDate = mm.hearingDate;
		} else {
			Date dt = (Date) mte.date.clone();
			int m = mte.date.getMonth();
			m += 2;
			if (m > 11) {
				m -= 12;
				int y = mte.date.getYear();
				y++;
				dt.setYear(y);
				dt.setMonth(m);
			} else {
				dt.setMonth(m);
			}
			lastDate = dt;
		}
		//		if (mte.text.startsWith("MOTION TO CONTINUE JURY TRIAL")) {
		//			System.out.print("");
		//		}
		for (TrackEntry hte : caseHearingsList) {
			HearingEntry hh = (HearingEntry) hte.getTypeSpecific();
			// hearing cannot before motion date:
			if (hte.date.before(mte.date)) {
				continue;
			}
			if (hh.oldDate != null) {// hearing reschedule or cancelling usually contains mention of scheduled date:
				if (hh.oldDate.after(lastDate)) {
					continue;
				}
				if (mm.hearingDate != null && !hh.oldDate.equals(lastDate)) {
					continue;
				}
			}
			//			if (mte.text.startsWith("NOTICE OF MOTION AND PLTF'S RENEWED MOTION FOR JUDGMENT ON THE PLEADINGS AS TO FIRST")) {
			//				if (hte.text.startsWith("LAW AND MOTION 302, PLAINTIFF ANTHONY LEE'S RENEWED MOTION FOR JUDGMENT ON THE PLEADINGS AS TO FIRST")) {
			//					System.out.print("");
			//				}
			//			}
			// actual hearing day (hte.date) cannot be too many days after the scheduled date (hearingdate):
			//			if (mm.hearingDate != null) {
			//				int daysAfter = utils.DateTime.daysInBetween(lastDate, hte.date);
			//				if (daysAfter >= MAX_ALLOWED_DAYS_WITH_GD) {
			//					break;
			//				}
			//			}
			// different subtypes cannot mix:
			if (hh.subtype != MotionEntry.TYPE_UNKNOWN && hh.subtype != mm.subtype) {
				continue;
			}
			double weight = 0.0;
			boolean bTooLate = false;
			if (mm.hearDates.size() > 0) {
				for (int i = mm.hearDates.size() - 1; i >= 0; i--) {
					Pair p = mm.hearDates.get(i);
					double v = (double) p.o1;
					Date d = (Date) p.o2;
					int daysAfter = utils.DateTime.daysInBetween(d, hte.date);
					if (daysAfter < MAX_ALLOWED_DAYS_WITH_GD) {
						if (daysAfter < -1 && !hh.offCalendar && hh.newDate == null)
							continue;// too early
						weight = v;
						break;
					}
					bTooLate = true;
				}
			} else {
				int daysAfter = utils.DateTime.daysInBetween(lastDate, hte.date);
				if (daysAfter < MAX_ALLOWED_DAYS_WITH_GD) {
					weight = 1.0;
				} else
					bTooLate = true;
			}
			if (weight <= IGNORE_THRESHOLD) {
				if (bTooLate)
					break;
				continue;
			}
			if (mte.filer != null && mte.filer.length() > 4 && hh.authors != null && hh.authors.length() > 4) {
				if (!mm.matchMotion(mte.filer, hh.authors)) {
					continue;
				}
				// reaches here means good match 
			} else if (mte.role != null && hh.authorRole != null) {
				if (!mte.role.equals(hh.authorRole)) {
					continue;
				}
				// reaches here means good match 
			}
			// for Motion:
			MotionEntry ms = (MotionEntry) mte.getTypeSpecific();
			Object mss = ms.getMotion();
			String mteMotionString = "";
			if (mss instanceof String) {
				mteMotionString = (String) mss;
			} else {
				@SuppressWarnings("unchecked")
				List<String> lst = (List<String>) mss;
				mteMotionString = lst.get(0);
			}
			List<Phrase> plistMte = TrackMotion.parseText(mteMotionString);
			// for Hearing:
			HearingEntry he = (HearingEntry) hte.getTypeSpecific();
			String hes = he.motion;
			List<Phrase> plistHte = TrackMotion.parseText(hes);
			Section sec1 = mte.sections.get(0);
			Section sec2 = hte.sections.get(0);
			List<ComparePhrases> cp1 = SFcomplex.compareTwoPhraseLists(sec1, sec2);
			List<ComparePhrases> cps = cp1;
			int scoreGraph = 0;
			if (cps != null) {
				scoreGraph = cps.get(0).score;
			}
			double score = mm.matchMotionScore(hh.motion);
			score *= weight;
			boolean bAgree = true;
			if (score <= IGNORE_THRESHOLD) {
				if (scoreGraph >= 1)
					bAgree = false;
				else
					continue;
			} else {
				if (scoreGraph < 1)
					bAgree = false;
			}
			if (!bAgree) {
				System.out.println("ScoreGraph:" + scoreGraph + ", score:" + score);
				System.out.println("mte:\n" + mte + "hte:\n" + hte);
				List<ComparePhrases> cp2 = SFcomplex.compareTwoPhraseListsComplete(sec1, sec2);
				if (cp2 != null && cp1 != null && cp2.get(0).score > cp1.get(0).score) {
					cps = cp2;
				}
				List<ComparePhrases> cp0 = SFcomplex.compareTwoPhraseLists(plistMte, plistHte);
				if (cp0 != null && cp0.get(0).score < scoreGraph) {
					System.out.println("cp0.score < scoreGraph");
				}
			}
			if (hh.offCalendar && score >= TRUST_THRESHOLD) {
				mm.setOffCalendar(hte.date);
				for (int i = mm.hearDates.size() - 1; i >= 0; i--) {
					Pair q = mm.hearDates.get(i);
					Date d = (Date) q.o2;
					if (hte.date.before(d)) {
						mm.hearDates.remove(i);
					} else {
						break;
					}
				}
				if (hh.newDate != null) {
					Pair p = new Pair(new Double(score), hh.newDate);
					mm.hearDates.add(p);
				}
				MotionLink ml = new MotionLink(mte, hte, score);
				ml.setComparePhrases(cps);
				addMLink(ml);
				return;
			}
			if (hh.newDate != null) {// moved to a new date
				Pair p = new Pair(new Double(score), hh.newDate);
				if (hh.newDate.after(lastDate)) {
					lastDate = hh.newDate;
					mm.hearDates.add(p);
				} else {
					boolean b = false;
					for (int i = mm.hearDates.size() - 1; i >= 0; i--) {
						Pair q = mm.hearDates.get(i);
						double v = (double) q.o1;
						Date d = (Date) q.o2;
						int daysAfter = utils.DateTime.daysInBetween(d, hh.newDate);
						if (daysAfter > 0) {
							mm.hearDates.add(i + 1, p);
							b = true;
							break;
						} else if (daysAfter == 0) {
							if (score > v) {
								mm.hearDates.remove(i);
								mm.hearDates.add(i, p);
								b = true;
								break;
							}
						}
					}
					if (!b) {
						mm.hearDates.add(0, p);
					}
				}
			}
			MotionLink ml = new MotionLink(mte, hte, score);
			ml.setComparePhrases(cps);
			addMLink(ml);
		}
	}

	void addMotionHearingCandidates() {
		for (MotionLink ml : mHearingLinks) {
			MotionEntry e1 = (MotionEntry) ml.t1.typeSpecific;
			HearingEntry e2 = (HearingEntry) ml.t2.typeSpecific;
			e1.addHearingCandidate(ml);
			e2.addMotionLinkCandidate(ml);
		}
	}

	void addMotionOrderCandidates() {
		for (MotionLink ml : mOrderLinks) {
			MotionEntry e1 = (MotionEntry) ml.t1.typeSpecific;
			OrderEntry e2 = (OrderEntry) ml.t2.typeSpecific;
			e1.addOrderCandidate(ml);
			e2.addMotionLinkCandidate(ml);
		}
	}

	void addMotionOppositionCandidates() {
		for (MotionLink ml : mOppositionLinks) {
			MotionEntry e1 = (MotionEntry) ml.t1.typeSpecific;
			OppositionEntry e2 = (OppositionEntry) ml.t2.typeSpecific;
			e1.addOppositionCandidate(ml);
			e2.addMotionLinkCandidate(ml);
		}
	}

	void addMotionReplyCandidates() {
		for (MotionLink ml : mReplyLinks) {
			MotionEntry e1 = (MotionEntry) ml.t1.typeSpecific;
			ReplyEntry e2 = (ReplyEntry) ml.t2.typeSpecific;
			e1.addReplyCandidate(ml);
			e2.addMotionLinkCandidate(ml);
		}
	}

	boolean findMatchingHearings(TrackEntry te) {
		//		if (ms.text.startsWith("NOTICE OF MOTION AND MOTION TO CONTINUE TRIAL DATE, ALL RELATED DEADLINES, INCLUDING DISCOVERY (TRANSACTION ID # 100045042)")) {
		//			System.out.print("");
		//		}
		MotionEntry ms = (MotionEntry) te.getTypeSpecific();
		Date hearingDate = ms.hearingDate;
		int i = 0;
		boolean ret = false;
		while (i < caseHearingsList.size() && hearingDate != null) {
			TrackEntry hte = caseHearingsList.get(i);
			HearingEntry lm = (HearingEntry) hte.getTypeSpecific();
			if (hte.date.before(te.date)) {
				i++;
				continue;
			}
			if (lm.oldDate != null) {
				if (!lm.oldDate.equals(hearingDate)) {
					i++;
					continue;
				}
			}
			int daysAfter = utils.DateTime.daysInBetween(hearingDate, hte.date);
			if (daysAfter >= MAX_ALLOWED_DAYS_WITH_GD) {
				i++;
				break;
			}
			if (lm.subtype != MotionEntry.TYPE_UNKNOWN && lm.subtype != ms.subtype) {
				i++;
				continue;
			}
			if (ms.matchMotion(lm.motion)) {
				if (te.filer != null && te.filer.length() > 4 && lm.authors != null && lm.authors.length() > 4) {
					if (!ms.matchMotion(te.filer, lm.authors)) {
						i++; // does not match, skip
						ms.matchMotion(te.filer, lm.authors);// this is for debugging
						continue;
					}
					// reaches here means good match 
				} else if (te.role != null && lm.authorRole != null) {
					if (!te.role.equals(lm.authorRole)) {
						i++; // does not match, skip
						te.role.equals(lm.authorRole);// this is for debugging
						continue;
					}
					// reaches here means good match 
				}
				// we check before and after the hearingDate differently:
				if (daysAfter <= 0) {
					if (lm.offCalendar) {
						ms.setOffCalendar(hte.date);
						ms.addHearingEntry(hte);
						caseHearingsList.remove(i);
						return true;
					}
					if (lm.newDate != null) {// moved to a new date
						hearingDate = lm.newDate;
						ms.addHearingEntry(hte);
						caseHearingsList.remove(i);
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
								ms.setOffCalendar(hte.date);
							} else if (s.startsWith("M")) {
								ms.setMoot();
							}
						}
						ret = true;
						//					break;
					}
					ms.addHearingEntry(hte);
					caseHearingsList.remove(i);
					continue;
				}
				// Otherwise, it's not for this Motion, ignore it:
				i++;
				continue;
			} else if (!ret && hte.date.equals(hearingDate)) {
				HearingEntry hr = lm;
				TrackEntry thr = hte;
				int ml = 0;
				if (hr.motion != null) {
					ml = ms.fuzzyMatchMotion(hr.motion);
				}
				List<TrackEntry> hrs = findHearing(hearingDate);
				if (hrs != null) {
					for (int k = 0; k < hrs.size(); k++) {
						TrackEntry he = hrs.get(k);
						HearingEntry het = (HearingEntry) he.getTypeSpecific();
						if (het == hr)
							continue;
						if (het.motion == null)
							continue;
						int kl = ms.fuzzyMatchMotion(het.motion);
						if (kl > ml) {
							ml = kl;
							hr = het;
							thr = he;
						}
					}
				}
				ms.addHearingEntry(thr);
				caseHearingsList.remove(thr);
				if (hr.offCalendar) {
					ms.setOffCalendar(thr.date);
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
								ms.setOffCalendar(thr.date);
							} else if (s.startsWith("M")) {
								ms.setMoot();
							}
						}
						ret = true;
					}
				}
				while (i < caseHearingsList.size()) {
					TrackEntry he = caseHearingsList.get(i);
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
			List<TrackEntry> hrs = findHearing(hearingDate);
			if (hrs.size() > 0) {
				TrackEntry hr = hrs.get(0);
				HearingEntry hr1 = (HearingEntry) hr.getTypeSpecific();
				if (hrs.size() > 1) {
					int ml = ms.fuzzyMatchMotion(hr1.motion);
					for (int k = 1; k < hrs.size(); k++) {
						TrackEntry he = hrs.get(k);
						HearingEntry hr2 = (HearingEntry) he.getTypeSpecific();
						if (hr2.motion == null)
							continue;
						int kl = ms.fuzzyMatchMotion(hr2.motion);
						if (kl > ml) {
							ml = kl;
							hr = he;
							hr1 = hr2;
						}
					}
				}
				ms.addHearingEntry(hr);
				caseHearingsList.remove(hr);
				if (hr1.offCalendar) {
					ms.setOffCalendar(hr.date);
					return true;
				}
				//						if (hr.hearingDate != null) {// moved to a new date
				//							hearingDate = hr.hearingDate;
				//							continue;
				//						}
				for (Pair p : hr1.gds) {
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

	public void findOrderLinks(TrackEntry te) {
		MotionEntry ms = (MotionEntry) te.getTypeSpecific();
		if (ms.isOff()) {
			return;
		}
		for (TrackEntry or : caseOrderList) {
			OrderEntry ore = (OrderEntry) or.getTypeSpecific();
			if (or.date.before(te.date)) {
				continue;
			}
			//			if (te.text.startsWith("NOTICE OF MOTION AND PLAINTIFFS MOTION TO SEAL PRELIMINARY-INJUNCTION PAPERS")) {
			//				if (or.text.startsWith("ORDER GRANTING DEFENDANTS' HOSIE RICE LLP, SPENCER HOSIE, AND DIANE S. RICE'S MOTION")) {
			//					System.out.print("");
			//				}
			//			}
			double weight = 0.0;
			int maxDays = MAX_ALLOWED_DAYS_NO_GD;
			if (ms.isTracked())
				maxDays = MAX_ALLOWED_DAYS_WITH_GD;
			for (int i = ms.hearDates.size() - 1; i >= 0; i--) {
				Pair p = ms.hearDates.get(i);
				Date d = (Date) p.o2;
				int daysAfter = utils.DateTime.daysInBetween(d, or.date);
				if (daysAfter < 0 || daysAfter >= maxDays) {
					continue;
				}
				weight = (double) p.o1;
			}
			double score = ms.matchMotionScore(ore.content);
			score *= weight;
			if (score <= IGNORE_THRESHOLD)
				continue;
			MotionLink ml = new MotionLink(te, or, score);
			addOrderLink(ml);
		}
	}

	boolean findMatchingOrders(TrackEntry te) {
		//		if (ms.text.startsWith("EX PARTE APPLICATION FOR ORDER TO FILE CROSS COMPLAINT AGAINST VIKING")) {
		//			System.out.print("");
		//		}
		MotionEntry ms = (MotionEntry) te.getTypeSpecific();
		Date hearingDate = ms.finalHearingDate;
		boolean ret = false;
		int i = 0;
		while (i < caseOrderList.size()) {
			TrackEntry or = caseOrderList.get(i);
			OrderEntry ore = (OrderEntry) or.getTypeSpecific();
			if (or.date.before(te.date)) {
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
			if (ms.matchMotion(ore.content)) {
				ms.addOrderEntry(or);
				caseOrderList.remove(i);
				for (Pair p : ore.gds) {
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
			List<TrackEntry> ors = findOrder(hearingDate, 2, ms.subtype);
			if (ors.size() > 0) {
				TrackEntry or = ors.get(0);
				OrderEntry ore = (OrderEntry) or.getTypeSpecific();
				if (ors.size() > 1) {
					int ml = ms.fuzzyMatchMotion(ore.content);
					for (int k = 1; k < ors.size(); k++) {
						TrackEntry or2 = ors.get(k);
						OrderEntry ore2 = (OrderEntry) or2.getTypeSpecific();
						int kl = ms.fuzzyMatchMotion(ore2.content);
						if (kl > ml) {
							ml = kl;
							or = or2;
							ore = ore2;
						}
					}
				}
				ms.addOrderEntry(or);
				caseOrderList.remove(or);
				for (Pair p : ore.gds) {
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

	public Date addMonths(Date d, int inc) {
		int y = d.getYear();
		int m = d.getMonth();
		m += inc;
		Date n = (Date) d.clone();
		if (m > 11) {
			m -= 12;
			y++;
			n.setYear(y);
			n.setMonth(m);
			return n;
		} else {
			n.setMonth(m);
			return n;
		}
	}

	void findReplieLinks(TrackEntry te) {
		MotionEntry ms = (MotionEntry) te.getTypeSpecific();
		Date hearingDate = ms.finalHearingDate;
		if (hearingDate == null) {
			hearingDate = addMonths(te.date, 2);
		}
		//		if (te.text.startsWith("MOTION / NOTICE OF MOTION AND MOTION TO SEAL DEFENDANTS' VERIFIED CROSS-COMPLAINT")) {
		//			System.out.print("");
		//		}
		for (int i = 0; i < caseReplyList.size(); i++) {
			TrackEntry le = caseReplyList.get(i);
			//			if (le.text.startsWith("REPLY MEMORANDUM IN SUPPORT OF PLAINTIFF/CROSS-DEFENDANTS SPECIAL MOTION TO STRIKE")) {
			//				System.out.print("");
			//			}
			if (le.date.before(te.date)) {
				continue;
			}
			if (hearingDate != null && le.date.after(hearingDate)) {
				break;
			}
			if (le.items == null) {
				continue;
			}
			String motion = (String) le.items.get("motion");
			if (motion == null) {
				continue;
			}
			double score = ms.matchMotionScore(motion);
			if (score <= IGNORE_THRESHOLD)
				continue;
			MotionLink ml = new MotionLink(te, le, score);
			addReplyLink(ml);
		}
	}

	boolean findMatchingReplies(TrackEntry te) {
		MotionEntry ms = (MotionEntry) te.getTypeSpecific();
		Date hearingDate = ms.finalHearingDate;
		for (int i = 0; i < caseReplyList.size(); i++) {
			TrackEntry le = caseReplyList.get(i);
			ReplyEntry lm = (ReplyEntry) le.getTypeSpecific();
			if (hearingDate != null && le.date.after(hearingDate)) {
				break;
			}
			if (le.items == null) {
				continue;
			}
			String motion = (String) le.items.get("motion");
			if (motion == null) {
				continue;
			}
			if (le.date.before(te.date)) {
				continue;
			}
			if (ms.matchMotion(motion)) {
				ms.addReplyEntry(le);
				caseReplyList.remove(i);
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

	/**
	 * Similarity between two motion-related entries, including:
	 *  Motion, Hearing, Order, Memorandom, declaration, opposition, reply.
	 * Similar means: two entries refer to the same motion
	 * value range: {0.0, 1.0}
	 * 
	 * @author yanyo
	 *
	 */
	public static class MotionLink {
		TrackEntry t1;
		TrackEntry t2;
		double value;
		double increment;
		List<ComparePhrases> cps;

		double getValue() {
			return value;
		}

		public MotionLink(TrackEntry _t1, TrackEntry _t2, double _v) {
			t1 = _t1;
			t2 = _t2;
			value = _v;
			increment = 0;
		}

		public void setComparePhrases(List<ComparePhrases> _cp) {
			cps = _cp;
		}

		public void inc(double _inc) {
			increment += _inc;
		}

		public void realizeIncrement() {
			value += increment;
			if (value > 1.0) {
				value = 1.0;
			}
			if (value < 0) {
				value = 0.0;
			}
			increment = 0;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(t1);
			sb.append(t2);
			sb.append("Score: " + value);
			return sb.toString();
		}
	}
}
