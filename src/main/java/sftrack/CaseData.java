package sftrack;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sftrack.DocketEntry.DockRef;
import sftrack.LegaLanguage.Srunner;

public class CaseData {
	private static final Logger log = LoggerFactory.getLogger(CaseData.class);
	static final int TOP_N = 3;
	static final int SEMICOLON_LENGTH = 30;
	//	static String dirroot = "/Users/yanyongxin/Documents/professional/lexprojects/legalgeno/jsonAll2";
	static String dirroot = null;
	//	String testText = null;
	//	String testLexID = null;
	//	int testDESerial = -1;
	CaseMeta meta;
	List<DocketEntry> delist;
	LegaLanguage onto;
	List<String> firmnamelist = new ArrayList<String>();
	List<String> partynamelist = new ArrayList<String>();
	List<Attorney> attorneys = new ArrayList<Attorney>();
	List<LitiParty> parties = new ArrayList<LitiParty>();
	List<Judge> judges = new ArrayList<Judge>();
	List<Lawfirm> lawfirms = new ArrayList<Lawfirm>();
	// Markman hearing event record. There is one such hearing, but there may be multiple records. We keep all of them during DE analysis.
	List<LitiEvent> markman = new ArrayList<LitiEvent>();
	List<LitiEvent> trialdays = new ArrayList<LitiEvent>();
	List<LitiEvent> terminates = new ArrayList<LitiEvent>();
	List<LitiEvent> judgments = new ArrayList<LitiEvent>();
	List<LitiEvent> judgeAssigns = new ArrayList<LitiEvent>();
	List<LitiEvent> dismissals = new ArrayList<LitiEvent>();
	List<LitiEvent> genericEvents = new ArrayList<LitiEvent>();// for all other events
	List<String> patents = new ArrayList<String>();

	public void vacateMemormy() {
		if (delist != null) {
			delist.clear();
			delist = null;
		}
		firmnamelist.clear();
		partynamelist.clear();
		attorneys.clear();
		parties.clear();
		judges.clear();
		lawfirms.clear();
		markman.clear();
		trialdays.clear();
		terminates.clear();
		judgeAssigns.clear();
		dismissals.clear();
		genericEvents.clear();
		patents.clear();
		attorneys = null;
		parties = null;
		judges = null;
		lawfirms = null;
		markman = null;
		trialdays = null;
		terminates = null;
		judgeAssigns = null;
		dismissals = null;
		genericEvents = null;
		patents = null;
		onto = null;
		meta = null;
	}

	public void addDismissal(LitiEvent e) {
		dismissals.add(e);
	}

	public void addPatents(List<String> pts) {
		for (String p : pts) {
			if (!patents.contains(p)) {
				patents.add(p);
			}
		}
	}

	public void addLitiEvents(LitiEvent e) {
		genericEvents.add(e);
	}

	public void addJudgeAssign(LitiEvent e) {
		judgeAssigns.add(e);
	}

	public void addTerminate(LitiEvent e) {
		terminates.add(e);
	}

	public void addJudgment(LitiEvent e) {
		judgments.add(e);
	}

	public void addMarkman(LitiEvent me) {
		markman.add(me);
	}

	public List<LitiEvent> getMarkman() {
		return markman;
	}

	public void addTrialEvent(LitiEvent me) {
		trialdays.add(me);
	}

	public List<LitiEvent> getTrialDays() {
		return trialdays;
	}

	public LitiParty getParty(String entityName) {
		for (LitiParty pt : parties) {
			if (pt.getEntityName().equals(entityName)) {
				return pt;
			}
		}
		return null;
	}

	public String getPartyRole(String entityName) {
		for (LitiParty pt : parties) {
			if (pt.getEntityName().equals(entityName)) {
				return pt.litiRole;
			}
		}
		return null;
	}

	public String getLawfirmRole(String entityName) {
		Lawfirm fm = null;
		for (Lawfirm pt : lawfirms) {
			if (pt.getEntityName().equals(entityName)) {
				fm = pt;
				break;
			}
		}
		if (fm == null) {
			return null;
		}
		int plaintiffCount = 0;
		int defendantCount = 0;
		if (fm.parties != null) {
			for (LitiParty p : fm.parties) {
				if (p.litiRole.equals(DocketEntry.ROLE_PLAINTIFF)) {
					plaintiffCount++;
				} else {
					defendantCount++;
				}
			}
		}
		if (defendantCount == 0 && plaintiffCount == 0) {
			return null;
		}
		if (defendantCount > plaintiffCount) {
			return DocketEntry.ROLE_DEFENDANT;
		} else if (defendantCount == plaintiffCount) {
			return DocketEntry.ROLE_BOTH;
		} else {
			return DocketEntry.ROLE_PLAINTIFF;
		}
	}

	public Attorney getAttorney(String entityName) {
		for (Attorney pt : attorneys) {
			if (pt.getEntityName().equals(entityName)) {
				return pt;
			}
		}
		return null;
	}

	public List<DocketEntry> getDEList() {
		return delist;
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();
		jo.put("id", this.meta.id);
		jo.put("cv", this.meta.full);
		JSONArray ja = new JSONArray();
		for (DocketEntry de : delist) {
			ja.add(de.toJSONObject());
		}
		jo.put("entrys", ja);
		if (trialdays.size() > 0) {
			ja = new JSONArray();
			for (LitiEvent de : this.trialdays) {
				ja.add(de.toJSON());
			}
			jo.put("trial", ja);
		}
		if (markman.size() > 0) {
			ja = new JSONArray();
			for (LitiEvent de : this.markman) {
				ja.add(de.toJSON());
			}
			jo.put("markman", ja);
		}
		if (dismissals.size() > 0) {
			ja = new JSONArray();
			for (LitiEvent de : dismissals) {
				ja.add(de.toJSON());
			}
			jo.put("dismissals", ja);
		}
		if (terminates.size() > 0) {
			ja = new JSONArray();
			for (LitiEvent de : terminates) {
				ja.add(de.toJSON());
			}
			jo.put("terminates", ja);
		}
		if (judgments.size() > 0) {
			ja = new JSONArray();
			for (LitiEvent de : judgments) {
				ja.add(de.toJSON());
			}
			jo.put("judgments", ja);
		}
		if (judgeAssigns.size() > 0) {
			ja = new JSONArray();
			for (LitiEvent de : judgeAssigns) {
				ja.add(de.toJSON());
			}
			jo.put("judgeAssigns", ja);
		}
		if (genericEvents.size() > 0) {
			ja = new JSONArray();
			for (LitiEvent de : genericEvents) {
				ja.add(de.toJSON());
			}
			jo.put("genericEvents", ja);
		}
		if (patents.size() > 0) {
			ja = new JSONArray();
			for (String p : patents) {
				ja.add(p);
			}
			jo.put("patents", ja);
		}

		JSONArray jap = new JSONArray();
		JSONArray jad = new JSONArray();
		JSONArray jao = new JSONArray();
		for (LitiParty pt : parties) {
			try {
				if (pt.litiRole.equals(DocketEntry.ROLE_DEFENDANT)) {
					jad.add(pt.getEntityName());
				} else if (pt.litiRole.equals(DocketEntry.ROLE_PLAINTIFF)) {
					jap.add(pt.getEntityName());
				} else if (pt.litiRole.equals(DocketEntry.ROLE_OTHER)) {
					jao.add(pt.getEntityName());
				}
			} catch (Exception ex) {
				pt.setLitiRole(DocketEntry.ROLE_OTHER);
				jao.add(pt.getEntityName());
			}
		}
		jo.put("plaintiffs", jap);
		jo.put("defendants", jad);
		jo.put("others", jao);
		return jo;
	}

	@SuppressWarnings("unchecked")
	public void postProcess() {
		// 1. verify references, reset eventCategory based on references
		// 2. create party outcomes summary
		// 3. create litigation phase summary 
		// 4. create case json, and save to file
		// 5. 

		//		Steps:
		// 1. save to json file with DEs only
		// 2. Show table view in explorer
		// 3. add summary processing
		// 4. show summary data in explorer
		this.findForwardRefs();
		JSONObject jo = toJSON();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(dirroot + "/" + this.meta.id + ".txt"));
			bw.append(jo.toJSONString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Run through the case Docket Entries.
	 * 
	 * @param serial
	 *            If serial >=0, it is the specific DE to work on, in verbose mode. if serial < 0, then run on all DE in quiet mode.
	 */
	public void run(int serial, boolean bAnalysis, int printlevel, String testText) {
		boolean verbose = false;
		if (serial >= 0 || printlevel > 1) {
			verbose = true;
		}
		Srunner srun = onto.createSrunner(verbose);
		int totalScore = 0;
		int totalAmbiguity = 0;
		for (int decount = 0; decount < delist.size(); decount++) {
			if (serial >= 0 && decount != serial) {
				continue;
			}
			DocketEntry de = delist.get(decount);
			de.setOnto(onto);
			String s = de.text;
			if (testText != null) {
				s = testText;
				log.debug("\n\nTest Text: " + testText + "\n");
			}
			if (printlevel > 0) {
				log.debug("\n\n" + decount + "\t" + de + "\n");
			}
			de.processedText = preprocessingText(s).trim();
			List<Phrase> phlist = deToPhrases(de.processedText, printlevel > 2);
			srun.insertList(phlist);
			srun.execute();
			if (bAnalysis) {
				Map<Integer, List<Phrase>> rpmap = srun.findAllPhrases();
				List<Integer> keylist = Analysis.buildKeyList(rpmap);
				int score = 0;
				int ambi = 1;
				if (keylist.size() > 0) {
					ambi = 0;
					keylist.add(phlist.size());
					ArrayList<Integer> segments = new ArrayList<Integer>();
					List<List<Analysis>> lla = Analysis.findBestNew(rpmap, keylist, TOP_N, segments);
					de.setBest(lla);
					if (printlevel > 0) {
						printBest(lla);
					}
					for (List<Analysis> la : lla) {
						SimpleAnalysis sp = getSimpleAnalysis(la);
						if (sp != null) {
							score += sp.getScore();
							ambi += sp.getAmbiguity();
						}
					}
					// fragmentation lowers score:
					score -= segments.size();
					totalScore += score;
					totalAmbiguity += ambi;
					//					log.info("segments: " + segments.size());
				}
				// List<ERGraph> genes = de.getGenes();
				// if (genes != null) {
				// ERGraph g = genes.get(0);
				// log.info("Gene:" + g.toString());
				// }
				if (printlevel > 0) {
					log.debug(meta.id + ", " + decount + ", score: " + score + ", ambi: " + ambi + ", tscore: " + totalScore + ", tambi: " + totalAmbiguity);
					//					if (de.type != null) {
					//						log.info("SubType: " + de.subType);
					//					}
					List<String> interpret = de.getInterpret();
					for (String ss : interpret) {
						log.debug(ss);
					}
				}
			}
			srun.reset();
		} // end for s
		srun.dispose();
		if (serial < 0) {
			postProcess();
		}
	}

	static String bracketRegex = "(?i)\\s*\\(\\s*\\p{Alpha}{2,}.[\\p{Alnum}\\p{Punct}\\p{Blank}&&[^(]]*?\\)\\s*";
	// [Transferred from Florida Southern on 12/14/2010.] 
	static String transferRegex = "(?i)\\s*\\[\\s*transfer\\p{Alpha}{2,}\\s*from\\s*[\\p{Alnum}\\p{Punct}\\p{Blank}]*?\\]\\s*";

	static String preprocessingText(String s) {
		// s =
		// s.replaceAll("(?i)\\s*\\([\\p{Alnum}\\p{Punct}\\p{Blank}&&[^(]]*?\\).{0,4}\\(\\s*Entered[\\p{Alnum}\\p{Punct}\\p{Blank}&&[^(]]*?\\)\\s*",
		// " ");
		// s =
		// s.replaceAll("(?i)\\s*\\(\\s*Atta[\\p{Alnum}\\p{Punct}\\p{Blank}&&[^(]]*?\\)\\s*",
		// " ");
		// this keeps things like (98 in 1:11-cv-00313-SLR), but remove
		// (attachment ...)
		s = s.replaceAll(bracketRegex, " ");
		// just in case there are nested brackets, do it one more time to make
		// sure:
		s = s.replaceAll(bracketRegex, " ");
		s = s.replaceAll(transferRegex, " ");

		s = s.replaceAll("\\[|\\]", "");
		s = s.replaceAll("\\(s\\)", "");// Number(s) => Number
		s = s.replaceAll("\\,{2,}", ",");
		s = s.replaceFirst("(?i)so\\s*ordered\\s+", "SO ORDERED, ");
		// The following semicolon replacement is quick and dirty fix to avoid unnecessary joining of parts separated by semicolons, 
		// which sometimes result in sentences too long to process. The solution still allows phrases to be joined, like:
		// Patent numbers 3,456,123; 5,567,789; 5,778,223 B3; 4,887,293.
		// But does not allow long phrases to be joined.
		String[] split = s.split("\\;");
		if (split.length > 2) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < split.length - 1; i++) {
				sb.append(split[i]);
				if (split[i + 1].length() > SEMICOLON_LENGTH) {
					sb.append(";;");
				} else {
					sb.append(";");
				}
			}
			sb.append(split[split.length - 1]);
			s = sb.toString();
		}
		// s = LtUtil.removeBrackets(s);
		return s;
	}

	static List<Phrase> deToPhrases(String s, boolean verbose) {
		List<LexToken> tokens = null;
		tokens = LexToken.tokenize(s);
		List<Phrase> phlist = Collections.synchronizedList(new ArrayList<Phrase>());
		if (tokens != null) {
			for (int i = 0; i < tokens.size(); i++) {
				LexToken tk = tokens.get(i);
				if (verbose) {
					log.debug(tk.toString());
				}
				Phrase ph = new Phrase(tk.text.toLowerCase(), i, i + 1, tokens);
				phlist.add(ph);
			}
		}
		return phlist;
	}

	static void printBest(List<List<Analysis>> lla) {
		log.debug("Best parses: ");
		// every different score:
		for (List<Analysis> la : lla) {
			if (la == null || la.size() == 0) {
				continue;
			}
			Analysis a = la.get(0);
			log.debug("\t" + a);
		}
	}

	static SimpleAnalysis getSimpleAnalysis(List<Analysis> la) {
		if (la == null || la.size() == 0) {
			return null;
		}
		Analysis a = la.get(0);
		SimpleAnalysis sp = new SimpleAnalysis(a.getScore(), la.size(), a.getText());
		return sp;
	}

	public CaseData(CaseMeta cm) {
		meta = cm;
		delist = new ArrayList<DocketEntry>();
	}

	public void addFirmParty(String fm, String pt) {
		firmnamelist.add(fm);
		partynamelist.add(pt);
	}

	public void addParty(String partyName, String role) {
		addParty(parties, partyName, role);
	}

	static LitiParty addParty(List<LitiParty> partyList, String partyName, String partyType) {
		LitiParty party = new LitiParty(partyName);
		if (partyType != null) {
			if (partyType.equalsIgnoreCase(DocketEntry.ROLE_PLAINTIFF)) {
				party.setLitiRole(DocketEntry.ROLE_PLAINTIFF);
			} else if (partyType.equalsIgnoreCase(DocketEntry.ROLE_DEFENDANT)) {
				party.setLitiRole(DocketEntry.ROLE_DEFENDANT);
			} else {
				party.setLitiRole(DocketEntry.ROLE_OTHER);
			}
		}
		int pos = partyList.indexOf(party);
		if (pos < 0) {
			partyList.add(party);
			return party;
		} else {
			LitiParty partyOld = partyList.get(pos);
			if (partyOld.getLitiRole().equals(DocketEntry.ROLE_OTHER)) {
				if (!party.getLitiRole().equals(DocketEntry.ROLE_OTHER)) {
					partyOld.setLitiRole(party.getLitiRole());
				}
			}
			return partyOld;
		}
	}

	private List<LitiParty> findMatchingParties(String partyName, List<LitiParty> pts) {
		List<LitiParty> ret = new ArrayList<LitiParty>();
		for (LitiParty p : pts) {
			if (p.matchName(partyName)) {
				ret.add(p);
			}
		}
		return ret;
	}

	/**
	 * by this time, List<LitiParty>, List<Attorney> have already been created. Also, party to attorney link already exists.
	 * We use party->attorney links to create attorney -> party links.
	 * 
	 */
	private void createAttorneyToPartyLinks() {
		for (LitiParty pt : parties) {
			List<Attorney> pta = pt.attorneys;
			for (Attorney at : pta) {
				at.addParty(pt);
			}
		}
	}

	/**
	 * Given the entity name of an attorney, such as AttorneyJohnSmith, find out which side is he representing.
	 * 
	 * @param attorneyEntityName
	 * @return "Plaintiff" or "Defendant"
	 */
	public String getAttorneyForPartyRole(String attorneyEntityName) {
		int pcount = 0;
		int dcount = 0;
		Attorney atn = null;
		for (Attorney at : attorneys) {
			if (at.getEntityName().equalsIgnoreCase(attorneyEntityName)) {
				atn = at;
				break;
			}
		}
		if (atn == null) {
			return null;
		}
		for (LitiParty pt : atn.parties) {
			String role = pt.getLitiRole();
			if (role == null) {
				continue;
			}
			if (role.equalsIgnoreCase(DocketEntry.ROLE_PLAINTIFF)) {
				pcount++;
			} else {
				dcount++;
			}
		}
		if (pcount > dcount) {
			return DocketEntry.ROLE_PLAINTIFF;
		} else if (pcount < dcount) {
			return DocketEntry.ROLE_DEFENDANT;
		} else {
			return DocketEntry.ROLE_BOTH;
		}
	}

	public void addDE(DocketEntry de) {
		delist.add(de);
	}

	public String getID() {
		return meta.getID();
	}

	@Override
	public String toString() {
		return getID() + "\t" + meta.full + "\t" + meta.filed + "\t" + "\t" + delist.size();
	}

	/**
	 * Parse one case docket report json object, decompose it into lawyer name,
	 * party name, law firm name, street address, phone number, fax number,
	 * email address. Add these entities to global data structures (firmbase,
	 * lawyerbase, addressbase, phonebase, etc.). Also create a MexFirm object
	 * that has links to all these related entities. Right now interaction with
	 * firmbase etc., are done at the lowest end of the calling sequence without
	 * passing these data structures down as arguments, this is possible because
	 * firmbase etc., are class globals (static). I don't like this because it's
	 * not possible to see this effect from arguments of calling functions, and
	 * accessing these data structures require the name of the class, which make
	 * it not modular. We may change it later.
	 * 
	 * @param file
	 */
	public static void parseCaseDocket(JSONParser parser, String json, List<String> attorneyList, List<String> partyList, List<String> judgeList) {
		if (json == null) {
			return;
		}
		try {
			json = json.trim();
			if (json.length() == 0) {
				return;
			}
			JSONObject jsonObject = (JSONObject) parser.parse(json);
			if (jsonObject != null) {
				String judgeObject = (String) jsonObject.get("assigned_to");
				if (judgeObject != null) {//Judge Sue L. Robinson
					if (judgeObject.toLowerCase().startsWith("judge")) {
						judgeList.add(judgeObject.substring(judgeObject.indexOf(" ")).trim());
					} else {
						judgeList.add(judgeObject);
					}
				}
			}
			jsonObject = (JSONObject) parser.parse(json);
			if (jsonObject != null) {
				String judgeObject = (String) jsonObject.get("referred_to");
				if (judgeObject != null) {//Judge Sue L. Robinson
					if (judgeObject.toLowerCase().startsWith("judge")) {
						judgeList.add(judgeObject.substring(judgeObject.indexOf(" ")).trim());
					} else {
						judgeList.add(judgeObject);
					}
				}
			}

			Object parties = jsonObject.get("parties");
			if (parties == null) {
				return;
			}
			if (!parties.getClass().equals(JSONArray.class)) {
				return;
			}
			JSONArray partyArray = (JSONArray) parties;
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> iterator = partyArray.iterator();
			while (iterator.hasNext()) {
				JSONObject onePartyRole = iterator.next();
				String partyName = (String) onePartyRole.get("name");
				if (!partyList.contains(partyName)) {
					partyList.add(partyName);
				}
				Object attorniesObj = onePartyRole.get("attornies");
				if (!attorniesObj.getClass().equals(JSONArray.class)) {
					continue;
				}
				JSONArray lawyerArray = (JSONArray) attorniesObj;
				@SuppressWarnings("unchecked")
				Iterator<JSONObject> ita = lawyerArray.iterator();
				while (ita.hasNext()) {
					JSONObject att = ita.next();
					String attName = (String) att.get("name");
					attName = attName.trim();
					if (!attorneyList.contains(attName)) {
						attorneyList.add(attName);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void findForwardRefs() {
		for (int i = 0; i < delist.size(); i++) {
			DocketEntry de = delist.get(i);
			if (de.drs != null) {
				for (int j = de.drs.size() - 1; j >= 0; j--) {
					DockRef dr = de.drs.get(j);
					int idx = dr.targetIndex;
					boolean used = false;
					for (int k = i - 1; k >= 0; k--) {
						DocketEntry df = delist.get(k);
						if (df.deIndex == idx) {
							String te = dr.targetEntity;
							if (df.head != null) {
								if (df.head.isKindOf(te)) {
									dr.targetSerial = k;
									df.addForwardRef(dr);
									used = true;
								} else {
									if (onto.isKindOf(te, df.head.getName())) {
										dr.targetSerial = k;
										df.addForwardRef(dr);
										used = true;
									}
								}
							}
						} else if (df.deIndex > 0 && df.deIndex < idx) {
							break;
						}
					}
					if (!used) {
						de.drs.remove(j);
					}
				}
				if (de.drs.size() == 0) {// all ref entries are invalid.
					de.drs = null;
				}
			}
		}
	}

	static class CaseMeta {
		String id;
		String full;
		String filed;
		String report;

		public CaseMeta(String line) {
			String[] split = line.split("\t");
			id = split[0];
			full = split[1];
			filed = split[2];
			report = split[3];
		}

		public String getID() {
			return id;
		}
	}

	static class Lawfirm extends LitiEntity {
		String nameRaw;
		String regex = null;
		String listUnder = null;
		String orgtype; // LLP, LLC, PA, etc
		List<String> partners;
		List<LitiParty> parties;

		public Lawfirm(String name) {
			nameRaw = name;
		}

		public void addParty(LitiParty party) {
			if (parties == null) {
				parties = new ArrayList<LitiParty>();
			}
			if (!parties.contains(party)) {
				parties.add(party);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (o.getClass() != this.getClass()) {
				return false;
			}
			Lawfirm ob = (Lawfirm) o;
			Pattern pt = getPattern();
			Matcher mt = pt.matcher(ob.getNameText());
			if (mt.matches()) {
				return true;
			}
			Pattern opt = ob.getPattern();
			mt = opt.matcher(getNameText());
			if (mt.matches()) {
				return true;
			}
			return false;
		}

		public String getRegex() {
			if (regex == null) {
				String[] reg = EntType.LawFirmNameToRegex(nameRaw);
				regex = reg[1];
				listUnder = reg[0];
			}
			return regex;
		}

		public String getListUnder() {
			if (regex == null) {
				String[] reg = EntType.LawFirmNameToRegex(nameRaw);
				regex = reg[1];
				listUnder = reg[0];
			}
			return listUnder;
		}

		public String getNameText() {
			return nameRaw;
		}

		public String getEntityType() {
			return "Lawfirm";
		}
	}

	public static class LitiParty extends LitiEntity {
		String nameRaw;
		String regex = null;
		String listUnder = null;
		String orgtype; // inc, corp, ltd, LLP, etc.
		List<Lawfirm> lawfirms;
		List<Attorney> attorneys = new ArrayList<Attorney>();
		String litiRole; // Plaintiff, Defendant

		public LitiParty(String raw) {
			nameRaw = raw;
		}

		public void addAttorney(Attorney lawyer) {
			int idx = attorneys.indexOf(lawyer);
			if (idx < 0) {
				attorneys.add(lawyer);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o.getClass() != this.getClass()) {
				return false;
			}
			LitiParty ot = (LitiParty) o;
			return this.nameRaw.equalsIgnoreCase(ot.nameRaw);
		}

		public void setOrgType(String tp) {
			orgtype = tp;
		}

		public String getOrgType() {
			return orgtype;
		}

		public void setLitiRole(String role) {
			litiRole = role;
		}

		public String getLitiRole() {
			return litiRole;
		}

		public String getRegex() {
			if (regex == null) {
				String[] reg = EntType.partyNameToRegex(nameRaw, 0);
				regex = reg[1];
				//				listUnder = reg[0];
				listUnder = this.getFirstToken();
			}
			return regex;
		}

		/**
		 * The best regex is the compromise of two competing demands:
		 * (1) allow short form, example: "LG Electronics USA Inc." can be just "LG"
		 * (2) distinguish among other similar names, like "LG Mobilecomm USA Inc.", etc.
		 * My strategy is to start with just the first token as required, all others as optional. If it does not pick up other party names, its good.
		 * If it does pick up other names, we'll remove one more token. Until it's unique or no more tokens can include.
		 * If when no more tokens to add it can still pick up other, "the other" party is considered the same as this party, and the most versatle
		 * regex will be selected to represent both.
		 * 
		 * @param otherParties
		 * @return
		 */
		public String createRegex(List<String> otherParties) {
			String[] reg = null;
			int significant = 0;
			do {
				reg = EntType.partyNameToRegex(nameRaw, significant);
				if (reg == null) {
					// break here allow the previous good regex to survive.
					break;
				}
				regex = reg[1];
				//				listUnder = reg[0];
				listUnder = this.getFirstToken();
				if (!listUnder.equalsIgnoreCase(reg[0])) {
					listUnder += " " + reg[0];
				}
				Pattern pt = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
				int count = 0;
				for (String pn : otherParties) {
					Matcher m = pt.matcher(pn);
					if (m.find()) {
						count++;
					}
				}
				// if this regex matched other parties names, we need to make it more specific by turning more optional tokens to required tokens.
				if (count > 0) {
					significant++;
				} else {
					break;
				}
			} while (reg != null);
			return regex;
		}

		public String getListUnder() {
			if (regex == null) {
				String[] reg = EntType.partyNameToRegex(nameRaw, 0);
				regex = reg[1];
				//				listUnder = reg[0];
				listUnder = this.getFirstToken();
			}
			return listUnder;
			//			return getFirstToken();
		}

		public String getNameText() {
			return nameRaw;
		}

		public String getEntityType() {
			return "OrgCo";
		}
	}

	static class Attorney extends LitiEntity {
		PersonName pname;
		String ontoText;
		String regex = null;
		String listUnder = null;
		List<Lawfirm> lawfirms;
		List<LitiParty> parties = new ArrayList<LitiParty>();

		public Attorney(PersonName p) {
			pname = p;
		}

		public void addParty(LitiParty p) {
			int idx = parties.indexOf(p);
			if (idx < 0)
				parties.add(p);
		}

		public String getRegex() {
			if (regex == null) {
				String[] reg = pname.getRegex(true);
				regex = reg[1];
				listUnder = reg[0];
			}
			return regex;
		}

		public String getListUnder() {
			if (regex == null) {
				String[] reg = pname.getRegex(true);
				regex = reg[1];
				listUnder = reg[0];
			}
			return listUnder;
		}

		public String getNameText() {
			return pname.getNameRaw();
		}

		public String getEntityType() {
			return "Attorney";
		}

	}

	static class Judge extends LitiEntity {
		PersonName pname;
		String judgetype; // "regular", "magistrate", "special"
		String type = "Judge";
		String regex = null;
		String listUnder = null;

		public Judge(PersonName pn, String t) {
			pname = pn;
			judgetype = t;
		}

		public String getRegex() {
			if (regex == null) {
				String[] reg = pname.getRegex(true);
				regex = reg[1];
				listUnder = reg[0];
			}
			return regex;
		}

		public String getEntityType() {
			return "Judge";
		}

		@Override
		public String getEntityName() {
			return getEntityType() + LexToken.getSingleName(getNameText());
		}

		public String getListUnder() {
			if (regex == null) {
				String[] reg = pname.getRegex(true);
				regex = reg[1];
				listUnder = reg[0];
			}
			return listUnder;
		}

		public String getNameText() {
			return pname.getNameRaw();
		}
	}

	static class PersonName {
		String nameRaw;
		String regex;
		String givenname;
		List<String> middlename = null;
		String surname;
		String suffix = null;
		String title;

		PersonName(String name) throws Exception {
			name = name.trim();
			Matcher m = EntType.ptEntitySuffixEx.matcher(name);
			if (m.find()) {
				throw new Exception("Invalide person name: " + name);
			}
			m = EntType.pJohnDoe.matcher(name);
			if (m.find()) {
				throw new Exception("Invalide person name: " + name);
			}
			m = EntType.pNum.matcher(name);
			if (m.find()) {
				throw new Exception("Invalide person name: " + name);
			}

			nameRaw = name;
			name = name.replaceAll("\\\".+?\\\"|\\(.+?\\)|\\\"\\S+|\\(\\S+|\\[\\S+|\\s\\'", " ").trim();// "Chen Min \"Jack\" Juan ,"
			name = name.replaceAll("(?i)\\b(esq|phd|dr|mr|phv|Rabbi|prof(essors*)?|INACTIVE|MDL\\s*NOT\\s*ADMITT?ED|TRANSFER\\s*ATT(ORNE)?Y)\\b", " ").trim();// " Antonio Valla , Esq.",
			// "Geoffrey Lottenberg , PHV";"Prof. Mark A. Lemley"
			name = name.replaceAll("(-\\s*NA|N/A)\\b", "").trim();// Jere-NA F. White, Jr.
			name = name.replaceAll("(?i)\\.|\\;|\\,|\\:|-(\\s|$)|\\s-|\\+|\\*", " ").trim();// John H. Smith, Jr. => John H Smith, Jr
			if (name.length() == 0) {
				throw new Exception("Invalide attorney name: " + nameRaw);
			}
			String[] split = name.split("\\s+");
			int usableLength = split.length;
			if (split.length > 5) {
				throw new Exception("Invalide attorney name: " + name);
			}
			int firstUse = 0;
			if (split.length > 0) {
				if (EntType.isSuffix(split[split.length - 1])) {
					suffix = split[split.length - 1];
					usableLength--;
				}
			}
			List<String> nm = new ArrayList<String>();
			while (usableLength > 0 && split[usableLength - 1].length() == 1) {
				nm.add(split[usableLength - 1]);
				usableLength--;
			}
			if (usableLength <= 0) {
				throw new Exception("Invalide attorney name: " + nameRaw);
			}
			surname = split[usableLength - 1];
			usableLength--;
			while (usableLength > 0) {
				nm.add(split[usableLength - 1]);
				usableLength--;
			}
			if (nm.size() > 0) {
				givenname = nm.remove(nm.size() - 1);
			}
			if (nm.size() > 0 && middlename == null) {
				middlename = new ArrayList<String>();
			}
			while (nm.size() > 0) {
				middlename.add(nm.remove(nm.size() - 1).toLowerCase());
			}
			if (usableLength > 1) {
				givenname = split[0];
				firstUse++;
			}
			int surnameLength = usableLength - firstUse;
			if (surnameLength > 1) {// van der waals, de Gennis, van Horn
				StringBuilder sb = new StringBuilder();
				for (int i = firstUse; i < usableLength; i++) {
					if (i > firstUse) {
						sb.append(" ");
					}
					sb.append(split[i]);
				}
				surname = sb.toString();
			}
		}

		public String[] getRegex(boolean isJudge) {
			String listUnder;
			if (givenname != null) {
				listUnder = givenname + " " + surname;
			} else {
				listUnder = surname;
			}
			StringBuilder sb = new StringBuilder();
			if (givenname != null) {
				if (isJudge) {
					sb.append("(");
					sb.append(givenname);
					sb.append(")?");
				} else {
					sb.append(givenname);
				}
				if (givenname.length() == 1) {
					sb.append("\\.?");
				}
			}
			if (middlename != null) {
				for (String mid : middlename) {
					sb.append("(\\s*(");
					sb.append(mid);
					if (mid.length() > 1) {
						String midinitial = mid.substring(0, 1);
						sb.append("|" + midinitial);
					}
					sb.append(")\\.?)?");
				}
			} else if (sb.length() > 0) {
				sb.append("(\\s*\\p{Alnum}\\.?)?");
			}
			if (sb.length() > 0) {
				sb.append("\\s*");
			}
			sb.append(surname);
			if (suffix != null) {
				sb.append("(\\,?\\s*");
				sb.append(suffix);
				sb.append("\\.?)?");
			}
			String[] ret = new String[2];
			String regex = sb.toString();
			ret[0] = listUnder;
			ret[1] = regex;
			return ret;
		}

		String getNameRaw() {
			return nameRaw;
		}
	}

	public static List<String> getPartyNames(List<LitiParty> partyList) {
		List<String> partyNames = new ArrayList<String>();
		for (int i = 0; i < partyList.size(); i++) {
			LitiParty pt = partyList.get(i);
			partyNames.add(pt.getNameText());
		}
		return partyNames;
	}

	/**
	 * Parse one case docket report json object, decompose it into lawyer name,
	 * party name, law firm name, street address, phone number, fax number,
	 * email address. Add these entities to global data structures (firmbase,
	 * lawyerbase, addressbase, phonebase, etc.). Also create a MexFirm object
	 * that has links to all these related entities. Right now interaction with
	 * firmbase etc., are done at the lowest end of the calling sequence without
	 * passing these data structures down as arguments, this is possible because
	 * firmbase etc., are class globals (static). I don't like this because it's
	 * not possible to see this effect from arguments of calling functions, and
	 * accessing these data structures require the name of the class, which make
	 * it not modular. We may change it later.
	 * 
	 * @param file
	 */
	public static void parseCaseReport(JSONParser parser, String json, List<Attorney> attorneyList, List<LitiParty> partyList, List<Judge> judgeList) {
		if (json == null) {
			return;
		}
		try {
			json = json.trim();
			if (json.length() == 0) {
				return;
			}
			JSONObject jsonObject = (JSONObject) parser.parse(json);
			if (jsonObject == null) {
				// sometimes, the meta data is not crawled. example: 2000025303, as of 2014.02.04.
				return;
			}
			Object jObject = jsonObject.get("assigned_to");
			if (jObject != null && jObject.getClass().equals(JSONArray.class)) {
				JSONArray ja = (JSONArray) jObject;
				Iterator<JSONObject> iterator = ja.iterator();
				while (iterator.hasNext()) {
					JSONObject jgObject = iterator.next();
					String judgeObject = (String) jgObject.get("text");
					if (judgeObject != null) {//Judge Sue L. Robinson
						String[] split = judgeObject.split("(?i:judge?\\s)");
						if (split != null && split.length > 0) {
							judgeObject = split[split.length - 1];
							PersonName jname = new PersonName(judgeObject.trim());
							Judge jdg = new Judge(jname, "judge");
							judgeList.add(jdg);
						}
					}
				}
			} else {
				try {
					String judgeObject = (String) jsonObject.get("assigned_to");
					if (judgeObject != null) {//Judge Sue L. Robinson
						String[] split = judgeObject.split("(?i:judge?\\s)");
						if (split != null && split.length > 0) {
							judgeObject = split[split.length - 1];
							PersonName jname = new PersonName(judgeObject.trim());
							Judge jdg = new Judge(jname, "judge");
							judgeList.add(jdg);
						}
					}
				} catch (Exception ex) {
					log.debug(ex.getMessage());
				}
			}
			try {
				String judgeObject = (String) jsonObject.get("referred_to");
				if (judgeObject != null) {//Judge Sue L. Robinson
					String[] split = judgeObject.split("(?i:judge?\\s)");
					if (split != null && split.length > 0) {
						judgeObject = split[split.length - 1];
						PersonName jname = new PersonName(judgeObject);
						Judge jdg = new Judge(jname, "magistrate");
						judgeList.add(jdg);
					}
				}
			} catch (Exception ex) {
				log.debug(ex.getMessage());
			}

			Object parties = jsonObject.get("parties");
			if (parties == null) {
				return;
			}
			if (!parties.getClass().equals(JSONArray.class)) {
				return;
			}
			JSONArray partyArray = (JSONArray) parties;
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> iterator = partyArray.iterator();
			while (iterator.hasNext()) {
				JSONObject onePartyRole = iterator.next();
				String partyName = (String) onePartyRole.get("name");
				String partyType = (String) onePartyRole.get("type");
				LitiParty party = addParty(partyList, partyName, partyType);

				Object attorniesObj = onePartyRole.get("attornies");
				if (!attorniesObj.getClass().equals(JSONArray.class)) {
					continue;
				}
				JSONArray lawyerArray = (JSONArray) attorniesObj;
				@SuppressWarnings("unchecked")
				Iterator<JSONObject> ita = lawyerArray.iterator();
				while (ita.hasNext()) {
					JSONObject att = ita.next();
					String attName = (String) att.get("name");
					attName = attName.trim();
					if (attName.length() == 0) {
						continue;
					}
					try {
						PersonName atpname = new PersonName(attName);
						Attorney atn = new Attorney(atpname);
						int idx = attorneyList.indexOf(atn);
						if (idx >= 0) {
							atn = attorneyList.get(idx);
						} else {
							attorneyList.add(atn);
						}
						party.addAttorney(atn);
					} catch (Exception ex) {
						// when a wrong name gets into the attorney field, like case LexID = 2000025193
						// "Duane Morris LLP" gets into the attorney field in meta data, we just ignore it.
						;
					}
				}
			}
			// this is the section that create mutually exclusive party name regexes:
			// This should also be done to lawfirm names since there are mistakes (case 79781, two firms start with Morris)
			List<String> partyNames = getPartyNames(partyList);
			for (int i = 0; i < partyNames.size(); i++) {
				LitiParty pt = partyList.get(i);
				String myName = partyNames.remove(i);
				pt.createRegex(partyNames);
				partyNames.add(i, myName);
			}
			// end create mutually exclusive party name regexes.

			// The following is necessary because often times parties with same "first name" form a joinder party, they are collectively referred as
			// "first name defendants". For example, in case 94238, we have "LG defendants" for "LG Electronics Inc., LG Electronics USA Inc., LG Mobilecomm USA Inc."
			// and 
			// "Kyocera Defendants" to refer to "Kyocera Corporation, Kyocera International Inc., Kyocera Wireless Corp., Kyocera Sanyo Telecom Inc." 
			Map<String, List<LitiParty>> pmap = new HashMap<String, List<LitiParty>>();
			Map<String, List<LitiParty>> dmap = new HashMap<String, List<LitiParty>>();
			Map<String, List<LitiParty>> ptmap;
			for (LitiParty pt : partyList) {
				String tk = pt.getFirstToken();
				String role = pt.getLitiRole();
				if (role == null) {
					log.info("role==null");
				}
				if (role.equals(DocketEntry.ROLE_PLAINTIFF)) {
					ptmap = pmap;
				} else {
					ptmap = dmap;
				}
				List<LitiParty> lpt = ptmap.get(tk);
				if (lpt == null) {
					lpt = new ArrayList<LitiParty>();
					ptmap.put(tk, lpt);
				}
				lpt.add(pt);
			}
			createPartyGroups(pmap, partyList, DocketEntry.ROLE_PLAINTIFF);
			createPartyGroups(dmap, partyList, DocketEntry.ROLE_DEFENDANT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void createPartyGroups(Map<String, List<LitiParty>> pmap, List<LitiParty> partyList, String role) {
		Set<Entry<String, List<LitiParty>>> set = pmap.entrySet();
		for (Entry<String, List<LitiParty>> ent : set) {
			String p = ent.getKey();
			List<LitiParty> lst = ent.getValue();
			if (lst.size() > 1) {
				String pp;
				if (p.length() <= 3) {
					pp = p.toUpperCase();
				} else {
					pp = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
				}
				if (role.equals(DocketEntry.ROLE_PLAINTIFF)) {
					pp += " Plaintiffs";
				} else {
					pp += " Defendants";
				}
				LitiParty party = new LitiParty(pp);
				party.setLitiRole(role);
				party.setChildren(lst);
				partyList.add(party);
			}
		}
	}

	public static void parseCaseReportOld(JSONParser parser, String json, List<Attorney> attorneyList, List<LitiParty> partyList, List<Judge> judgeList) {
		if (json == null) {
			return;
		}
		try {
			json = json.trim();
			if (json.length() == 0) {
				return;
			}
			JSONObject jsonObject = (JSONObject) parser.parse(json);
			if (jsonObject != null) {
				String judgeObject = (String) jsonObject.get("assigned_to");
				if (judgeObject != null) {//Judge Sue L. Robinson
					if (judgeObject.toLowerCase().startsWith("judge")) {
						judgeObject = judgeObject.substring(judgeObject.indexOf(" ")).trim();
					}
					PersonName jname = new PersonName(judgeObject);
					Judge jdg = new Judge(jname, "judge");
					judgeList.add(jdg);
				}
				judgeObject = (String) jsonObject.get("referred_to");
				if (judgeObject != null) {//Judge Sue L. Robinson
					if (judgeObject.toLowerCase().startsWith("judge")) {
						judgeObject = judgeObject.substring(judgeObject.indexOf(" ")).trim();
					}
					PersonName jname = new PersonName(judgeObject);
					Judge jdg = new Judge(jname, "magistrate");
					judgeList.add(jdg);
				}
			}

			Object parties = jsonObject.get("parties");
			if (parties == null) {
				return;
			}
			if (!parties.getClass().equals(JSONArray.class)) {
				return;
			}
			JSONArray partyArray = (JSONArray) parties;
			List<String> pfirst = new ArrayList<String>();
			List<String> dfirst = new ArrayList<String>();
			List<String> ppfirst = new ArrayList<String>();
			List<String> ddfirst = new ArrayList<String>();
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> iterator = partyArray.iterator();
			while (iterator.hasNext()) {
				JSONObject onePartyRole = iterator.next();
				String partyName = (String) onePartyRole.get("name");
				String[] raws = partyName.split("\\s+");
				for (int i = 0; i < raws.length; i++) {
					raws[i] = raws[i].replaceAll("\\p{Punct}", ""); // "Cephalon," ==> "Cephalon", "Inc." ==> "Inc"
				}
				LitiParty party = new LitiParty(partyName);
				int pos = partyList.indexOf(party);
				if (pos < 0) {
					partyList.add(party);
				} else {
					party = partyList.get(pos);
				}
				String partyType = (String) onePartyRole.get("type");
				if (partyType != null) {
					if (partyType.equalsIgnoreCase(DocketEntry.ROLE_PLAINTIFF)) {
						party.setLitiRole(DocketEntry.ROLE_PLAINTIFF);
						pfirst.add(raws[0].toLowerCase());
					}
					if (partyType.equalsIgnoreCase(DocketEntry.ROLE_DEFENDANT)) {
						party.setLitiRole(DocketEntry.ROLE_DEFENDANT);
						dfirst.add(raws[0].toLowerCase());
					}
				}

				Object attorniesObj = onePartyRole.get("attornies");
				if (!attorniesObj.getClass().equals(JSONArray.class)) {
					continue;
				}
				JSONArray lawyerArray = (JSONArray) attorniesObj;
				@SuppressWarnings("unchecked")
				Iterator<JSONObject> ita = lawyerArray.iterator();
				while (ita.hasNext()) {
					JSONObject att = ita.next();
					String attName = (String) att.get("name");
					attName = attName.trim();
					if (attName.length() == 0) {
						continue;
					}
					PersonName atpname = new PersonName(attName);
					Attorney atn = new Attorney(atpname);
					int idx = attorneyList.indexOf(atn);
					if (idx >= 0) {
						atn = attorneyList.get(idx);
					} else {
						attorneyList.add(atn);
					}
					party.addAttorney(atn);
				}
			}
			// this is the section that create mutually exclusive party name regexes:
			// This should also be done to lawfirm names since there are mistakes (case 79781, two firms start with Morris)
			List<String> partyNames = getPartyNames(partyList);
			for (int i = 0; i < partyNames.size(); i++) {
				LitiParty pt = partyList.get(i);
				String myName = partyNames.remove(i);
				pt.createRegex(partyNames);
				partyNames.add(i, myName);
			}
			// end create mutually exclusive party name regexes.

			// The following is necessary because often times parties with same "first name" form a joinder party, they are collectively referred as
			// "first name defendants". For example, in case 94238, we have "LG defendants" for "LG Electronics Inc., LG Electronics USA Inc., LG Mobilecomm USA Inc."
			// and 
			// "Kyocera Defendants" to refer to "Kyocera Corporation, Kyocera International Inc., Kyocera Wireless Corp., Kyocera Sanyo Telecom Inc." 
			while (pfirst.size() > 0) {
				String p = pfirst.remove(0);
				if (pfirst.contains(p)) {// repetition, more than one
					if (!ppfirst.contains(p)) {
						ppfirst.add(p);
					}
				}
			}
			for (String p : ppfirst) {
				String pp;
				if (p.length() <= 3) {
					pp = p.toUpperCase();
				} else {
					pp = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
				}
				LitiParty party = new LitiParty(pp + " Plaintiffs");
				party.setLitiRole(DocketEntry.ROLE_PLAINTIFF);
				partyList.add(party);
			}
			while (dfirst.size() > 0) {
				String d = dfirst.remove(0);
				if (dfirst.contains(d)) {// repetition, more than one
					if (!ddfirst.contains(d)) {
						ddfirst.add(d);
					}
				}
			}
			for (String p : ddfirst) {
				String pp;
				if (p.length() <= 3) {
					pp = p.toUpperCase();
				} else {
					pp = p.substring(0, 1).toUpperCase() + p.substring(1).toLowerCase();
				}
				LitiParty party = new LitiParty(pp + " Defendants");
				party.setLitiRole(DocketEntry.ROLE_DEFENDANT);
				partyList.add(party);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static abstract class LitiEntity implements LitiEntityInterface {
		Pattern ptn = null;
		List<LexToken> tklist = null;
		List<? extends LitiEntity> children = null;

		public void setChildren(List<? extends LitiEntity> ff) {
			children = ff;
		}

		public String getOntoText() {
			return getEntityName() + "\tinstanceOf\t" + getEntityType();
		}

		public String getEntityName() {
			return getEntityType() + LexToken.getSingleName(getNameText());
		}

		@Override
		public String toString() {
			return getEntityName();
		}

		protected void generateTokens() {
			tklist = LexToken.tokenize(this.getNameText());
		}

		public String getFirstToken() {
			if (tklist == null) {
				generateTokens();
			}
			return tklist.get(0).getText().toLowerCase();
		}

		public String getDictText() {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"text\":\"");
			sb.append(getRegex());
			sb.append("\",\"type\":\"NP\",\"list\":\"");
			sb.append(getListUnder());
			sb.append("\",\"entity\":\"");
			sb.append(getEntityName());
			sb.append("\"}\"");
			return sb.toString();
		}

		public Pattern getPattern() {
			if (ptn == null) {
				ptn = Pattern.compile(getRegex(), Pattern.CASE_INSENSITIVE);
			}
			return ptn;
		}

		public boolean matchName(String name) {
			Pattern p = getPattern();
			Matcher m = p.matcher(name);
			return m.matches();
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o.getClass() != this.getClass()) {
				return false;
			}
			LitiEntity a = (LitiEntity) o;
			return getEntityName().equals(a.getEntityName());
		}

	}

	interface LitiEntityInterface {
		String getRegex();

		String getDictText();

		String getOntoText();

		String getEntityName();

		String getListUnder();

		String getNameText();

		String getEntityType();

		String getFirstToken();

		Pattern getPattern();

		void setChildren(List<? extends LitiEntity> ff);

		boolean matchName(String name);
	}

	static class MarkmanEvent {
		String eventdate; // the date from DE text. If no, using DE filing date
		int serial; // DE serial
		int index; // DE index

		public MarkmanEvent(String date, int ser, int idx) {
			serial = ser;
			index = idx;
			eventdate = date;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(serial);
			sb.append(",");
			sb.append(index);
			sb.append(")");
			sb.append("Markman ");
			sb.append(eventdate);
			return sb.toString();
		}

		@SuppressWarnings("unchecked")
		public JSONObject toJSON() {
			JSONObject jo = new JSONObject();
			jo.put("date", eventdate);
			jo.put("serial", Integer.toString(serial));
			jo.put("index", Integer.toString(index));
			return jo;
		}

		public MarkmanEvent(JSONObject jo) {
			eventdate = (String) jo.get("date");
			serial = Integer.valueOf((String) jo.get("serial"));
			index = Integer.valueOf((String) jo.get("index"));
		}
	}

	static class Terminate {
		int serial;
		int index;
	}
}
