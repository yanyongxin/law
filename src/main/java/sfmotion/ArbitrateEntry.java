package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArbitrateEntry extends Entry {
	static final String arbitrate = "\\bARBITRAT";
	static final Pattern parbitrate = Pattern.compile(arbitrate, Pattern.CASE_INSENSITIVE);

	public ArbitrateEntry(String _sdate, String _text) {
		super(_sdate, _text, ARBITRATE);
	}

	public static ArbitrateEntry parse(String _sdate, String _text) {
		Matcher m = parbitrate.matcher(_text);
		if (m.find()) {
			ArbitrateEntry entry = new ArbitrateEntry(_sdate, _text);
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
			ArbitrateEntry d = ArbitrateEntry.parse(splits[1], splits[2]);
			if (d != null) {
				wr1.write(line + "\n");
				count1++;
				continue;
			}
		}
		br.close();
		wr1.write("count: " + count1 + "\n");
		wr1.close();
		System.out.println("Arbitrate count : " + count1);
	}
}
