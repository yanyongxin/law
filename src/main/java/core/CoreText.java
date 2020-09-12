package core;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import core.LegaLanguage.Srunner;
import sftrack.DocketEntry;

public class CoreText {
	static final String rulesFile = "sftrack/docketParse.drl";
	static final String triplesFile = "src/main/resources/sftrack/triples.txt";
	static final String lexiconFile = "src/main/resources/sftrack/lexicon.txt";
	static final int TOP_N = 3;
	static LegaLanguage legalang;
	static String[] testText = {
			//			"EVIDENCE IN SUPPORT OF DEFENDANTS MOTION FOR SUMMARY JUDGMENT OR SUMMARY ADJUDICATION INCLUDING DECLARATIONS IN SUPPORT",
			//			"MOTION TO STRIKE COMPLAINT FILED BY DEFENDANT HYUNDAI MOTOR AMERICA HEARING SET FOR JUL-18-2017 AT 02:00 PM IN DEPT 302",
			//			"MEMORANDUM OF POINTS AND AUTHORITIES AND DEMURRER TO FIRST AMENDED COMPLAINT FILED BY DEFENDANT HYUNDAI MOTOR AMERICA",
			//			"ORDER GRANTING ATTORNEY'S MOTION TO BE RELIEVED AS COUNSEL",
			"EX PARTE APPLICATION FOR ORDER SHORTENING TIME FOR DEFENDANTS MOTION TO COMPEL THE SECOND DEPOSITION SESSION OF PLAINTIFF",
			//			"DISMISSAL WITHOUT PREJUDICE OF CROSS COMPLAINT FILED BY PLAINTIFF" 
	};

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("args: debug|nodebug");
			System.exit(1);
		}
		boolean debug = false;
		if (args[0].equalsIgnoreCase("debug")) {
			debug = true;
		}
		System.out.println("Initialization ...");
		legalang = LegaLanguage.initializeRuleEngine(rulesFile, triplesFile, lexiconFile);
		long starttime = System.currentTimeMillis();
		parseText(testText, debug);
		long endTime = System.currentTimeMillis() - starttime;
		System.out.println("Total time(ms): " + endTime);
	}

	static void parseText(String[] textArray, boolean debugPrint) {
		Srunner srun = legalang.createSrunner(debugPrint);
		Entity.getGlobalid();
		for (String text : textArray) {
			System.out.println(text);
			Entity.resetSerial();
			try {
				List<Phrase> phlist = generatePhraseList(text);
				srun.insertList(phlist);
				srun.execute();
				Map<Integer, List<Phrase>> rpmap = srun.findAllPhrases();
				if (rpmap.size() > 0) {
					List<Integer> keylist = Analysis.buildKeyList(rpmap);
					keylist.add(phlist.get(phlist.size() - 1).endToken);
					ArrayList<Integer> segments = new ArrayList<Integer>();
					List<List<Analysis>> lla = Analysis.findBestNew(rpmap, keylist, TOP_N, segments);
					List<Phrase> plist = DocketEntry.getPhraseList(lla);
					for (Phrase ph : plist) {
						System.out.println(ph.pprint("", false));
					}
				} else {
					System.out.println("No phrase found!");
				}
				srun.reset();
			} catch (Exception ex) {
				fail(ex.getMessage());
			}
		}
		srun.dispose();
	}

	static List<Phrase> generatePhraseList(String text) {
		List<Phrase> phlist = Collections.synchronizedList(new ArrayList<Phrase>());
		List<LexToken> tokens = new ArrayList<>();
		int j = 0;
		int start = 0;
		List<LexToken> tks = LexToken.tokenize(text);
		tokens.addAll(tks);
		for (LexToken tk : tks) {
			tk.start += start; // shift to sentence coordinates
			tk.end += start;
			tk.parent = text;
			Phrase ph = new Phrase(tk.text.toLowerCase(), j, j + 1, tokens);
			if (tk.type == LexToken.LEX_SERIALNUMBER && !tk.text.equalsIgnoreCase("1ST") && !tk.text.equalsIgnoreCase("2ND")) {
				ph.setSynType("NUMP");
				Entity cls = legalang.getEntity("SerialValue");
				Entity e = new Entity(tk.text.toLowerCase(), cls, Entity.TYPE_INSTANCE, legalang, j);
				ph.setGraph(e);
			}
			phlist.add(ph);
			j++;
		}
		return phlist;
	}

}
