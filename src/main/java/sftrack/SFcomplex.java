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

import sfmotion.EntitiesAndCaseDockets;
import sfmotion.LegalCase;
import sfmotion.TrackEntry;
import sfmotion.TrackEntry.DePhrase;
import sfmotion.TrackEntry.Section;
import sftrack.LegaLanguage.Srunner;
import utils.Pair;

public class SFcomplex {
	static LegaLanguage legalang;
	static final int TOP_N = 3;
	static String[] entityResources = { "C:\\data\\191023\\dockets\\judgeparty/ca_sfc_party.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_judge.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_attorney.txt",
			"C:\\data\\191023\\dockets/testline.txt" };

	public static void main(String[] args) throws IOException {
		System.out.println("Initialization ...");
		legalang = LegaLanguage.initializeRuleEngine();
		EntitiesAndCaseDockets etcd = new EntitiesAndCaseDockets(entityResources);
		compareTwoEntries(etcd);
		//		parseAllEntries(etcd);
	}

	static void compareTwoEntries(EntitiesAndCaseDockets etcd) {
		LegalCase cs = etcd.cases.get(0);
		TrackEntry e1 = cs.entries.get(0);
		TrackEntry e2 = cs.entries.get(1);

		Section sec1 = e1.sections.get(0);
		Section sec2 = e2.sections.get(0);
		Map<Integer, List<Phrase>> rpmap1 = null;
		List<Phrase> phlist1 = null;
		ArrayList<Integer> segments1 = null;
		List<List<Analysis>> lla1 = null;
		List<Phrase> plist1 = null;
		try {
			Srunner srun = legalang.createSrunner(true);
			phlist1 = generatePhraseList(sec1);
			srun.insertList(phlist1);
			srun.execute();
			rpmap1 = srun.findAllPhrases();
			if (rpmap1.size() > 0) {
				List<Integer> keylist = Analysis.buildKeyList(rpmap1);
				keylist.add(phlist1.get(phlist1.size() - 1).endToken);
				segments1 = new ArrayList<Integer>();
				lla1 = Analysis.findBestNew(rpmap1, keylist, TOP_N, segments1);
				plist1 = DocketEntry.getPhraseList(lla1);
				//						System.out.println(e.text);
				for (Phrase ph : plist1) {
					System.out.println(ph.pprint("", false));
				}
				//ERGraph g = plist.get(0).getGraph();
			} else {
				System.out.println("No phrase found!");
			}
			srun.dispose();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}

		Map<Integer, List<Phrase>> rpmap2 = null;
		List<Phrase> phlist2 = null;
		ArrayList<Integer> segments2 = null;
		List<List<Analysis>> lla2 = null;
		List<Phrase> plist2 = null;
		try {
			Srunner srun = legalang.createSrunner(true);
			phlist2 = generatePhraseList(sec2);
			srun.insertList(phlist2);
			srun.execute();
			rpmap2 = srun.findAllPhrases();
			if (rpmap2.size() > 0) {
				List<Integer> keylist = Analysis.buildKeyList(rpmap2);
				keylist.add(phlist2.get(phlist2.size() - 1).endToken);
				segments2 = new ArrayList<Integer>();
				lla2 = Analysis.findBestNew(rpmap2, keylist, TOP_N, segments2);
				plist2 = DocketEntry.getPhraseList(lla2);
				//						System.out.println(e.text);
				for (Phrase ph : plist2) {
					System.out.println(ph.pprint("", false));
				}
				//ERGraph g = plist.get(0).getGraph();
			} else {
				System.out.println("No phrase found!");
			}
			srun.dispose();
		} catch (Exception ex) {
			fail(ex.getMessage());
		}

	}

	static void compareEntries(EntitiesAndCaseDockets etcd) {
		LegalCase cs = etcd.cases.get(0);
		for (TrackEntry e : cs.entries) {
			for (Section sec : e.sections) {
				if (sec.dephrases.size() == 0 && sec.doneList.size() == 0)
					continue;
				try {
					//					Entity.resetSerial();
					Srunner srun = legalang.createSrunner(true);
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
						//ERGraph g = plist.get(0).getGraph();
					} else {
						System.out.println("No phrase found!");
					}
					srun.dispose();
				} catch (Exception ex) {
					fail(ex.getMessage());
				}
			}
		}
	}

	static void parseAllEntries(EntitiesAndCaseDockets etcd) {
		int caseCount = 0;
		int skipped = 0;
		List<String> triedText = new ArrayList<>();
		List<String> skipText = new ArrayList<>();
		for (LegalCase cs : etcd.cases) {
			caseCount++;
			String csid = cs.getID();
			System.out.println("\n================ " + csid + " ==================\n");
			//			CaseParties cp = etcd.parties.get(cs.getID());
			//			List<Party> parties = cp.getParties();
			//			for (Party pt : parties) {
			//				System.out.println(pt);
			//			}
			//			CaseAttorneys ats = etcd.attorneys.get(cs.getID());
			//			List<Attorney> attorneys = ats.getAttorneys();
			//			for (Attorney at : attorneys) {
			//				System.out.println(at.toNamePattern());
			//			}
			for (TrackEntry e : cs.entries) {
				if (e.text.startsWith("Payment")) {
					continue;
				}
				String printed = cs.getID() + "\t" + e.getDate() + "\t" + e.text;
				boolean bP = false;
				for (Section sec : e.sections) {
					if (sec.dephrases.size() == 0 && sec.doneList.size() == 0)
						continue;
					if (triedText.contains(sec.text)) {
						skipped++;
						skipText.add(sec.text);
						continue;
					}
					triedText.add(sec.text);
					if (!bP) {
						System.out.println(printed);
						bP = true;
					}
					try {
						//					Entity.resetSerial();
						Srunner srun = legalang.createSrunner(true);
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
							//ERGraph g = plist.get(0).getGraph();
						} else {
							System.out.println("No phrase found!");
						}
						srun.dispose();
					} catch (Exception ex) {
						fail(ex.getMessage());
					}
					break;// only do the first section for now. ignore "filed by" and everything after 
				}
			}
		}
		System.out.println("CaseCount: " + caseCount);
		System.out.println("Skipped: " + skipped);
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
				} else if (dp.entity instanceof sfmotion.EntitiesAndCaseDockets.Attorney) {
					//					legal.ExtractEntities.Attorney attorney = (legal.ExtractEntities.Attorney)p.entity;
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Attorney"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof sfmotion.EntitiesAndCaseDockets.Judge) {
					//					legal.ExtractEntities.Judge judge = (legal.ExtractEntities.Judge)p.entity;
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Judge"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof sfmotion.EntitiesAndCaseDockets.Clerk) {
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Clerk"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof sfmotion.EntitiesAndCaseDockets.Reporter) {
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Reporter"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof sfmotion.EntitiesAndCaseDockets.SFCaseNumber) {
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
					Phrase ph = new Phrase(tk.text.toLowerCase(), j, j + 1, tokens);
					if (tk.type == LexToken.LEX_SERIALNUMBER) {
						ph.setSynType("NUMP");
						Entity cls = legalang.getEntity("SerialValue");
						Entity e = new Entity(tk.text.toLowerCase(), cls, Entity.TYPE_INSTANCE, legalang, j);
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
