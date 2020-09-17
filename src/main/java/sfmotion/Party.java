package sfmotion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import core.Entity;
import core.LegaLanguage;

public class Party {

	static final List<String> states = new ArrayList<>();

	static final Pattern pCorp = Pattern.compile(
			"\\b(INC|CORP(ORATION)?S?|CO|GMBH|COMPANY|INSTITUTE|LTD|LP|FUND|ENTERPRISES?|CITIBANK|LOAN|MORTUARY|L\\.?L\\.?C|HEALTHCARE|HOSPITAL"
					+ "|LIMITED|PARTNERSHIP|UNIVERSITY|COLLEGE|PREPARATORY|HIGH|SCHOOL|INSURANCE|EXCHANGE|BANC|CENTERS?|PROPERTIES|MUSEUM|FOUNDATION"
					+ "|LLP|PC|PLC|BANK|SYSTEM|TRUST|NEPHROLOGY|ASSOCIATES|RESTAURANT|CAFE|SECURIT(Y|IES)|BANCORP|AUTO|ELECTRIC|PHARMACEUTICALS"
					+ "|PARTNERS|WELLS FARGO|MEDICAL|GROUP|ENGINEERING|CLINIC|INCORPORATED|ORIGINATIONS|HOLDINGS?|ACCOUNTS?|TRANSPORTATION|LAB(ORATORIE)?S)\\b"
					+ "|\\b(N\\.A\\.|P\\.C\\.|L\\.P\\.|\\.COM|S\\.?A\\.?R\\.?L\\.?|N\\.V\\.|B\\.V\\.)",
			Pattern.CASE_INSENSITIVE);
	static final Pattern pCorpSuffix = Pattern.compile("\\b(INC|CORP|CO|LTD|LP|FSB|LLC|LLP|PC|PLC|NA|TRUST)\\b|\\b(N\\.\\s?A\\b\\.?|P\\.\\s?C\\b\\.?|L\\.\\s?P\\b\\.?)", Pattern.CASE_INSENSITIVE);
	static final Pattern pCorpAttach = Pattern.compile("\\bA (CALIFORNIA|DELAWARE|NEW YORK|BUSINESS|TRUST|SOLE PROPRIETORSHIP)|\\bAN ENTITY", Pattern.CASE_INSENSITIVE);
	static final Pattern pGov = Pattern.compile("\\b(CITY|COUNTY|STATE|DISTRICT|SAN FRANCISCO|LOS ANGELES|CCSF|OFFICER)\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern pOrg = Pattern.compile(
			"\\b(AND|OF|SF|FOR|THE|CALIFORNIA|UNITED|MEMORIAL|CATHEDRAL|ALLIANCE|HOME|OWNER|ASSOC(IATION)?"
					+ "|CATHOLIC|CHARITIES|JEWISH|FAMILY|SISTERS|HERMITAGE|MISSIONARY|BAPTIST|CHURCH|CHILDREN|SERVI?CES?|PUBLIC"
					+ "|FIRST|NATIONAL|REALTY|GUARDIAN|VALLEY|UNION|TERRACE|UTILITY|CONSUMERS?|ACTION|NETWORK|FRANCHISE|TAX|BOARD"
					+ "|PROJECT|AMERICAN|RESEARCH|BUREAU|EMERGENCY|SOUTH|DAKOTA|VILLAGE|HORSE|SANCTUARY|RETIREMENT|COMMUNITY|RESCUE"
					+ "|EUROPE|INHERITANCE|FUNDING|INTERNATIONAL|PRIMATE|PROTECT|LEAGUE|NURSING|HOUSING|COALITION|STARS|PACIFIC"
					+ "|DIRECTORY|MODEL|ESTATE|MANAGEMENT|STUDIOS?|AGING|PLACE|HOMEOWNERS?|DISCOVER|PROFESSIONAL|COLLECTION|CONSULTANS"
					+ "|TRANSPORT|REDWOOD|NURSERY|MINISTRIES|MORTGAGE|INVESTORS?|COMPENSATION|OPERATING|CLUB|INDUSTRIAL|PRESERVATION"
					+ "|WELFARE|COMMISSION|PLAN|OUTDOOR|AFL-CIO|CONSTRUCTIONS?|STREET|NORTHERN|NEWS|COUNCIL|EDUCATION|ALL|PETITIONERS"
					+ "|TOGETHER|CALIFORNIANS|UNIDAS|SAFETY|EQUIPMENT|SPROUT|UNKNOWN|DEPOSITOR|PARKING|RESTORE|ASSURANCE|PRESERVATION"
					+ "|SAVE|TRADING|INDIAN|SPRINGS|VINEYARDS|UNITE|LOCAL|STATEWIDE|ENFORCEMENT|PERCENT|METROPOLITAN|JOINT|APPRENTIESHIP"
					+ "|COMMITTEE|CONGREGATION|BROTHERHOOD|AGAINST|DOMESTIC|VIOLENCE|FIDELITY|BROKERAGE|UNDERWRITERS?|REPUBLIC|STUDENT"
					+ "|ORGANIZATION|TERMINATOR|METAL|RELIABLE|EXPERTS|NEIGHBORS|DWELLERS|PEOPLE|ORGANIZED|TO|EMPLOYMENT|RIGHTS|AGENCY)\\b",
			Pattern.CASE_INSENSITIVE);
	static final Pattern pDoe = Pattern.compile("^(DOE|MOE|ROE|ZOE)S?|\\d+\\s*THROUGH|\\bINCLUSIVE\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern pNum = Pattern.compile("\\s\\d+\\s");
	static final Pattern pBrackets = Pattern.compile("\\(.+?\\)");
	static final Pattern pErrSued = Pattern.compile("(ERRONEOUSLY\\s)?(NAM|SU)ED\\s(\\w+ )*AS", Pattern.CASE_INSENSITIVE);
	static final Pattern pIndividual = Pattern.compile("(\\bAS )?AN INDIVIDUAL\\b|INDIVIDUALLY", Pattern.CASE_INSENSITIVE);
	//	static final Pattern pIndividually = Pattern.compile("(\\bAS )?AN INDIVIDUAL\\b|INDIVIDUALLY", Pattern.CASE_INSENSITIVE);
	static final Pattern pMinor = Pattern.compile("\\bA MINOR\\b", Pattern.CASE_INSENSITIVE);
	static final Pattern pOnBehalfOf = Pattern.compile("( AND )?ON BEHALF OF\\b|\\bO/?B/?O\\b|ON THEIR OWN BEHALF", Pattern.CASE_INSENSITIVE);
	static final Pattern pTrustee = Pattern.compile("TRUSTEE OF\\b", Pattern.CASE_INSENSITIVE);
	static final String regAsSomethingOf = "\\bAS (\\w+ )+OF ";
	public static final Map<String, Integer> mapRole = new HashMap<>();
	static final Map<Integer, String> mapRoleReverse = new HashMap<>();
	static final public int TYPE_UNKNOWN = 0;
	static final public int TYPE_INDIVIDUAL = 1;
	static final public int TYPE_CORPORATION = 2;// CORP, INC, LLC, LTD, LLP, GROUP
	static final public int TYPE_ORGANIZATION = 3;// CORP, INC, LLC, LTD, LLP, GROUP
	static final public int TYPE_GOVERNMENT = 4;
	static final public int TYPE_DOESROESMOES = 5;
	static final public int TYPE_MINOR = 6;
	static final public int TYPE_OTHER = 10;

	public static final int ROLE_UNKNOWN = 0;
	public static final int ROLE_PLAINTIFF = 1;
	public static final int ROLE_DEFENDANT = 2;
	public static final int ROLE_CROSS_PLAINTIFF = 3;
	public static final int ROLE_CROSS_DEFENDANT = 4;
	public static final int ROLE_PETITIONER = 5;
	public static final int ROLE_RESPONDENT = 6;
	public static final int ROLE_MINOR = 7;
	public static final int ROLE_CLAIMANT = 8;
	public static final int ROLE_DECEDENT = 9;
	public static final int ROLE_APPELLANT = 10;
	public static final int ROLE_TRUSTEE = 11;
	public static final int ROLE_CONSERVATEE = 12;
	public static final int ROLE_CONSERVATOR = 13;
	public static final int ROLE_GUARDIAN_AD_LITEM = 14;
	public static final int ROLE_INTERVENOR_DEFENDANT = 15;
	public static final int ROLE_INTERESTED_PARTY = 16;
	public static final int ROLE_ASSIGNEE = 17;
	public static final int ROLE_INTERVENOR = 18;
	public static final int ROLE_SUPPORTER = 19;
	public static final int ROLE_APPLICANT = 20;
	public static final int ROLE_CROSS_APPELLANT = 21;
	public static final int ROLE_INTERPLEADER = 22;
	public static final int ROLE_OTHER = 23;

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
		mapRole.put("APPLICANT", ROLE_APPLICANT);
		mapRole.put("CROSS APPELLANT", ROLE_CROSS_APPELLANT);
		mapRole.put("INTERPLEADER", ROLE_INTERPLEADER);
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
		mapRoleReverse.put(ROLE_SUPPORTER, "SUPPORTER");
		mapRoleReverse.put(ROLE_OTHER, "OTHER");

	}
	public List<String> raw = new ArrayList<>();
	public String name;
	public PersonName namePerson; // use only if type = TYPE_INDIVIDUAL
	public CorpName nameCorp; // use only if type = TYPE_ORGANIZATION
	public String address; // some have address in their name field

	List<Integer> roles = new ArrayList<>();
	public int type;// INDIVIDUAL, CORPORATION, GOVERNMENT, DOESROES, UNKNOWN
	List<CorpName> dba;
	public String errSued;//SUED ERRONEOUSLY HEREIN AS TAKITAKI MITIGLI, (ERRONEOUSLY SUED HEREIN AS "ALDRY BONIFACIO")
	//HILL, DAVID INDIVIDUALLY, AND ON BEHALF OF THE ESTATE OF JAMES D. HILL, DECEASED
	//GRAVES, NICHOLAS INDIVIDUALLY, AND ON BEHALF OF ALL OTHERS SIMILARLY SITUATED
	String onBehalfOf;
	List<String> asSomethingOf;
	public Entity entity;

	public void createEntity(LegaLanguage legalang) {
		if (entity != null)
			return;
		if (type == sfmotion.Party.TYPE_INDIVIDUAL || type == sfmotion.Party.TYPE_MINOR) {
			entity = new Entity(this.name, legalang.getEntity("IndividualParty"), Entity.TYPE_INSTANCE, legalang, -1);
			entity.setObject(this);
		} else if (type == sfmotion.Party.TYPE_DOESROESMOES) {
			entity = new Entity(this.name, legalang.getEntity("GenericParty"), Entity.TYPE_INSTANCE, legalang, -1);
			entity.setObject(this);
		} else {
			entity = new Entity(this.name, legalang.getEntity("OrgCoParty"), Entity.TYPE_INSTANCE, legalang, -1);
			entity.setObject(this);
		}
	}

	public void addDba(String _d) {
		if (dba == null) {
			dba = new ArrayList<>();
		}
		String[] _dbas = _d.split("(\\s|\\b)(A|B|C|D)\\)");
		if (_dbas.length > 1) {
			for (String s : _dbas) {
				String ss = s.trim();
				if (ss.length() > 2) {
					CorpName on = new CorpName(s.trim());
					dba.add(on);
				}
			}
		} else {
			CorpName on = new CorpName(_d);
			dba.add(on);
		}
	}

	public void addDba(List<CorpName> listCorp) {
		if (dba != null) {
			dba.addAll(listCorp);
		} else {
			dba = listCorp;
		}
	}

	public Party(String _raw, String _n, int _r) {
		raw.add(_raw);
		name = _n;
		roles.add(_r);
	}

	public Party(String _raw, String _n, String _r) {
		name = _n;
		Integer r = mapRole.get(_r);
		roles.add(r);
	}

	public Party(String _raw, String _n, int _r, int _type) {
		raw.add(_raw);
		name = _n;
		roles.add(_r);
		type = _type;
	}

	public void setPersonName(PersonName pn) {
		namePerson = pn;
	}

	public void setType(int _type) {
		type = _type;
	}

	public void setCorp(CorpName _corp) {
		nameCorp = _corp;
	}

	public void setName(String _name) {
		name = _name;
	}

	public void setOnBehalfOf(String _s) {
		onBehalfOf = _s;
	}

	public void addAsSomethingOf(String _s) {
		if (asSomethingOf == null) {
			asSomethingOf = new ArrayList<>();
		}
		asSomethingOf.add(_s);
	}

	public void setErrSued(String _err) {
		errSued = _err;
	}

	public Party(String _raw, PersonName _pn, int _r) {
		raw.add(_raw);
		namePerson = _pn;
		name = namePerson.canonicalName();
		roles.add(_r);
		type = TYPE_INDIVIDUAL;
	}

	public Party(String _raw, CorpName _on, int _r) {
		raw.add(_raw);
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
		//		if (_p.nameCorp != null && this.nameCorp != null) {
		//			if (_p.nameCorp.stem.equals("HOSIE RICE") && this.nameCorp.stem.equals("HOSIE RICE")) {
		//				System.out.println("HOSIE RICE");
		//			}
		//		}
		if (_p.type != type) {
			return false;
		}
		if (type == TYPE_INDIVIDUAL) {
			boolean b = namePerson.samePerson(_p.namePerson);
			if (!b)
				return false;
		} else if (type == TYPE_CORPORATION) {
			List<Object> listAnd = Utils.listAND(raw, _p.raw);
			if (listAnd.isEmpty()) {
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
		return true;
	}

	public void setAddress(String _addr) {
		address = _addr;
	}

	public void combine(Party _p) {// sameParty(_p)=true;
		for (String rw : _p.raw) {
			if (!raw.contains(rw))
				raw.add(rw);
		}
		for (Integer r : _p.roles) {
			if (!roles.contains(r)) {
				roles.add(r);
			}
		}
	}

	public void addRole(int _role) {
		roles.add(_role);
	}

	public void orderRaw() {
		raw.sort(new Cmp());
	}

	public static class Cmp implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			return o2.length() - o1.length();
		}

	}

	/**
	 * 		CAPITAL ONE BANK (USA), N.A.
			 CRIST, LINDA J(MOTHER)
			 OPORTUN INC. (FORMERLY PROGRESS FINANCIAL CORP.)
			 SHAUL, NAFTALI AKA ( ELI SHAUL )
			 CURREY, SCOTT MONTGOMERY (SBN 242320)
			 RYAN, SANDRA LYNN APPELL (AKA SANDRA APPELL RYAN)
			 (LAST NAME UNKNOWN), FREDY AN INDIVIDUAL
			 CATHAY MORTUARY - (WAH SANG) A CALIFORNIA CORPORATION
			 SCHWARTZ, DAVID (SCHWARTZ AND ASSOCAITES LANDSCAPE ARCHITECTURE INC)
			 LO, CHONG SHUNG (AKA CHUNG SHUNG LO) (AKA WENDY LO)	
			 DOE 1 (SECURITY GUARD A)
			 SHARP, EVA (SUED AS SHELLEY SHARP)
			 RUIZ (MARTINEZ), IGNACIO
			 ARELLANO, FREDDY (OWNER)
			 NATIONAL COLLEGIATE STUDENT LOAN TRUST 2007-2 A DELAWARE STATUTORY TRUST(S)
			 WKPE, INC., DBA BADGER FIRE PROTECTION (INCORRECTLY SUED HEREIN AS "BADGER FIRE PROTECTION")
			 HOSEK, LOUIS ET, AL (SAN FRANCISCO COUNTY SUPERIOR COURT CASE CGC-16-554603)
			 A-AZTEC RENTS & SELLS, INC. (SUED HEREIN AS A-AZTEC RENTS AND SELLS, INC. (AKA AZTEX TENTS))
			 OHANA PARTNERS, INC (ERRONEOUSLY SUED AS STUART RENTAL COMPANY AKA STUART EVENT RENTALS AKA STUART RENTALS)
			 ECIG 101 DIGITAL CIGARETTES (ERRONEOUSLY SUED AS E-CIG 101)
			 AUNDRES, RICHARD (FROM CONSOLIDATED CASE # CGC-17-556931)
			 PAT'S LIEN SERVICE, INC. (A CALIFORNIA CORPORATION)
	*/

	public static Party parse(String _name, int role) {
		String raw = _name;
		//		if (_name.startsWith("TOYOTA MOTOR SALES U.S.A.INC.( A CALIFORNIA CORPORATION")) {//PROPERTY SUBJECT TO DISPOSITION: $5,558.00 U.S. CURRENCY
		//			System.out.println();// total 12 instances in SF party data
		//		}
		if (_name.startsWith("PROPERTY SUBJECT TO")) {//PROPERTY SUBJECT TO DISPOSITION: $5,558.00 U.S. CURRENCY
			return null;// total 12 instances in SF party data
		}
		if (_name.startsWith("$")) {//$884.00 U.S. CURRENCY
			return null;// total 12 instances in SF party data
		}
		//		if (_name.startsWith("PLANT CONSTRUCTION COMPANY")) {
		//			System.out.println("PLANT CONSTRUCTION COMPANY");
		//		}
		//		create a Party first, then modify its contents.
		Party party = new Party(raw, _name, role);
		// remove brackets:
		int idx1 = _name.indexOf('(');
		if (idx1 >= 0) {
			int idx2 = _name.lastIndexOf(')');
			if (idx2 > idx1) {
				String inBrackets = _name.substring(idx1 + 1, idx2).trim();
				_name = _name.substring(0, idx1) + _name.substring(idx2 + 1);
				Matcher m = pErrSued.matcher(inBrackets);
				if (m.find()) {
					String errsued = inBrackets.substring(m.end()).trim();
					party.setErrSued(errsued);
					party.setName(_name);
				}
			}
		}
		Matcher m = pErrSued.matcher(_name);
		if (m.find()) {
			String errsued = _name.substring(m.end()).trim();
			party.setErrSued(errsued);
			_name = _name.substring(0, m.start()).trim();
			party.setName(_name);
		}
		//		Matcher m1 = pBrackets.matcher(_name);
		//		if (m1.find()) {
		//			String inBracket = m1.group();
		//			_name = _name.substring(0, m1.start()).trim() + " " + _name.substring(m1.end()).trim();
		//		}
		m = pDoe.matcher(_name);
		if (m.find()) {//DOES 1 TO 25, INCLUSIVE
			party.setType(TYPE_DOESROESMOES);
			return party;
		}
		String[] splitCO = _name.split("\\bC/O\\b");
		if (splitCO.length > 1) {
			_name = splitCO[0];
		}
		String[] splitdba = _name.split("(AND|ALSO|NOW)?\\s((A|F)?D(/|\\.)?B(/|\\.)?A(/|\\.)?|DOING BUSINESS AS|FORMERLY KNOWN AS|KNOWN AS)");
		if (splitdba.length > 1) {//"TAYLOR, GEORGE DBA MASTER ROOTER PLUMBING", "WESTLAKE SERVICES, LLC DBA WESTLAKE FINANCIAL SERVICES"
			_name = splitdba[0].trim();
			party.setName(_name);
			for (int i = 1; i < splitdba.length; i++) {
				party.addDba(splitdba[i]);
			}
		}
		m = pCorp.matcher(_name);
		if (!m.find()) {
			m = pCorpAttach.matcher(_name);
		}
		if (m.find(0)) {
			Matcher mm = pIndividual.matcher(_name);
			if (!(mm.find() && mm.start() < m.start())) {
				CorpName on = new CorpName(_name);// "COSTCO WHOLESALE CORPORATION","SEPHORA USA, INC.", "LC BUSINESS SYSTEMS CORP"
				party.setName(_name);
				party.setCorp(on);
				party.setType(TYPE_CORPORATION);
				return party;
			}
		}
		m = pGov.matcher(_name);
		if (m.find()) {//CITY AND COUNTY SAN FRANCISCO TAX COLLECTOR
			party.setName(_name);
			party.setType(TYPE_GOVERNMENT);
			return party;
		}
		m = pIndividual.matcher(_name);
		if (m.find()) {
			String s1 = _name.substring(0, m.start()).trim();
			String s2 = _name.substring(m.end());
			party.setType(TYPE_INDIVIDUAL);
			_name = s1;
			party.setName(_name);
			if (s2.length() > 10) {
				m = pOnBehalfOf.matcher(s2);
				if (m.find()) {
					party.setOnBehalfOf(s2.substring(m.end()).trim());
				}
				String[] asSomethingOf = s2.split(regAsSomethingOf);
				if (asSomethingOf.length > 1) {
					for (int i = 0; i < asSomethingOf.length; i++) {
						if (asSomethingOf[i].length() > 5)
							party.addAsSomethingOf(asSomethingOf[i]);
					}
				}
			}
			//			return party;
		}
		m = pOnBehalfOf.matcher(_name);
		if (m.find()) {
			String s1 = _name.substring(0, m.start()).trim();
			String s2 = _name.substring(m.end());
			party.setType(TYPE_UNKNOWN);
			_name = s1;
			party.setName(_name);
			party.setOnBehalfOf(s2.trim());
		}
		m = pTrustee.matcher(_name);//RITTER, KARYN TRUSTEE OF THE RITTER LITTLEFIELD LIVING TRUST
		if (m.find()) {
			_name = _name.substring(0, m.start()).trim();
		}
		m = pOrg.matcher(_name);
		if (m.find()) {//AMERICAN HOME SHIELD OF CALIFORNIA
			party.setName(_name);
			party.setType(TYPE_ORGANIZATION);
			return party;
		}
		//		String[] aka = _name.split(" A/?K/?A/? | ALSO KNOWN AS ");
		//		if (aka.length > 1) {
		//			_name = aka[0];
		//			for (int i = 1; i < aka.length; i++) {
		//				party.addAKA(aka[i]);
		//			}
		//			party.setName(_name);
		//			if (party.type == TYPE_UNKNOWN) {
		//				party.setType(TYPE_INDIVIDUAL);
		//			}
		//		}
		//		if(role==ROLE_MINOR) {

		m = pNum.matcher(_name);
		if (m.find()) {
			String addr = _name.substring(m.start()).trim();
			_name = _name.substring(0, m.start()).trim();
			party.setAddress(addr);
		}
		PersonName pn = new PersonName(_name);
		if (pn.surname == null) {//SAN FRANCISCO PUBLIC ADMINISTRATOR
			party.setType(TYPE_ORGANIZATION);
		} else {
			party.setPersonName(pn);
			party.setType(TYPE_INDIVIDUAL);
		}
		return party;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String rw : raw) {
			sb.append(rw + "\n");
		}
		if (namePerson != null) {
			sb.append("\t==>IND:" + namePerson.completeCanonicalName() + " pattern: " + namePerson.getPattern().toString());
			if (namePerson.akas != null) {
				for (String a : namePerson.akas) {
					sb.append("\n\tAKA: " + a);
				}
			}
		} else if (nameCorp != null) {
			sb.append("\t==>CORP:" + nameCorp);
		} else if (type == TYPE_GOVERNMENT) {
			sb.append("\t==>GOV: " + name);
		} else if (type == TYPE_ORGANIZATION) {
			sb.append("\t==>ORG: " + name);
		} else if (type == TYPE_DOESROESMOES) {
			sb.append("\t==>DOES: " + name);
		}
		for (Integer r : roles) {
			String rl = mapRoleReverse.get(r);
			sb.append("; " + rl);
		}
		if (this.errSued != null) {
			sb.append("\n\tErr: " + errSued);
		}
		if (this.dba != null) {
			for (CorpName d : dba) {
				sb.append("\n\tDBA: " + d);
			}
		}
		return sb.toString();
	}

	public static class CorpName {
		public List<String> types = new ArrayList<>(); // inc, llp, corp, company, N.A., 
		public String stem; // Citiank
		public String name;
		public String attachment; // A MUNICIPAL CORPORATION, A NEW YORK CORPORATION, AS TRUSTEE OF THE XIAO ZHENG FAMILY LIVING TRUST, AS ASSIGNEE OF CITIBANK, N.A
		public Pattern pattern;

		public CorpName(String _name) {
			name = _name.replaceAll("^\\W+|\\W+$", "").trim();
			stem = name;
			Matcher m2 = pCorpAttach.matcher(_name);
			if (m2.find()) {
				stem = name.substring(0, m2.start()).trim();
				attachment = name.substring(m2.start()).trim();
			}
			decompose(stem);
			buildPattern();
		}

		private void buildPattern() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < stem.length(); i++) {
				char c = stem.charAt(i);
				if (Character.isAlphabetic(c) || Character.isDigit(c)) {
					sb.append(c);
				} else if (Character.isSpaceChar(c)) {
					sb.append("\\s*");
				} else if (c == '.') {
					sb.append("\\.?");
				} else {
					sb.append("\\" + c);
				}
			}
			//			sb.append(stem.replaceAll("\\s+", "\\\\s*"));
			if (types != null && types.size() > 0) {
				sb.append("\\s*\\,?\\s{0,2}(");
				for (int i = 0; i < types.size(); i++) {
					String tp = types.get(i);
					boolean isSpace = false;
					if (i > 0) {
						sb.append("|");
					}
					for (int j = 0; j < tp.length(); j++) {
						char c = tp.charAt(j);
						if (Character.isAlphabetic(c) || Character.isDigit(c)) {
							sb.append(c);
						} else if (Character.isSpaceChar(c)) {
							if (!isSpace) {
								sb.append("\\s*");
								isSpace = true;
							}
						} else if (c == '.') {
							sb.append("\\.?");
						} else {
							sb.append("\\" + c);
						}
					}
					sb.append("\\.?");
				}
				sb.append(")?");
			}
			String reg = sb.toString();
			try {
				pattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
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
			_name = _name.trim();
			String[] split = _name.split("\\,|\\(|\\[|\\)|\\]");
			stem = split[0].trim();
			//			if (split.length > 2) {
			//				System.out.println(_name);
			//			}
			if (split.length > 1 || (split.length == 1 && (stem.length() < _name.length()))) {
				for (int i = 1; i < split.length - 1; i++) {
					types.add(split[i].trim());
				}
				String s = split[split.length - 1].trim();
				Matcher m = pCorpSuffix.matcher(s);
				if (m.find()) {
					String sufx = m.group();
					String remain = s.substring(m.end()).trim();
					if (remain.length() > 0 && remain.charAt(0) == '.') {
						sufx = sufx + ".";
						remain = remain.substring(1).trim();
					}
					types.add(sufx);
					String rr = remain.replaceAll("^\\W+|\\W+$", "").trim();
					if (rr.length() > 1) {
						this.attachment = remain + " " + attachment;
						attachment = attachment.trim();
					}
					if (split.length == 1) {
						stem = s.substring(0, m.start()).trim();
					}
				} else {
					this.attachment = s;
				}
			} else {
				split = _name.split("\\s+");
				String last = split[split.length - 1];
				Matcher m = pCorpSuffix.matcher(last);
				if (m.find()) {
					types.add(last);
					stem = _name.substring(0, _name.length() - last.length()).trim();
				}
			}
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("stem: " + stem + "; ");
			for (String s : types) {
				sb.append(" " + s);
			}
			sb.append(" pattern: " + this.pattern.toString());
			return sb.toString();
		}
	}

}
