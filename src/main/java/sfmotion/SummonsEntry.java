package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//
public class SummonsEntry {
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

	public SummonsEntry() {
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

	public static boolean parse(TrackEntry e) {
		if (!e.text.startsWith("SUMMONS")) {
			return false;
		}
		Matcher m = pSummons1.matcher(e.text);
		if (m.find()) {
			e.storeItem("document", "COMPLAINT");
			String filer = m.group("filer");
			if (filer != null) {
				e.storeItem("filer", filer);
			}
			String receiver = m.group("receiver");
			if (receiver != null) {
				e.storeItem("receiver", receiver);
			}
			String serveDate = m.group("serveDate");
			if (serveDate != null) {
				e.storeItem("serveDate", serveDate);
			}
			String transactionID = m.group("transactionID");
			if (transactionID != null) {
				e.storeItem("transactionID", serveDate);
			}
			String prejudgmentClaim = m.group("prejudgmentClaim");
			if (prejudgmentClaim != null) {
				e.storeItem("prejudgmentClaim", prejudgmentClaim);
			}
			String pos = m.group("pos");
			if (pos != null) {
				e.storeItem("pos", pos);
			}
			String serviceMethod = m.group("serviceMethod");
			if (serviceMethod != null) {
				e.storeItem("serviceMethod", serviceMethod);
			}
			e.setType(TrackEntry.SUMMONS);
			return true;
		} else {
			m = pSummons2.matcher(e.text);
			if (m.find()) {
				String document = m.group("document");
				if (document != null) {
					e.storeItem("document", document);
				}
				String receiver = m.group("receiver");
				if (receiver != null) {
					e.storeItem("receiver", receiver);
				}
				e.setType(TrackEntry.SUMMONS);
				return true;
			} else {
				String[] splits = e.text.split(sreg);
				boolean bready = false;
				if (splits.length > 1)
					bready = true;
				for (String p : splits) {
					p = p.trim();
					if (p.startsWith("SUMMONS")) {// SUMMONS ON COMPLAINT,
						int id = p.indexOf(" ON ");
						if (id > 0) {
							e.storeItem("document", p.substring(id + 4));
						}
					} else if (p.startsWith("PROOF OF SERVICE")) {//PROOF OF SERVICE ONLY,
						e.storeItem("pos", p);
					} else if (p.startsWith("PERSONAL") || p.startsWith("SUBSTITUTE") || p.startsWith("MAIL") || p.startsWith("POSTING")) {//SERVED OCT-31-2016, 
						e.storeItem("serviceMethod", p);
					} else if (p.startsWith("SERVED")) {
						e.storeItem("serveDate", p);
					} else if (p.startsWith("[ORIGINALLY")) {//[ORIGINALLY FILED NOV-02-16] 
						e.storeItem("originalFile", p);
					} else if (p.startsWith("FILED BY")) {//FILED BY PLAINTIFF WILSON, MICHAEL GEARY 
						e.storeItem("filer", p.substring(1 + "FILED BY".length()).trim());
					} else if (p.startsWith("AS TO")) {// DEFENDANT HAYNES, BRIAN
						e.storeItem("receiver", p.substring(5).trim());
					} else if (p.startsWith("(TRANSACT")) {//(TRANSACTION ID # 60107832),
						e.storeItem("transactionID", p);
					} else if (p.startsWith("TO ")) {
						e.storeItem("receiver", p.substring(3).trim());
					} else {
						e.storeItem("noUse", p);
					}
				}
				if (bready) {
					e.setType(TrackEntry.SUMMONS);
					return true;
				}
			}
		}
		return false;
	}

	public String toString() {
		return "";
	}

}
