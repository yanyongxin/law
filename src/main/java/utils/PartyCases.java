package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class PartyCases {
	static TreeMap<String, List<String>> caseParty = new TreeMap<>();
	static TreeSet<String> noCase = new TreeSet<>();

	public static void main(String[] args) throws IOException {
		if (args.length != 5) {
			System.out.println("args: docketfileIn partyfileIn docNotInPartyOut casePartyOut caseNotUsedOut");
			System.exit(1);
		}
		BufferedReader br1 = new BufferedReader(new FileReader(args[0]));
		String line;
		int lineNumber = 0;
		while ((line = br1.readLine()) != null) {
			String[] items = line.split("\\t");
			lineNumber++;
			if (items.length != 3) {
				System.out.println("line " + lineNumber + ": " + line);
				continue;
			}
			String[] cases = items[2].split("\\s+");
			int count = Integer.valueOf(items[1]);
			if (cases.length != count) {
				System.out.println("caseNumber wrong: line : " + lineNumber + ", text: " + line);
			}
			for (String cs : cases) {
				cs = cs.toUpperCase();
				List<String> cslist = caseParty.get(cs);
				if (cslist == null) {
					cslist = new ArrayList<>();
					caseParty.put(cs, cslist);
				}
				if (!cslist.contains(items[0])) {
					cslist.add(items[0]);
				}
			}
		}
		br1.close();
		System.out.println("Number of lines in " + args[0] + ": " + lineNumber);
		noCase.addAll(caseParty.keySet());
		String currentCase = "";
		BufferedReader br2 = new BufferedReader(new FileReader(args[1]));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(args[2]));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(args[3]));
		while ((line = br2.readLine()) != null) {
			String[] items = line.split("\\t");
			if (!items[0].equals(currentCase)) {
				currentCase = items[0];
				List<String> parties = caseParty.get(currentCase);
				if (parties == null) {
					wr1.write(currentCase + "\n");
				} else {
					noCase.remove(currentCase);
					for (String party : parties) {
						wr2.write(currentCase + "\t" + party + "\n");
					}
				}
			}
		}
		br2.close();
		wr1.close();
		wr2.close();
		BufferedWriter wr3 = new BufferedWriter(new FileWriter(args[4]));
		for (String cs : noCase) {
			List<String> parties = caseParty.get(cs);
			for (String party : parties) {
				wr3.write(cs + "\t" + party + "\n");
			}
		}
		wr3.close();
	}

}
