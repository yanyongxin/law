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
			"C:\\data\\191023\\dockets/testline_1.txt" };

	public static void main(String[] args) throws IOException {
		System.out.println("Initialization ...");
		legalang = LegaLanguage.initializeRuleEngine();
		EntitiesAndCaseDockets etcd = new EntitiesAndCaseDockets(entityResources);
		compareTwoEntries(etcd);
		//	parseAllEntries(etcd);
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
		List<ComparePhrases> cps = new ArrayList<>();
		for (int i = 0; i < plist1.size(); i++) {
			Phrase p1 = plist1.get(i);
			for (int j = 0; j < plist2.size(); j++) {
				Phrase p2 = plist2.get(j);
				ComparePhrases cp = new ComparePhrases(sec1, p1, sec2, p2);
				cps.add(cp);
			}
		}
		Collections.sort(cps);
		for (int i = 0; i < cps.size(); i++) {
			ComparePhrases cp = cps.get(i);
			System.out.println(i + ":" + cp + "\n");
		}
	}

	static boolean compareEntity(Entity e1, Entity e2) {
		if (e1.entityType == e2.entityType) {
			if (e1.theClass.name.equals(e2.theClass.name))
				return true;
			else
				return false;
		}
		return false;
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
					if (tk.type == LexToken.LEX_SERIALNUMBER && !tk.text.equalsIgnoreCase("1ST") && !tk.text.equalsIgnoreCase("2ND")) {
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

	static class ComparePhrases implements Comparable<ComparePhrases> {
		int score;
		Section sec1, sec2;
		Phrase ph1, ph2;
		List<List<Link>> graphs = new ArrayList<>(); // linked graphs
		List<Pair> entityPairs = new ArrayList<>();
		List<Pair> linkPairs = new ArrayList<>();
		List<String> hiLinks = new ArrayList<>();// both graph has shared node

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Score: " + score + "\n");
			for (int i = 0; i < graphs.size(); i++) {
				List<Link> g = graphs.get(i);
				sb.append("graph_" + i + "\n");
				for (Link lk : g) {
					sb.append("\t" + lk.toString() + "\n");
				}
			}
			return sb.toString();
		}

		public ComparePhrases(Section _s1, Phrase _p1, Section _s2, Phrase _p2) {
			sec1 = _s1;
			sec2 = _s2;
			ph1 = _p1;
			ph2 = _p2;
			compareGraphs(ph1.graph, ph2.graph);
		}

		private void compareGraphs(ERGraph g1, ERGraph g2) {
			// 1. find corresponding entities
			for (int i = 0; i < g1.entities.size(); i++) {
				Entity e1 = g1.entities.get(i);
				for (int j = 0; j < g2.entities.size(); j++) {
					Entity e2 = g2.entities.get(j);
					if (e1.entityType == e2.entityType
							&& e1.theClass.name.equals(e2.theClass.name)) {
						Pair p = new Pair(e1, e2);
						entityPairs.add(p);
					}
				}
			}
			// 2. find corresponding links
			if (g1.links != null && g2.links != null)
				for (int i = 0; i < g1.links.size(); i++) {
					Link lk1 = g1.links.get(i);
					for (int j = 0; j < g2.links.size(); j++) {
						Link lk2 = g2.links.get(j);
						if (lk1.type.equals(lk2.type)) {
							Pair p1 = new Pair(lk1.e1, lk2.e1);
							if (!entityPairs.contains(p1))
								continue;
							Pair p2 = new Pair(lk1.e2, lk2.e2);
							if (!entityPairs.contains(p2))
								continue;
							Pair p = new Pair(lk1, lk2);
							linkPairs.add(p);
						}
					}
				}
			// 3. find corresponding high links
			for (int i = 0; i < linkPairs.size() - 1; i++) {
				Pair p1 = linkPairs.get(i);
				Link lk11 = (Link) p1.o1;
				Link lk12 = (Link) p1.o2;
				for (int j = i + 1; j < linkPairs.size(); j++) {
					Pair p2 = linkPairs.get(j);
					Link lk21 = (Link) p2.o1;
					Link lk22 = (Link) p2.o2;
					if (lk11.e1 == lk21.e1) {// first graph has a shared node
						String s = "" + i + "" + j + "11";
						if (lk12.e1 == lk22.e1) {// the second graphs same
							hiLinks.add(s);
						}
					} else if (lk11.e1 == lk21.e2) {
						String s = "" + i + "" + j + "12";
						if (lk12.e1 == lk22.e2) {// the second graphs same
							hiLinks.add(s);
						}
					} else if (lk11.e2 == lk21.e1) {// first graph has a shared node
						String s = "" + i + "" + j + "21";
						if (lk12.e2 == lk22.e1) {// the second graphs same
							hiLinks.add(s);
						}
					} else if (lk11.e2 == lk21.e2) {
						String s = "" + i + "" + j + "22";
						if (lk12.e2 == lk22.e2) {// the second graphs same
							hiLinks.add(s);
						}
					}
				}
			}
			// find continuous graphs:
			for (int i = 0; i < hiLinks.size(); i++) {
				String s = hiLinks.get(i);
				int ilk_1 = Integer.parseInt(s.substring(0, 1));
				int ilk_2 = Integer.parseInt(s.substring(1, 2));
				Link lk1 = (Link) linkPairs.get(ilk_1).o1;
				Link lk2 = (Link) linkPairs.get(ilk_2).o1;
				List<Link> grf1 = null;
				List<Link> grf2 = null;
				for (List<Link> grf : graphs) {
					if (grf.contains(lk1)) {
						grf1 = grf;
						break;
					}
				}
				for (List<Link> grf : graphs) {
					if (grf.contains(lk2)) {
						grf2 = grf;
						break;
					}
				}
				if (grf1 != null && grf2 != null) {
					if (grf1 != grf2) { // combine
						grf1.addAll(grf2);
						graphs.remove(grf2);
					}
				} else if (grf1 != null) {
					grf1.add(lk2);
				} else if (grf2 != null) {
					grf2.add(lk1);
				} else {
					List<Link> g = new ArrayList<>();
					g.add(lk1);
					g.add(lk2);
					graphs.add(g);
				}
			}
			// finally, calculating score:
			int shareEntity = entityPairs.size();
			int shareLinks = linkPairs.size();
			int shareHiLinks = hiLinks.size();
			score = shareEntity + 2 * shareLinks + 3 * shareHiLinks;
		}

		@Override
		public int compareTo(ComparePhrases o) {
			return o.score - score;
		}

	}
}
