package legal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractEntities {

	public static void main(String[] args) throws IOException {
		if (args.length != 4) {
			System.out.println("args: partyfile partyoutfile docketfile docketOutfile");
			System.exit(-1);
		}
		String partyfile = args[0];
		String outfile = args[1];
		String docketfile = args[2];
		String docketOutfile = args[3];
		Map<String, CaseNames> partymap = readParties(partyfile);

		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (CaseNames pn : partymap.values()) {
			wr.write(pn + "\n");
		}
		wr.close();

		List<Case> cases = readCases(docketfile);

		identifileParties(cases, docketOutfile, partymap);
	}

	static void identifileParties(List<Case> cases, String outfile, Map<String, CaseNames> partymap) throws IOException {
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (Case c : cases) {
			CaseNames cn = partymap.get(c.id);
			for (Entry e : c.entries) {
				String s = findParties(e.text, cn);
				wr.write(c.id + "\t" + e.sdate + "\t" + s + "\n");
			}
		}
		wr.close();

	}

	static String findParties(String text, CaseNames cn) {
		String remainText = text;
		Map<String, String> replaceMap = new HashMap<>();
		int replaceIndex = 1;
		for (Party p : cn.parties) {
			int idx = remainText.indexOf(p.raw);
			if (idx >= 0) {
				//				String s1 = remainText.substring(0, idx);
				//				String s2 = p.raw;
				//				String s3 = remainText.substring(idx + p.raw.length());
				String s4 = "&" + replaceIndex++ + "&";
				remainText = remainText.replace(p.raw, s4);
				replaceMap.put(s4, p.raw);
			}
			idx = remainText.indexOf(p.name);
			if (idx >= 0) {
				//				String s1 = remainText.substring(0, idx);
				//				String s2 = p.raw;
				//				String s3 = remainText.substring(idx + p.raw.length());
				String s4 = "&" + replaceIndex++ + "&";
				remainText = remainText.replace(p.name, s4);
				replaceMap.put(s4, p.name);
			}
			if (p.nameCorp != null) {
				idx = remainText.indexOf(p.nameCorp.stem);
				if (idx >= 0) {
					//					String s1 = remainText.substring(0, idx);
					//					String s2 = p.nameCorp.stem;
					//					String s3 = remainText.substring(idx + p.nameCorp.stem.length());
					String s4 = "&" + replaceIndex++ + "&";
					remainText = remainText.replace(p.nameCorp.stem, s4);
					replaceMap.put(s4, p.nameCorp.stem);
				}
			}
			if (p.namePerson != null) {
				Pattern ptn = p.namePerson.getPattern();
				Matcher m = ptn.matcher(remainText);
				if (m.find()) {
					//					String s1 = remainText.substring(0, m.start());
					//					String s2 = m.group();
					//					String s3 = remainText.substring(m.end());
					//					remainText = s1 + "<=<" + s2 + ">=>" + s3;
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
		if (!replaceMap.isEmpty()) {
			for (String key : replaceMap.keySet()) {
				String value = "<=<" + replaceMap.get(key) + ">=>";
				String num = key.substring(1, 2);
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

	static Map<String, CaseNames> readParties(String partyfile) throws IOException {
		Map<String, CaseNames> plist = new TreeMap<>();
		String line;
		BufferedReader brp = new BufferedReader(new FileReader(partyfile));
		String id = "";
		CaseNames cn = null;
		while ((line = brp.readLine()) != null) {
			line = line.toUpperCase();
			String[] items = line.split("\\t");
			if (!items[0].equals(id)) {
				id = items[0];
				cn = plist.get(id);
				if (cn == null) {
					cn = new CaseNames(id);
					plist.put(id, cn);
				}
			}
			int role = findRole(items[2]);
			//			Party p = new Party(items[1], role);
			Party p = Party.parse(items[1], role);
			if (p != null) {
				cn.addParty(p);
			}
		}
		brp.close();
		return plist;
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

	static class CaseNames {
		String id;
		List<Party> parties = new ArrayList<>();

		public CaseNames(String _id) {
			id = _id;
		}

		void addParty(Party _p) {
			for (Party p : parties) {
				if (p.sameParty(_p)) {
					return;
				}
			}
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
