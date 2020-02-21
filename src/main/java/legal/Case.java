package legal;

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

import javax.print.attribute.standard.MediaSize.Other;

public class Case {
	//	static final String regSep = "AS TO|FILED BY|\\(TRANSACTION.+?\\)|\\(FEE.+?\\|HEARING SET|)";
	static final String[] ROLENAMES = { "APPELLANT", "CROSS DEFENDANT", "CROSS COMPLAINANT", "DEFENDANT", "PLAINTIFF", "CLAIMANT", "OTHER", };
	static int MAX_ALLOWED_DAYS_WITH_GD = 4;
	static int MAX_ALLOWED_DAYS_NO_GD = 14;

	static List<Case> cases = new ArrayList<>();
	static Map<String, CaseNames> mapParty;
	Map<String, List<Entry>> mdamap = new TreeMap<>();
	List<Entry> entries;
	List<List<Entry>> daily; // entries of the same day
	List<Other> others; // others, that cannot organized into anything
	// Motions In limine grouped together:

	String casetype; // PERSONAL INJURY/PROPERTY DAMAGE - VEHICLE RELATED
	String caseSubtype; // VEHICLE RELATED
	String id;
	List<String> names = new ArrayList<>();
	List<Pair> namep = new ArrayList<>();
	ComplaintEntry complaint = null;
	List<Entity> gel = new ArrayList<>();
	List<Link> glk = new ArrayList<>();
	List<PartyCluster> clusters = new ArrayList<>();
	Date lastDate; // date of the last entry;

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("args: infile outfile partyfile");
			System.exit(-1);
		}
		String infile = args[0];
		String outfile = args[1];
		String partyfile = args[2];

		mapParty = readParties(partyfile);
		List<Case> cases = readCases(infile);
		processCases(cases);
		writeCases(cases, outfile);
	}

	static void writeCases(List<Case> cases, String outfile) throws IOException {
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (Case cs : cases) {
			cs.writeGlobalEntities(wr);
			cs.writePartyClusters(wr);
		}
		wr.close();
	}

	static void findCaseParties(List<Case> cases, Map<String, CaseNames> map) {
		for (Case cs : cases) {
			CaseNames cn = map.get(cs.id);

			for (Entry e : cs.entries) {

			}
		}
	}

	static void processCases(List<Case> cases) throws IOException {
		for (Case cs : cases) {
			cs.analyze();
			cs.convertToPairs();
			cs.findParties();
			Collections.sort(cs.gel);
			int cnt = cs.splitConcatenatedEntities();
			System.out.println("Split Concatenates: " + cnt);
			int cnt1 = cs.splitAKAEntities();
			System.out.println("Split AKAs: " + cnt1);
			cs.cleanGel();
			cs.relinkParyClusters();
		}
	}

	static List<Case> readCases(String infile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(infile));
		List<Case> cases = new ArrayList<>();
		String caseID = "";
		String line;
		List<Entry> entries = null;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (!items[0].equals(caseID)) {
				caseID = items[0];
				entries = new ArrayList<>();
				Case cs = new Case(caseID, entries);
				cases.add(cs);
			}
			Entry en = new Entry(items[1], items[2]);
			entries.add(en);
		}
		br.close();
		return cases;
	}

	static String readEntries(String infile, List<Entry> entries) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(infile));
		String caseID = null;
		String line;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (caseID == null) {
				caseID = items[0];
			}
			Entry en = new Entry(items[1], items[2]);
			entries.add(en);
		}
		br.close();
		return caseID;
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

	public Case(String _id, List<Entry> _es) {
		id = _id;
		entries = _es;
	}

	public void addEntry(Entry _e) {
		entries.add(_e);
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
					String role = "";
					if (e.roles != null && e.roles.size() > 0) {
						role = e.roles.get(0);
					}
					Entity ee = new Entity(split[j].trim(), "AKA-" + role, e.type, e.count);
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
		for (Entry e : entries) {
			sb.append(e.toString() + "\n");
		}
		return sb.toString();
	}

	public void sortEntries() {
		Collections.sort(entries);
	}

	public void analyze() throws IOException {
		Entry e = entries.get(0);
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
		List<Entity> children; // when one entity line consists of multiple entities.

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
				String left = nameNormalized.substring(e.nameNormalized.length());
				if (left.length() > 4) {
					if (Character.isLetterOrDigit(left.charAt(0))) {// in the middle of a word
						return null;
					}
					left = left.trim();
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
		String caseID;
		int count;
		List<Entity> gel; // global entity list
		List<Entity> list; // global entity list
		List<Link> lklist = new ArrayList<>();

		public PartyCluster(String _cid, String _text, String _role, int _count, List<Entity> _gel) {
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
					CaseNames cn = mapParty.get(caseID);
					List<Party> parties = cn.getParties();
					String[] names = ss.split("(?=(\\b\\w+\\,))");
					for (String nm : names) {
						nm = nm.trim();
						if (nm.length() > 6) {
							Entity e = new Entity(nm, role, "UNKNOWN", count);
							e = addToGel(e);
							if (!list.contains(e))
								list.add(e);
						}
					}
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
