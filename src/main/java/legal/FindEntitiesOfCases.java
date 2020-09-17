package legal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import legal.LegalCase.CaseNames;
import sfmotion.Party;
import sfmotion.PartyCluster;
import utils.Pair;

public class FindEntitiesOfCases {
	static Map<String, CaseNames> mapParty;

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			//C:\data\191023\dockets\judgeparty/ca_sfc_party.txt C:\data\191023\dockets/motions_3.txt C:\data\191023\dockets\cases\all/d_3.txt 
			System.out.println("args: inPartyFile inDocketEntryFile outCaseEntitiesFile");
			System.exit(-1);
			/*
			 * inPartyFile contain lines:
			case_id	fullname	mst_party_type_name	is_active
			CA_SFC_464143	SAUNDERS, EDGAR V.	Decedent	1
			CA_SFC_464143	SAN FRANCISCO PUBLIC ADMINISTRATOR	Petitioner	1
			CA_SFC_464143	SAN FRANCISCO PUBLIC ADMINISTRATOR	Other	1
			CA_SFC_464154	FUENTES, FRANCISCO	Respondent	1
			CA_SFC_464154	FUENTES, NANCY EVELYN	Petitioner	1
			CA_SFC_464155	FUENTES, FRANCISCO	Respondent	1
			CA_SFC_464155	FUENTES, ISOLINA	Petitioner	1
			CA_SFC_464156	MARTINEZ JR, ROBERT D	Respondent	1
			CA_SFC_464156	MACKEY, KIMBERLY	Petitioner	1
			
			inDocketEntryFile contains docket entry lines:
			CA_SFC_464174	2017-01-03	PERSONAL INJURY/PROPERTY DAMAGE - VEHICLE RELATED, COMPLAINT FILED BY PLAINTIFF BARRIENTOS, CLAUDIA CRUZ AS TO DEFENDANT YOUNG, LILLIE PEARL YOUNG, CHARLIE DOES 1 TO 10 SUMMONS ISSUED, JUDICIAL COUNCIL CIVIL CASE COVER SHEET FILED CASE MANAGEMENT CONFERENCE SCHEDULED FOR JUN-07-2017 PROOF OF SERVICE DUE ON MAR-06-2017 CASE MANAGEMENT STATEMENT DUE ON MAY-15-2017 (Fee:$450.00)
			CA_SFC_464174	2017-01-03	NOTICE TO PLAINTIFF
			CA_SFC_464174	2017-01-11	SUMMONS ON COMPLAINT (TRANSACTION ID # 60057326), PROOF OF SERVICE ONLY, FILED BY PLAINTIFF BARRIENTOS, CLAUDIA CRUZ SERVED JAN-06-2017, PERSONAL SERVICE AS TO DEFENDANT YOUNG, LILLIE PEARL
			CA_SFC_464174	2017-01-11	SUMMONS ON COMPLAINT (TRANSACTION ID # 60057326), PROOF OF SERVICE ONLY, FILED BY PLAINTIFF BARRIENTOS, CLAUDIA CRUZ SERVED JAN-06-2017, PERSONAL SERVICE AS TO DEFENDANT YOUNG, CHARLIE
			CA_SFC_464174	2017-02-02	ANSWER TO COMPLAINT (TRANSACTION ID # 60156623) FILED BY DEFENDANT YOUNG, LILLIE PEARL (Fee:$450.00)
			CA_SFC_464174	2017-02-02	JURY FEES (TRANSACTION ID # 60156623) DEPOSITED BY DEFENDANT YOUNG, LILLIE PEARL (Fee:$150.00)
			CA_SFC_464174	2017-02-21	ANSWER TO COMPLAINT (TRANSACTION ID # 60240135) FILED BY DEFENDANT YOUNG, CHARLIE (Fee:$450.00)
			CA_SFC_464174	2017-05-23	CASE MANAGEMENT STATEMENT (TRANSACTION ID # 60637622) FILED BY PLAINTIFF BARRIENTOS, CLAUDIA CRUZ JURY DEMANDED, ESTIMATED TIME FOR TRIAL: 4.0 DAYS
			
			outCaseEntitiesFile:
			
			============== Global Entities:===================
			
			BARRIENTOS CLAUDIA CRUZ
				(BARRIENTOS, CLAUDIA CRUZ)
					Role: PLAINTIFF, 
					Type: INDIVIDUAL
					Count: 7
			
			CITY AND COUNTY OF SAN FRANCISCO SFGH
					Role: CLAIMANT, 
					Type: UNKNOWN
					Count: 2
			
			DOES 1 TO 10
					Role: DEFENDANT, 
					Type: UNKNOWN
					Count: 1
			
			YOUNG CHARLIE
				(YOUNG, CHARLIE)
					Role: DEFENDANT, 
					Type: INDIVIDUAL
					Count: 4
			
			YOUNG LILLIE PEARL
				(YOUNG, LILLIE PEARL)
					Role: DEFENDANT, 
					Type: INDIVIDUAL
					Count: 4
			
			============================= Party Clusters: ===============================
			
			BARRIENTOS, CLAUDIA CRUZ
			
				BARRIENTOS CLAUDIA CRUZ
				(BARRIENTOS, CLAUDIA CRUZ)
					Role: PLAINTIFF, 
					Type: INDIVIDUAL
					Count: 7
			
			
			YOUNG, LILLIE PEARL YOUNG, CHARLIE DOES 1 TO 10
			
				YOUNG LILLIE PEARL
				(YOUNG, LILLIE PEARL)
					Role: DEFENDANT, 
					Type: INDIVIDUAL
					Count: 4
			
				YOUNG CHARLIE
				(YOUNG, CHARLIE)
					Role: DEFENDANT, 
					Type: INDIVIDUAL
					Count: 4
			
				DOES 1 TO 10
					Role: DEFENDANT, 
					Type: UNKNOWN
					Count: 1
			
			
			CITY AND COUNTY OF SAN FRANCISCO (SFGH
			
				CITY AND COUNTY OF SAN FRANCISCO SFGH
					Role: CLAIMANT, 
					Type: UNKNOWN
					Count: 2
			
			
			YOUNG, CHARLIE
			
				YOUNG CHARLIE
				(YOUNG, CHARLIE)
					Role: DEFENDANT, 
					Type: INDIVIDUAL
					Count: 4
			
			
			YOUNG, LILLIE PEARL
			
				YOUNG LILLIE PEARL
				(YOUNG, LILLIE PEARL)
					Role: DEFENDANT, 
					Type: INDIVIDUAL
					Count: 4
			
			
			YOUNG, LILLIE PEARL YOUNG, CHARLIE
			
				YOUNG LILLIE PEARL
				(YOUNG, LILLIE PEARL)
					Role: DEFENDANT, 
					Type: INDIVIDUAL
					Count: 4
			
				YOUNG CHARLIE
				(YOUNG, CHARLIE)
					Role: DEFENDANT, 
					Type: INDIVIDUAL
					Count: 4
			
			
			BARRIENTOS, CLAUDIA CRUZ
			
				BARRIENTOS CLAUDIA CRUZ
				(BARRIENTOS, CLAUDIA CRUZ)
					Role: PLAINTIFF, 
					Type: INDIVIDUAL
					Count: 7
			
			(repeat for another case ID below)
			 */
		}
		String partyfile = args[0];
		String infile = args[1];
		String outfile = args[2];

		mapParty = readParties(partyfile);
		List<LegalCase> cases = readCases(infile);
		processCases(mapParty, cases);
		writeCases(cases, outfile);
	}

	static void writeCases(List<LegalCase> cases, String outfile) throws IOException {
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (LegalCase cs : cases) {
			writeGlobalEntities(cs, wr);
			writePartyClusters(cs, wr);
		}
		wr.close();
	}

	static void processCases(Map<String, CaseNames> mapParty, List<LegalCase> cases) throws IOException {
		for (LegalCase cs : cases) {
			cs.analyze();
			cs.convertToPairs();
			CaseNames cn = mapParty.get(cs.getID());
			cs.findParties(cn);
			Collections.sort(cs.globalEntityList);
			int cnt = cs.splitConcatenatedEntities();
			System.out.println("Split Concatenates: " + cnt);
			int cnt1 = cs.splitAKAEntities();
			System.out.println("Split AKAs: " + cnt1);
			cs.cleanGel();
			cs.relinkPartyClusters();
		}
	}

	static List<LegalCase> readCases(String infile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(infile));
		List<LegalCase> cases = new ArrayList<>();
		String caseID = "";
		String line;
		List<TrackEntry> entries = null;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (!items[0].equals(caseID)) {
				caseID = items[0];
				entries = new ArrayList<>();
				LegalCase cs = new LegalCase(caseID, entries);
				cases.add(cs);
			}
			TrackEntry en = new TrackEntry(items[1], items[2]);
			entries.add(en);
		}
		br.close();
		return cases;
	}

	static String readEntries(String infile, List<TrackEntry> entries) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(infile));
		String caseID = null;
		String line;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			if (caseID == null) {
				caseID = items[0];
			}
			TrackEntry en = new TrackEntry(items[1], items[2]);
			entries.add(en);
		}
		br.close();
		return caseID;
	}

	private static void writeGlobalEntities(LegalCase cs, BufferedWriter wr) throws IOException {
		wr.write("\n============== Global Entities:===================\n\n");
		for (CaseEntity e : cs.globalEntityList) {
			wr.write(e + "\n");
		}
	}

	private static void writePartyClusters(LegalCase cs, BufferedWriter wr) throws IOException {
		wr.write("============================= Party Clusters: ===============================\n\n");
		for (PartyCluster pc : cs.clusters) {
			wr.write(pc.toString() + "\n");
		}
	}

	public void writeNames(LegalCase cs, BufferedWriter wr) throws IOException {
		wr.write(cs.complaint.toString() + "\n");
		for (String s : cs.names) {
			wr.write(s + "\n");
		}
	}

	public void writePairs(LegalCase cs, BufferedWriter wr) throws IOException {
		wr.write(cs.complaint.toString() + "\n");
		for (Pair p : cs.namep) {
			String s = (String) p.o1;
			Integer c = (Integer) p.o2;
			wr.write(s + "\t" + c + "\n");
		}
	}

	public static void main1(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("args: partyfile outfile ");
			System.exit(-1);
		}
		String partyfile = args[0];
		String outfile = args[1];
		Map<String, CaseNames> plist = readParties(partyfile);

		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		for (String key : plist.keySet()) {
			CaseNames pn = plist.get(key);
			wr.write(pn + "\n");
		}
		wr.close();
	}

	static Map<String, CaseNames> readParties(String partyfile) throws IOException {
		Map<String, CaseNames> plist = new HashMap<>();
		String line;
		BufferedReader brp = new BufferedReader(new FileReader(partyfile));
		String id = "";
		CaseNames cn = null;
		while ((line = brp.readLine()) != null) {
			String[] items = line.split("\\t");
			if (!items[0].equals(id)) {
				id = items[0];
				cn = new CaseNames(id);
				plist.put(id, cn);
			}
			Party p = new Party(items[1], items[1], items[2]);
			cn.addParty(p);
		}
		brp.close();
		return plist;
	}

}
