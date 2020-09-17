package sfmotion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class DeclarationEntry {
	static final String left = "(?=\\(TRANSACTION\\sID\\s.{6,40}?\\)|\\(SEALED DOCUMENT\\)|FILED\\s+BY)";
	static final String right = "(?<=\\(TRANSACTION\\sID\\s.{6,40}?\\)|\\(SEALED DOCUMENT\\))";
	static final String breaks = left + "|" + right;
	static final String left2 = "";
	static final String headRight = "(?<=DECLARATION( OF)?+)";
	static final String regRelLeftBreak = "(?=(IN SUPPORT|\\bISO\\b|IN OPPOSITION|(?<=DECLARATION )FOR\\b|REGARDING|\\bRE\\b\\:?+|IN RESPONSE TO|PURSUANT TO))";
	static final String regRelRightBreak = "(?<=(IN SUPPORT \\w+|\\bISO\\b|IN OPPOSITION TO|^DECLARATION FOR\\b|REGARDING|\\bRE\\b\\:?+|IN RESPONSE TO|PURSUANT TO|^DECLARATION (OF)?+))";
	static final String regRelations = "IN SUPPORT \\w+|\\bISO\\b|IN OPPOSITION TO|\\bFOR\\b|REGARDING|\\bRE\\b\\:?+|IN RESPONSE TO|PURSUANT TO";
	static final String regSplit1 = headRight + "|" + regRelRightBreak + "|" + regRelLeftBreak;
	static final String head = "DECLARATION\\s";
	static final String headOf = "(DECLARATION( OF)?+\\s)+";
	static final String author1 = "(?<author1>.+?\\s)";
	static final String author2 = "(?<author2>(DEFENDANT\\S*|PLAINTIF\\S*|RESPONDENT\\S*|.+?S\\'?)\\s)";
	static final String anything = "(?<anything>.+?\\s)";
	static final String anythingEnd = "(?<anything>.+?)$";
	static final String headRight2 = "(?<=" + head + ")";
	static final String regDocs = "(?<doc>((AMENDED|UNOPPOSED|STIPULATED|EX PART\\S*|VERIFIED|SPECIAL)\\s)?(COMPLIANCE|MEMO\\b|MOTION|MEET AND CONFER|ORDER|DECLARATION|DEFAULT|AMENDMENT|PRELIMINARY INJUNCTION|NOTICE|NEW TRIAL|DEMU*RRER|MIL\\b|MTN|MSJ|(?<!IN\\s)OPPOSITION|(?<!IN\\s)RESPONSE|(JOINT )?STIPULATION|PETITIONS?\\b|REPLY|REQUEST|OBJECTION|EX\\sPARTE\\sAPP(LICATION)?|(?<!EX\\sPARTE\\s)APP(LICATION)?\\b))";
	static final String regDocLeftBreak = "(?=" + regDocs + ")";
	static final String regRelLeftBreak2 = "(?=(IN SUPPORT|\\bISO\\b|IN OPPOSITION|(?<=DECLARATION )FOR\\b|REGARDING|\\bRE\\b\\:?+|IN RESPONSE TO|PURSUANT TO))";
	static final String regRelRightBreak2 = "(?<=(IN SUPPORT \\w+|\\bISO\\b|IN OPPOSITION TO|^DECLARATION FOR\\b|REGARDING|\\bRE\\b\\:?+|IN RESPONSE TO|PURSUANT TO|^DECLARATION (OF)?+))";
	static final String regRelations2 = "(?<relation>IN SUPPORT \\w+|\\bISO\\b|IN OPPOSITION TO|IN OPPOS\\.? TO|\\bFOR\\b|REGARDING|\\bRE\\b\\:?+|IN RESPONSE TO|PURSUANT TO)\\s+";
	static final String regSplit2 = headRight2 + "|" + regRelRightBreak2 + "|" + regRelLeftBreak2;
	static final String reg5_1 = headOf + author1 + regRelations2 + author2 + regDocs;
	static final Pattern p5_1 = Pattern.compile(reg5_1, Pattern.CASE_INSENSITIVE);
	static final String reg5_2 = headOf + author1 + regRelations2 + anything + regDocs;
	static final Pattern p5_2 = Pattern.compile(reg5_2, Pattern.CASE_INSENSITIVE);
	static final String reg4_1 = headOf + author1 + regRelations2 + regDocs;
	static final Pattern p4_1 = Pattern.compile(reg4_1, Pattern.CASE_INSENSITIVE);
	static final String reg4_2 = head + regRelations2 + author2 + regDocs;
	static final Pattern p4_2 = Pattern.compile(reg4_2, Pattern.CASE_INSENSITIVE);
	static final String reg4_3 = head + regRelations2 + anything + regDocs;
	static final Pattern p4_3 = Pattern.compile(reg4_3, Pattern.CASE_INSENSITIVE);
	static final String reg3 = head + regRelations2 + regDocs;
	static final Pattern p3 = Pattern.compile(reg3, Pattern.CASE_INSENSITIVE);
	static final String reg2 = headOf + anythingEnd;
	static final Pattern p2 = Pattern.compile(reg2, Pattern.CASE_INSENSITIVE);
	static final String reg4_4 = headOf + author1 + regRelations2 + anythingEnd;
	static final Pattern p4_4 = Pattern.compile(reg4_4, Pattern.CASE_INSENSITIVE);
	static final Pattern pDocs = Pattern.compile(regDocs, Pattern.CASE_INSENSITIVE);
	static final Pattern pRelation = Pattern.compile(regRelations2, Pattern.CASE_INSENSITIVE);

	String raw;
	Map<String, String> items = new TreeMap<>();
	List<String> splits = new ArrayList<>();
	List<String> useless = new ArrayList<>();
	SFMotionEntry entry;

	public DeclarationEntry() {
	}

	public static boolean parse(TrackEntry e) {
		if (!e.text.startsWith("DECLARATION")) {
			return false;
		}
		DeclarationEntry entry = new DeclarationEntry();
		String[] split1 = e.text.split(breaks);
		String stem = null;
		for (String p : split1) {
			p = p.trim();
			if (p.length() == 0)
				continue;
			if (p.startsWith("(TRANSACTION")) {
				entry.items.put("transactionID", p);
				continue;
			} else if (p.startsWith("FILED BY")) {
				String pp = p.substring("FILED BY".length()).trim();
				entry.items.put("filer", pp);
				continue;
			} else if (p.startsWith("(SEAL")) {
				entry.items.put("sealed", p);
				continue;
			} else {
				entry.items.put("stem", p);
				stem = p;
			}
		}

		if (stem != null) {
			if (!entry.mp3(stem, p3)) {
				if (!entry.mp5_1(stem, p5_1)) {
					if (!entry.mp4_1(stem, p4_1)) {
						if (!entry.mp4_2(stem, p4_2)) {
							if (!entry.mp5_2(stem, p5_2)) {
								if (!entry.mp4_3(stem, p4_3)) {
									if (!entry.mp4_4(stem, p4_4)) {
										entry.mp2(stem, p2);
									}
								}
							}
						}
					}
				}
			}
		}
		e.setType(TrackEntry.DECLARATION);
		e.setTypeSpecific(entry);
		return true;
	}

	// Order of use: p3, p5_1, p4_1, p4_2, p5_2, p4_3, p2

	boolean mp3(String s, Pattern pt) {
		Matcher m = pt.matcher(s);
		if (m.find()) {
			String relation = m.group("relation");
			String doc = m.group("doc");
			int docStart = m.start("doc");
			if (docStart > 0) {
				doc = s.substring(docStart);
			}
			items.put("relation", relation);
			items.put("doc", doc);
			return true;
		}
		return false;
	}

	boolean mp4_1(String s, Pattern pt) {
		//reg4_1 = headOf + author1 + regRelations + regDocs;
		Matcher m = pt.matcher(s);
		if (m.find()) {
			String author1 = m.group("author1");
			/*
			 * Error can occur:
			 * DECLARATION OF ANTHONY K. LEE IN SUPPORT OF PLAINTIFFS MOTION FOR PRELIMINARY INJUNCTION **[REDACTED]**
			==>	author1: ANTHONY K. LEE IN SUPPORT OF PLAINTIFFS MOTION 
				relation: FOR
				document: PRELIMINARY INJUNCTION **[REDACTED]**
			 */
			Matcher mm = pRelation.matcher(author1);
			if (mm.find()) {
				return false;// let mp5_1 handle it
			}
			String relation = m.group("relation");
			String doc = m.group("doc");
			int docStart = m.start("doc");
			if (docStart > 0) {
				doc = s.substring(docStart);
			}
			items.put("author1", author1);
			items.put("relation", relation);
			items.put("doc", doc);
			return true;
		}
		return false;
	}

	boolean mp5_1(String s, Pattern pt) {
		//reg5_1 = headOf + author1 + regRelations + author2 + regDocs;
		Matcher m = pt.matcher(s);
		if (m.find()) {
			String author1 = m.group("author1");
			String author2 = m.group("author2");
			Matcher mm = pDocs.matcher(author2);
			if (mm.find()) {
				/*
				 * Error can occur:
				 * 		DECLARATION OF JULIE L. FIEBER IN SUPPORT OF OPPOSITION TO DEFENDANTS DEMURRER TO FOURTH AMENDED COMPLAINT
						author1: JULIE L. FIEBER 
						relation: IN SUPPORT OF
				==>		author2: OPPOSITION TO DEFENDANTS 
						document: DEMURRER TO FOURTH AMENDED COMPLAINT
				*/
				return false;// let mp4_1 handle it.
			}
			String relation = m.group("relation");
			String doc = m.group("doc");
			int docStart = m.start("doc");
			if (docStart > 0) {
				doc = s.substring(docStart);
			}
			items.put("author1", author1);
			items.put("relation", relation);
			items.put("author2", author2);
			items.put("doc", doc);
			return true;
		}
		return false;
	}

	boolean mp4_2(String s, Pattern pt) {
		// head + regRelations + author2 + regDocs;
		Matcher m = pt.matcher(s);
		if (m.find()) {
			String author2 = m.group("author2");
			String relation = m.group("relation");
			String doc = m.group("doc");
			int docStart = m.start("doc");
			if (docStart > 0) {
				doc = s.substring(docStart);
			}
			items.put("relation", relation);
			items.put("author2", author2);
			items.put("doc", doc);
			return true;
		}
		return false;
	}

	boolean mp5_2(String s, Pattern pt) {
		// reg5_2 = headOf + author1 + regRelations + anything + regDocs;
		Matcher m = pt.matcher(s);
		if (m.find()) {
			String author1 = m.group("author1");
			String anything = m.group("anything");
			String relation = m.group("relation");
			String doc = m.group("doc");
			int docStart = m.start("doc");
			if (docStart > 0) {
				doc = s.substring(docStart);
			}
			items.put("author1", author1);
			items.put("relation", relation);
			items.put("anything", anything);
			items.put("doc", doc);
			return true;
		}
		return false;
	}

	boolean mp4_3(String s, Pattern pt) {
		// reg4_3 = head + regRelations + anything + regDocs;
		Matcher m = pt.matcher(s);
		if (m.find()) {
			String author2 = m.group("anything");
			String relation = m.group("relation");
			String doc = m.group("doc");
			int docStart = m.start("doc");
			if (docStart > 0) {
				doc = s.substring(docStart);
			}
			items.put("relation", relation);
			items.put("author2", author2);
			items.put("doc", doc);
			return true;
		}
		return false;
	}

	boolean mp4_4(String s, Pattern pt) {
		// reg5_2 = headOf + author1 + regRelations + anything + regDocs;
		Matcher m = pt.matcher(s);
		if (m.find()) {
			String author1 = m.group("author1");
			String anything = m.group("anything");
			String relation = m.group("relation");
			items.put("author1", author1);
			items.put("relation", relation);
			items.put("anything", anything);
			return true;
		}
		return false;
	}

	boolean mp2(String s, Pattern pt) {
		// reg2 = headOf + anything;
		Matcher m = pt.matcher(s);
		if (m.find()) {
			String anything = m.group("anything");
			items.put("anything", anything);
			return true;
		}
		return false;
	}

}
