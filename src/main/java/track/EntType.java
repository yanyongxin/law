package track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sftrack.CaseData.LitiParty;

public class EntType {
	static final String pllc = "pllc|p\\.\\s*l\\.\\s*l\\.\\s*c\\."; // Professional
																	// Limited
																	// Liability
																	// Company,
																	// a kind of
																	// LLC
	static final String pllp = "pllp|p\\.\\s*l\\.\\s*l\\.\\s*p\\.";
	static final String aplc = "a\\.?\\s*p\\.?\\s*l\\.?\\s*c\\.?"; // A
																	// Professional
																	// Law
																	// Corporation
	static final String APC = "a?\\s+professional\\s+corporation"; // A
																	// Professional
																	// Corporation
	static final String apc = "apc|a\\.\\s*p\\.\\s*c\\."; // A Professional
															// Corporation
	static final String plc = "p\\.?\\s*l\\.?\\s*c\\.?"; // Public limited
															// company
	static final String psc = "p\\.?\\s*s\\.?\\s*c\\.?";
	static final String lllc = "lllc|l\\.\\s*l\\.\\s*l\\.\\s*c\\."; // error for
																	// LLC,
																	// probably
																	// legal LLC
	static final String lllp = "lllp|l\\.\\s*l\\.\\s*l\\.\\s*p\\.";
	static final String llc = "llc|l\\.\\s*l\\.\\s*c\\.";
	static final String llp = "llp|l\\.\\s*l\\.\\s*p\\.";
	static final String lpa = "lpa|l\\.\\s*p\\.\\s*a\\.";
	static final String alc = "alc|a\\.\\s*l\\.\\s*c\\."; // A Law Corporation
	static final String lc = "l\\.?\\s*c\\.?";
	static final String ll = "l\\.?\\s*l\\.?";
	static final String lp = "l\\.?\\s*p\\.?";
	static final String na = "na|n\\.\\s*a\\.";
	static final String pa = "pa|p\\.\\s*a\\.";
	static final String paEx = "p\\.\\s*a\\."; // for ptEntitySuffixEx
	static final String pc = "p\\.?\\s*c\\.?";
	static final String pd = "p\\.?\\s*d\\.?";
	static final String pl = "p\\.?\\s*l\\.?";
	static final String sc = "s\\.?\\s*c\\.?";
	static final String ltd = "ltd\\.?";
	static final String inc = "inc\\.?";
	static final String incorp = "incorporated";
	static final String corp = "corp\\.?";
	static final String corporation = "corporation";
	static final String co = "co\\.";
	static final String company = "company";
	static final String group = "group";
	static final String Associates = "associates";
	static final String Assoc = "assoc\\.?";
	static final String esquire = "esquire";
	static final String esq = "esqs?\\.?";
	static final String etal = "et\\s*al";
	static final String etc = "etc\\.?";
	static final String attorneyAtLaw = "((attorney|counsellor|atty|att\\S+)s?\\s+at\\s+law)";
	static final String foundation = "foundation";
	static final String attorneys = "attorneys";
	static final Pattern ptEntitySuffix = Pattern.compile("\\b(" + pllc + "|" + pllp + "|" + aplc + "|" + APC + "|" + apc + "|" + plc + "|" + psc + "|" + lllc + "|" + lllp + "|"
			+ llc + "|" + llp + "|" + lpa + "|" + alc + "|" + lc + "|" + ll + "|" + lp + "|" + pa + "|" + na + "|" + pc + "|" + pd + "|" + pl + "|" + sc + "|" + inc + "|" + incorp
			+ "|" + foundation + "|" + corporation + "|" + corp + "|" + group + "|" + ltd + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
	static final Pattern ptEntitySuffixEx = Pattern.compile("\\b(" + pllc + "|" + pllp + "|" + aplc + "|" + APC + "|" + apc + "|" + plc + "|" + psc + "|" + lllc + "|" + lllp + "|"
			+ llc + "|" + llp + "|" + lpa + "|" + alc + "|" + lc + "|" + ll + "|" + lp + "|" + paEx + "|" + pc + "|" + pd + "|" + pl + "|" + sc + "|" + inc + "|" + incorp + "|"
			+ foundation + "|" + corporation + "|" + corp + "|" + ltd + ")" + "(-|\\(|\\,|\\;|\\*|\\.)*\\s*$", Pattern.CASE_INSENSITIVE);

	static final Map<String, Pattern> entmap = new HashMap<String, Pattern>();
	static final List<String> regex = new ArrayList<String>();

	static void init0() {
		Pattern pt;

		pt = Pattern.compile("\\b(" + pllc + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(pllc, pt);
		pt = Pattern.compile("\\b(" + pllp + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(pllp, pt);
		pt = Pattern.compile("\\b(" + aplc + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(aplc, pt);
		pt = Pattern.compile("\\b(" + plc + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(plc, pt);
		pt = Pattern.compile("\\b(" + psc + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(psc, pt);
		pt = Pattern.compile("\\b(" + lllc + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(lllc, pt);
		pt = Pattern.compile("\\b(" + lllp + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(lllp, pt);
		pt = Pattern.compile("\\b(" + llc + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(llc, pt);
		pt = Pattern.compile("\\b(" + llp + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(llp, pt);
		pt = Pattern.compile("\\b(" + lpa + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(lpa, pt);
		pt = Pattern.compile("\\b(" + na + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(pa, pt);
		pt = Pattern.compile("\\b(" + pa + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(pa, pt);
		pt = Pattern.compile("\\b(" + pc + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(pc, pt);
		pt = Pattern.compile("\\b(" + incorp + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(incorp, pt);
		pt = Pattern.compile("\\b(" + corporation + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(corporation, pt);
		pt = Pattern.compile("\\b(" + corp + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(corp, pt);
		pt = Pattern.compile("\\b(" + ltd + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(ltd, pt);
		pt = Pattern.compile("\\b(" + inc + ")" + "(\\s|$|-|\\(|\\,|\\;|\\*|\\.)", Pattern.CASE_INSENSITIVE);
		entmap.put(inc, pt);
	}

	static final String fmtypeRegex = inc + "|" + llc + "|" + pc + "|" + llp + "|" + corp + "|" + ltd + "|" + pa;

	static void init1() {
		regex.add(inc);
		regex.add(llc);
		regex.add(llp);
		regex.add(incorp);
		regex.add(corporation);
		regex.add(corp);
		regex.add(ltd);
		// regex.add(pllc);
		// regex.add(pllp);
		// regex.add(aplc);
		// regex.add(plc);
		// regex.add(psc);
		// regex.add(lllc);
		// regex.add(lllp);
		// regex.add(lpa);
		// regex.add(pa);
		// regex.add(pc);
	}

	static void init() {
		Pattern pt;
		pt = Pattern.compile(ltd, Pattern.CASE_INSENSITIVE);
		entmap.put(ltd, pt);
		pt = Pattern.compile(inc, Pattern.CASE_INSENSITIVE);
		entmap.put(inc, pt);
		pt = Pattern.compile(llc, Pattern.CASE_INSENSITIVE);
		entmap.put(llc, pt);
		pt = Pattern.compile(llp, Pattern.CASE_INSENSITIVE);
		entmap.put(llp, pt);
		pt = Pattern.compile(incorp, Pattern.CASE_INSENSITIVE);
		entmap.put(incorp, pt);
		pt = Pattern.compile(corporation, Pattern.CASE_INSENSITIVE);
		entmap.put(corporation, pt);
		pt = Pattern.compile(corp, Pattern.CASE_INSENSITIVE);
		entmap.put(corp, pt);

		// pt = Pattern.compile(pllc , Pattern.CASE_INSENSITIVE);
		// entmap.put(pllc, pt);
		// pt = Pattern.compile(pllp, Pattern.CASE_INSENSITIVE);
		// entmap.put(pllp, pt);
		// pt = Pattern.compile(aplc, Pattern.CASE_INSENSITIVE);
		// entmap.put(aplc, pt);
		// pt = Pattern.compile(plc, Pattern.CASE_INSENSITIVE);
		// entmap.put(plc, pt);
		// pt = Pattern.compile(psc, Pattern.CASE_INSENSITIVE);
		// entmap.put(psc, pt);
		// pt = Pattern.compile(lllc , Pattern.CASE_INSENSITIVE);
		// entmap.put(lllc, pt);
		// pt = Pattern.compile(lllp, Pattern.CASE_INSENSITIVE);
		// entmap.put(lllp, pt);
		// pt = Pattern.compile(lpa, Pattern.CASE_INSENSITIVE);
		// entmap.put(lpa, pt);
		// pt = Pattern.compile(pa , Pattern.CASE_INSENSITIVE);
		// entmap.put(pa, pt);
		// pt = Pattern.compile(pc , Pattern.CASE_INSENSITIVE);
		// entmap.put(pc, pt);
	}

	public static boolean isOrgCo(String line) {
		Matcher m = ptEntitySuffix.matcher(line);
		if (m.find()) {
			return true;
		}
		return false;
	}

	static synchronized String findCoTypePattern(String s) {
		if (entmap.size() == 0) {
			init();
		}
		Set<Entry<String, Pattern>> set = entmap.entrySet();
		for (Entry<String, Pattern> ent : set) {
			String rgx = ent.getKey();
			Pattern pt = ent.getValue();
			Matcher mt = pt.matcher(s);
			if (mt.matches()) {
				return rgx;
			}
		}
		return null;
	}

	static String findCoTypePattern1(String s) {
		if (regex.size() == 0) {
			init();
		}
		for (String r : regex) {
			if (Pattern.matches(r, s)) {
				return r;
			}
		}
		return null;
	}

	/**
	 * Given a list of Strings, List<String> fm, check if they are already in
	 * the known list of party names, List<String> fmRgxList. For those who are
	 * not, create regular expressions and put them in the known list.
	 * 
	 * @param fmRgxList
	 * @param fm
	 */
	public static void checkPartyOntoList(List<String> fmRgxList, List<String> fm) {
		List<String> remain = new ArrayList<String>();
		for (int i = fm.size() - 1; i >= 0; i--) {
			boolean bmatch = false;
			for (String rgx : fmRgxList) {
				Pattern ptn = Pattern.compile(rgx, Pattern.CASE_INSENSITIVE);
				Matcher m = ptn.matcher(fm.get(i));
				if (m.matches()) {
					break;
				}
			}
			if (!bmatch) {
				remain.add(fm.get(i));
			}
		}
		for (String rm : remain) {
			String rgx = rm.replaceAll("\\s+", "\\\\s+");
			rgx += "(" + fmtypeRegex + ")?";
			fmRgxList.add(rgx);
		}
	}

	public static void MergeParties(List<Litigant> partyLitigants, List<String> fm) throws Exception {
		List<String> remain = new ArrayList<String>();
		for (int i = 0; i < fm.size(); i++) {
			if (fm.get(i).length() <= 2) {
				continue;
			}
			boolean bmatch = false;
			for (Litigant lt : partyLitigants) {
				Pattern ptn = lt.ptn;
				Matcher m = ptn.matcher(fm.get(i));
				if (m.matches()) {
					bmatch = true;
					break;
				}
			}
			if (!bmatch) {
				remain.add(fm.get(i));
			}
		}
		for (String rm : remain) {
			partyLitigants.add(new Litigant(rm, "OrgCo"));
		}
	}

	public static void MergeAdditionalParties(List<LitiParty> parties, List<String> fm) throws Exception {
		List<String> remain = new ArrayList<String>();
		for (int i = 0; i < fm.size(); i++) {
			if (fm.get(i).length() <= 2) {
				continue;
			}
			boolean bmatch = false;
			for (LitiParty lt : parties) {
				Pattern ptn = lt.getPattern();
				Matcher m = ptn.matcher(fm.get(i));
				if (m.matches()) {
					bmatch = true;
					break;
				}
			}
			if (!bmatch) {
				remain.add(fm.get(i));
			}
		}
		for (String rm : remain) {
			parties.add(new LitiParty(rm));
		}
	}

	private static String standardNameRegexReplacement(String r) {
		r = r.replace(",", "\\,?");
		r = r.replace(".", "\\.?");
		r = r.replace("(", "\\(");
		r = r.replace(")", "\\)");
		r = r.replace("- ", " ");
		r = r.replace(" -", " ");
		return r.trim();
	}

	public static String judgeNameToRegex(String s) {
		if (s.endsWith("Jr")) {
			s += ".";
		}
		String[] split = s.trim().split("\\s+");
		StringBuilder sb = new StringBuilder();
		if (split.length == 2) {

		}
		for (int i = 0; i < split.length; i++) {
			String r = split[i];
			boolean mid = false;
			if (r.matches("\\p{Alnum}\\.?")) {
				mid = true;
			}
			r = standardNameRegexReplacement(r);
			if ((i <= 1 && i < split.length - 1) || mid) {
				// we have a first name or middle initial, we make it optional
				// judges differ from others in that first name can be optional also
				r = "(" + r + ")?";
			}
			sb.append(r);
			if (i < split.length - 1) {
				sb.append("\\s*");
			}
			if (split.length == 2 && i == 0 && split[0].length() > 1 && split[1].length() > 2) {
				// missing middle initial, put an optional one in.
				sb.append("(\\p{Alnum}\\.?\\s*)?");
			}
		}
		return sb.toString();
		//		return r;
	}

	public static String personNameToRegex(String s) {
		if (s.endsWith("Jr")) {
			s += ".";
		}
		String[] split = s.trim().split("\\s+");
		StringBuilder sb = new StringBuilder();
		if (split.length == 2) {

		}
		for (int i = 0; i < split.length; i++) {
			String r = split[i];
			boolean mid = false;
			if (r.matches("\\p{Alnum}\\.?")) {
				mid = true;
			}
			r = standardNameRegexReplacement(r);
			if (mid) {// we have a middle initial, we make it optional
				r = "(" + r + ")?";
			}
			sb.append(r);
			if (i < split.length - 1) {
				sb.append("\\s*");
			}
			if (split.length == 2 && i == 0 && split[0].length() > 1 && split[1].length() > 2) {
				// missing middle initial, put an optional one in.
				sb.append("(\\p{Alnum}\\.?\\s*)?");
			}
		}
		return sb.toString();
		//		return r;
	}

	/**
	 * Fish & Richardson ==> fish\\s*\\&\\s*richardson\\,?\\s*\\(fmtypeRegex\\)?
	 * 
	 * @param s
	 *            : output of normalized stuff, "Potter Anderson & Corroon"
	 * @return
	 */
	public static String[] LawFirmNameToRegex(String s) {
		String r = s.trim().replaceAll("\\s+", "\\\\s*");
		r = r.replace(",", "\\,?");
		r = r.replace(".", "\\.?");
		r = r.replace("&", "\\&?");
		r += "\\s*\\,?\\s*(" + fmtypeRegex + ")?";
		String[] split = s.split("\\s+");
		if (split[0].equalsIgnoreCase("the")) {
			split[0] = split[1];
		}
		String ret[] = new String[2];
		ret[0] = split[0];
		ret[1] = r;
		return ret;
	}

	public static String[] partyNameToRegex(String party, int significant) {
		party = party.trim();
		party = party.replaceAll("(?i:\\busa\\b)", "u.s.a.");
		party = party.replaceAll("\\*|\\?", "");

		party = party.replaceAll("(?i:(mr|dr)\\.?\\s)", "");
		party = party.replaceAll("(?i:\\bph\\.?\\sd\\.?\\s)", "");

		String[] split = party.split("\\s+");
		String rgx = EntType.findCoTypePattern(split[split.length - 1]);
		StringBuilder sb = new StringBuilder();
		if (significant >= split.length) {
			return null;
		}
		for (int i = 0; i < split.length; i++) {
			if (i == split.length - 1 && rgx != null && split.length > 1) {
				if (i > significant) {
					sb.append("(\\s*\\,?\\s*" + rgx + ")?");
				} else {
					sb.append("\\s*\\,?\\s*" + rgx);
				}
			} else {
				split[i] = split[i].replaceAll("\\.", "\\\\.?");
				split[i] = split[i].replaceAll("\\,", "\\\\,?");
				split[i] = split[i].replaceAll("\\(", "\\\\s\\*\\\\(");
				split[i] = split[i].replaceAll("\\)", "\\\\)\\\\s\\*");
				split[i] = split[i].replaceAll("\\+", "\\\\+");
				// if (split[i].endsWith(".")) {
				// split[i] = split[i].concat("?");
				// }
				// if (split[i].endsWith(",")) {
				// split[i] = split[i].concat("?");
				// }
				if (i > significant) {
					sb.append("\\s*" + "(" + split[i] + ")?");
				} else {
					if (i > 0) {
						sb.append("\\s*" + split[i]);
					} else {
						sb.append(split[i]);
					}
				}
			}
		}
		if (split.length >= 2) {
			split[1] = sb.toString();
			return split;
		} else {
			String[] ret = new String[2];
			ret[0] = split[0];
			ret[1] = sb.toString();
			return ret;
		}
		//		return sb.toString();
	}

	// Lawyer's address association through firms, so addresses are not listed.

	static Pattern pInc = Pattern.compile("\\b(inc|corp|co|ltd|corporation|llc|llp|associates|assoc|p\\.c)\\b", Pattern.CASE_INSENSITIVE);
	static Pattern pJohnDoe = Pattern.compile("\\b(john\\s+doe|jane\\s+doe)s?\\b", Pattern.CASE_INSENSITIVE);
	static Pattern pNum = Pattern.compile("\\d|\\#|\\s\\&|/", Pattern.CASE_INSENSITIVE);
	static Pattern pOther = Pattern.compile("\\b(us|usca|mrs|ms)\\b", Pattern.CASE_INSENSITIVE);

	// static Pattern pdba = Pattern.compile("d/b/a", Pattern.CASE_INSENSITIVE);

	public static String[] parsePersonName(String name, boolean isJudge) throws Exception {
		String nameRaw;
		String suffix = null;
		Matcher m = ptEntitySuffixEx.matcher(name);
		if (m.find()) {
			throw new Exception("Invalide attorney name: " + name);
		}
		m = pJohnDoe.matcher(name);
		if (m.find()) {
			throw new Exception("Invalide attorney name: " + name);
		}
		m = pNum.matcher(name);
		if (m.find()) {
			throw new Exception("Invalide attorney name: " + name);
		}

		nameRaw = name;
		name = name.replaceAll("\\\".+?\\\"|\\(.+?\\)|\\\"\\S+|\\(\\S+|\\[\\S+|\\s\\'", " ").trim();// "Chen Min \"Jack\" Juan ,"
		name = name.replaceAll("(?i)\\b(esq|phd|dr|mr|phv|Rabbi|prof(essors*)?|INACTIVE|MDL\\s*NOT\\s*ADMITT?ED|TRANSFER\\s*ATT(ORNE)?Y)\\b", " ").trim();// " Antonio Valla , Esq.",
		// "Geoffrey Lottenberg , PHV";"Prof. Mark A. Lemley"
		name = name.replaceAll("(-\\s*NA|N/A)\\b", "").trim();// Jere-NA F. White, Jr.
		name = name.replaceAll("(?i)\\.|\\(|\\)|\\;|\\,|\\:|-(\\s|$)|\\s-|\\+|\\*", " ").trim();// John H. Smith, Jr. => John H Smith, Jr
		if (name.length() == 0) {
			throw new Exception("Invalide attorney name: " + nameRaw);
		}
		String[] split = name.split("\\s+");
		int usableLength = split.length;
		if (split.length > 5) {
			throw new Exception("Invalide attorney name: " + name);
		}
		int firstUse = 0;
		if (split.length > 0) {
			if (isSuffix(split[split.length - 1])) {
				suffix = split[split.length - 1];
				usableLength--;
			}
		}
		List<String> nm = new ArrayList<String>();
		while (usableLength > 0 && split[usableLength - 1].length() == 1) {
			nm.add(split[usableLength - 1]);
			usableLength--;
		}
		if (usableLength <= 0) {
			throw new Exception("Invalide attorney name: " + nameRaw);
		}
		String surname = "";
		String givenname = "";
		ArrayList<String> middlename = null;
		surname = split[usableLength - 1];
		usableLength--;
		while (usableLength > 0) {
			nm.add(split[usableLength - 1]);
			usableLength--;
		}
		if (nm.size() > 0) {
			givenname = nm.remove(nm.size() - 1);
		}
		if (nm.size() > 0 && middlename == null) {
			middlename = new ArrayList<String>();
		}
		while (nm.size() > 0) {
			middlename.add(nm.remove(nm.size() - 1).toLowerCase());
		}
		if (usableLength > 1) {
			givenname = split[0];
			firstUse++;
		}
		int surnameLength = usableLength - firstUse;
		if (surnameLength > 1) {// van der waals, de Gennis, van Horn
			StringBuilder sb = new StringBuilder();
			for (int i = firstUse; i < usableLength; i++) {
				if (i > firstUse) {
					sb.append(" ");
				}
				sb.append(split[i]);
			}
			surname = sb.toString();
		}
		String listUnder = givenname + " " + surname;
		StringBuilder sb = new StringBuilder();
		if (isJudge) {
			sb.append("(");
			sb.append(givenname);
			sb.append(")?");
		} else {
			sb.append(givenname);
		}
		if (givenname.length() == 1) {
			sb.append("\\.?");
		}
		if (middlename != null) {
			for (String mid : middlename) {
				sb.append("(\\s*(");
				sb.append(mid);
				if (mid.length() > 1) {
					String midinitial = mid.substring(0, 1);
					sb.append("|" + midinitial);
				}
				sb.append(")\\.?)?");
			}
		} else {
			sb.append("(\\s*\\p{Alnum}\\.?)?");
		}
		sb.append("\\s*");
		sb.append(surname);
		if (suffix != null) {
			sb.append("(\\,?\\s*");
			sb.append(suffix);
			sb.append("\\.?)?");
		}
		String[] ret = new String[2];
		String regex = sb.toString();
		ret[0] = listUnder;
		ret[1] = regex;
		return ret;
	}

	public static boolean isSuffix(String s) {
		return (s.equalsIgnoreCase("jr") || s.equalsIgnoreCase("sr") || s.equalsIgnoreCase("II") || s.equalsIgnoreCase("III") || s.equalsIgnoreCase("IV"));
	}

	/**
	 * Break a list of names into List<String> of names;
	 * 
	 * @param str
	 *            (1) John M. Desmarais and Edward C. Donovan (2) John Lehmann, Nick Pilon, Karl Harris, George Bush, Jr.
	 * @return
	 */
	public static List<String> BreakNames(String str) {
		List<String> names = new ArrayList<String>();
		String[] split = str.split("(?i:\\s+and\\s+)");
		for (int i = 0; i < split.length; i++) {
			String sn = split[i];
			String[] sp = sn.split("\\,"); // John Lehmann, Nick Pilon, Karl Harris, George Bush, Jr. 
			if (sp.length > 1) {
				String name = sp[0];
				for (int k = 1; k < sp.length; k++) {
					if (name == null) {
						name = sp[k];
						continue;
					}
					String[] spp = sp[k].split("\\s+");
					if (isSuffix(spp[0])) {
						name += sp[k];
						names.add(name);
						name = null;
					} else {
						names.add(name);
						name = sp[k];
					}
				}
				if (name != null) {
					names.add(name);
				}
			} else {
				names.add(sn);
			}
		}
		return names;
	}
}
