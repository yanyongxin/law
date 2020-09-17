package sftrack;

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.conf.ConstraintJittingThresholdOption;
import org.kie.internal.io.ResourceFactory;

import core.Entity;
import core.LegaLanguage;
import core.LexToken;
import core.Phrase;
import court.EntitiesAndCaseDockets;
import legal.FindEntitiesOfCases;
import legal.TrackEntry;
import legal.TrackEntry.DePhrase;
import legal.TrackEntry.Section;
import utils.Pair;

public class SFFilter {
	static LegaLanguage legalang;
	static final int TOP_N = 3;
	static final String rulesFile = "sftrack/docketParse.drl";
	static final String triplesFile = "src/main/resources/sftrack/triples.txt";
	static final String lexiconFile = "src/main/resources/sftrack/lexicon.txt";
	static String[] entityResources = { "C:\\data\\200523\\0713\\sfc_file/ca_sfc_party.tsv",
			"C:\\data\\200523\\0713\\sfc_file/ca_sfc_judge.tsv",
			"C:\\data\\200523\\0713\\sfc_file/ca_sfc_attorney.tsv",
			"\\data\\200523/motions.txt" };

	private static LegaLanguage initializeRuleEngine() {
		try {
			//Loading KieServices:
			KieServices ks = KieServices.Factory.get();
			//Creating KieFileSystem:
			KieFileSystem kfs = ks.newKieFileSystem();
			//Loading rules file: docketParse.drl:
			Resource dd = ResourceFactory.newClassPathResource(rulesFile);
			kfs.write("src/main/resources/sftrack/docketParse.drl", dd);
			KieBuilder kbuilder = ks.newKieBuilder(kfs);
			kbuilder.buildAll();
			KieContainer kcontainer = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
			KieBaseConfiguration kbConfig = KieServices.Factory.get().newKieBaseConfiguration();
			kbConfig.setOption(ConstraintJittingThresholdOption.get(-1));
			KieBase kbase = kcontainer.newKieBase(kbConfig);
			legalang = LegaLanguage.create(kbase, triplesFile, lexiconFile);
		} catch (Exception ex) {
			fail("Knowledge Base loading error!");
		}
		return legalang;
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Initialization ...");
		legalang = SFFilter.initializeRuleEngine();
		EntitiesAndCaseDockets etcd = new EntitiesAndCaseDockets(entityResources);
		int caseCount = 0;
		int entryCount = 0;
		int skipped = 0;
		int cycle = 1000;
		int cycCount = cycle;
		Set<String> triedText = new HashSet<>();
		Set<String> triedSet = new TreeSet<>();
		for (FindEntitiesOfCases cs : etcd.cases) {
			caseCount++;
			for (TrackEntry e : cs.entries) {
				entryCount++;
				if (entryCount >= cycCount) {
					cycCount += cycle;
					System.out.println("caseCount:" + caseCount + "; entryCount:" + entryCount + "; triedSetSize:" + triedSet.size());
				}
				if (e.text.startsWith("Payment")) {
					continue;
				}
				for (Section sec : e.sections) {
					if (sec.dephrases.size() == 0 && sec.doneList.size() == 0)
						continue;
					if (triedText.contains(sec.text)) {
						skipped++;
						continue;
					}
					triedText.add(sec.text);
					try {
						//					Entity.resetSerial();
						List<Phrase> phlist = generatePhraseList(sec);
						StringBuilder sb = new StringBuilder();
						for (int i = 0; i < phlist.size(); i++) {
							Phrase ph = phlist.get(i);
							if (ph.getSentence().get(i).getType() == LexToken.LEX_ENTITY) {
								sb.append("entity_id ");
							} else {
								sb.append(ph.getText().toLowerCase().trim() + " ");
							}
						}
						String sbb = sb.toString().trim();
						//						if (!triedSet.contains(sbb)) {
						triedSet.add(sbb);
						//						}
					} catch (Exception ex) {
						fail(ex.getMessage());
					}
					break;// only do the first section for now. ignore "filed by" and everything after 
				}
			}
			//			break;
		}
		System.out.println("caseCount:" + caseCount + "; entryCount:" + entryCount + "; triedSetSize:" + triedSet.size());
		System.out.println("Skipped: " + skipped);
		BufferedWriter wr = new BufferedWriter(new FileWriter(args[0]));
		for (String s : triedSet) {
			wr.write(s + "\n");
		}
		wr.close();
	}

	static List<Phrase> generatePhraseList(Section sec) {
		List<Phrase> phlist = Collections.synchronizedList(new ArrayList<Phrase>());
		List<LexToken> tokens = new ArrayList<>();
		for (DePhrase p : sec.dephrases) {
			Pair pair = new Pair(Integer.valueOf(p.start), p);
			sec.doneList.add(pair);
		}
		Collections.sort(sec.doneList);// this result in reverse order
		int top = sec.doneList.size() - 1;
		for (int i = top; i >= 0; i--) {
			Pair p = sec.doneList.get(i);
			if (p.o2 instanceof DePhrase) {
				DePhrase dp = (DePhrase) (p.o2);
				LexToken tk = new LexToken(sec.text, dp.text.toLowerCase(), dp.start, dp.end, LexToken.LEX_ENTITY);
				Phrase ph = new Phrase(dp.text.toLowerCase(), tokens.size(), tokens.size() + 1, tokens);
				tokens.add(tk);
				phlist.add(ph);
				ph.setSynType("NP");//Clerk, Judge, Attorney, party
				if (dp.entity instanceof sfmotion.Party) {
					sfmotion.Party party = (sfmotion.Party) dp.entity;
					if (party.type == sfmotion.Party.TYPE_INDIVIDUAL || party.type == sfmotion.Party.TYPE_MINOR) {
						Entity e3 = new Entity(ph.getText(), legalang.getEntity("IndividualParty"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
						ph.setGraph(e3);
					} else if (party.type == sfmotion.Party.TYPE_DOESROESMOES) {
						Entity e3 = new Entity(ph.getText(), legalang.getEntity("GenericParty"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
						ph.setGraph(e3);
					} else {
						Entity e3 = new Entity(ph.getText(), legalang.getEntity("OrgCoParty"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
						ph.setGraph(e3);
					}
				} else if (dp.entity instanceof court.EntitiesAndCaseDockets.Attorney) {
					//					legal.ExtractEntities.Attorney attorney = (legal.ExtractEntities.Attorney)p.entity;
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Attorney"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof court.EntitiesAndCaseDockets.Judge) {
					//					legal.ExtractEntities.Judge judge = (legal.ExtractEntities.Judge)p.entity;
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Judge"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof court.EntitiesAndCaseDockets.Clerk) {
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Clerk"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof court.EntitiesAndCaseDockets.Reporter) {
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Reporter"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof court.EntitiesAndCaseDockets.SFCaseNumber) {
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("SFCaseNumber"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				}
			} else {
				int start = ((Integer) p.o1).intValue();
				List<LexToken> tks = LexToken.tokenize((String) (p.o2));
				int j = tokens.size();
				tokens.addAll(tks);
				for (LexToken tk : tks) {
					tk.start += start; // shift to sentence coordinates
					tk.end += start;
					tk.parent = sec.text;
					Phrase ph = new Phrase(tk.getText().toLowerCase(), j, j + 1, tokens);
					if (tk.getType() == LexToken.LEX_SERIALNUMBER) {
						ph.setSynType("NUMP");
						Entity cls = legalang.getEntity("SerialValue");
						Entity e = new Entity(tk.getText().toLowerCase(), cls, Entity.TYPE_INSTANCE, legalang, j);
						ph.setGraph(e);
					}
					phlist.add(ph);
					j++;
				}
			}
		}
		return phlist;
	}

	static List<Phrase> deToPhrases(String s) {
		List<LexToken> tokens = null;
		tokens = LexToken.tokenize(s);
		List<Phrase> phlist = Collections.synchronizedList(new ArrayList<Phrase>());
		if (tokens != null) {
			for (int i = 0; i < tokens.size(); i++) {
				LexToken tk = tokens.get(i);
				Phrase ph = new Phrase(tk.getText().toLowerCase(), i, i + 1, tokens);
				phlist.add(ph);
			}
		}
		return phlist;
	}
}
