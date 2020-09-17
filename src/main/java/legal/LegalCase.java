package legal;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sfmotion.CaseLink;
import sfmotion.ComplaintEntry;
import sfmotion.Party;
import sfmotion.PartyCluster;
import sfmotion.PersonName;
import utils.Pair;

public class LegalCase {
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
	static final String[] ROLENAMES = { "APPELLANT", "CROSS DEFENDANT", "CROSS COMPLAINANT", "DEFENDANT", "PLAINTIFF", "CLAIMANT", "OTHER", };
	static int MAX_ALLOWED_DAYS_WITH_GD = 4;
	static int MAX_ALLOWED_DAYS_NO_GD = 14;

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
	public List<CaseEntity> globalEntityList = new ArrayList<>();
	public List<CaseLink> glk = new ArrayList<>();
	public List<PartyCluster> clusters = new ArrayList<>();
	public List<PersonName> judges = new ArrayList<>();
	public Date lastDate; // date of the last entry;

	public LegalCase(String _id, List<TrackEntry> _es) {
		id = _id;
		entries = _es;
	}

	public LegalCase(String _id) {
		id = _id;
	}

	public String getID() {
		return id;
	}

	CaseEntity addToGel(CaseEntity e) {
		for (CaseEntity t : globalEntityList) {
			if (t.equals(e)) {
				t.combine(e);
				return t;
			}
		}
		globalEntityList.add(e);
		return e;
	}

	public void addEntry(TrackEntry _e) {
		entries.add(_e);
	}

	Pattern pPrep = Pattern.compile("^(OF|FOR|ALSO|IN|FEE)\\s", Pattern.CASE_INSENSITIVE);

	public void cleanGel() {
		int i = 0;

		while (i < globalEntityList.size()) {
			CaseEntity e = globalEntityList.get(i);
			Matcher m = pPrep.matcher(e.nameNormalized);
			if (m.find()) {
				globalEntityList.remove(i);
				continue;
			}
			i++;
		}
	}

	int splitConcatenatedEntities() {
		int cnt = 0;
		boolean more = false;
		do {
			nextRound: for (int i = 0; i < globalEntityList.size() - 1; i++) {
				CaseEntity e = globalEntityList.get(i);
				more = false;
				for (int j = i + 1; j < globalEntityList.size(); j++) {
					CaseEntity ee = globalEntityList.get(j);
					CaseEntity e2 = ee.decomposeHead(e);
					if (e2 != null) {
						e.combine(ee);
						e2 = addToGel(e2);
						ee.children = new ArrayList<>();
						ee.children.add(e);
						ee.children.add(e2);
						globalEntityList.remove(j);
						cnt++;
						Collections.sort(globalEntityList);
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
		int beginSize = globalEntityList.size();
		for (int i = 0; i < beginSize; i++) {
			CaseEntity e = globalEntityList.get(i);
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
					int idx = globalEntityList.indexOf(ee);
					if (idx >= 0) {
						CaseEntity eee = globalEntityList.get(idx);
						eee.combine(ee);
					} else {
						globalEntityList.add(ee);
					}
				}
			}
		}
		return cnt;
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
		//		TrackEntry e = entries.get(0);
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

	void findParties(CaseNames cn) {
		String sp = complaint.plaintiffs;
		PartyCluster pp = new PartyCluster(this.id, sp, "PLAINTIFF", 1, globalEntityList);
		pp.parse(cn);
		clusters.add(pp);
		String sd = complaint.defendants;
		if (sd != null) {
			PartyCluster pd = new PartyCluster(this.id, sd, "DEFENDANT", 1, globalEntityList);
			pd.parse(cn);
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
			PartyCluster pc = new PartyCluster(this.id, ns, role, cnt, globalEntityList);
			pc.parse(cn);
			clusters.add(pc);
			//			List<Pair> plist = decomposePartyList(ns);
			//			partylist = new ArrayList<>();
			//			for (Pair pp : plist) {
			//				addToPartyList((String) pp.o1, (String) pp.o2, role, cnt, partylist);
			//			}
		}
	}

	public void relinkPartyClusters() {
		for (PartyCluster pc : clusters) {
			pc.useLeafEntities();
		}
	}

	public static class CaseNames {
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

		public List<Party> getParties() {
			return parties;
		}
	}

}
