package legal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import utils.MyStringUtils;

public class PersonName implements Comparable<PersonName> {
	static final int GivMidSur = 1;
	static final int SurGivMid = 2;
	static final List<String> suffixNames = new ArrayList<>();
	static {
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
		suffixNames.add("ATTORNEY AT LAW");
	}
	String raw;
	String surname;
	String givname;
	Object midname;
	List<String> suffixes; // JR, MD, DDS,
	List<String> akas;
	boolean isSpanish = false;

	Pattern namePattern;

	public Pattern getPattern() {
		if (namePattern != null)
			return namePattern;
		String regex = this.getMediumRegex();
		namePattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		return namePattern;
	}

	public Pattern getPatternOld() {
		if (namePattern != null)
			return namePattern;
		String sufx = "";
		if (suffixes != null) {
			for (String s : suffixes) {
				if (s.endsWith(".")) {
					s = s.substring(0, s.length() - 1) + "\\.?";
				} else {
					s += "\\.?";
				}
				String r = "(\\,?\\s*" + s + ")?";
				sufx += r;
			}
		}
		String p1 = surname + sufx + ",\\s*" + givname;
		if (midname != null) {
			if (midname instanceof String) {
				String mid = (String) midname;
				p1 += "(\\s*" + mid;
				if (mid.length() == 1) {
					p1 += "\\.?";
				}
				p1 += ")?";
			} else {
				@SuppressWarnings("unchecked")
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					p1 += "(\\s*" + mid;
					if (mid.length() == 1) {
						p1 += "\\.?";
					}
					p1 += ")?";
				}
			}
		}
		String p2 = givname;
		if (midname != null) {
			if (midname instanceof String) {
				String mid = (String) midname;
				p2 += "(\\s*" + mid;
				if (mid.length() == 1) {
					p2 += "\\.?";
				}
				p2 += ")?";
			} else {
				@SuppressWarnings("unchecked")
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					p2 += "(\\s*" + mid;
					if (mid.length() == 1) {
						p2 += "\\.?";
					}
					p2 += ")?";
				}
			}
			//			p2 += "(\\s*" + midname;
			//			if (midname.length() == 1) {
			//				p2 += "\\.?";
			//			}
			//			p2 += ")?";
		}
		p2 += "\\s*" + surname + sufx;
		String reg = p1 + "|" + p2;
		namePattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
		return namePattern;
	}

	public String getWeakRegex() {
		String sufx = "";
		if (suffixes != null) {
			for (String s : suffixes) {
				if (s.endsWith(".")) {
					s = s.substring(0, s.length() - 1) + "\\.?";
				} else {
					s += "\\.?";
				}
				String r = "(\\,?\\s*" + s + ")?";
				sufx += r;
			}
		}
		String p1 = "\\b" + surname + sufx + ",\\s*" + givname + "\\b";
		if (givname.length() == 1) { // allow M. to MING-XING
			p1 += "(\\w|-)*\\.?";
		}
		if (midname != null) {
			if (midname instanceof String) {
				String mid = (String) midname;
				p1 += "(\\s*" + mid;
				if (mid.length() == 1) {
					p1 += "(\\w|-)*\\.?"; // allow M. to MING-XING
				}
				p1 += ")?";
			} else {
				@SuppressWarnings("unchecked")
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					p1 += "(\\s*" + mid;
					if (mid.length() == 1) {
						p1 += "(\\w|-)*\\.?";
					}
					p1 += ")?";
				}
			}
		}
		String p2 = "\\b" + givname.substring(0, 1);
		if (givname.length() == 1) {
			p2 += "(\\.\\s*)?";
		} else {
			p2 += "(" + givname.substring(1) + "\\s*)?";
		}
		if (midname != null) {
			if (midname instanceof String) {
				String mid = (String) midname;
				p2 += "(\\s*" + mid;
				if (mid.length() == 1) {
					p2 += "(\\w|-)*\\.?";
				}
				p2 += ")?";
			} else {
				@SuppressWarnings("unchecked")
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					p2 += "(\\s*" + mid;
					if (mid.length() == 1) {
						p2 += "(\\w|-)*\\.?";
					}
					p2 += ")?";
				}
			}
		}
		p2 = "(" + p2 + "\\s*)?" + "\\b" + surname + "\\b" + sufx;
		String regex = p1 + "|" + p2;
		if (givname.length() == 1 && midname != null && (midname instanceof String)) {
			// "A. JAMES ROBERTSON, II" can appear as "JAMES A. ROBERTSON, II" also. Don't know why.
			String mid = (String) midname;
			if (mid.length() > 1) {
				PersonName pp = new PersonName();
				pp.surname = surname;
				pp.givname = mid;
				pp.midname = givname;
				if (suffixes != null) {
					for (String sx : suffixes) {
						pp.addSuffix(sx);
					}
				}
				String rgx = pp.getWeakRegex();
				regex = regex + "|" + rgx;
			}
		}
		return regex;
	}

	public String getMediumRegex() {
		StringBuilder sb = new StringBuilder();
		String sufx = "";
		//		if (raw != null && raw.equalsIgnoreCase("GALLAGHER, 4) JAMES")) {
		//			System.out.println();
		//		}
		if (suffixes != null) {
			for (String s : suffixes) {
				sufx = MyStringUtils.regConvert(s);
				if (sufx.endsWith(".")) {
					sufx = sufx + "?";
				} else {
					sufx += "\\.?";
				}
				sb.append("(\\,?\\s*" + s + ")?");
			}
		}
		sb.insert(0, "\\b" + MyStringUtils.regConvert(surname));
		sb.append("\\,\\s*" + MyStringUtils.regConvert(givname) + "\\b");
		String p1 = "\\b" + MyStringUtils.regConvert(surname) + sufx + "\\,\\s*" + MyStringUtils.regConvert(givname) + "\\b";
		if (givname.length() == 1) { // allow M. to MING-XING
			sb.append("(\\w|-)*\\.?");
		}
		if (midname != null) {
			if (midname instanceof String) {
				String mid = (String) midname;
				sb.append("(\\s*" + MyStringUtils.regConvert(mid));
				if (mid.length() == 1) {
					sb.append("(\\w|-)*\\.?"); // allow M. to MING-XING
				}
				sb.append(")?");
			} else {
				@SuppressWarnings("unchecked")
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					sb.append("(\\s*" + MyStringUtils.regConvert(mid));
					if (mid.length() == 1) {
						sb.append("(\\w|-)*\\.?");
					}
					sb.append(")?");
				}
			}
		}

		String r = "";
		String r1 = MyStringUtils.regConvert(givname.substring(0, 1)) + "(\\.|" + MyStringUtils.regConvert(givname.substring(1)) + ")?\\s*"; // givname surname, g. surname
		if (midname == null) {
			String r2 = MyStringUtils.regConvert(givname) + "(\\s\\w+|\\w\\.)"; // givname unknownmidname surname
			String r3 = MyStringUtils.regConvert(givname) + "\\s(\\w\\.\\s*){0,2}"; // givname unknown_midinitial_1. unknown_midinitial_2. surname
			r = "\\b(" + r1 + "|" + r2 + "|" + r3 + ")\\s*";
		} else {
			if (midname instanceof String) {
				String mid = (String) midname;
				if (mid.length() == 1) {
					String r4 = MyStringUtils.regConvert(givname) + "\\s+" + MyStringUtils.regConvert(mid) + "((\\.|(\\w|-)+)\\s*|\\s+)"; // givname m. surname
					String r5 = MyStringUtils.regConvert(givname.substring(0, 1)) + "\\.\\s*" + MyStringUtils.regConvert(mid) + "\\.\\s*"; // g. m. surname
					String r6 = MyStringUtils.regConvert(givname.substring(0, 1) + mid) + "\\s+"; // gm surname
					r = "\\b(" + r1 + "|" + r4 + "|" + r5 + "|" + r6 + ")";
				} else {
					String giv = givname;
					if (givname.length() == 1) {
						giv = MyStringUtils.regConvert(giv) + "\\.?";
					}
					String r3 = MyStringUtils.regConvert(giv) + "\\s+" + MyStringUtils.regConvert(mid) + "\\s+"; // givname midname surname
					String r4 = MyStringUtils.regConvert(giv) + "\\s+" + MyStringUtils.regConvert(mid.substring(0, 1)) + "(\\.\\s*|\\s+)"; // givname m. surname
					String r5 = MyStringUtils.regConvert(givname.substring(0, 1)) + "\\.\\s*" + MyStringUtils.regConvert(mid.substring(0, 1)) + "\\.\\s*"; // g. m. surname
					String r6 = MyStringUtils.regConvert(givname.substring(0, 1) + mid.substring(0, 1)) + "\\s+"; // gm surname
					r = "\\b(" + r1 + "|" + r3 + "|" + r4 + "|" + r5 + "|" + r6 + ")";
				}
			} else {
				r = "\\b" + MyStringUtils.regConvert(givname) + "\\s+";
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					r += MyStringUtils.regConvert(mid);
					if (mid.length() == 1) {
						r += "(\\.|(\\w|-)+)?";
					}
				}
			}
		}
		r += "\\b" + MyStringUtils.regConvert(surname) + "S?\\b" + sufx;// the S? is added to accommodate "John Smiths opposition" 
		String regex = p1 + "|" + r;
		if (givname.length() == 1 && midname != null && (midname instanceof String)) {
			// "A. JAMES ROBERTSON, II" can also appear as "JAMES A. ROBERTSON, II", don't know why.
			String mid = (String) midname;
			if (mid.length() > 1) {
				PersonName pp = new PersonName();
				pp.surname = surname;
				pp.givname = mid;
				pp.midname = givname;
				if (suffixes != null) {
					for (String sx : suffixes) {
						pp.addSuffix(sx);
					}
				}
				String rgx = pp.getMediumRegex();
				regex = regex + "|" + rgx;
			}
		}
		return regex;
	}

	public String getMediumRegex1() {
		String sufx = "";
		if (suffixes != null) {
			for (String s : suffixes) {
				if (s.endsWith(".")) {
					s = s.substring(0, s.length() - 1) + "\\.?";
				} else {
					s += "\\.?";
				}
				String r = "(\\,?\\s*" + s + ")?";
				sufx += r;
			}
		}
		String p1 = "\\b" + surname + sufx + "\\,\\s*" + givname + "\\b";
		if (givname.length() == 1) { // allow M. to MING-XING
			p1 += "(\\w|-)*\\.?";
		}
		if (midname != null) {
			if (midname instanceof String) {
				String mid = (String) midname;
				p1 += "(\\s*" + mid;
				if (mid.length() == 1) {
					p1 += "(\\w|-)*\\.?"; // allow M. to MING-XING
				}
				p1 += ")?";
			} else {
				@SuppressWarnings("unchecked")
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					p1 += "(\\s*" + mid;
					if (mid.length() == 1) {
						p1 += "(\\w|-)*\\.?";
					}
					p1 += ")?";
				}
			}
		}

		String r = "";
		String r1 = givname.substring(0, 1) + "(\\.|" + givname.substring(1) + ")?\\s*"; // givname surname, g. surname
		if (midname == null) {
			String r2 = givname + "(\\s\\w+|\\w\\.)"; // givname unknownmidname surname
			String r3 = givname + "\\s(\\w\\.\\s*){0,2}"; // givname unknown_midinitial_1. unknown_midinitial_2. surname
			r = "\\b(" + r1 + "|" + r2 + "|" + r3 + ")\\s*";
		} else {
			if (midname instanceof String) {
				String mid = (String) midname;
				if (mid.length() == 1) {
					String r4 = givname + "\\s+" + mid + "((\\.|(\\w|-)+)\\s*|\\s+)"; // givname m. surname
					String r5 = givname.substring(0, 1) + "\\.\\s*" + mid + "\\.\\s*"; // g. m. surname
					String r6 = givname.substring(0, 1) + mid + "\\s+"; // gm surname
					r = "\\b(" + r1 + "|" + r4 + "|" + r5 + "|" + r6 + ")";
				} else {
					String giv = givname;
					if (givname.length() == 1) {
						giv += "\\.?";
					}
					String r3 = giv + "\\s+" + mid + "\\s+"; // givname midname surname
					String r4 = giv + "\\s+" + mid.substring(0, 1) + "(\\.\\s*|\\s+)"; // givname m. surname
					String r5 = givname.substring(0, 1) + "\\.\\s*" + mid.substring(0, 1) + "\\.\\s*"; // g. m. surname
					String r6 = givname.substring(0, 1) + mid.substring(0, 1) + "\\s+"; // gm surname
					r = "\\b(" + r1 + "|" + r3 + "|" + r4 + "|" + r5 + "|" + r6 + ")";
				}
			} else {
				r = "\\b" + givname + "\\s+";
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					r += mid;
					if (mid.length() == 1) {
						r += "(\\.|(\\w|-)+)?";
					}
				}
			}
		}
		r += "\\b" + surname + "S?\\b" + sufx;// the S? is added to accommodate "John Smiths opposition" 
		String regex = p1 + "|" + r;
		if (givname.length() == 1 && midname != null && (midname instanceof String)) {
			// "A. JAMES ROBERTSON, II" can appear as "JAMES A. ROBERTSON, II" also. Don't know why.
			String mid = (String) midname;
			if (mid.length() > 1) {
				PersonName pp = new PersonName();
				pp.surname = surname;
				pp.givname = mid;
				pp.midname = givname;
				if (suffixes != null) {
					for (String sx : suffixes) {
						pp.addSuffix(sx);
					}
				}
				String rgx = pp.getMediumRegex();
				regex = regex + "|" + rgx;
			}
		}
		return regex;
	}

	public String getMediumRegexOld() {
		String sufx = "";
		if (suffixes != null) {
			for (String s : suffixes) {
				if (s.endsWith(".")) {
					s = s.substring(0, s.length() - 1) + "\\.?";
				} else {
					s += "\\.?";
				}
				String r = "(\\,?\\s*" + s + ")?";
				sufx += r;
			}
		}
		String p1 = "\\b" + surname + sufx + "\\,\\s*" + givname + "\\b";
		if (givname.length() == 1) { // allow M. to MING-XING
			p1 += "(\\w|-)*\\.?";
		}
		if (midname != null) {
			if (midname instanceof String) {
				String mid = (String) midname;
				p1 += "(\\s*" + mid;
				if (mid.length() == 1) {
					p1 += "(\\w|-)*\\.?"; // allow M. to MING-XING
				}
				p1 += ")?";
			} else {
				@SuppressWarnings("unchecked")
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					p1 += "(\\s*" + mid;
					if (mid.length() == 1) {
						p1 += "(\\w|-)*\\.?";
					}
					p1 += ")?";
				}
			}
		}

		String p2 = "\\b" + givname.substring(0, 1);
		if (givname.length() == 1) {
			p2 += "(\\.|(\\w|-)+)?";
		} else {
			p2 += "(\\.|" + givname.substring(1) + ")?";
		}
		if (midname != null) {
			if (midname instanceof String) {
				String mid = (String) midname;
				p2 += "(\\s*" + mid;
				if (mid.length() == 1) {// mid initial may be a full middle name
					p2 += "(\\.|(\\w|-)+)?";
				}
				p2 += ")?";
			} else {
				@SuppressWarnings("unchecked")
				List<String> midlist = (List<String>) midname;
				for (String mid : midlist) {
					p2 += "(\\s*" + mid;
					if (mid.length() == 1) {
						p2 += "(\\.|(\\w|-)+)?";
					}
					p2 += ")?";
				}
			}
		} else { // when no midname is available, matching both givname and surname are considered a match
			p2 += "(\\s*\\w+\\.?){0,2}";
		}
		p2 += "\\s*" + "\\b" + surname + "\\b" + sufx;
		String regex = p1 + "|" + p2;
		if (givname.length() == 1 && midname != null && (midname instanceof String)) {
			// "A. JAMES ROBERTSON, II" can appear as "JAMES A. ROBERTSON, II" also. Don't know why.
			String mid = (String) midname;
			if (mid.length() > 1) {
				PersonName pp = new PersonName();
				pp.surname = surname;
				pp.givname = mid;
				pp.midname = givname;
				if (suffixes != null) {
					for (String sx : suffixes) {
						pp.addSuffix(sx);
					}
				}
				String rgx = pp.getMediumRegex();
				regex = regex + "|" + rgx;
			}
		}
		return regex;
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
			if (midname instanceof String) {
				sb.append(" " + midname);
			} else {
				List<String> mlist = (List<String>) midname;
				for (String mid : mlist) {
					sb.append(" " + mid);
				}
			}
		}
		sb.append(" " + surname);
		if (suffixes != null) {
			for (String sufx : suffixes) {
				sb.append(", " + sufx);
			}
		}
		return sb.toString();
	}

	public String toString() {
		return normalName();
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

	public PersonName() {

	}

	public PersonName(String name) {
		raw = name;
		name = name.replaceAll("\\,\\,", ",");
		int idx = name.indexOf("(");
		if (idx > 0) {
			name = name.substring(0, idx);
		}
		idx = name.indexOf("**");
		if (idx > 0) {
			name = name.substring(0, idx);
		}
		name = name.replaceAll("\\*", "");
		String[] splitAKA = name.split(" A/?K/?A/? | ALSO KNOWN AS ");
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

	public static PersonName parse(String name, int flag) {
		String[] items = name.split("\\s+");
		PersonName pn = new PersonName();
		pn.raw = name;
		if (flag == GivMidSur) {
			pn.givname = items[0].replace('.', ' ').trim();
			int length = items.length;
			for (; length > 1; length--) {
				String last = items[length - 1].replaceAll("\\p{Punct}", "");
				if (suffixNames.contains(last)) {
					pn.addSuffix(last);
				} else {
					break;
				}
			}
			if (length == 3) {
				pn.midname = items[1].replaceAll("\\p{Punct}", "").trim();
				pn.surname = items[2].replaceAll("\\p{Punct}", "").trim();
			} else if (items.length == 2) {
				pn.surname = items[1].replaceAll("\\p{Punct}", "").trim();
			} else if (length > 3) {
				pn.surname = items[length - 1];
				List<String> mlist = new ArrayList<>();
				for (int i = 1; i < length - 1; i++) {
					String mid = items[i].replaceAll("\\p{Punct}", "").trim();
					mlist.add(mid);
				}
				pn.midname = mlist;
			}
		} else {
			pn = new PersonName(name);
		}
		return pn;
	}

	@SuppressWarnings("unchecked")
	public boolean samePerson(PersonName pn2) {
		if (!surname.equals(pn2.surname)) {
			return false;
		}
		if (givname != null && pn2.givname != null) {
			if (givname.length() == 1 || pn2.givname.length() == 1) {
				if (givname.charAt(0) != pn2.givname.charAt(0))
					return false;
			}
			if (givname.length() > 1 || pn2.givname.length() > 1) {
				if (!givname.equalsIgnoreCase(pn2.givname))
					return false;
			}
		}
		if (midname != null && pn2.midname != null) {
			if (midname instanceof String) {
				if (!(pn2.midname instanceof String)) {
					return false;
				}
				String mid1 = (String) midname;
				String mid2 = (String) (pn2.midname);
				if (mid1.length() == 1 || mid2.length() == 1) {
					if (mid1.charAt(0) != mid2.charAt(0))
						return false;
				}
				if (mid1.length() > 1 || mid2.length() > 1)
					if (!mid1.equalsIgnoreCase(mid2))
						return false;
			} else {
				if (!(pn2.midname instanceof List)) {
					return false;
				}
				List<String> mlist1 = (List<String>) midname;
				List<String> mlist2 = (List<String>) (pn2.midname);
				if (mlist1.size() != mlist2.size()) {
					return false;
				}
				for (int i = 0; i < mlist1.size(); i++) {
					String mid1 = mlist1.get(i);
					String mid2 = mlist2.get(i);
					if (mid1.length() == 1 || mid2.length() == 1) {
						if (mid1.charAt(0) != mid2.charAt(0))
							return false;
					}
					if ((mid1.length() > 1 || mid2.length() > 1) && !mid1.equalsIgnoreCase(mid2))
						return false;
				}
			}
		}
		List<Object> listXor = Utils.listXOR(suffixes, pn2.suffixes);
		for (Object o : listXor) {
			String s = (String) o;
			if ("JR".equalsIgnoreCase(s)) {
				return false;
			}
			if ("II".equalsIgnoreCase(s)) {
				return false;
			}
			if ("III".equalsIgnoreCase(s)) {
				return false;
			}
			if ("IV".equalsIgnoreCase(s)) {
				return false;
			}
			if ("SR".equalsIgnoreCase(s)) {
				return false;
			}
		}
		return true;
	}

	void addSuffix(String s) {
		if (suffixes == null) {
			suffixes = new ArrayList<>();
		}
		if (!suffixes.contains(s)) {
			suffixes.add(s);
		}
	}

	void combine(PersonName _p) {
		if (givname == null) {
			if (_p.givname != null)
				givname = _p.givname;
		} else {
			if (givname.length() == 1 && _p.givname != null && _p.givname.length() > 1) {
				givname = _p.givname; // replace first initial with full first name;
			}
		}
		if (midname == null) {
			if (_p.midname != null)
				midname = _p.midname;
		} else {
			if (midname instanceof String && _p.midname instanceof String) {
				String mid1 = (String) midname;
				String mid2 = (String) (_p.midname);
				if (mid1.length() == 1 && _p.midname != null && mid2.length() > 1) {
					midname = _p.midname; // replace mid initial with full mid name;
				}
			}
		}
	}

	@Override
	public int compareTo(PersonName o) {
		String s1 = this.normalName();
		String s2 = o.normalName();
		return s1.compareTo(s2);
	}
}
