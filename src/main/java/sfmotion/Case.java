package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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

import common.Role;
import utils.Pair;

public class Case {
	//	static final String regSep = "AS TO|FILED BY|\\(TRANSACTION.+?\\)|\\(FEE.+?\\|HEARING SET|)";
	public static Pattern ptransactionID = Pattern.compile("\\(TRANSACTION\\sID\\s\\#\\s+(\\d+)\\)", Pattern.CASE_INSENSITIVE);
	static final String[] ROLENAMES = { "APPELLANT", "CROSS DEFENDANT", "CROSS COMPLAINANT", "DEFENDANT", "PLAINTIFF", "OTHER", };
	static int MAX_ALLOWED_DAYS_WITH_GD = 4;
	static int MAX_ALLOWED_DAYS_NO_GD = 14;

	Map<String, List<SFMotionEntry>> mdamap = new TreeMap<>();
	Map<String, List<SFMotionEntry>> transactions = new TreeMap<>();
	public List<SFMotionEntry> entries = new ArrayList<>();
	List<List<SFMotionEntry>> daily; // entries of the same day
	//	List<ComplaintPetition> claims; // Claims: Complaint, counter-claims, amended complaint,
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
	List<SFMotionEntry> otherMilist; // declarations, proof of services, from both parties lumped together

	String casetype; // PERSONAL INJURY/PROPERTY DAMAGE - VEHICLE RELATED
	String caseSubtype; // VEHICLE RELATED
	public String id;
	List<String> names = new ArrayList<>();
	List<Pair> namep = new ArrayList<>();
	ComplaintEntry complaint = null;
	List<Party> partylist;
	List<Entity> gel = new ArrayList<>();
	List<Link> glk = new ArrayList<>();
	List<PartyCluster> clusters = new ArrayList<>();
	public Date lastDate; // date of the last entry;

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("args: infile outfile");
			System.exit(-1);
		}
		String infile = args[0];
		String outfile = args[1];
		BufferedReader br = new BufferedReader(new FileReader(infile));
		String line;
		String caseID = null;
		List<SFMotionEntry> entries = new ArrayList<>();
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (caseID == null) {
				caseID = items[0];
			}
			SFMotionEntry en = new SFMotionEntry(items[1], items[2]);
			entries.add(en);
		}
		br.close();
		Case cs = new Case(caseID, entries);

		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		cs.analyze();
		cs.convertToPairs();
		cs.findParties();
		//		cs.writePairs(wr);
		//		cs.writeNames(wr);
		//		cs.writeParties(wr);
		Collections.sort(cs.gel);
		int cnt = cs.splitConcatenatedEntities();
		System.out.println("Split Concatenates: " + cnt);
		int cnt1 = cs.splitAKAEntities();
		System.out.println("Split AKAs: " + cnt1);
		cs.cleanGel();
		cs.relinkParyClusters();
		cs.writeGlobalEntities(wr);
		cs.writePartyClusters(wr);
		wr.close();
	}

	public Case(String _id) {
		id = _id;
	}

	Entity addToGel(Entity e) {
		for (Entity t : gel) {
			if (t.equals(e)) {
				t.combine(e);
				return t;
			}
		}
		gel.add(e);
		return e;
	}

	public Case(String _id, List<SFMotionEntry> _es) {
		id = _id;
		entries = _es;
	}

	public void addEntry(SFMotionEntry _e) {
		entries.add(_e);
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
		for (SFMotionEntry e : entries) {
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
				List<SFMotionEntry> list = transactions.get(tid);
				if (list != null) {
					for (SFMotionEntry e : list) {
						if (e.type == null) {
							motionlist.remove(e);
						} else if (ms != e && e.type.equals(SFMotionEntry.MOTION)) {
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
			List<SFMotionEntry> list = transactions.get(tid);
			List<MotionEntry> mlist = new ArrayList<>();
			for (SFMotionEntry e : list) {
				boolean be = false;
				if (e.type != null && e.type.equals(SFMotionEntry.MOTION)) {
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
			for (SFMotionEntry e : list) {
				if (e.type == null || !e.type.equals(SFMotionEntry.MOTION)) {
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
				List<SFMotionEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (SFMotionEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		for (OppositionEntry ms : oppositions) {
			String tid = ms.transactionID;
			if (tid != null) {
				List<SFMotionEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (SFMotionEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		for (OrderEntry ms : orlist) {
			String tid = ms.transactionID;
			if (tid != null) {
				List<SFMotionEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (SFMotionEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		for (HearingEntry ms : hrlist) {
			String tid = ms.transactionID;
			if (tid != null) {
				List<SFMotionEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (SFMotionEntry e : list) {
						entries.remove(e);
					}
				}
			}
		}
		for (ReplyEntry ms : replies) {
			String tid = ms.transactionID;
			if (tid != null) {
				List<SFMotionEntry> list = transactions.get(tid);
				if (list != null) {
					count += list.size() - 1;// -1 to not including ms itself
					for (SFMotionEntry e : list) {
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
			Entity e = gel.get(i);
			Matcher m = pPrep.matcher(e.nameNormalized);
			if (m.find()) {
				gel.remove(i);
				continue;
			}
			i++;
		}
	}

	int splitConcatenatedEntities() {
		int cnt = 0;
		boolean more = false;
		do {
			nextRound: for (int i = 0; i < gel.size() - 1; i++) {
				Entity e = gel.get(i);
				more = false;
				for (int j = i + 1; j < gel.size(); j++) {
					Entity ee = gel.get(j);
					Entity e2 = ee.decomposeHead(e);
					if (e2 != null) {
						e.combine(ee);
						e2 = addToGel(e2);
						ee.children = new ArrayList<>();
						ee.children.add(e);
						ee.children.add(e2);
						gel.remove(j);
						cnt++;
						Collections.sort(gel);
						more = true;
						break nextRound;
					} else {
						break;
					}
				}
			}
		} while (more);
		return cnt;
	}

	int splitAKAEntities() {
		int cnt = 0;
		int beginSize = gel.size();
		for (int i = 0; i < beginSize; i++) {
			Entity e = gel.get(i);
			String[] split = e.name.split("\\sAKA\\s");
			if (split.length > 1) {
				e.name = split[0].trim();
				e.nameNormalized = Entity.getNormalizedName(e.name);
				e.reparseName();
				for (int j = 1; j < split.length; j++) {
					String rl = "NO_Role";
					if (e.roles.size() > 0)
						rl = e.roles.get(0);
					Entity ee = new Entity(split[j].trim(), "AKA-" + rl, e.type, e.count);
					cnt++;
					int idx = gel.indexOf(ee);
					if (idx >= 0) {
						Entity eee = gel.get(idx);
						eee.combine(ee);
					} else {
						gel.add(ee);
					}
				}
			}
		}
		return cnt;
	}

	public void writeGlobalEntities(BufferedWriter wr) throws IOException {
		wr.write("\n============== Global Entities:===================\n\n");
		for (Entity e : gel) {
			wr.write(e + "\n");
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(id + "\n");
		for (SFMotionEntry e : entries) {
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

	public void sortEntries() {
		Collections.sort(entries);
	}

	public void analyze() throws IOException {
		SFMotionEntry e = entries.get(0);
		//		complaint = new Complaint(id + "\t" + e.sdate + "\t" + e.text);

		complaint = new ComplaintEntry(e.sdate, e.text);

		for (int i = 1; i < entries.size(); i++) {
			String[] split = entries.get(i).text.split("FILED BY");
			if (split.length >= 2) {
				names.add(split[1].replaceAll("^\\W+|\\W+$", ""));
			}
		}
		cleanUpNames();
		for (int i = 0; i < names.size(); i++) {
			String[] split = names.get(i).split("AS TO");
			if (split.length > 1) {
				names.remove(i);
				names.add(i, split[0].replaceAll("^\\W+|\\W+$", ""));
				names.add(split[1].replaceAll("^\\W+|\\W+$", ""));
			}
		}
		for (int i = 0; i < names.size(); i++) {
			String[] split = names.get(i).split(LLCP);
			if (split.length > 1) {
				names.remove(i);
				names.add(i, split[0].replaceAll("^\\W+|\\W+$", ""));
				for (int j = 1; j < split.length; j++)
					names.add(split[j].replaceAll("^\\W+|\\W+$", ""));
			}
		}
		for (int i = 0; i < names.size(); i++) {
			String[] split = names.get(i).split("HEARING SET|JURY DEMANDED|\\(Fee|SERVED|\\(TRANSACTION");
			if (split.length > 1) {
				names.remove(i);
				names.add(i, split[0].replaceAll("^\\W+|\\W+$", ""));
			}
		}
		Collections.sort(names);
	}

	private void cleanUpNames() {
		List<String> namec = new ArrayList<>();
		int cnt = 0;
		for (String name : names) {
			String[] split = name.split("\\s+IS\\s+");
			namec.add(split[0].trim());
			if (split.length > 1) {
				cnt++;
			}
		}
		if (cnt > 0) {
			names = namec;
		}
	}

	public List<SFMotionEntry> getEntries() {
		return entries;
	}

	public String getID() {
		return id;
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

	void findParties() {
		String sp = complaint.plaintiffs;
		PartyCluster pp = new PartyCluster(sp, "PLAINTIFF", 1, gel);
		pp.parse();
		clusters.add(pp);
		String sd = complaint.defendants;
		PartyCluster pd = new PartyCluster(sd, "DEFENDANT", 1, gel);
		pd.parse();
		clusters.add(pd);
		for (Pair p : namep) {
			String s = (String) p.o1;
			Integer cnt = (Integer) p.o2;
			String ns = s;
			String role = null;
			for (String r : ROLENAMES) {
				if (s.startsWith(r)) {
					ns = s.substring(r.length()).replaceAll("^\\W+|\\W+$", "");
					role = r;
					break;
				}
			}
			PartyCluster pc = new PartyCluster(ns, role, cnt, gel);
			pc.parse();
			clusters.add(pc);
			//			List<Pair> plist = decomposePartyList(ns);
			//			partylist = new ArrayList<>();
			//			for (Pair pp : plist) {
			//				addToPartyList((String) pp.o1, (String) pp.o2, role, cnt, partylist);
			//			}
		}
	}

	private void writePartyClusters(BufferedWriter wr) throws IOException {
		wr.write("============================= Party Clusters: ===============================\n\n");
		for (PartyCluster pc : clusters) {
			wr.write(pc.toString() + "\n");
		}
	}

	private void relinkParyClusters() {
		for (PartyCluster pc : clusters) {
			pc.useLeafEntities();
		}
	}

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
		for (SFMotionEntry e : entries) {
			Matcher mm = ptransactionID.matcher(e.text);
			if (mm.find()) {
				String transactionID = mm.group(1);
				List<SFMotionEntry> list = transactions.get(transactionID);
				if (list == null) {
					list = new ArrayList<>();
					transactions.put(transactionID, list);
				}
				list.add(e);
			}
		}
	}

	static class Entity implements Comparable<Entity> {
		String name;
		String nameNormalized;
		List<String> roles = new ArrayList<>(); // defendant, plaintiff, etc.
		String type; // INDIVIDUAL, ENTITY, UNKNOWN
		int min = 1; // minimum number of entities
		int count;
		int entityCount = 1;
		String surname;
		String givenname;
		String midname;
		List<Entity> children;

		public Entity(String _name, String _role, String _type, int _count) {
			if (_role != null)
				roles.add(_role);
			init(_name, roles, _type, _count, 1);
		}

		public Entity getLeaf() {
			if (children != null) {
				return children.get(0).getLeaf();
			} else {
				return this;
			}
		}

		public Entity(String _name, String _role, String _type, int _count, int _entityCount) {
			if (_role != null)
				roles.add(_role);
			init(_name, roles, _type, _count, _entityCount);
		}

		public Entity(String _name, List<String> _roles, String _type, int _count) {
			init(_name, _roles, _type, _count, 1);
		}

		void init(String _name, List<String> _roles, String _type, int _count, int _entityCount) {
			name = _name.replaceAll("\\,+", ",");
			if (_roles != null && _roles != roles) {
				roles.addAll(_roles);
			}
			type = _type;
			count = _count;
			entityCount = _entityCount;
			nameNormalized = getNormalizedName(name);
			if (nameNormalized.startsWith(" ")) {
				System.out.print(" debug ");
			}
			parseName();
		}

		static String getNormalizedName(String _s) {
			return _s.replaceAll("\\p{Punct}", " ").replaceAll("\\s+", " ").trim();
		}

		private void parseName() {
			if (surname != null)
				return;
			if (type.equalsIgnoreCase("INDIVIDUAL")) {
				if (parseIndividualName())
					return;
			}
			if (type.equalsIgnoreCase("ENTITY")) {
				return;
			}
			if (parseIndividualName()) {
				type = "INDIVIDUAL";
			}
		}

		private void reparseName() {
			surname = null;
			givenname = null;
			midname = null;
			parseName();
		}

		boolean parseIndividualName() {
			int idx = name.indexOf(',');
			if (idx >= 0) {
				String n1 = name.substring(0, idx).trim();
				String[] n1split = n1.split("\\s+");
				if (n1split.length > 1) {
					return false;
				}
				surname = n1;
				String rest = name.substring(idx + 1).trim();
				String[] split = rest.split("\\s+");
				givenname = split[0].trim();
				if (split.length > 1) {
					midname = split[1].replaceAll("\\W+", "");
				}
				return true;
			} else {
				String[] split = this.nameNormalized.split("\\s+");
				if (split.length == 3 && split[1].length() == 1) {
					surname = split[2];
					midname = split[1];
					givenname = split[0];
					return true;
				}
			}
			return false;
		}

		Entity decomposeHead(Entity e) {
			if (nameNormalized.startsWith(e.nameNormalized)) {
				String left = nameNormalized.substring(e.nameNormalized.length()).trim();
				left = left.trim();
				if (left.length() > 4) {
					if (Character.isLetterOrDigit(left.charAt(0))) {// in the middle of a word
						return null;
					}
					String[] tks = e.nameNormalized.split("\\s+");
					String lastTk = tks[tks.length - 1];
					int idx = name.indexOf(lastTk);
					int start = idx + lastTk.length() + 1;
					String name2 = name.substring(start);
					Entity e2 = new Entity(name2, this.roles, this.type, this.count);
					return e2;
				}
			}
			return null;
		}

		void addRoles(Entity e) {
			for (String r : e.roles) {
				if (!roles.contains(r)) {
					roles.add(r);
				}
			}
		}

		void setMin(int _min) {
			min = _min;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(nameNormalized + "\n");
			if (surname != null) {
				sb.append("\t(" + surname + ", " + givenname);
				if (midname != null) {
					sb.append(" " + midname);
				}
				sb.append(")\n");
			}
			if (roles.size() > 0) {
				sb.append("\t\tRole: ");
				for (String r : roles) {
					sb.append(r + ", ");
				}
				sb.append("\n");
			}
			sb.append("\t\tType: " + type + "\n");
			sb.append("\t\tCount: " + count + "\n");
			if (entityCount > 1) {
				sb.append("\t\tentityCount: " + entityCount + "\n");
			}
			return sb.toString();
		}

		@Override
		public int compareTo(Entity o) {
			return nameNormalized.compareToIgnoreCase(o.nameNormalized);
		}

		public boolean equals(Object o) {
			if (!(o instanceof Entity)) {
				return false;
			}
			Entity e = (Entity) o;
			if (surname != null && e.surname != null) {
				if (!surname.equalsIgnoreCase(e.surname)) {
					return false;
				}
				if (!givenname.equalsIgnoreCase(e.givenname)) {
					return false;
				}
				if (midname != null && e.midname != null) {
					if (midname.length() == 1 || e.midname.length() == 1) {
						if (midname.charAt(0) != e.midname.charAt(0)) {
							return false;
						}
					} else {
						if (!midname.equalsIgnoreCase(e.midname)) {
							return false;
						}
					}
				}
				return true;
			}
			return nameNormalized.equalsIgnoreCase(e.nameNormalized);
		}

		public void combine(Entity e) {
			addRoles(e);
			parseName();
			count += e.count;
		}
	}

	static class Link {
		Entity e1;
		Entity e2;
		String link;// AGENT, OWNER, TRUSTEE

		public Link(Entity _e1, Entity _e2, String _l) {
			if (_e1 == null || _e2 == null) {
				System.out.print(" debug ");
			}
			e1 = _e1;
			e2 = _e2;
			link = _l;
		}

		public boolean equals(Object o) {
			if (!(o instanceof Link))
				return false;
			Link lk = (Link) o;
			return e1.equals(lk.e1) && e2.equals(lk.e2) && link.equalsIgnoreCase(lk.link);
		}

		public boolean sameArgs(Link lk) {
			return e1.equals(lk.e1) && e2.equals(lk.e2);
		}

		public void relink() {
			e1 = e1.getLeaf();
			e2 = e2.getLeaf();
		}

		public String toString() {
			return link + " : {" + e1.name + "<==>" + e2.name + "}";
		}
	}

	static class PartyCluster {
		String text;
		String role;
		int count;
		List<Entity> gel; // global entity list
		List<Entity> list; // global entity list
		List<Link> lklist = new ArrayList<>();

		public PartyCluster(String _text, String _role, int _count, List<Entity> _gel) {
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
			for (Entity e : list) {
				sb.append("\t" + e.toString() + "\n");
			}
			if (lklist.size() > 0) {
				sb.append("Links:\n");
				for (Link lk : lklist) {
					sb.append("\t" + lk.toString() + "\n");
				}
			}
			return sb.toString();
		}

		public void useLeafEntities() {
			List<Entity> nlist = new ArrayList<>();
			for (Entity e : list) {
				if (e.children != null && e.children.size() > 1) {
					for (Entity c : e.children) {
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
				for (Link lk : lklist) {
					lk.relink();
				}
			}
		}

		Entity dba(String ss) {
			Entity currentEntity = null;
			String[] dbas = ss.split("\\b(AND )?DBA\\b");
			if (dbas.length > 1) {
				Entity e = new Entity(dbas[0].trim(), role, "INDIVIDUAL", count);
				e = addToGel(e);
				if (!list.contains(e))
					list.add(e);
				currentEntity = e;
				for (int i = 1; i < dbas.length; i++) {
					String ds = dbas[i].trim();
					if (ds.length() < 4)
						continue;
					e = new Entity(ds, role, "ENTITY", count);
					e = addToGel(e);
					if (!list.contains(e))
						list.add(e);
					Link lk = new Link(currentEntity, e, "DBA");
					lklist.add(lk);
				}
			}
			return currentEntity;
		}

		private Entity findCurrentEntity() {
			for (int i = list.size() - 1; i >= 0; i--) {
				Entity e = list.get(i);
				if (e.type.equalsIgnoreCase("INDIVIDUAL"))
					return e;
			}
			return null;
		}

		void parse() {
			String[] sperr = text.split("ERRONEOUSLY SUED");
			if (sperr.length > 1) {
				text = sperr[0];
			}
			String[] splits = text.split(regexPartyTypeBreak);
			list = new ArrayList<Entity>();
			Entity currentEntity = null;
			for (String s : splits) {
				String ss = s.replaceAll("^\\W+|\\W+$", "");
				if (ss.length() <= 3)
					continue;
				Entity dbaCurrent = dba(ss);
				if (dbaCurrent != null) {
					currentEntity = dbaCurrent;
					continue;
				}
				Matcher m = pIndividual.matcher(ss);
				if (m.find()) {
					ss = ss.substring(0, m.start()).trim().replaceAll("\\,$", "").trim();
					Entity e = new Entity(ss, role, "INDIVIDUAL", count);
					e = addToGel(e);
					if (!list.contains(e))
						list.add(e);
					currentEntity = e;
					continue;
				}
				m = pAsA.matcher(ss);
				if (m.find()) {
					String rel = m.group("relation");
					String[] rels = rel.split("/");
					ss = ss.substring(m.end()).replaceAll("^\\W+|\\W+$", "").trim();
					Matcher mm = pEntity.matcher(ss);
					Entity e;
					if (mm.find()) {
						ss = ss.substring(0, mm.start()).trim().replaceAll("\\,$", "").trim();
						e = new Entity(ss, role, "ENTITY", count, 2);
					} else {
						e = new Entity(ss, role, "ENTITY", count);
					}
					e = addToGel(e);
					if (!list.contains(e))
						list.add(e);
					// add relations:
					if (currentEntity == null) {
						currentEntity = findCurrentEntity();
					}
					if (currentEntity != null) {
						for (String ls : rels) {
							Link lk = new Link(currentEntity, e, ls.trim());
							lklist.add(lk);
						}
					}
					continue;
				}
				m = pSuedAs.matcher(ss);
				if (m.find()) {
					String s1 = ss.substring(0, m.start()).replaceAll("\\W+$", "").trim();
					String s2 = ss.substring(m.end()).replaceAll("\\W+$", "").trim();
					Entity e1 = new Entity(s1, role, "ENTITY", count);
					Entity e2 = new Entity(s2, "SuedAs", "ENTITY", count);
					e1 = addToGel(e1);
					if (!list.contains(e1))
						list.add(e1);
					e2 = addToGel(e2);
					if (!list.contains(e2))
						list.add(e2);
					// add relations:
					Link lk = new Link(e1, e2, "SuedAs");
					lklist.add(lk);
					continue;
				}
				m = pEntity.matcher(ss);
				if (m.find()) {
					ss = ss.substring(0, m.start()).trim().replaceAll("\\,$", "").trim();
					if (ss.length() < 2) {
						// the previous entity is an entity
						if (list.size() > 0) {
							Entity ep = list.get(list.size() - 1);
							if (ep.type.equals("UNKNOWN")) {
								ep.type = "ENTITY";
							}
						}
					} else {
						Entity e = new Entity(ss, role, "ENTITY", count);
						e = addToGel(e);
						if (!list.contains(e))
							list.add(e);
					}
					continue;
				}
				m = pInc.matcher(ss);
				if (m.find()) {
					Entity e = new Entity(ss.replaceAll("^\\W+|\\W+$", ""), role, "ENTITY", count);
					e = addToGel(e);
					if (!list.contains(e))
						list.add(e);
					continue;
				}

				Entity dba = dba(ss);
				if (dba != null) {
					currentEntity = dba;
				} else {
					Entity e = new Entity(ss, role, "UNKNOWN", count);
					e = addToGel(e);
					if (!list.contains(e))
						list.add(e);
				}
			}
		}

		Entity addToGel(Entity e) {
			for (Entity t : gel) {
				if (t.equals(e)) {
					t.combine(e);
					return t;
				}
			}
			gel.add(e);
			return e;
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
