package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DismissalEntry extends Entry {//
	static final String entire = "DISMISSAL.{3,50}ENTIRE ACTION";
	static final Pattern pEntire = Pattern.compile(entire, Pattern.CASE_INSENSITIVE);
	static final String partial = "DISMISSAL.+?\\bAS TO (?<dismissedParty>.+?$)";
	static final Pattern pPartial = Pattern.compile(partial, Pattern.CASE_INSENSITIVE);
	static final String onfile = "DISMISSALS? ON FILE|\\.\\s*DISMISSALS? FILED";
	static final Pattern pOnfile = Pattern.compile(onfile, Pattern.CASE_INSENSITIVE);
	static final String complaint = "DISMISSAL (?<with>WITH(OUT)?) PREJUDICE (?<claim>(\\w+\\s+)+(COMPLAINT|CLAIM))?";
	static final Pattern pComplaint = Pattern.compile(complaint, Pattern.CASE_INSENSITIVE);
	static final String notice = "NOTICE OF ENTRY OF DISMISSAL";
	static final Pattern pNotice = Pattern.compile(notice, Pattern.CASE_INSENSITIVE);
	static final String left = "(?=\\(TRANSACTION\\sID\\s.{6,40}?\\)|JURY DEMAND|FILED\\s+BY|ESTIMATED)";

	public DismissalEntry(String _sdate, String _text) {
		super(_sdate, _text, DISMISSAL);
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 7) {
			System.out.println("args: infile outfile astofile onfile compfile notice remaining");
			System.exit(-1);
		}
		String infile = args[0];
		BufferedReader br = new BufferedReader(new FileReader(infile));
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(args[1]));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(args[2]));
		BufferedWriter wr3 = new BufferedWriter(new FileWriter(args[3]));
		BufferedWriter wr4 = new BufferedWriter(new FileWriter(args[4]));
		BufferedWriter wr5 = new BufferedWriter(new FileWriter(args[5]));
		BufferedWriter wr6 = new BufferedWriter(new FileWriter(args[6]));
		String line;
		int count1 = 0;
		int count2 = 0;
		int count3 = 0;
		int count4 = 0;
		int count5 = 0;
		int count6 = 0;
		while ((line = br.readLine()) != null) {
			String[] splits = line.split("\\t");
			if (splits.length != 3)
				continue;
			CaseSettledEntry stl = CaseSettledEntry.parse(splits[1], splits[2]);
			if (stl != null) {
				System.out.println(line);
				continue;
			}
			DismissalEntry d = DismissalEntry.parse(splits[1], splits[2]);
			if (d != null) {
				wr1.write(line + "\n");
				count1++;
				continue;
			}
			d = DismissalEntry.parsePartial(splits[1], splits[2]);
			if (d != null) {
				wr2.write(line + "\n");
				count2++;
				continue;
			}
			d = DismissalEntry.parseOnfile(splits[1], splits[2]);
			if (d != null) {
				wr3.write(line + "\n");
				count3++;
				continue;
			}
			d = DismissalEntry.parseComplaint(splits[1], splits[2]);
			if (d != null) {
				wr4.write(line + "\n");
				count4++;
				continue;
			}
			d = DismissalEntry.parseNotice(splits[1], splits[2]);
			if (d != null) {
				wr5.write(line + "\n");
				count5++;
				continue;
			}
			wr6.write(line + "\n");
			count6++;
		}
		br.close();
		wr1.write("count: " + count1 + "\n");
		wr1.close();
		wr2.write("count: " + count2 + "\t");
		wr2.close();
		wr3.write("count: " + count3 + "\t");
		wr3.close();
		wr4.write("count: " + count4 + "\t");
		wr4.close();
		wr5.write("count: " + count5 + "\t");
		wr5.close();
		wr6.write("count: " + count6 + "\t");
		wr6.close();
		System.out.println("Dismissal Entire : " + count1 + ", partial: " + count2 + ", on file: "
				+ count3 + ", complaint: " + count4 + ", notice: " + count5 + ", remain: " + count6);
	}

	public static DismissalEntry parse(String _sdate, String _text) {
		Matcher m = pEntire.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		m = pPartial.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		m = pOnfile.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		m = pComplaint.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		m = pNotice.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		return null;
	}

	public static DismissalEntry parsePartial(String _sdate, String _text) {
		Matcher m = pPartial.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		return null;
	}

	public static DismissalEntry parseOnfile(String _sdate, String _text) {
		Matcher m = pOnfile.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		return null;
	}

	public static DismissalEntry parseComplaint(String _sdate, String _text) {
		Matcher m = pComplaint.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		return null;
	}

	public static DismissalEntry parseNotice(String _sdate, String _text) {
		Matcher m = pNotice.matcher(_text);
		if (m.find()) {
			DismissalEntry entry = new DismissalEntry(_sdate, _text);
			return entry;
		}
		return null;
	}
}
