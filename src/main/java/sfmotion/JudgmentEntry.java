package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JudgmentEntry extends TrackEntry {
	//	Line 10794: CA_SFC_468543	2019-06-27	NOTICE OF ENTRY OF JUDGMENT (TRANSACTION ID # 100075969)
	//	Line 6733: CA_SFC_466097	2017-11-21	JUDGMENT, DISSOLUTION OF MARRIAGE - STATUS ONLY, MARITAL STATUS ENDS SEP-19-2017
	static final String judgment = "^(NOTICE OF ENTRY OF )?JUDGMENT";
	static final Pattern pjudgment = Pattern.compile(judgment, Pattern.CASE_INSENSITIVE);

	public JudgmentEntry(String _sdate, String _text) {
		super(_sdate, _text, JUDGMENT);
	}

	public static JudgmentEntry parse(String _sdate, String _text) {
		Matcher m = pjudgment.matcher(_text);
		if (m.find()) {
			JudgmentEntry entry = new JudgmentEntry(_sdate, _text);
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
			JudgmentEntry d = JudgmentEntry.parse(splits[1], splits[2]);
			if (d != null) {
				wr1.write(line + "\n");
				count1++;
				continue;
			}
		}
		br.close();
		wr1.write("count: " + count1 + "\n");
		wr1.close();
		System.out.println("Judgment : " + count1);
	}
}
