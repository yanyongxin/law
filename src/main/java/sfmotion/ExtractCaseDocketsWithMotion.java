package sfmotion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractCaseDocketsWithMotion {
	static Pattern pMotion = Pattern.compile("MOTION|(?<!UNDER\\s)APPLICATION|DEMURRER", Pattern.CASE_INSENSITIVE);

	// keep only the cases that contain "MOTION|APPLICATION|DEMURRER"
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("args: infile motionfile noMotionfile");
			System.exit(-1);
		}
		String infile = args[0];
		String outfile = args[1];
		String noMotionfile = args[2];
		BufferedReader br = new BufferedReader(new FileReader(infile));
		BufferedWriter wr = new BufferedWriter(new FileWriter(outfile));
		BufferedWriter wrNo = new BufferedWriter(new FileWriter(noMotionfile));
		String line;
		List<String> dockets = new ArrayList<>();
		String current_case_id = "";
		boolean bMotion = false;
		int mCount = 0; // case count with "MOTION"
		int count = 0;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			String case_id = items[0];//CA_SFC_470097
			String docket_entry = items[2];//THE COURT ORDERED THE FOLLOWING JUDGMENT ENTERED:...
			if (!case_id.equals(current_case_id)) {
				count++;
				if (bMotion) {
					String d1 = dockets.get(0);
					String[] dd = d1.split("\\t");
					if (ComplaintPetition.testClaims(dd[2])) {
						mCount++;
						for (String d : dockets) {
							wr.write(d + "\n");
						}
					} else {
						for (String d : dockets) {
							wrNo.write(d + "\n");
						}
					}
					bMotion = false;
				} else {
					for (String d : dockets) {
						wrNo.write(d + "\n");
					}
				}
				dockets.clear();
				current_case_id = case_id;
			}
			dockets.add(line);
			if (!bMotion) {
				Matcher m = pMotion.matcher(docket_entry);
				if (m.find()) {
					//				int id = docket_entry.indexOf("MOTION");
					//				if (id >= 0) {
					bMotion = true;
				}
			}
		}
		System.out.println("Total Count: " + count + ", Motion Count: " + mCount);
		wr.close();
		wrNo.close();
		br.close();
	}

}
