package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettlementConferenceEntry extends Entry {
	static final String settlementConference = "SETTLEMENT CONFERENCE";
	static final Pattern psettlementConference = Pattern.compile(settlementConference, Pattern.CASE_INSENSITIVE);

	public SettlementConferenceEntry(String _sdate, String _text) {
		super(_sdate, _text, SETTLECONFERENCE);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("args: infile outfile");
			System.exit(-1);
		}
		String infile = args[0];
		BufferedReader br = new BufferedReader(new FileReader(infile));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(args[1]));
		String line;
		int count1 = 0;
		while ((line = br.readLine()) != null) {
			String[] splits = line.split("\\t");
			if (splits.length != 3)
				continue;
			SettlementConferenceEntry d = SettlementConferenceEntry.parse(splits[1], splits[2]);
			if (d != null) {
				wr1.write(line + "\n");
				count1++;
				continue;
			}
		}
		br.close();
		wr1.write("count: " + count1 + "\n");
		wr1.close();
		System.out.println("Case Settled : " + count1);
	}

	public static SettlementConferenceEntry parse(String _sdate, String _text) {
		Matcher m = psettlementConference.matcher(_text);
		if (m.find()) {
			SettlementConferenceEntry entry = new SettlementConferenceEntry(_sdate, _text);
			return entry;
		}
		return null;
	}

}
