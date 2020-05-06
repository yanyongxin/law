package legal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.Entry.DePhrase;

/**
 * Input three resource files: (1) party (2) judge (3) attorney;
 * Input one data file: docket entries of cases, one entry each line of text;
 * 
 * Generate three files:
 *  (1) docket entry output file. Each identified entity is surrounded by <==< >==>
 *    Example:
 * 		DECLARATION OF ASHWIN LADVA (TRANSACTION ID # 100033395) FILED BY PLAINTIFF BARRIENTOS, CLAUDIA CRUZ
 * 	  becomes:
 *		DECLARATION OF <=<ASHWIN LADVA>=> (TRANSACTION ID # 100033395) FILED BY PLAINTIFF <=<BARRIENTOS, CLAUDIA CRUZ>=>
 *	(2) party list file:
 * @author yanyo
 *
 */
public class ExtractEntities {
	static Pattern pClerk = Pattern.compile("CLERK\\:\\s*(\\w.+?\\w\\w)(?=(\\;|\\.|\\,|\\s*\\(|\\s*$))", Pattern.CASE_INSENSITIVE);
	static Map<String, Integer> wordmap = new TreeMap<>();
	public Map<String, CaseParties> parties;
	public Map<String, CaseAttorneys> attorneys;
	public List<Judge> judges;
	public Map<String, Clerk> clerks = new TreeMap<>();
	public List<Case> cases;

	static List<Judge> formJudgePatterns(List<PersonName> judgelist) {
		List<Judge> jps = new ArrayList<>();
		for (PersonName pn : judgelist) {
			//			String regW = pn.getWeakRegex();
			String regM = pn.getMediumRegex();
			String regJudge = "(HON(ORABLE|\\.)|(VISITING\\s)?JUDGE.?|VJ)\\s+";
			// regJudge is optional for surname with givname of just an initial, but compulsory for just a surname:
			String regex = "(" + regJudge + ")*(" + regM + ")|(" + regJudge + ")+" + pn.surname;
			Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			Judge j = new Judge(pn, p);
			jps.add(j);
		}
		return jps;
	}

	public ExtractEntities(String[] args) throws IOException {
		String partyInputfile = args[0];
		String judgeInputfile = args[1];
		String attorneyInputfile = args[2];
		String docketInputfile = args[3];
		long starttime = System.currentTimeMillis();
		// parties are organized on a per case bases:
		System.out.println("Read parties ... ");
		parties = readParties(partyInputfile);
		// attorneys are organized on a per case bases:
		long attorneyTime = System.currentTimeMillis() - starttime;
		System.out.println("Read attorneys ... " + attorneyTime);
		attorneys = readAttorneys(attorneyInputfile);
		// judges are in one list, because the judge file I got only cover some of the cases:
		long judgeTime = System.currentTimeMillis() - starttime;
		System.out.println("Read judges ... " + judgeTime);
		List<PersonName> judgelist = readJudgeList(judgeInputfile);
		// regex patterns are used directly, so convert judge names to regex patterns
		judges = formJudgePatterns(judgelist);

		// read docket entries for each case:
		long caseTime = System.currentTimeMillis() - starttime;
		System.out.println("read cases ... " + caseTime);
		cases = readCases(docketInputfile);
		// identify and mark mentions of entities in docket entries, also find mentions of clerks:
		long identifyTime = System.currentTimeMillis() - starttime;
		System.out.println("Identify entities in docket entries ... " + identifyTime);
		identifyEntities_1(cases);
		long endTime = System.currentTimeMillis() - starttime;
		System.out.println("Done! " + endTime);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 8) {
			System.out.println("args: partyInputfile judgeInputfile attorneyInputfile docketInputfile entityOutfile docketOutfile wordOutfile");
			System.exit(-1);
		}
	}

	public static void main_1(String[] args) throws IOException {
		if (args.length != 8) {
			System.out.println("args: partyInputfile judgeInputfile attorneyInputfile docketInputfile entityOutfile docketOutfile wordOutfile");
			System.exit(-1);
		}
		String partyInputfile = args[0];
		String judgeInputfile = args[1];
		String attorneyInputfile = args[2];
		String docketInputfile = args[3];
		String entityOutfile = args[4];
		String docketOutfile = args[5];
		String wordOutfile = args[6];
		String phraseOutfile = args[7];
		long starttime = System.currentTimeMillis();
		// parties are organized on a per case bases:
		System.out.println("Read parties ... ");
		Map<String, CaseParties> partymap = readParties(partyInputfile);
		// attorneys are organized on a per case bases:
		long attorneyTime = System.currentTimeMillis() - starttime;
		System.out.println("Read attorneys ... " + attorneyTime);
		Map<String, CaseAttorneys> attorneymap = readAttorneys(attorneyInputfile);
		// judges are in one list, because the judge file I got only cover some of the cases:
		long judgeTime = System.currentTimeMillis() - starttime;
		System.out.println("Read judges ... " + judgeTime);
		List<PersonName> judgelist = readJudgeList(judgeInputfile);
		// regex patterns are used directly, so convert judge names to regex patterns
		List<Judge> judges = formJudgePatterns(judgelist);

		// write parties, judges, clerks into one file:
		long writeTime = System.currentTimeMillis() - starttime;
		System.out.println("write parties, attorneys, judges ... " + writeTime);
		BufferedWriter wr = new BufferedWriter(new FileWriter(entityOutfile));
		wr.write("\n\n================= Parties ====================\n\n");
		for (CaseParties pn : partymap.values()) {
			wr.write("\n" + pn + "\n");
		}

		wr.write("\n\n================= Attorneys ====================\n\n");
		for (CaseAttorneys pn : attorneymap.values()) {
			wr.write("\n" + pn + "\n");
		}

		wr.write("\n\n================= Judges ====================\n\n");
		Collections.sort(judgelist);
		for (PersonName pn : judgelist) {
			wr.write(pn + "\n");
		}
		// read docket entries for each case:
		long caseTime = System.currentTimeMillis() - starttime;
		System.out.println("read cases ... " + caseTime);
		List<Case> cases = readCases(docketInputfile);
		// identify and mark mentions of entities in docket entries, also find mentions of clerks:
		long identifyTime = System.currentTimeMillis() - starttime;
		System.out.println("Identify entities in docket entries ... " + identifyTime);
		Map<String, Clerk> clerks = identifyEntities(cases, docketOutfile, partymap, attorneymap, judges);
		long clerkTime = System.currentTimeMillis() - starttime;
		System.out.println("write clerks ... " + clerkTime);
		wr.write("\n\n================= Clerks ====================\n\n");
		for (String key : clerks.keySet()) {
			Clerk I = clerks.get(key);
			wr.write(key + "\t" + I.count + "\n");
		}
		wr.close();
		long wordTime = System.currentTimeMillis() - starttime;
		System.out.println("write words ... " + wordTime);
		List<Pair> wordpairs = new ArrayList<>();
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(wordOutfile));
		for (String word : wordmap.keySet()) {
			if (word.length() == 0)
				continue;
			Integer cnt = wordmap.get(word);
			wr2.write(word + "\t" + cnt + "\n");
			Pair p = new Pair(cnt, word);
			wordpairs.add(p);
		}
		wr2.write("\n\n===================== Count Sorted =========================\n\n");
		Collections.sort(wordpairs);
		for (Pair p : wordpairs) {
			wr2.write(p.o2 + "\t" + p.o1 + "\n");
		}
		wr2.close();
		long endTime = System.currentTimeMillis() - starttime;
		System.out.println("Done! " + endTime);
	}

	static Map<String, Clerk> identifyEntities(List<Case> cases, String outfile, Map<String, CaseParties> partymap,
			Map<String, CaseAttorneys> attorneymap, List<Judge> judges) throws IOException {
		Map<String, Clerk> clerks = new TreeMap<>();
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (Case c : cases) {
			CaseParties cp = partymap.get(c.id);
			CaseAttorneys ca = attorneymap.get(c.id);
			//			if (c.id.equals("CA_SFC_464349")) {
			//				System.out.println("CA_SFC_464349");
			//			}
			for (Entry e : c.entries) {
				if (e.text.startsWith("Payment"))
					continue;
				String s = findEntities(e.text, cp, ca, judges, clerks);
				wr.write(c.id + "\t" + e.sdate + "\t" + s + "\n");
			}
		}
		wr.close();
		return clerks;
	}

	public static class Clerk {
		String name;
		int count;

		public Clerk(String _name) {
			name = _name;
			count = 1;
		}

		int increment() {
			count++;
			return count;
		}

		public String toString() {
			return name;
		}
	}

	void identifyEntities_1(List<Case> cases) throws IOException {
		for (Case c : cases) {
			CaseParties cp = parties.get(c.id);
			CaseAttorneys ca = attorneys.get(c.id);
			for (Entry e : c.entries) {
				if (e.text.startsWith("Payment"))
					continue;

				findEntities_1(e, cp, ca, judges, clerks);
			}
		}
	}

	static void breakTwo(String str, String template, int idx, int offset, List<Pair> list, List<DePhrase> plist, Object o) {
		// list contains strings that contains no recognized entities
		int index1 = offset;
		String s1 = str.substring(0, idx);
		int len = idx + template.length();
		int index2 = offset + len;
		String s2 = str.substring(len);
		String s10 = s1.trim();
		String s20 = s2.trim();
		if (s10.length() > 0)
			list.add(new Pair(Integer.valueOf(index1), s1));
		if (s20.length() > 0)
			list.add(new Pair(Integer.valueOf(index2), s2));
		DePhrase dp = new DePhrase(template, offset + idx, index2, o);
		plist.add(dp);
	}

	static void findEntities_1(Entry entry, CaseParties cn, CaseAttorneys ca, List<Judge> judges, Map<String, Clerk> clerks) {
		String text = entry.text;
		List<Pair> doneList = entry.doneList; // strings contains no entity of interest.
		List<DePhrase> dephrases = entry.dephrases;// entities found in docket entry text
		List<Pair> workList = new ArrayList<>();// Strings to be check for entities of interest in the current iteration
		List<Pair> nextList = new ArrayList<>();// to be worked on in the next iteration,contains strings may contain more entities of interest
		nextList.add(new Pair(new Integer(0), text)); // initialize (offset, text)
		while (!nextList.isEmpty()) {
			workList.clear();
			workList.addAll(nextList);
			nextList.clear();
			for (Pair pr : workList) {
				int offset = (Integer) (pr.o1);
				String str = (String) (pr.o2);
				boolean b = false;
				// for parties:
				for (Party p : cn.parties) {
					int idx;
					for (String raw : p.raw) {
						idx = str.indexOf(raw);
						if (idx >= 0) {
							breakTwo(str, raw, idx, offset, nextList, dephrases, p);
							b = true;
							break;
						}
					}
					if (b)
						break;
					idx = str.indexOf(p.name);
					if (idx >= 0) {
						breakTwo(str, p.name, idx, offset, nextList, dephrases, p);
						b = true;
						break;
					}
					if (p.errSued != null) {
						idx = str.indexOf(p.errSued);
						if (idx >= 0) {
							breakTwo(str, p.errSued, idx, offset, nextList, dephrases, p);
							b = true;
							break;
						}
					}
					if (p.nameCorp != null) {
						idx = str.indexOf(p.nameCorp.stem);
						if (idx >= 0) {
							breakTwo(str, p.nameCorp.stem, idx, offset, nextList, dephrases, p);
							b = true;
							break;
						}
					}
					if (p.namePerson != null) {
						Pattern ptn = p.namePerson.getPattern();
						Matcher m = ptn.matcher(str);
						if (m.find()) {
							idx = m.start();
							breakTwo(str, m.group(), idx, offset, nextList, dephrases, p);
							b = true;
							break;
						}
					}
				} //	end for parties
				if (b)
					continue;// next in the workList
				if (ca != null) {
					for (Attorney p : ca.atts) {
						if (p.name != null) {
							Pattern ptn = p.name.getPattern();
							Matcher m = ptn.matcher(str);
							if (m.find()) {
								breakTwo(str, m.group(), m.start(), offset, nextList, dephrases, p);
								b = true;
								break;
							}
						}
					}
				}
				if (b)
					continue;
				for (Judge j : judges) {
					Pattern ptn = j.pattern;
					Matcher m = ptn.matcher(str);
					if (m.find()) {
						breakTwo(str, m.group(), m.start(), offset, nextList, dephrases, j);
						b = true;
						break;
					}
				}
				if (b)
					continue;
				int index = str.indexOf("CLERK");
				if (index >= 0) {
					Matcher m = pClerk.matcher(str);
					if (m.find()) {
						String clerkName = m.group(1);
						Clerk clk = clerks.get(clerkName);
						if (clk == null) {
							clk = new Clerk(clerkName);
						} else {
							clk.increment();
						}
						clerks.put(clerkName, clk);
						breakTwo(str, m.group(), m.start(), offset, nextList, dephrases, clk);
						b = true;
					}
				}
				if (b)
					continue;
				// if reaches here, the string has not matched any entity:
				doneList.add(pr);
			} // end for pr : workList
		} // while(!nextList.isEmpty())
			// at this point, dephrase, and doneList are useful. 
	}

	static String findEntities(String text, CaseParties cn, CaseAttorneys ca, List<Judge> judges, Map<String, Clerk> clerks) {
		String remainText = text;
		//		if (text.startsWith("LAW AND MOTION 302, PLAINTIFF ANTHONY LEE'S UNOPPOSED MOTION")) {
		//			System.out.println();
		//		}
		Map<String, String> replaceMap = new HashMap<>();
		int replaceIndex = 1;
		for (Party p : cn.parties) {
			int idx;
			for (String raw : p.raw) {
				idx = remainText.indexOf(raw);
				if (idx >= 0) {
					String s4 = "&" + replaceIndex++ + "&";
					remainText = remainText.replace(raw, s4);
					replaceMap.put(s4, raw);
				}
			}
			idx = remainText.indexOf(p.name);
			if (idx >= 0) {
				String s4 = "&" + replaceIndex++ + "&";
				remainText = remainText.replace(p.name, s4);
				replaceMap.put(s4, p.name);
			}
			if (p.errSued != null) {
				idx = remainText.indexOf(p.errSued);
				if (idx >= 0) {
					String s4 = "&" + replaceIndex++ + "&";
					remainText = remainText.replace(p.errSued, s4);
					replaceMap.put(s4, p.errSued);
				}
			}
			if (p.nameCorp != null) {
				idx = remainText.indexOf(p.nameCorp.stem);
				if (idx >= 0) {
					String s4 = "&" + replaceIndex++ + "&";
					remainText = remainText.replace(p.nameCorp.stem, s4);
					replaceMap.put(s4, p.nameCorp.stem);
				}
			}
			if (p.namePerson != null) {
				Pattern ptn = p.namePerson.getPattern();
				Matcher m = ptn.matcher(remainText);
				if (m.find()) {
					String s4 = "&" + replaceIndex++ + "&";
					remainText = remainText.replace(m.group(), s4);
					replaceMap.put(s4, m.group());
					m = ptn.matcher(remainText);
					if (m.find()) {
						s4 = "&" + replaceIndex++ + "&";
						remainText = remainText.replace(m.group(), s4);
						replaceMap.put(s4, m.group());
					}
				}
			}
		}
		if (ca != null)
			for (Attorney p : ca.atts) {
				if (p.name != null) {
					Pattern ptn = p.name.getPattern();
					Matcher m = ptn.matcher(remainText);
					if (m.find()) {
						String s4 = "&" + replaceIndex++ + "&";
						remainText = remainText.replace(m.group(), s4);
						replaceMap.put(s4, m.group());
						m = ptn.matcher(remainText);
						if (m.find()) {
							s4 = "&" + replaceIndex++ + "&";
							remainText = remainText.replace(m.group(), s4);
							replaceMap.put(s4, m.group());
						}
					}
				}
			}
		for (Judge j : judges) {
			Pattern ptn = j.pattern;
			Matcher m = ptn.matcher(remainText);
			if (m.find()) {
				String s4 = "&" + replaceIndex++ + "&";
				String replaced = m.group();
				remainText = remainText.replace(replaced, s4);
				replaceMap.put(s4, replaced);
				m = ptn.matcher(remainText);
				if (m.find()) {
					s4 = "&" + replaceIndex++ + "&";
					remainText = remainText.replace(m.group(), s4);
					replaceMap.put(s4, m.group());
				}
			}
		}
		int index = remainText.indexOf("CLERK");
		if (index >= 0) {
			Matcher m = pClerk.matcher(remainText);
			if (m.find()) {
				String s4 = "&" + replaceIndex++ + "&";
				String replaced = m.group();
				String clerkName = m.group(1);
				Clerk clk = clerks.get(clerkName);
				if (clk == null) {
					clk = new Clerk(clerkName);
				} else {
					clk.increment();
				}
				clerks.put(clerkName, clk);
				remainText = remainText.replace(replaced, s4);
				replaceMap.put(s4, replaced);
				m = pClerk.matcher(remainText);
				if (m.find()) {
					s4 = "&" + replaceIndex++ + "&";
					remainText = remainText.replace(m.group(), s4);
					replaceMap.put(s4, m.group());
				}
			}
		}
		String cleanText = remainText.replaceAll("\\&\\d+\\&|\\(.+?\\)|\\\"|\\*", " ");
		String[] words = cleanText.split("\\s+");
		for (String s : words) {
			if (s.matches("^(\\W|\\d).+|.+?\\d$"))// begin or end with numbers or non-alpha symbols
				continue;
			s = s.replaceAll("\\p{Punct}$", "").trim();
			Integer cnt = wordmap.get(s);
			if (cnt != null) {
				cnt += 1;
			} else {
				cnt = 1;
			}
			wordmap.put(s, cnt);
		}

		if (!replaceMap.isEmpty()) {
			for (String key : replaceMap.keySet()) {
				String value = "<=<" + replaceMap.get(key) + ">=>";
				String num = key.substring(1, key.length() - 1);
				String regex = "\\&" + num + "\\&";
				remainText = remainText.replaceAll(regex, value);
			}
		}
		return remainText;
	}

	static List<Case> readCases(String infile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(infile));
		List<Case> cases = new ArrayList<>();
		String caseID = "";
		String line;
		List<Entry> entries = null;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (items.length < 3)
				continue;
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

	static Map<String, CaseParties> readParties(String partyfile) throws IOException {
		Map<String, CaseParties> mapParty = new TreeMap<>();
		BufferedReader br = new BufferedReader(new FileReader(partyfile));
		String line = br.readLine();// throw away the first line, they are column names
		String id = "";
		CaseParties cn = null;
		while ((line = br.readLine()) != null) {
			line = line.toUpperCase();
			String[] items = line.split("\\t");
			if (!items[0].equals(id)) {
				id = items[0];
				//				if (id.equals("CA_SFC_464181")) {
				//					System.out.println("CA_SFC_464181");
				//				}
				cn = mapParty.get(id);
				if (cn == null) {
					cn = new CaseParties(id);
					mapParty.put(id, cn);
				}
			}
			int role = findRole(items[2]);
			//			Party p = new Party(items[1], role);
			Party p = Party.parse(items[1], role);
			if (p != null) {
				cn.addParty(p);
			}
		}
		br.close();
		Party.Cmp cmp = new Party.Cmp();
		for (CaseParties csn : mapParty.values()) {
			for (Party p : csn.parties) {
				p.raw.sort(cmp);
			}
		}
		return mapParty;
	}

	static Map<String, CaseAttorneys> readAttorneys(String attorneyfile) throws IOException {
		Map<String, CaseAttorneys> mapLawyers = new TreeMap<>();
		BufferedReader br = new BufferedReader(new FileReader(attorneyfile));
		String id = "";
		CaseAttorneys cn = null;
		String line = br.readLine(); // skip the first line, column names. check file format to make sure this is true.
		while ((line = br.readLine()) != null) {
			line = line.toUpperCase();
			String[] items = line.split("\\t");
			if (!items[0].equals(id)) {
				id = items[0];
				//				if (id.equals("CA_SFC_466227")) {
				//					System.out.println();
				//				}
				cn = mapLawyers.get(id);
				if (cn == null) {
					cn = new CaseAttorneys(id);
					mapLawyers.put(id, cn);
				}
			}
			cn.addAttorney(items[1], items[2]);
		}
		br.close();
		return mapLawyers;
	}

	static List<PersonName> readJudgeList(String judgefile) throws IOException {
		List<PersonName> judges = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(judgefile));
		String line = br.readLine();
		while ((line = br.readLine()) != null) {
			line = line.toUpperCase();
			String[] items = line.split("\\t");
			if (items[1].startsWith("APPELLATE"))
				continue;
			PersonName _p = PersonName.parse(items[1], PersonName.GivMidSur);
			//			if (items[1].equalsIgnoreCase("A. JAMES ROBERTSON II")) {
			//				System.out.println();
			//			}
			if (_p != null) {
				boolean b = false;
				for (PersonName p : judges) {
					if (p.samePerson(_p)) {
						p.combine(_p);
						b = true;
						break;
					}
				}
				if (!b)
					judges.add(_p);
			}
		}
		br.close();
		return judges;
	}

	static int findRole(String _r) {
		Integer role = Party.mapRole.get(_r);
		if (role != null) {
			return role.intValue();
		} else {
			if (!(_r.startsWith("MST_PARTY_TYPE_NAME") || _r.startsWith("NOT YET CLASSIFIED")))
				System.out.println("Unknown Role: " + _r);
		}
		return Party.ROLE_UNKNOWN;
	}

	public static class CaseParties {
		String id;
		List<Party> parties = new ArrayList<>();

		public CaseParties(String _id) {
			id = _id;
		}

		void addParty(Party _p) {
			for (Party p : parties) {
				if (p.sameParty(_p)) {
					p.combine(_p);
					return;
				}
			}
			parties.add(_p);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(id);
			for (Party p : parties) {
				sb.append("\n" + p);
			}
			return sb.toString();
		}

		public List<Party> getParties() {
			return parties;
		}
	}

	static class CaseJudges {
		String id;
		List<Judge> judges = new ArrayList<>();

		public CaseJudges(String _id) {
			id = _id;
		}

		void addJudge(Judge _j) {
			PersonName _p = _j.name;
			for (Judge j : judges) {
				PersonName p = j.name;
				if (p.samePerson(_p)) {
					p.combine(_p);
					return;
				}
			}
			judges.add(_j);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(id);
			for (Judge j : judges) {
				PersonName p = j.name;
				sb.append("\n" + p.normalName());
			}
			return sb.toString();
		}

		List<Judge> getJudges() {
			return judges;
		}
	}

	public static class Judge {
		PersonName name;
		Pattern pattern;

		public Judge(PersonName _name, Pattern _pattern) {
			name = _name;
			pattern = _pattern;
		}
	}

	public static class Lawfirm {
		static List<Lawfirm> lawfirms = new ArrayList<>();
		String name;

		public Lawfirm(String _name) {
			name = _name;
		}

		public static Lawfirm findFirm(String _name) {
			for (Lawfirm firm : lawfirms) {
				if (_name.equalsIgnoreCase(firm.name))
					return firm;
			}
			return null;
		}

		public static Lawfirm createFirm(String _name) {
			for (Lawfirm firm : lawfirms) {
				if (_name.equalsIgnoreCase(firm.name))
					return firm;
			}
			Lawfirm firm = new Lawfirm(_name);
			lawfirms.add(firm);
			return firm;
		}

		public boolean equals(Object o) {
			if (!(o instanceof Lawfirm))
				return false;
			Lawfirm fm = (Lawfirm) o;
			return name.equalsIgnoreCase(fm.name);
		}
	}

	public static class Attorney {
		PersonName name;
		List<Lawfirm> firms = new ArrayList<>();
		static List<Attorney> attorneys = new ArrayList<>();

		protected Attorney(String _name, String _firm) {
			name = PersonName.parse(_name, PersonName.SurGivMid);
			addFirm(_firm);
		}

		public void addFirm(String _firm) {
			Lawfirm fm = Lawfirm.createFirm(_firm);
			if (!firms.contains(fm)) {
				firms.add(fm);
			}
		}

		public String toString() {
			return name.toString();
		}

		public static Attorney createAttorney(String _name, String _firm) {
			_name = _name.replaceAll("\\(.+?\\)", "");
			PersonName pn = PersonName.parse(_name, PersonName.SurGivMid);
			// check if already in the registry
			for (Attorney at : attorneys) {
				if (at.name.samePerson(pn)) {
					at.name.combine(pn);
					if (!_firm.equalsIgnoreCase("None"))
						at.addFirm(_firm);
					return at;
				}
			}
			// not in the registry. Create a new Attorney:
			Attorney at = new Attorney(_name, _firm);
			attorneys.add(at);
			return at;
		}

		public boolean same(Attorney at) {
			return name.samePerson(at.name);
		}
	}

	static class CaseAttorneys {
		String caseID;
		List<Attorney> atts = new ArrayList<>();

		public CaseAttorneys(String id) {
			caseID = id;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(caseID);
			for (Attorney a : atts) {
				sb.append("\n" + a);
			}
			return sb.toString();
		}

		void addAttorney(String _name, String _firm) {
			int idx = _name.indexOf('&');
			if (idx >= 0)//FERGUSON & BERLAND
				return;
			idx = _name.indexOf("OFFICE");
			if (idx >= 0)
				return;
			idx = _name.indexOf(" ' ");
			if (idx >= 0)
				return;
			idx = _name.indexOf("DEPARTMENT");
			if (idx >= 0)
				return;

			Attorney at = Attorney.createAttorney(_name, _firm);
			for (Attorney att : atts) {
				if (att.same(at)) {
					return;
				}
			}
			atts.add(at);
		}
	}
}
