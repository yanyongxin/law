package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrialEntry extends TrackEntry {
	static final String trial = "^TRIAL\\s";
	static final Pattern ptrial = Pattern.compile(trial, Pattern.CASE_INSENSITIVE);

	public TrialEntry(String _sdate, String _text) {
		super(_sdate, _text, TRIAL);
	}

	public static TrialEntry parse(String _sdate, String _text) {
		Matcher m = ptrial.matcher(_text);
		if (m.find()) {
			TrialEntry entry = new TrialEntry(_sdate, _text);
			return entry;
		}
		return null;
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
			TrialEntry d = TrialEntry.parse(splits[1], splits[2]);
			if (d != null) {
				wr1.write(line + "\n");
				count1++;
				continue;
			}
		}
		br.close();
		wr1.write("count: " + count1 + "\n");
		wr1.close();
		System.out.println("Trial Count : " + count1);
	}
}
