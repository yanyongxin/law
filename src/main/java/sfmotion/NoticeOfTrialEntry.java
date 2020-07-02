package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NoticeOfTrialEntry extends TrackEntry {
	static final String noticeOfTimeAndPlaceOfTrial = "^NOTICE OF TIME";
	static final Pattern pnoticeOfTimeAndPlaceOfTrial = Pattern.compile(noticeOfTimeAndPlaceOfTrial, Pattern.CASE_INSENSITIVE);

	public NoticeOfTrialEntry(String _sdate, String _text) {
		super(_sdate, _text, NOTICEOFTRIAL);
	}

	public static NoticeOfTrialEntry parse(String _sdate, String _text) {
		Matcher m = pnoticeOfTimeAndPlaceOfTrial.matcher(_text);
		if (m.find()) {
			NoticeOfTrialEntry entry = new NoticeOfTrialEntry(_sdate, _text);
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
			NoticeOfTrialEntry d = NoticeOfTrialEntry.parse(splits[1], splits[2]);
			if (d != null) {
				wr1.write(line + "\n");
				count1++;
				continue;
			}
		}
		br.close();
		wr1.write("count: " + count1 + "\n");
		wr1.close();
		System.out.println("count : " + count1);
	}
}
