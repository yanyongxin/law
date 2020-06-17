package legal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.LegalCase.CaseNames;

public class PartyCluster {
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
