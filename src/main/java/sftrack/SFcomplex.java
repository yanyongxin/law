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
import legal.ExtractEntities;
import sftrack.Ontology.Srunner;

public class SFcomplex {
	static Ontology onto;
	static final int TOP_N = 3;
	static String[] entityResources = { "C:\\data\\191023\\dockets\\judgeparty/ca_sfc_party.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_judge.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_attorney.txt",
			"C:\\data\\191023\\dockets/motions_3.txt" };

	public static void main(String[] args) throws IOException {
		//		if (args.length != 2) {
		//			System.out.println("args: infile outfile");
		//			System.exit(1);
		//		}
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
		for (Case cs : exE.cases) {
			for (Entry e : cs.entries) {
				try {
					Srunner srun = onto.createSrunner(true);
					List<LexToken> tokens = null;
					tokens = LexToken.tokenize(e.text);
					List<Phrase> phlist = Collections.synchronizedList(new ArrayList<Phrase>());
					if (tokens != null) {
						int i = 0;
						while (i < tokens.size()) {
							LexToken tk = tokens.get(i);
							boolean bDe = false;
							for (DePhrase p : e.dephrases) {
								if (tk.start == p.start) {
									int j = i;
									while (j < tokens.size()) {
										LexToken ltk = tokens.get(j);
										if (ltk.end >= p.end) {
											if (ltk.end > p.end) {// John Smiths opposition ...
												LexToken tk1 = ltk.split(p.end);
												tokens.add(j + 1, tk1);
											}
											break;
										} else {
											j++;
										}
									}
									Phrase ph = new Phrase(p.text.toLowerCase(), i, j + 1, tokens);
									phlist.add(ph);
									i = j + 1;
									ph.synType = "NP";//Clerk, Judge, Attorney, party
									if (p.entity instanceof legal.Party) {
										legal.Party party = (legal.Party) p.entity;
										if (party.type == legal.Party.TYPE_INDIVIDUAL || party.type == legal.Party.TYPE_MINOR) {
											Entity e3 = new Entity(ph.getText(), onto.getEntity("HumanName"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
											ph.setGraph(e3);
										} else if (party.type == legal.Party.TYPE_DOESROESMOES) {
											Entity e3 = new Entity(ph.getText(), onto.getEntity("IntelName"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
											ph.setGraph(e3);
										} else {
											Entity e3 = new Entity(ph.getText(), onto.getEntity("OrgCoName"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
											ph.setGraph(e3);
										}
									} else if (p.entity instanceof legal.ExtractEntities.Attorney) {
										//					legal.ExtractEntities.Attorney attorney = (legal.ExtractEntities.Attorney)p.entity;
										Entity e3 = new Entity(ph.getText(), onto.getEntity("Attorney"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
										ph.setGraph(e3);
									} else if (p.entity instanceof legal.ExtractEntities.Judge) {
										//					legal.ExtractEntities.Judge judge = (legal.ExtractEntities.Judge)p.entity;
										Entity e3 = new Entity(ph.getText(), onto.getEntity("Judge"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
										ph.setGraph(e3);
									} else if (p.entity instanceof legal.ExtractEntities.Clerk) {
										Entity e3 = new Entity(ph.getText(), onto.getEntity("Clerk"), Entity.TYPE_INSTANCE, onto, ph.getBegToken());
										ph.setGraph(e3);
									}
									bDe = true;
									break;
								}
							}
							if (!bDe) {
								Phrase ph = new Phrase(tk.text.toLowerCase(), i, i + 1, tokens);
								phlist.add(ph);
								i++;
							}
						}
					}
					//					List<Phrase> phlist = CaseData.deToPhrases(test2, false);
					srun.insertList(phlist);
					srun.execute();
					Map<Integer, List<Phrase>> rpmap = srun.findAllPhrases();
					List<Integer> keylist = Analysis.buildKeyList(rpmap);
					//			assertTrue(keylist.size() > 0);
					keylist.add(tokens.size());
					ArrayList<Integer> segments = new ArrayList<Integer>();
					List<List<Analysis>> lla = Analysis.findBestNew(rpmap, keylist, TOP_N, segments);
					List<Phrase> plist = DocketEntry.getPhraseList(lla);
					System.out.println(e.text);
					for (Phrase ph : plist) {
						System.out.println(ph.pprint("", false));
					}
					ERGraph g = plist.get(0).getGraph();
					srun.dispose();
				} catch (Exception ex) {
					fail(ex.getMessage());
				}
			}
			break;
		}

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
