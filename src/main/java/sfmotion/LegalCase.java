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

import utils.Pair;

public class LegalCase {
	//	static final String regSep = "AS TO|FILED BY|\\(TRANSACTION.+?\\)|\\(FEE.+?\\|HEARING SET|)";
	static final String[] ROLENAMES = { "APPELLANT", "CROSS DEFENDANT", "CROSS COMPLAINANT", "DEFENDANT", "PLAINTIFF", "CLAIMANT", "OTHER", };
	static int MAX_ALLOWED_DAYS_WITH_GD = 4;
	static int MAX_ALLOWED_DAYS_NO_GD = 14;

	static Map<String, CaseNames> mapParty;
	public Map<String, List<TrackEntry>> mdamap = new TreeMap<>();
	public List<TrackEntry> entries;
	public List<List<TrackEntry>> daily; // entries of the same day
	// Motions In limine grouped together:

	public String casetype; // PERSONAL INJURY/PROPERTY DAMAGE - VEHICLE RELATED
	public String caseSubtype; // VEHICLE RELATED
	public String id;
	public List<String> names = new ArrayList<>();
	public List<Pair> namep = new ArrayList<>();
	public ComplaintEntry complaint = null;
	public List<CaseEntity> gel = new ArrayList<>();
	public List<CaseLink> glk = new ArrayList<>();
	public List<PartyCluster> clusters = new ArrayList<>();
	public List<PersonName> judges = new ArrayList<>();
	public Date lastDate; // date of the last entry;

	public String getID() {
		return id;
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("args: infile outfile partyfile");
			System.exit(-1);
		}
		String infile = args[0];
		String outfile = args[1];
		String partyfile = args[2];

		mapParty = readParties(partyfile);
		List<LegalCase> cases = readCases(infile);
		processCases(cases);
		writeCases(cases, outfile);
	}

	static void writeCases(List<LegalCase> cases, String outfile) throws IOException {
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (LegalCase cs : cases) {
			cs.writeGlobalEntities(wr);
			cs.writePartyClusters(wr);
		}
		wr.close();
	}

	static void processCases(List<LegalCase> cases) throws IOException {
		for (LegalCase cs : cases) {
			cs.analyze();
			cs.convertToPairs();
			cs.findParties();
			Collections.sort(cs.gel);
			int cnt = cs.splitConcatenatedEntities();
			System.out.println("Split Concatenates: " + cnt);
			int cnt1 = cs.splitAKAEntities();
			System.out.println("Split AKAs: " + cnt1);
			cs.cleanGel();
			cs.relinkPartyClusters();
		}
	}

	static List<LegalCase> readCases(String infile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(infile));
		List<LegalCase> cases = new ArrayList<>();
		String caseID = "";
		String line;
		List<TrackEntry> entries = null;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (!items[0].equals(caseID)) {
				caseID = items[0];
				entries = new ArrayList<>();
				LegalCase cs = new LegalCase(caseID, entries);
				cases.add(cs);
			}
			TrackEntry en = new TrackEntry(items[1], items[2]);
			entries.add(en);
		}
		br.close();
		return cases;
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

	public LegalCase(String _id) {
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

	public LegalCase(String _id, List<TrackEntry> _es) {
		id = _id;
		entries = _es;
	}

	public void addEntry(TrackEntry _e) {
		entries.add(_e);
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

	int splitConcatenatedEntities() {
		int cnt = 0;
		boolean more = false;
		do {
			nextRound: for (int i = 0; i < gel.size() - 1; i++) {
				CaseEntity e = gel.get(i);
				more = false;
				for (int j = i + 1; j < gel.size(); j++) {
					CaseEntity ee = gel.get(j);
					CaseEntity e2 = ee.decomposeHead(e);
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
			CaseEntity e = gel.get(i);
			String[] split = e.name.split("\\sAKA\\s");
			if (split.length > 1) {
				e.name = split[0].trim();
				e.nameNormalized = CaseEntity.getNormalizedName(e.name);
				e.reparseName();
				for (int j = 1; j < split.length; j++) {
					String role = "";
					if (e.roles != null && e.roles.size() > 0) {
						role = e.roles.get(0);
					}
					CaseEntity ee = new CaseEntity(split[j].trim(), "AKA-" + role, e.type, e.count);
					cnt++;
					int idx = gel.indexOf(ee);
					if (idx >= 0) {
						CaseEntity eee = gel.get(idx);
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
		return sb.toString();
	}

	public void sortEntries() {
		Collections.sort(entries);
	}

	public void analyze() throws IOException {
		TrackEntry e = entries.get(0);
		//		complaint = new Complaint(id + "\t" + e.sdate + "\t" + e.text);

		//		complaint = new ComplaintEntry(e.sdate, e.text);

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
		PartyCluster pp = new PartyCluster(this.id, sp, "PLAINTIFF", 1, gel);
		pp.parse();
		clusters.add(pp);
		String sd = complaint.defendants;
		if (sd != null) {
			PartyCluster pd = new PartyCluster(this.id, sd, "DEFENDANT", 1, gel);
			if (sd.startsWith("HART, BRENDAN TAYLOR, DAVID")) {
				System.out.print("");
			}
			pd.parse();
			clusters.add(pd);
		}
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
			PartyCluster pc = new PartyCluster(this.id, ns, role, cnt, gel);
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

	private void relinkPartyClusters() {
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

		void parse() {
			String[] sperr = text.split("ERRONEOUSLY SUED");
			if (sperr.length > 1) {
				text = sperr[0];
			}
			String[] splits = text.split(regexPartyTypeBreak);
			list = new ArrayList<CaseEntity>();
			CaseEntity currentEntity = null;
			for (String s : splits) {
				String ss = s.replaceAll("^\\W+|\\W+$", "");
				if (ss.length() <= 3)
					continue;
				CaseEntity dbaCurrent = dba(ss);
				if (dbaCurrent != null) {
					currentEntity = dbaCurrent;
					continue;
				}
				Matcher m = pIndividual.matcher(ss);
				if (m.find()) {
					ss = ss.substring(0, m.start()).trim().replaceAll("\\,$", "").trim();
					CaseEntity e = new CaseEntity(ss, role, "INDIVIDUAL", count);
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
					CaseEntity e;
					if (mm.find()) {
						ss = ss.substring(0, mm.start()).trim().replaceAll("\\,$", "").trim();
						e = new CaseEntity(ss, role, "ENTITY", count, 2);
					} else {
						e = new CaseEntity(ss, role, "ENTITY", count);
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
							CaseLink lk = new CaseLink(currentEntity, e, ls.trim());
							lklist.add(lk);
						}
					}
					continue;
				}
				m = pSuedAs.matcher(ss);
				if (m.find()) {
					String s1 = ss.substring(0, m.start()).replaceAll("\\W+$", "").trim();
					String s2 = ss.substring(m.end()).replaceAll("\\W+$", "").trim();
					CaseEntity e1 = new CaseEntity(s1, role, "ENTITY", count);
					CaseEntity e2 = new CaseEntity(s2, "SuedAs", "ENTITY", count);
					e1 = addToGel(e1);
					if (!list.contains(e1))
						list.add(e1);
					e2 = addToGel(e2);
					if (!list.contains(e2))
						list.add(e2);
					// add relations:
					CaseLink lk = new CaseLink(e1, e2, "SuedAs");
					lklist.add(lk);
					continue;
				}
				m = pEntity.matcher(ss);
				if (m.find()) {
					ss = ss.substring(0, m.start()).trim().replaceAll("\\,$", "").trim();
					if (ss.length() < 2) {
						// the previous entity is an entity
						if (list.size() > 0) {
							CaseEntity ep = list.get(list.size() - 1);
							if (ep.type.equals("UNKNOWN")) {
								ep.type = "ENTITY";
							}
						}
					} else {
						CaseEntity e = new CaseEntity(ss, role, "ENTITY", count);
						e = addToGel(e);
						if (!list.contains(e))
							list.add(e);
					}
					continue;
				}
				m = pInc.matcher(ss);
				if (m.find()) {
					CaseEntity e = new CaseEntity(ss.replaceAll("^\\W+|\\W+$", ""), role, "ENTITY", count);
					e = addToGel(e);
					if (!list.contains(e))
						list.add(e);
					continue;
				}

				CaseEntity dba = dba(ss);
				if (dba != null) {
					currentEntity = dba;
				} else {
					CaseNames cn = mapParty.get(caseID);
					List<Party> parties = cn.getParties();
					String[] names = ss.split("(?=(\\b\\w+\\,))");
					for (String nm : names) {
						nm = nm.trim();
						if (nm.length() > 6) {
							CaseEntity e = new CaseEntity(nm, role, "UNKNOWN", count);
							e = addToGel(e);
							if (!list.contains(e))
								list.add(e);
						}
					}
				}
			}
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

	public static void main1(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("args: partyfile outfile ");
			System.exit(-1);
		}
		String partyfile = args[0];
		String outfile = args[1];
		Map<String, CaseNames> plist = readParties(partyfile);

		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (String key : plist.keySet()) {
			CaseNames pn = plist.get(key);
			wr.write(pn + "\n");
		}
		wr.close();
	}

	static Map<String, CaseNames> readParties(String partyfile) throws IOException {
		Map<String, CaseNames> plist = new HashMap<>();
		String line;
		BufferedReader brp = new BufferedReader(new FileReader(partyfile));
		String id = "";
		CaseNames cn = null;
		while ((line = brp.readLine()) != null) {
			String[] items = line.split("\\t");
			if (!items[0].equals(id)) {
				id = items[0];
				cn = new CaseNames(id);
				plist.put(id, cn);
			}
			Party p = new Party(items[1], items[1], items[2]);
			cn.addParty(p);
		}
		brp.close();
		return plist;
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

}
