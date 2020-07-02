package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnswerEntry extends TrackEntry {
	//	Line 958: CA_SFC_464264	2017-12-12	ANSWER TO CROSS COMPLAINT (TRANSACTION ID # 17345056) FILED BY CROSS DEFENDANT LAMBERT/O'CONNOR DEVELOPMENT 450RI LLC (Fee:$450.00)
	//	Line 7220: CA_SFC_466231	2017-05-10	ANSWER TO 1ST AMENDED COMPLAINT (TRANSACTION ID # 100015495) FILED BY DEFENDANT BUCKLEY, CASSANDRA (Fee:$450.00)
	static final String answer = "^(AMENDED )?ANSWER";
	static final Pattern panswer = Pattern.compile(answer, Pattern.CASE_INSENSITIVE);

	public AnswerEntry(String _sdate, String _text) {
		super(_sdate, _text, ANSWER);
	}

	public static AnswerEntry parse(String _sdate, String _text) {
		Matcher m = panswer.matcher(_text);
		if (m.find()) {
			AnswerEntry entry = new AnswerEntry(_sdate, _text);
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
			AnswerEntry d = AnswerEntry.parse(splits[1], splits[2]);
			if (d != null) {
				wr1.write(line + "\n");
				count1++;
				continue;
			}
		}
		br.close();
		wr1.write("count: " + count1 + "\n");
		wr1.close();
		System.out.println("Answer : " + count1);
	}
}
