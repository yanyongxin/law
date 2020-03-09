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

public class ExtractEntities {
	static Pattern pClerk = Pattern.compile("CLERK\\:\\s*(\\w.+?\\w\\w)(\\;|\\.|\\,)", Pattern.CASE_INSENSITIVE);

	static List<Pattern> formJudgePatterns(List<PersonName> judgelist) {
		List<Pattern> jps = new ArrayList<>();
		for (PersonName pn : judgelist) {
			//			String regW = pn.getWeakRegex();
			String regM = pn.getMediumRegex();
			String regJudge = "(HON(ORABLE|\\.)|(VISITING\\s)?JUDGE.?|VJ)\\s+";
			// regJudge is optional for surname with givname of just an initial, but compulsory for just a surname:
			String regex = "(" + regJudge + ")*(" + regM + ")|(" + regJudge + ")+" + pn.surname;
			Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			jps.add(p);
		}
		return jps;
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 5) {
			System.out.println("args: partyfile judgefile docketfile partyoutfile docketOutfile");
			System.exit(-1);
		}
		String partyfile = args[0];
		String judgefile = args[1];
		String docketfile = args[2];
		String outfile = args[3];
		String docketOutfile = args[4];
		Map<String, CaseParties> partymap = readParties(partyfile);
		List<PersonName> judgelist = readJudgeList(judgefile);
		List<Pattern> judgePatterns = formJudgePatterns(judgelist);
		//		Map<String, CaseJudges> judgemap = readJudges(judgefile);

		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		wr.write("\n\n================= Parties ====================\n\n");
		for (CaseParties pn : partymap.values()) {
			wr.write("\n" + pn + "\n");
		}

		wr.write("\n\n================= Judges ====================\n\n");
		Collections.sort(judgelist);
		for (PersonName pn : judgelist) {
			wr.write(pn + "\n");
		}

		List<Case> cases = readCases(docketfile);

		Map<String, Integer> clerks = identifyEntities(cases, docketOutfile, partymap, judgePatterns);
		wr.write("\n\n================= Clerks ====================\n\n");
		for (String key : clerks.keySet()) {
			Integer I = clerks.get(key);
			wr.write(key + "\t" + I + "\n");
		}
		wr.close();
	}

	static Map<String, Integer> identifyEntities(List<Case> cases, String outfile,
			Map<String, CaseParties> partymap, List<Pattern> judgePatterns) throws IOException {
		Map<String, Integer> clerks = new TreeMap<>();
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (Case c : cases) {
			CaseParties cn = partymap.get(c.id);
			//			if (c.id.equals("CA_SFC_464245")) {
			//				System.out.println("CA_SFC_464245");
			//			}
			for (Entry e : c.entries) {
				String s = findEntities(e.text, cn, judgePatterns, clerks);
				wr.write(c.id + "\t" + e.sdate + "\t" + s + "\n");
			}
		}
		wr.close();
		return clerks;
	}

	static String findEntities(String text, CaseParties cn, List<Pattern> judgePatterns, Map<String, Integer> clerks) {
		String remainText = text;
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
		for (Pattern ptn : judgePatterns) {
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
				Integer I = clerks.get(clerkName);
				if (I == null) {
					I = 1;
				} else {
					I++;
				}
				clerks.put(clerkName, I);
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

	static Map<String, CaseJudges> readJudges(String judgefile) throws IOException {
		Map<String, CaseJudges> mapJudges = new TreeMap<>();
		String line;
		BufferedReader br = new BufferedReader(new FileReader(judgefile));
		String id = "";
		CaseJudges cn = null;
		while ((line = br.readLine()) != null) {
			line = line.toUpperCase();
			String[] items = line.split("\\t");
			if (!items[0].equals(id)) {
				id = items[0];
				//				if (id.equals("CA_SFC_466227")) {
				//					System.out.println();
				//				}
				cn = mapJudges.get(id);
				if (cn == null) {
					cn = new CaseJudges(id);
					mapJudges.put(id, cn);
				}
			}
			PersonName p = PersonName.parse(items[1], PersonName.GivMidSur);
			if (p != null) {
				cn.addJudge(p);
			}
		}
		br.close();
		return mapJudges;
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
			if (items[1].equalsIgnoreCase("A. JAMES ROBERTSON II")) {
				System.out.println();
			}
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

	static class CaseParties {
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

		List<Party> getParties() {
			return parties;
		}
	}

	static class CaseJudges {
		String id;
		List<PersonName> judges = new ArrayList<>();

		public CaseJudges(String _id) {
			id = _id;
		}

		void addJudge(PersonName _p) {
			for (PersonName p : judges) {
				if (p.samePerson(_p)) {
					p.combine(_p);
					return;
				}
			}
			judges.add(_p);
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(id);
			for (PersonName p : judges) {
				sb.append("\n" + p.normalName());
			}
			return sb.toString();
		}

		List<PersonName> getJudges() {
			return judges;
		}
	}

}
