package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//
public class SummonsEntry extends TrackEntry {
	//SUMMONS ON COMPLAINT (TRANSACTION ID # 60057326), PROOF OF SERVICE ONLY, FILED BY PLAINTIFF BARRIENTOS, CLAUDIA CRUZ SERVED JAN-06-2017, PERSONAL SERVICE AS TO DEFENDANT YOUNG, LILLIE PEARL
	//SUMMONS ON COMPLAINT FILED BY PLAINTIFF STRONG, PAUL SERVED MAR-14-2017, POSTING AND MAILING AS TO DEFENDANT TAYLOR, DAVID
	static final String regSummons1 = "^SUMMONS\\s*ON\\s*COMPLAINT\\,?\\s*(?<transactionID>\\(TRANSACTION\\s*ID\\s*\\#\\s*\\d+\\))?\\,?\\s*(?<prejudgmentClaim>PREJUDGMENT\\sCLAIM\\sOF\\sRIGHT\\sOF\\sPOSSESSION\\,?\\s?)?(?<pos>PROOF\\s*OF\\s*SERVICE\\s*(ONLY)?)?.+?FILED\\s*BY\\s*(?<filer>.+?)"
			+ "\\s(SERVED\\s*(ON\\s)?(?<serveDate>\\w{3,10}-\\d\\d-\\d{2,4})\\,\\s*)" + "(?<serviceMethod>.+?\\s*)AS\\s*TO\\s(?<receiver>.+?)$";
	static final Pattern pSummons1 = Pattern.compile(regSummons1, Pattern.CASE_INSENSITIVE);
	static final String regSummons2 = "^SUMMONS\\s*ISSUED\\s*(ON\\s*(?<document>.+?))?TO(?<receiver>.+?)$";
	static final String sreg = "\\;|\\s/\\s|(?=(PROOF\\s+OF\\s+SERVICE|\\(Fee.{3,10}?\\)|\\[ORIGINALLY\\sFILED|\\(TRANSACTION\\sID\\s.{6,20}?\\)|SERVED\\s(ON\\s+)?\\w{3,10}-\\d\\d-\\d{2,4}|(PERSONAL|SUBSTITUTE)\\s*SERVICE|\\bAS\\sTO\\b|(?<!AS\\s)TO\\sDEFENDANT|PREJUDGMENT\\sCLAIM|MAIL\\sAND\\sACK|FILED\\s+BY))";

	static final Pattern pSummons2 = Pattern.compile(regSummons2, Pattern.CASE_INSENSITIVE);
	static final String testStrings[] = {
			"SUMMONS ON COMPLAINT, PROOF OF SERVICE ONLY, SERVED NOV-01-16, PERSONAL SERVICE [ORIGINALLY FILED NOV-04-16] FILED BY PLAINTIFF WILSON, MICHAEL GEARY AS TO DEFENDANT DOUGLAS, DANIELLE",
			"SUMMONS ON COMPLAINT, PROOF OF SERVICE ONLY, SERVED OCT-31-2016, PERSONAL SERVICE [ORIGINALLY FILED NOV-02-16] FILED BY PLAINTIFF WILSON, MICHAEL GEARY AS TO DEFENDANT HAYNES, BRIAN",
			"SUMMONS ON COMPLAINT (TRANSACTION ID # 60107832), PROOF OF SERVICE ONLY, FILED BY PLAINTIFF BUTTERY, PAMELA TRUSTEE OF THE PAMELA BUTTERY 1990 TRUST PRETLOW, PAULA B. TRUSTEE OF THE PAULA B. PRETLOW TRUST MAHLBUBANI, VINITI NARAIN GENG, HELENA THE HELENA H. GENG LIVING TRUST FOX, JOANNE SAAL, JEFFREY A. TRUSTEE OF THE SAAL REVOCABLE LIVING TRUST SAAL, JEANNETTE C. TRUSTEE OF THE SAAL REVOCABLE LIVING TRUST MACDONALD, ELAINE LUM CAMP, EVA LUM LUM JR, JACKSON LUM, EVONNE AGABIAN, NINA COLELLA, GIOVANNI COLELLA, VANESSA JERNIGAN, FRANK H. TRUSTEE OF THE FRANK H. JERNIGAN FAMILY TRUST GERALD AND PATRICIA DODSON, TTEE LIVING TRUST DATED 2/2/95 FARRELL, CATHERINE STRICKLAND, THERESA STRICKLAND, TYRONE REID, ANDREA D. INDIVIDUALLY AND AS TRUSTEE UNDER THE JAMES H. AND ANDREA D. REID LIVING TRUST FINKELMAN, HERBERT I. TTEE, LIVING TRUST DTD 6/13/96 SPENCER, STIRLING DEMASI, GARY ROSENBERG, JEROLD ROSENBERG, PHYLLIS KIMN, SEUNG RATNER, JOYCE ADLER, JOEL TRUSTEE OF THE ADLER T...",
			"SUMMONS ON COMPLAINT, PROOF OF SERVICE ONLY, SERVED NOV-08-16, PERSONAL SERVICE FILED BY PLAINTIFF WILSON, MICHAEL GEARY AS TO DEFENDANT GOLDSTEIN, DAVID",
			"SUMMONS ON COMPLAINT (TRANSACTION ID # 100009693), PROOF OF SERVICE ONLY, FILED BY PLAINTIFF LEE, ANTHONY K SERVED JAN-05-2017, MAIL AND ACKNOWLEDGEMENT, ACKNOWLEDGMENT DATE JAN-09-2017 AS TO DEFENDANT HOSIE RICE LLP A CALIFORNIA LIMITED LIABILITY PARTNERSHIP",
			"SUMMONS ON COMPLAINT (TRANSACTION ID # 61436346), PROOF OF SERVICE ONLY, FILED BY CROSS COMPLAINANT TRANSBAY JOINT POWERS AUTHORITY SERVED NOV-14-2017, PERSONAL SERVICE AS TO CROSS DEFENDANT KILROY REALTY CORP., A MARYLAND CORPORATION",
			"SUMMONS ON COMPLAINT (TRANSACTION ID # 60057326), PROOF OF SERVICE ONLY, FILED BY PLAINTIFF BARRIENTOS, CLAUDIA CRUZ SERVED JAN-06-2017, PERSONAL SERVICE AS TO DEFENDANT YOUNG, LILLIE PEARL",
			"SUMMONS ISSUED TO PLAINTIFF LEE, ANTHONY K", };

	static String[] testBreaks(String s) {
		return s.split(sreg);
	}

	List<String> noUse;

	public SummonsEntry(String _sdate, String _text, Map<String, Object> _items) {
		super(_sdate, _text, SUMMONS);
		items = _items;
	}

	public SummonsEntry(String _sdate, String _text, Map<String, Object> _items, List<String> _noUse) {
		super(_sdate, _text, SUMMONS);
		items = _items;
		noUse = _noUse;
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("args: infile outfile failfile");
			System.exit(-1);
		}
		String infile = args[0];
		String outfile = args[1];
		BufferedReader br = new BufferedReader(new FileReader(infile));
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(args[2]));
		String line;
		while ((line = br.readLine()) != null) {
			String[] splits = line.split("\\t");
			if (splits.length != 3) {
				continue;
			}
			SummonsEntry se = parse(splits[1], splits[2]);
			if (se != null) {
				wr.write(se.toString() + "\n");
			} else {
				wr2.write(line + "\n");
			}
		}
		br.close();
		wr.close();
		wr2.close();
	}

	static boolean testSummons(String s) {
		Matcher m = pSummons1.matcher(s);
		if (m.find())
			return true;
		m = pSummons2.matcher(s);
		if (m.find()) {
			String document = m.group("document");
			String receiver = m.group("receiver");
			return true;
		}
		return false;
	}

	static SummonsEntry parse(String _sdate, String _text) {
		if (!_text.startsWith("SUMMONS")) {
			return null;
		}
		Map<String, Object> items = new TreeMap<>();
		Matcher m = pSummons1.matcher(_text);
		if (m.find()) {
			String item;
			items.put("document", "COMPLAINT");
			item = m.group("filer");
			if (item != null) {
				items.put("filer", item);
			}
			item = m.group("receiver");
			if (item != null) {
				items.put("receiver", item);
			}
			item = m.group("serveDate");
			if (item != null) {
				items.put("serveDate", item);
			}
			item = m.group("transactionID");
			if (item != null) {
				items.put("transactionID", item);
			}
			item = m.group("prejudgmentClaim");
			if (item != null) {
				items.put("prejudgmentClaim", item);
			}
			item = m.group("pos");
			if (item != null) {
				items.put("pos", item);
			}
			item = m.group("serviceMethod");
			if (item != null) {
				items.put("serviceMethod", item);
			}
			return new SummonsEntry(_sdate, _text, items);
		} else {
			m = pSummons2.matcher(_text);
			if (m.find()) {
				String item = m.group("document");
				if (item != null) {
					items.put("document", item);
				}
				item = m.group("receiver");
				if (item != null) {
					items.put("receiver", item);
				}
				return new SummonsEntry(_sdate, _text, items);
			} else {
				List<String> noUse = new ArrayList<>();
				String[] splits = _text.split(sreg);
				boolean bready = false;
				if (splits.length > 1)
					bready = true;
				for (String p : splits) {
					p = p.trim();
					if (p.startsWith("SUMMONS")) {// SUMMONS ON COMPLAINT,
						int id = p.indexOf(" ON ");
						if (id > 0) {
							items.put("document", p.substring(id + 4));
						}
					} else if (p.startsWith("PROOF OF SERVICE")) {//PROOF OF SERVICE ONLY,
						items.put("pos", p);
					} else if (p.startsWith("PERSONAL") || p.startsWith("SUBSTITUTE") || p.startsWith("MAIL") || p.startsWith("POSTING")) {//SERVED OCT-31-2016, 
						items.put("serviceMethod", p);
					} else if (p.startsWith("SERVED")) {
						items.put("serveDate", p);
					} else if (p.startsWith("[ORIGINALLY")) {//[ORIGINALLY FILED NOV-02-16] 
						items.put("originalFile", p);
					} else if (p.startsWith("FILED BY")) {//FILED BY PLAINTIFF WILSON, MICHAEL GEARY 
						items.put("filer", p.substring(1 + "FILED BY".length()).trim());
					} else if (p.startsWith("AS TO")) {// DEFENDANT HAYNES, BRIAN
						items.put("receiver", p.substring(5).trim());
					} else if (p.startsWith("(TRANSACT")) {//(TRANSACTION ID # 60107832),
						items.put("transactionID", p);
					} else if (p.startsWith("TO ")) {
						items.put("receiver", p.substring(3).trim());
					} else {
						noUse.add(p);
					}
				}
				if (bready) {
					return new SummonsEntry(_sdate, _text, items, noUse);
				}
			}
		}
		return null;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString() + "\n");
		for (String key : items.keySet()) {
			sb.append("\t" + key + ": " + items.get(key) + "\n");
		}
		if (noUse != null) {
			for (String s : noUse) {
				sb.append("\t" + "No Use: " + s + "\n");
			}
		}
		return sb.toString();
	}

}
