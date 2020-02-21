package legal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Party {

	static final List<String> suffixNames = new ArrayList<>();
	static final List<String> states = new ArrayList<>();

	static final Pattern pCorp = Pattern.compile("\\b(INC|CORP(ORATION)?|CO|LTD|LP|FUND|LOAN|LLC|CENTER|LLP|PC|PLC|BANK|SYSTEM)\\b|\\b(N\\.A\\.|P\\.C\\.|L\\.P\\.)", Pattern.CASE_INSENSITIVE);
	static final Pattern pCorpAttach = Pattern.compile("\\bA (CALIFORNIA|DELAWARE|NEW YORK|BUSINESS|TRUST|SOLE PROPRIETORSHIP)|\\bAN ENTITY", Pattern.CASE_INSENSITIVE);
	static final Pattern pGov = Pattern.compile("\\b(CITY|COUNTY|STATE)\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern pOrg = Pattern.compile("\\b(AND|OF|THE|CALIFORNIA)\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern pDoe = Pattern.compile("^(DOE|MOE|ROE|ZOE)S?|\\d+\\s*THROUGH|\\bINCLUSIVE\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern pNum = Pattern.compile("\\s\\d+\\s");
	static final Pattern pBrackets = Pattern.compile("\\(.+?\\)");
	static final Map<String, Integer> mapRole = new HashMap<>();
	static final Map<Integer, String> mapRoleReverse = new HashMap<>();
	static final int TYPE_UNKNOWN = 0;
	static final int TYPE_INDIVIDUAL = 1;
	static final int TYPE_CORPORATION = 2;// CORP, INC, LLC, LTD, LLP, GROUP
	static final int TYPE_ORGANIZATION = 3;// CORP, INC, LLC, LTD, LLP, GROUP
	static final int TYPE_GOVERNMENT = 4;
	static final int TYPE_DOESROESMOES = 5;
	static final int TYPE_OTHER = 10;

	static final int ROLE_UNKNOWN = 0;
	static final int ROLE_PLAINTIFF = 1;
	static final int ROLE_DEFENDANT = 2;
	static final int ROLE_CROSS_PLAINTIFF = 3;
	static final int ROLE_CROSS_DEFENDANT = 4;
	static final int ROLE_PETITIONER = 5;
	static final int ROLE_RESPONDENT = 6;
	static final int ROLE_MINOR = 7;
	static final int ROLE_CLAIMANT = 8;
	static final int ROLE_DECEDENT = 9;
	static final int ROLE_APPELLANT = 10;
	static final int ROLE_TRUSTEE = 11;
	static final int ROLE_CONSERVATEE = 12;
	static final int ROLE_CONSERVATOR = 13;
	static final int ROLE_GUARDIAN_AD_LITEM = 14;
	static final int ROLE_INTERVENOR_DEFENDANT = 15;
	static final int ROLE_INTERESTED_PARTY = 16;
	static final int ROLE_ASSIGNEE = 17;
	static final int ROLE_INTERVENOR = 18;
	static final int ROLE_OTHER = 20;

	static {
		mapRole.put("PLAINTIFF", ROLE_PLAINTIFF);
		mapRole.put("DEFENDANT", ROLE_DEFENDANT);
		mapRole.put("CROSS PLAINTIFF", ROLE_CROSS_PLAINTIFF);
		mapRole.put("CROSS DEFENDANT", ROLE_CROSS_DEFENDANT);
		mapRole.put("PETITIONER", ROLE_PETITIONER);
		mapRole.put("RESPONDENT", ROLE_RESPONDENT);
		mapRole.put("MINOR", ROLE_MINOR);
		mapRole.put("CLAIMANT", ROLE_CLAIMANT);
		mapRole.put("DECEDENT", ROLE_DECEDENT);
		mapRole.put("APPELLANT", ROLE_APPELLANT);
		mapRole.put("TRUSTEE", ROLE_TRUSTEE);
		mapRole.put("CONSERVATEE", ROLE_CONSERVATEE);
		mapRole.put("CONSERVATOR", ROLE_CONSERVATOR);
		mapRole.put("GUARDIAN AD LITEM", ROLE_GUARDIAN_AD_LITEM);
		mapRole.put("INTERVENOR DEFENDANT", ROLE_INTERVENOR_DEFENDANT);
		mapRole.put("INTERESTED PARTY", ROLE_INTERESTED_PARTY);
		mapRole.put("ASSIGNEE", ROLE_ASSIGNEE);
		mapRole.put("INTERVENOR", ROLE_INTERVENOR);
		mapRole.put("OTHER", ROLE_OTHER);

		mapRoleReverse.put(ROLE_PLAINTIFF, "PLAINTIFF");
		mapRoleReverse.put(ROLE_DEFENDANT, "DEFENDANT");
		mapRoleReverse.put(ROLE_CROSS_PLAINTIFF, "CROSS PLAINTIFF");
		mapRoleReverse.put(ROLE_CROSS_DEFENDANT, "CROSS DEFENDANT");
		mapRoleReverse.put(ROLE_PETITIONER, "PETITIONER");
		mapRoleReverse.put(ROLE_RESPONDENT, "RESPONDENT");
		mapRoleReverse.put(ROLE_MINOR, "MINOR");
		mapRoleReverse.put(ROLE_CLAIMANT, "CLAIMANT");
		mapRoleReverse.put(ROLE_DECEDENT, "DECEDENT");
		mapRoleReverse.put(ROLE_APPELLANT, "APPELLANT");
		mapRoleReverse.put(ROLE_TRUSTEE, "TRUSTEE");
		mapRoleReverse.put(ROLE_CONSERVATEE, "CONSERVATEE");
		mapRoleReverse.put(ROLE_CONSERVATOR, "CONSERVATOR");
		mapRoleReverse.put(ROLE_GUARDIAN_AD_LITEM, "GUARDIAN AD LITEM");
		mapRoleReverse.put(ROLE_INTERVENOR_DEFENDANT, "INTERVENOR DEFENDANT");
		mapRoleReverse.put(ROLE_INTERESTED_PARTY, "INTERESTED PARTY");
		mapRoleReverse.put(ROLE_ASSIGNEE, "ASSIGNEE");
		mapRoleReverse.put(ROLE_INTERVENOR, "INTERVENOR");
		mapRoleReverse.put(ROLE_OTHER, "OTHER");

		suffixNames.add("JR");
		suffixNames.add("SR");
		suffixNames.add("III");
		suffixNames.add("3RD");
		suffixNames.add("MD");
		suffixNames.add("DDS");
		suffixNames.add("DR");
		suffixNames.add("ESQ");
		suffixNames.add("PHD");
		suffixNames.add("RN");
		suffixNames.add("IV");
		suffixNames.add("II");
	}
	String raw;
	String name;
	PersonName namePerson; // use only if type = TYPE_INDIVIDUAL
	CorpName nameCorp; // use only if type = TYPE_ORGANIZATION
	String address; // some have address in their name field

	List<Integer> roles = new ArrayList<>();
	int type;// INDIVIDUAL, CORPORATION, GOVERNMENT, DOESROES, UNKNOWN
	List<String> dba;
	String errSued;//SUED ERRONEOUSLY HEREIN AS TAKITAKI MITIGLI, (ERRONEOUSLY SUED HEREIN AS "ALDRY BONIFACIO")

	public void addDba(String _d) {
		if (dba == null) {
			dba = new ArrayList<>();
		}
		dba.add(_d);
	}

	public Party(String _raw, String _n, int _r) {
		raw = _raw;
		name = _n;
		roles.add(_r);
	}

	public Party(String _raw, String _n, String _r) {
		name = _n;
		Integer r = mapRole.get(_r);
		roles.add(r);
	}

	public Party(String _raw, String _n, int _r, int _type) {
		raw = _raw;
		name = _n;
		roles.add(_r);
		type = _type;
	}

	public Party(String _raw, PersonName _pn, int _r) {
		raw = _raw;
		namePerson = _pn;
		name = namePerson.canonicalName();
		roles.add(_r);
		type = TYPE_INDIVIDUAL;
	}

	public Party(String _raw, CorpName _on, int _r) {
		raw = _raw;
		nameCorp = _on;
		name = nameCorp.name;
		roles.add(_r);
		type = TYPE_CORPORATION;
	}

	/**
	 * find out if the name _n is the reference to the same party as this one
	 * @param _n
	 * @param _r
	 * @return
	 */
	public boolean sameParty(Party _p) {
		if (_p.type != type) {
			return false;
		}
		if (type == TYPE_INDIVIDUAL) {
			PersonName pn = _p.namePerson;
			if (!namePerson.surname.equals(_p.namePerson.surname)) {
				return false;
			}
			if (!namePerson.givname.equals(_p.namePerson.givname)) {
				return false;
			}
			if (namePerson.midname != null && _p.namePerson.midname != null && !namePerson.midname.equals(_p.namePerson.midname)) {
				return false;
			}
		} else if (type == TYPE_CORPORATION) {
			if (!raw.equals(_p.raw)) {
				if (!this.nameCorp.equals(_p.nameCorp)) {
					return false;
				}
			}
		} else {
			String nm = name;
			if (nm.length() > 18) {
				nm = nm.substring(0, 18);
			}
			if (!_p.name.startsWith(nm)) {
				return false;
			}
		}
		for (Integer r : _p.roles) {
			if (!roles.contains(r)) {
				roles.add(r);
			}
		}
		return true;
	}

	public static Party parse(String _name, int role) {
		String raw = _name;
		if (_name.startsWith("PROPERTY SUBJECT TO")) {
			return null;
		}
		Matcher m1 = pBrackets.matcher(_name);
		if (m1.find()) {
			String inBracket = m1.group();
			if (inBracket.length() > 14) {
				// remove it
				_name = _name.substring(0, m1.start()).trim();
			}
		}
		Matcher m = pDoe.matcher(_name);
		if (m.find()) {//DOES 1 TO 25, INCLUSIVE
			Party p = new Party(raw, _name, role, TYPE_DOESROESMOES);
			return p;
		}
		String[] splitCO = _name.split("\\bC/O\\b");
		if (splitCO.length > 1) {
			_name = splitCO[0];
		}
		m = pNum.matcher(_name);
		if (m.find()) {
			String stem = _name.substring(0, m.start());
			String addr = _name.substring(m.start()).trim();
			CorpName on = new CorpName(stem);
			Party p = new Party(raw, on, role);
			p.address = addr;
			return p;
		}
		String[] splitdba = _name.split("\\bF?DBA\\b|DOING BUSINESS AS");
		if (splitdba.length > 1) {//"TAYLOR, GEORGE DBA MASTER ROOTER PLUMBING", "WESTLAKE SERVICES, LLC DBA WESTLAKE FINANCIAL SERVICES"
			_name = splitdba[0].trim();
		}
		m = pCorp.matcher(_name);
		if (m.find()) {
			CorpName on = new CorpName(_name);// "COSTCO WHOLESALE CORPORATION","SEPHORA USA, INC.", "LC BUSINESS SYSTEMS CORP"
			Party p = new Party(raw, on, role);
			return p;
		}
		m = pCorpAttach.matcher(_name);
		if (m.find()) {
			CorpName on = new CorpName(_name);
			Party p = new Party(raw, on, role);
			return p;
		}
		m = pGov.matcher(_name);
		if (m.find()) {//CITY AND COUNTY SAN FRANCISCO TAX COLLECTOR
			Party p = new Party(raw, _name, role, TYPE_GOVERNMENT);
			return p;
		}
		m = pOrg.matcher(_name);
		if (m.find()) {//AMERICAN HOME SHIELD OF CALIFORNIA
			Party p = new Party(raw, _name, role, TYPE_ORGANIZATION);
			return p;
		}
		String[] asa = _name.split("ERRONEOUSLY SUED HEREIN AS|SUED (ERRONEOUSLY )HEREIN AS|(?<!ALSO KNOWN) AS\\s+(AN?\\s)?");
		if (asa.length > 1) {
			_name = asa[0].trim();
		}
		String[] aka = _name.split("\\bAKA\\b|ALSO KNOWN AS");
		if (aka.length > 1) {
			_name = aka[0];
		}
		if (_name.contains("INDIVIDUAL")) {//"BALEK, STANISLAS AN INDIVIDUAL", 
			String[] split = _name.split("(\\bAN )?INDIVIDUAL");
			PersonName pn = new PersonName(split[0].trim());//DIMAPASOC, ALLEN AKA ALLEN A. DIMAPASOC, AN INDIVIDUAL
			Party p = new Party(raw, pn, role);
			for (int i = 1; i < splitdba.length; i++) {
				p.addDba(splitdba[i]);
			}
			return p;
		}
		//		if(role==ROLE_MINOR) {
		PersonName pn = new PersonName(_name);
		Party p;
		if (pn.surname == null) {//SAN FRANCISCO PUBLIC ADMINISTRATOR
			p = new Party(raw, _name, role, TYPE_ORGANIZATION);
		} else {
			p = new Party(raw, pn, role); //"SAUNDERS, EDGAR V.", "FUENTES, FRANCISCO", 
		}
		for (int i = 1; i < splitdba.length; i++) {
			p.addDba(splitdba[i]);
		}
		return p;
		//		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(raw + "\t");
		for (Integer r : roles) {
			String rl = mapRoleReverse.get(r);
			sb.append(" " + rl);
		}
		if (namePerson != null) {
			sb.append("==>IND:" + namePerson.completeCanonicalName());
		} else if (nameCorp != null) {
			sb.append("==>CORP:" + nameCorp);
		} else if (type == TYPE_GOVERNMENT) {
			sb.append("==>GOV: " + name);
		} else if (type == TYPE_ORGANIZATION) {
			sb.append("==>ORG: " + name);
		} else if (type == TYPE_DOESROESMOES) {
			sb.append("==>DOES: " + name);
		}
		return sb.toString();
	}

	static class PersonName {
		String surname;
		String givname;
		String midname;
		List<String> suffixes; // JR, MD, DDS,
		List<String> akas;
		boolean isSpanish = false;

		Pattern namePattern;

		public Pattern getPattern() {
			if (namePattern != null)
				return namePattern;
			String p1 = surname + ",\\s*" + givname;
			if (midname != null) {
				p1 += "(\\s*" + midname;
				if (midname.length() == 1) {
					p1 += "\\.?";
				}
				p1 += ")?";
			}
			String p2 = givname;
			if (midname != null) {
				p2 += "(\\s*" + midname;
				if (midname.length() == 1) {
					p2 += "\\.?";
				}
				p2 += ")?";
			}
			p2 += "\\s*" + surname;
			if (suffixes != null) {
				for (String s : suffixes) {
					if (s.endsWith(".")) {
						s = s.substring(0, s.length() - 1) + "\\.?";
					} else {
						s += "\\.?";
					}
					String r = "(\\,?\\s*" + s + ")?";
					p2 += r;
				}
			}
			String reg = p1 + "|" + p2;
			namePattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
			return namePattern;
		}

		public String canonicalName() {
			StringBuilder sb = new StringBuilder();
			sb.append(surname);
			sb.append(" ");
			sb.append(givname);
			if (midname != null) {
				sb.append(" " + midname);
			}
			return sb.toString();
		}

		public String normalName() {
			StringBuilder sb = new StringBuilder();
			sb.append(givname);
			if (midname != null) {
				sb.append(" " + midname);
			}
			sb.append(" " + surname);
			return sb.toString();
		}

		public String completeCanonicalName() {
			StringBuilder sb = new StringBuilder();
			sb.append(surname);
			if (suffixes != null) {
				for (String s : suffixes) {
					sb.append(" " + s);
				}
			}
			sb.append(", " + givname);
			if (midname != null)
				sb.append(" " + midname);
			return sb.toString();
		}

		public PersonName(String name) {
			int idx = name.indexOf("(");
			if (idx > 0) {
				name = name.substring(0, idx);
			}
			idx = name.indexOf("**");
			if (idx > 0) {
				name = name.substring(0, idx);
			}
			name = name.replaceAll("\\*", "");
			String[] splitAKA = name.split("\\sAKA\\s");
			if (splitAKA.length > 1) {
				akas = new ArrayList<>();
				for (int i = 1; i < splitAKA.length; i++) {
					akas.add(splitAKA[i]);
				}
				name = splitAKA[0];
			}
			String[] split = name.split("\\,");
			if (split.length > 1) {
				String[] split1 = split[0].split("\\s+");
				surname = split1[0];
				for (int i = 1; i < split1.length; i++) {
					String s = split1[i].replaceAll("\\p{Punct}", "");
					if (suffixNames.contains(s)) {
						addSuffix(s);
					} else {
						surname += " " + s;
						isSpanish = true;
					}
				}
				String[] split2 = split[1].trim().split("\\s+");
				givname = split2[0];
				if (split2.length > 1) {
					midname = split2[1].replaceAll("\\p{Punct}", "").trim();
					for (int j = 2; j < split2.length; j++) {
						midname += " " + split2[j];
					}
				}
			}
		}

		void addSuffix(String s) {
			if (suffixes == null) {
				suffixes = new ArrayList<>();
			}
			if (!suffixes.contains(s)) {
				suffixes.add(s);
			}
		}
	}

	static class CorpName {
		List<String> types = new ArrayList<>(); // inc, llp, corp, company, N.A., 
		String stem; // Citiank
		String name;
		String attachment; // A MUNICIPAL CORPORATION, A NEW YORK CORPORATION, AS TRUSTEE OF THE XIAO ZHENG FAMILY LIVING TRUST, AS ASSIGNEE OF CITIBANK, N.A

		public CorpName(String _name) {
			name = _name.trim();
			stem = name;
			Matcher m2 = pCorpAttach.matcher(_name);
			if (m2.find()) {
				stem = name.substring(0, m2.start()).trim();
				attachment = m2.group().trim();
			}
			decompose(stem);
		}

		public boolean equals(Object o) {
			if (!(o instanceof CorpName)) {
				return false;
			}
			CorpName cn = (CorpName) o;
			if (stem.equals(cn.stem)) {
				return true;
			}
			return false;
		}

		private void decompose(String _name) {
			String[] split = _name.split("\\,");
			stem = split[0].trim();
			if (split.length > 1) {
				for (int i = 1; i < split.length; i++) {
					types.add(split[i].trim());
				}
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("stem: " + stem + "; ");
			for (String s : types) {
				sb.append(" " + s);
			}
			return sb.toString();
		}
	}

}
