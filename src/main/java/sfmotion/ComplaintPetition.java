package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComplaintPetition {
	static final String regPetition = "^((PETITION\\s*)?VERIFIED\\s*)?PETITION\\s*((FOR|TO|RE)\\b\\s*(?<petitionType>.+?))?" + "FILED\\s*BY\\s*(PETITIONER|PLAINTIFF)\\s*(?<petitioner>.+?)"
			+ "(AS\\s*TO\\s*(?<concerned>.+?)\\s*)?" + "(?<summons>SUMMONS\\s*ISSUED\\s*(AND\\s*FILED\\s*)?)?" + "(?<coverSheet>JUDICIAL\\s*COUNCIL\\s*CIVIL\\s*CASE\\s*COVER\\s*SHEET\\s*FILED\\s*)?"
			+ "(HEARING SET FOR\\s*(?<hearingDate>.+?))?" + "\\(Fee\\:(?<fee>.+?)\\)";
	//PETITION FOR INJUNCTION PROHIBITING HARASSMENT FILED BY PLAINTIFF TONG, KONG MIN AS TO DEFENDANT SHEN, CHUN YAN LIANG, JUN JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED (Fee:IFP)";
	//CA_SFC_467065	2017-03-06	PETITION FOR CHANGE OF NAME, RESIDENCY VERIFIED, FILED BY PETITIONER WORCESTER, ALISON HILARY JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED (Fee:450.00)
	//CA_SFC_466907	2017-02-21	PETITION TO CONFIRM ARBITRATION AWARD ****PETITION ORDERED SEALED PER THE MAY 16, 2017 ORDER.**** FILED BY PETITIONER BMC PARTNERS, LLP FORMERLY KNOWN AS BINGHAM MCCUTCHEN, LLP AS TO RESPONDENT DISANTO, MICHAEL D. SUMMONS ISSUED, JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED (Fee:$450.00)

	static final Pattern pPetition = Pattern.compile(regPetition, Pattern.CASE_INSENSITIVE);
	static final String testString = "MALPRACTICE - MEDICAL/DENTAL, COMPLAINT FILED BY PLAINTIFF PEREZ, JASON AN INDIVIDUAL AS TO DEFENDANT SMITH, ERIC P. AN INDIVIDUAL BERMAN SKIN INSTITUTE MEDICAL GROUP, INC. A CORPORATION DOES 1-20 SUMMONS ISSUED, JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED CASE MANAGEMENT CONFERENCE SCHEDULED FOR JUN-21-2017 PROOF OF SERVICE DUE ON MAR-21-2017 CASE MANAGEMENT STATEMENT DUE ON MAY-30-2017 (Fee:$450.00)";
	static final String testString2 = "PETITION FOR WRIT OF MANDATE/ PROHIBITION/ CERTIFICATION FILED BY PETITIONER O'DORISIO MD, JAMES EDWARD AS TO RESPONDENT MEDICAL BOARD OF CALIFORNIA JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED (Fee:450.00)";
	static final String testString3 = "PETITION FOR DISSOLUTION FILED BY PETITIONER STOBBE-BUELOW, BENJAMIN AS TO RESPONDENT STOBBE-BUELOW, MEGHAN SUMMONS ISSUED AND FILED (Fee:$450.00)";
	static final String testString4 = "CA_SFC_466348	2017-01-09	PETITION FOR APPOINTMENT OF GUARDIAN OF ESTATE FILED BY PETITIONER HABERMAN, JENNIFER MICHELLE AS TO MINOR HABERMAN, ALAIA JEAN HEARING SET FOR FEB-14-2017 AT 01:00 PM IN DEPT 204 (Fee:$450.00)";
	static final String testString5 = "PETITION FOR INJUNCTION PROHIBITING HARASSMENT FILED BY PLAINTIFF TONG, KONG MIN AS TO DEFENDANT SHEN, CHUN YAN LIANG, JUN JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED (Fee:IFP)";

	public static void main(String[] args) throws IOException {
		//		testPetition(testString2);
		//		if (!testPetition(testString5)) {
		//			System.exit(1);
		//		}
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
			ComplaintEntry ct = new ComplaintEntry(splits[1], splits[2]);
			if (ct.parse()) {
				wr.write(ct.toString() + "\n");
				continue;
			}
			Petition pt = new Petition(line);
			if (pt.parsePetition(splits[2])) {
				wr.write(pt.toString() + "\n");
				continue;
			}
			wr2.write(line + "\n");
		}
		br.close();
		wr.close();
		wr2.close();
	}

	static boolean test(String s) {
		Matcher m = ComplaintEntry.pComplaint.matcher(s);
		if (m.find()) {
			String caseType = m.group("caseType");
			String plaintiffs = m.group("plaintiffs");
			String defendants = m.group("defendants");
			String summons = m.group("summons");
			String coverSheet = m.group("coverSheet");
			String caseManagementConferenceDate = m.group("caseManagementConferenceDate");
			String posDate = m.group("posDate");
			String caseManageStatementDate = m.group("caseManageStatementDate");
			String fee = m.group("fee");
			System.out.println();
		}
		return false;
	}

	public static boolean testClaims(String s) {
		Matcher m = ComplaintEntry.pComplaint.matcher(s);
		if (m.find())
			return true;
		m = pPetition.matcher(s);
		if (m.find())
			return true;
		return false;
	}

	static boolean testPetition(String s) {
		Petition p = new Petition(s);
		boolean b = p.parsePetition(s);
		System.out.println(p.toString());
		return false;
	}

	static class Petition {
		String raw;
		String petitioner;
		String petitionType;
		String concerned;
		String summons;
		String coverSheet;
		String hearingDate;
		String fee;

		public Petition(String _raw) {
			raw = _raw;
		}

		boolean parsePetition(String s) {
			Matcher m = pPetition.matcher(s);
			if (m.find()) {
				petitionType = m.group("petitionType");
				petitioner = m.group("petitioner");
				concerned = m.group("concerned");
				summons = m.group("summons");
				coverSheet = m.group("coverSheet");
				hearingDate = m.group("hearingDate");
				fee = m.group("fee");
				return true;
			}
			return false;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(raw + "\n");
			sb.append("\t" + "petitionType: " + petitionType + "\n");
			sb.append("\t" + "petitioner: " + petitioner + "\n");
			sb.append("\t" + "concerned: " + concerned + "\n");
			sb.append("\t" + "summons: " + summons + "\n");
			sb.append("\t" + "hearingDate: " + hearingDate + "\n");
			sb.append("\t" + "coverSheetPt: " + coverSheet + "\n");
			sb.append("\t" + "fee: " + fee + "\n");
			return sb.toString();
		}

	}

}
