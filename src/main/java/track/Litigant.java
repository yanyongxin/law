package track;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Litigant {
	private static final Logger log = LoggerFactory.getLogger(Litigant.class);
	String nameText;
	String listUnder;
	String regex;
	Pattern ptn;
	String dictText;
	String ontoText;
	String type;
	String entityName;

	public Litigant(String s, String tp) throws Exception {
		nameText = s;
		type = tp;
		String[] reglist;
		if (type.equals("OrgCo")) {
			reglist = EntType.partyNameToRegex(s, 0);
		} else if (type.equals("Lawfirm")) {
			reglist = EntType.LawFirmNameToRegex(s);
		} else if (type.equals("Judge")) {
//			reglist = EntType.judgeNameToRegex(s);
			reglist = EntType.parsePersonName(s, true);
		} else {
//			reglist = EntType.personNameToRegex(s);
			reglist = EntType.parsePersonName(s, false);
		}
		listUnder = reglist[0];
		regex = reglist[1];
		try {
			ptn = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		} catch (Exception ex) {
			log.error("regex error : " + regex);
		}
		entityName = type + LexToken.getSingleName(s);
		ontoText = getOntoText();// must be the last one
	}

	public String getDictText() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"text\":\"");
		sb.append(regex);
		sb.append("\",\"type\":\"NP\",\"list\":\"");
		sb.append(listUnder);
		sb.append("\",\"entity\":\"");
		sb.append(entityName);
		sb.append("\"}\"");
		return sb.toString();
	}

	public String getOntoText() {
		StringBuilder sb = new StringBuilder();
		sb.setLength(0);
		sb.append(entityName);
		sb.append("\tinstanceOf\t");
		sb.append(type);
		if (type.equals("Attorney") || type.equals("OtherPerson")) {
			sb.append("\n");
			sb.append(entityName);
			sb.append("\tinstanceOf\tHumanName");
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(nameText);
		sb.append("\n");
		sb.append(getDictText());
		sb.append("\n");
		sb.append(getOntoText());
		return sb.toString();
	}
}
