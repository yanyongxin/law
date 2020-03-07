package legal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractA {

	static final Pattern pA = Pattern.compile(" A ", Pattern.CASE_INSENSITIVE);

	static Map<String, AttachA> alist = new TreeMap<>();

	public static void main(String[] args) throws IOException {
		// Extract "A California Corporation" etc.
		if (args.length != 2) {
			System.out.println("args: infile outfile");
			//Example: ca_sfc_party.txt a_corp.txt
			System.exit(1);
		}
		BufferedReader br = new BufferedReader(new FileReader(args[0]));
		String line;
		while ((line = br.readLine()) != null) {
			String[] split = line.split("\\t");
			//CA_SFC_464170	KHOURY, ZACHARY MICHEL	Defendant	1
			Matcher m = pA.matcher(split[1]);
			if (m.find()) {
				String a = split[1].substring(m.start()).trim();
				AttachA aa = alist.get(a);
				if (aa == null) {
					aa = new AttachA(a);
					alist.put(a, aa);
				} else {
					aa.inc();
				}
			}
		}
		br.close();
		BufferedWriter wr = new BufferedWriter(new FileWriter(args[1]));
		for (String a : alist.keySet()) {
			wr.write(a + "\t" + alist.get(a).getCount() + "\n");
		}
		wr.close();
	}

	static class AttachA implements Comparable<AttachA> {
		String attach;
		int count;

		public AttachA(String a) {
			attach = a;
			count = 1;
		}

		public int inc() {
			count++;
			return count;
		}

		public int getCount() {
			return count;
		}

		@Override
		public int compareTo(AttachA o) {
			return attach.compareToIgnoreCase(o.attach);
		}
	}
}
