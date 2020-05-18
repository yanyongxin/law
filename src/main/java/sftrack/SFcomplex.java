package sftrack;

import static org.junit.Assert.fail;

import java.io.IOException;

//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.conf.ConstraintJittingThresholdOption;
import org.kie.internal.io.ResourceFactory;

import legal.Case;
import legal.Entry;
import legal.Entry.DePhrase;
import legal.Entry.Section;
import legal.ExtractEntities;
import sftrack.Ontology.Srunner;
import utils.Pair;

public class SFcomplex {
	static Ontology onto;
	static final int TOP_N = 3;
	static String[] entityResources = { "C:\\data\\191023\\dockets\\judgeparty/ca_sfc_party.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_judge.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_attorney.txt",
			"C:\\data\\191023\\dockets/testline.txt" };

	public static void main(String[] args) throws IOException {
		System.out.println("Initialization ...");
		try {
			System.out.println("Loading KieServices: ");
			KieServices ks = KieServices.Factory.get();
			System.out.println("Creating KieFileSystem: ");
			KieFileSystem kfs = ks.newKieFileSystem();
			System.out.println("Loading genome/thousand.drl: ");
			Resource dd = ResourceFactory.newClassPathResource("sftrack/thousand.drl");
			System.out.println("kfs.write sftrack/thousand.drl: ");
			kfs.write("src/main/resources/sftrack/thousand.drl", dd);
			KieBuilder kbuilder = ks.newKieBuilder(kfs);
			kbuilder.buildAll();
			KieContainer kcontainer = ks.newKieContainer(kbuilder.getKieModule().getReleaseId());
			KieBaseConfiguration kbConfig = KieServices.Factory.get().newKieBaseConfiguration();
			kbConfig.setOption(ConstraintJittingThresholdOption.get(-1));
			KieBase kbase = kcontainer.newKieBase(kbConfig);
			Ontology.LoadWorld("src/main/resources/sftrack/genericOnto.txt", "src/main/resources/sftrack/genericdict.txt");
			onto = Ontology.createOntology(kbase);
		} catch (Exception ex) {
			fail("Knowledge Base loading error!");
		}
		//		String testText = "Complaint against Comcast with jury demand by General Electric Company";
		//		String test1 = "MOTION TO STRIKE COMPLAINT FILED BY DEFENDANT HYUNDAI MOTOR AMERICA HEARING SET FOR JUL-18-2017 AT 02:00 PM IN DEPT 302";
		//		String test2 = "MEMORANDUM OF POINTS AND AUTHORITIES AND DEMURRER TO FIRST AMENDED COMPLAINT FILED BY DEFENDANT HYUNDAI MOTOR AMERICA";
		ExtractEntities exE = new ExtractEntities(entityResources);
		int caseCount = 0;
		for (Case cs : exE.cases) {
			caseCount++;
			//			if (caseCount < 2)
			//				continue;
			System.out.println("\n================ " + cs.getID() + " ==================\n");
			//			CaseParties cp = exE.parties.get(cs.getID());
			//			List<Party> parties = cp.getParties();
			//			for (Party pt : parties) {
			//				System.out.println(pt);
			//			}
			for (Entry e : cs.entries) {
				if (e.text.startsWith("Payment")) {
					continue;
				}
				System.out.println(cs.getID() + "\t" + e.getDate() + "\t" + e.text);
				for (Section sec : e.sections) {
					if (sec.dephrases.size() == 0 && sec.doneList.size() == 0)
						continue;
					try {
						//					Entity.resetSerial();
						Srunner srun = onto.createSrunner(true);
						List<Phrase> phlist = generatePhraseList(sec);
						srun.insertList(phlist);
						srun.execute();
						Map<Integer, List<Phrase>> rpmap = srun.findAllPhrases();
						if (rpmap.size() > 0) {
							List<Integer> keylist = Analysis.buildKeyList(rpmap);
							//			assertTrue(keylist.size() > 0);
							//					keylist.add(tokens.size());
							keylist.add(phlist.get(phlist.size() - 1).endToken);
							ArrayList<Integer> segments = new ArrayList<Integer>();
							List<List<Analysis>> lla = Analysis.findBestNew(rpmap, keylist, TOP_N, segments);
							List<Phrase> plist = DocketEntry.getPhraseList(lla);
							//						System.out.println(e.text);
							for (Phrase ph : plist) {
								System.out.println(ph.pprint("", false));
							}
							//							ERGraph g = plist.get(0).getGraph();
						} else {
							System.out.println("No phrase found!");
						}
						srun.dispose();
					} catch (Exception ex) {
						fail(ex.getMessage());
					}
				}
			}
			//			break;
		}
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
				ph.synType = "NP";//Clerk, Judge, Attorney, party
				if (dp.entity instanceof legal.Party) {
					legal.Party party = (legal.Party) dp.entity;
					if (party.type == legal.Party.TYPE_INDIVIDUAL || party.type == legal.Party.TYPE_MINOR) {
						Entity e3 = new Entity(ph.getText(), onto.getEntity("IndividualParty"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
						ph.setGraph(e3);
					} else if (party.type == legal.Party.TYPE_DOESROESMOES) {
						Entity e3 = new Entity(ph.getText(), onto.getEntity("GenericParty"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
						ph.setGraph(e3);
					} else {
						Entity e3 = new Entity(ph.getText(), onto.getEntity("OrgCoParty"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
						ph.setGraph(e3);
					}
				} else if (dp.entity instanceof legal.ExtractEntities.Attorney) {
					//					legal.ExtractEntities.Attorney attorney = (legal.ExtractEntities.Attorney)p.entity;
					Entity e3 = new Entity(ph.getText(), onto.getEntity("Attorney"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof legal.ExtractEntities.Judge) {
					//					legal.ExtractEntities.Judge judge = (legal.ExtractEntities.Judge)p.entity;
					Entity e3 = new Entity(ph.getText(), onto.getEntity("Judge"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof legal.ExtractEntities.Clerk) {
					Entity e3 = new Entity(ph.getText(), onto.getEntity("Clerk"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof legal.ExtractEntities.Reporter) {
					Entity e3 = new Entity(ph.getText(), onto.getEntity("Reporter"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
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
					Phrase ph = new Phrase(tk.text.toLowerCase(), j, j + 1, tokens);
					if (tk.type == LexToken.LEX_SERIALNUMBER) {
						ph.setSynType("NUMP");
						Entity cls = onto.getEntity("SerialValue");
						Entity e = new Entity(tk.text.toLowerCase(), cls, Entity.TYPE_INSTANCE, onto, j);
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
				Phrase ph = new Phrase(tk.text.toLowerCase(), i, i + 1, tokens);
				phlist.add(ph);
			}
		}
		return phlist;
	}
}
