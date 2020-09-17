package apps;

import static org.junit.Assert.fail;

import java.io.IOException;

//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import core.Analysis;
import core.ERGraph;
import core.Entity;
import core.LegaLanguage;
import core.LegaLanguage.Srunner;
import core.LexToken;
import core.Link;
import core.Phrase;
import court.EntitiesAndCaseDockets;
import legal.LegalCase;
import legal.TrackEntry;
import legal.TrackEntry.DePhrase;
import legal.TrackEntry.Section;
import utils.Pair;

public class SFcomplex {
	static final String rulesFile = "sftrack/docketParse.drl";
	static final String triplesFile = "src/main/resources/sftrack/triples.txt";
	static final String lexiconFile = "src/main/resources/sftrack/lexicon.txt";
	static LegaLanguage legalang;
	static final int TOP_N = 3;
	static String[] entityResources = { "C:\\data\\191023\\dockets\\judgeparty/ca_sfc_party.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_judge.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_attorney.txt",
			"C:\\data\\191023\\dockets/testline_1.txt" };

	public static void main(String[] args) throws IOException {
		System.out.println("Initialization ...");
		legalang = LegaLanguage.initializeRuleEngine(rulesFile, triplesFile, lexiconFile);
		EntitiesAndCaseDockets etcd = new EntitiesAndCaseDockets(entityResources);
		//		compareTwoEntries(etcd);
		long starttime = System.currentTimeMillis();
		parseAllEntries(etcd);
		long endTime = System.currentTimeMillis() - starttime;
		System.out.println("Total time(ms): " + endTime);
	}

	static ComparePhrases compareTwoEntries(EntitiesAndCaseDockets etcd) {
		LegalCase cs = etcd.cases.get(0);
		TrackEntry e1 = cs.entries.get(0);
		TrackEntry e2 = cs.entries.get(1);

		Section sec1 = e1.sections.get(0);
		Section sec2 = e2.sections.get(0);
		return compareTwoSections(sec1, sec2);
	}

	static ComparePhrases compareTwoSections(Section sec1, Section sec2) {
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
				keylist.add(phlist1.get(phlist1.size() - 1).getEndToken());
				segments1 = new ArrayList<Integer>();
				lla1 = Analysis.findBestNew(rpmap1, keylist, TOP_N, segments1);
				plist1 = Analysis.getPhraseList(lla1);
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
				keylist.add(phlist2.get(phlist2.size() - 1).getEndToken());
				segments2 = new ArrayList<Integer>();
				lla2 = Analysis.findBestNew(rpmap2, keylist, TOP_N, segments2);
				plist2 = Analysis.getPhraseList(lla2);
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
		if (cps.size() > 0)
			return cps.get(0);
		else
			return null;
		//		for (int i = 0; i < cps.size(); i++) {
		//			ComparePhrases cp = cps.get(i);
		//			System.out.println(i + ":" + cp + "\n");
		//		}

	}

	/**
	 * Find common graph parts of two sentence sections using "best parses" 
	 * @param sec1
	 * @param sec2
	 * @return
	 */
	static List<ComparePhrases> compareTwoPhraseLists(Section sec1, Section sec2) {
		List<Phrase> plist1 = sec1.plist;
		List<Phrase> plist2 = sec2.plist;
		List<ComparePhrases> cps = new ArrayList<>();
		for (int i = 0; i < plist1.size(); i++) {
			Phrase p1 = plist1.get(i);
			for (int j = 0; j < plist2.size(); j++) {
				Phrase p2 = plist2.get(j);
				ComparePhrases cp = new ComparePhrases(sec1, p1, sec2, p2);
				if (cp.score > 0)
					cps.add(cp);
			}
		}
		if (cps.size() == 0)
			return null;
		Collections.sort(cps);
		return cps;
	}

	static List<ComparePhrases> compareTwoPhraseLists(List<Phrase> plist1, List<Phrase> plist2) {
		List<ComparePhrases> cps = new ArrayList<>();
		for (int i = 0; i < plist1.size(); i++) {
			Phrase p1 = plist1.get(i);
			for (int j = 0; j < plist2.size(); j++) {
				Phrase p2 = plist2.get(j);
				ComparePhrases cp = new ComparePhrases(p1, p2);
				if (cp.score > 0)
					cps.add(cp);
			}
		}
		if (cps.size() == 0)
			return null;
		Collections.sort(cps);
		return cps;
	}

	/**
	 * Find common graph parts of two sentence sections using "all possible parses" 
	 * @param sec1
	 * @param sec2
	 * @return
	 */
	static List<ComparePhrases> compareTwoPhraseListsComplete(Section sec1, Section sec2) {
		Map<Integer, List<Phrase>> map1 = sec1.rpmap;
		Map<Integer, List<Phrase>> map2 = sec2.rpmap;
		List<ComparePhrases> cps = new ArrayList<>();
		for (List<Phrase> list1 : map1.values()) {
			for (List<Phrase> list2 : map2.values()) {
				for (int i = 0; i < list1.size(); i++) {
					Phrase p1 = list1.get(i);
					for (int j = 0; j < list2.size(); j++) {
						Phrase p2 = list2.get(j);
						ComparePhrases cp = new ComparePhrases(sec1, p1, sec2, p2);
						cps.add(cp);
					}
				}
			}
		}
		Collections.sort(cps);
		return cps;
	}

	static boolean compareEntity(Entity e1, Entity e2) {
		if (e1.getEntityType() == e2.getEntityType()) {
			if (e1.getTheClass().getName().equals(e2.getTheClass().getName()))
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
						keylist.add(phlist.get(phlist.size() - 1).getEndToken());
						ArrayList<Integer> segments = new ArrayList<Integer>();
						List<List<Analysis>> lla = Analysis.findBestNew(rpmap, keylist, TOP_N, segments);
						List<Phrase> plist = Analysis.getPhraseList(lla);
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
		HashSet<String> usedText = new HashSet<>();
		//		List<String> triedText = new ArrayList<>();
		List<String> skipText = new ArrayList<>();
		Entity.getGlobalid();
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
				Entity.resetSerial();
				if (e.text.startsWith("Payment")) {
					continue;
				}
				String printed = cs.getID() + "\t" + e.getDate() + "\t" + e.text;
				boolean bP = false;
				Srunner srun = legalang.createSrunner(true);
				for (Section sec : e.sections) {
					if (sec.dephrases.size() == 0 && sec.doneList.size() == 0)
						continue;
					if (usedText.contains(sec.text)) {
						//						if (triedText.contains(sec.text)) {
						skipped++;
						skipText.add(sec.text);
						continue;
					}
					usedText.add(sec.text);
					if (!bP) {
						//						System.out.println(printed);
						bP = true;
					}
					try {
						//					Entity.resetSerial();
						//						Srunner srun = legalang.createSrunner(true);
						List<Phrase> phlist = generatePhraseList(sec);
						srun.insertList(phlist);
						srun.execute();
						Map<Integer, List<Phrase>> rpmap = srun.findAllPhrases();
						if (rpmap.size() > 0) {
							List<Integer> keylist = Analysis.buildKeyList(rpmap);
							//			assertTrue(keylist.size() > 0);
							//					keylist.add(tokens.size());
							keylist.add(phlist.get(phlist.size() - 1).getEndToken());
							ArrayList<Integer> segments = new ArrayList<Integer>();
							List<List<Analysis>> lla = Analysis.findBestNew(rpmap, keylist, TOP_N, segments);
							List<Phrase> plist = Analysis.getPhraseList(lla);
							System.out.println(e.text);
							for (Phrase ph : plist) {
								System.out.println(ph.pprint("", false));
							}
							//ERGraph g = plist.get(0).getGraph();
						} else {
							System.out.println("No phrase found!");
						}
						srun.reset();
						//						srun.dispose();
					} catch (Exception ex) {
						fail(ex.getMessage());
					}
					break;// only do the first section for now. ignore "filed by" and everything after 
				}
				srun.dispose();
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
				ph.setSynType("NP");//Clerk, Judge, Attorney, party
				if (dp.entity instanceof sfmotion.Party) {
					sfmotion.Party party = (sfmotion.Party) dp.entity;
					party.createEntity(legalang);
					if (party.type == sfmotion.Party.TYPE_INDIVIDUAL || party.type == sfmotion.Party.TYPE_MINOR) {
						Entity e3 = new Entity(ph.getText(), party.entity, Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
						ph.setGraph(e3);
					} else if (party.type == sfmotion.Party.TYPE_DOESROESMOES) {
						Entity e3 = new Entity(ph.getText(), party.entity, Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
						ph.setGraph(e3);
					} else {
						Entity e3 = new Entity(ph.getText(), party.entity, Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
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
					if (tk.getType() == LexToken.LEX_SERIALNUMBER && !tk.getText().equalsIgnoreCase("1ST") && !tk.getText().equalsIgnoreCase("2ND")) {
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

	static class ComparePhrases implements Comparable<ComparePhrases>, Comparator<MatchGraph> {
		int score;
		Section sec1, sec2;
		Phrase ph1, ph2;
		List<MatchGraph> graph = new ArrayList<>(); // linked graphs
		List<Pair> entityPairs = new ArrayList<>();
		List<Pair> linkPairs = new ArrayList<>();
		List<String> hiLinks = new ArrayList<>();// both graph has shared node

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Score: " + score + "\n");
			for (int i = 0; i < graph.size(); i++) {
				MatchGraph g = graph.get(i);
				sb.append("graph_" + i + "\n");
				sb.append(g.toString());
			}
			return sb.toString();
		}

		public ComparePhrases(Section _s1, Phrase _p1, Section _s2, Phrase _p2) {
			sec1 = _s1;
			sec2 = _s2;
			ph1 = _p1;
			ph2 = _p2;
			findMatchingGraphParts(ph1.getGraph(), ph2.getGraph());
		}

		public ComparePhrases(Phrase _p1, Phrase _p2) {
			ph1 = _p1;
			ph2 = _p2;
			findMatchingGraphParts(ph1.getGraph(), ph2.getGraph());
		}

		private boolean isNoticeOfMotionLink(Link lk) {
			if (lk.getArg1().isKindOf("DocLegalNotice") && lk.getArg2().isKindOf("DocLegalMotion")) {
				return true;
			}
			return false;
		}

		private boolean isMotionLink(Link lk) {
			if (lk.getArg1().isKindOf("DocLegalMotion") || lk.getArg1().isKindOf("DocLegalDemurrer") || lk.getArg1().isKindOf("DocApplication")) {
				if (lk.getType().equalsIgnoreCase("hasAttribute") || lk.getType().equalsIgnoreCase("hasOwner") || lk.getType().equalsIgnoreCase("ugoerOfProc"))
					return false;
				return true;
			}
			return false;
		}

		private static int motionMaxID(List<Link> lks) {
			int maxID = 0;
			for (Link lk : lks) {
				Entity e1 = lk.getArg1();
				if (e1.isSet())
					continue;
				if (e1.isKindOf("DocLegalMotion") || e1.isKindOf("DocLegalDemurrer") || e1.isKindOf("DocApplication")) {
					if (e1.getID() > maxID) {
						maxID = e1.getID();
					}
				}
			}
			return maxID;
		}

		private boolean isMotionGraph(List<Link> g) {
			for (Link lk : g) {
				if (isMotionLink(lk))
					return true;
			}
			return false;
		}

		// this checks for motion graph:
		private void findMatchingGraphParts(ERGraph g1, ERGraph g2) {
			// 1.1 find corresponding entities
			for (int i = 0; i < g1.getEntities().size(); i++) {
				Entity e1 = g1.getEntities().get(i);
				for (int j = 0; j < g2.getEntities().size(); j++) {
					Entity e2 = g2.getEntities().get(j);
					if (e1.getEntityType() == e2.getEntityType()
							&& e1.getTheClass().getName().equals(e2.getTheClass().getName())) {
						Pair p = new Pair(e1, e2);
						entityPairs.add(p);
					}
				}
			}
			// 1.2 extend to sets:
			int initialSize = entityPairs.size();
			for (int i = 0; i < initialSize; i++) {
				Pair p = entityPairs.get(i);
				Entity e1 = (Entity) p.o1;
				Entity e2 = (Entity) p.o2;
				List<Link> list1 = g1.containLinkList("hasMember", null, e1);
				List<Link> list2 = g2.containLinkList("hasMember", null, e2);
				if (list1 == null && list2 == null)
					continue;
				List<Entity> elist1 = new ArrayList<>();
				if (list1 != null && list1.size() > 0) {
					for (Link lk : list1) {
						elist1.add(lk.getArg1());
					}
				} else {
					elist1.add(e1);
				}
				List<Entity> elist2 = new ArrayList<>();
				if (list2 != null && list2.size() > 0) {
					for (Link lk : list2) {
						elist2.add(lk.getArg1());
					}
				} else {
					elist2.add(e2);
				}
				for (Entity ee1 : elist1) {
					for (Entity ee2 : elist2) {
						Pair pp = new Pair(ee1, ee2);
						if (!entityPairs.contains(pp))
							entityPairs.add(pp);
					}
				}
			}
			// 1.3 extend to "define"
			initialSize = entityPairs.size();
			for (int i = 0; i < initialSize; i++) {
				Pair p = entityPairs.get(i);
				Entity e1 = (Entity) p.o1;
				Entity e2 = (Entity) p.o2;
				List<Link> list1_1 = g1.containLinkList("define", null, e1);
				List<Link> list1_2 = g1.containLinkList("define", e1, null);
				List<Link> list2_1 = g2.containLinkList("define", null, e2);
				List<Link> list2_2 = g2.containLinkList("define", e2, null);
				if (list1_1 == null && list1_2 == null && list2_1 == null && list2_2 == null)
					continue;
				List<Entity> elist1 = new ArrayList<>();
				if (list1_1 != null) {
					for (Link lk : list1_1) {
						elist1.add(lk.getArg1());
					}
				}
				if (list1_2 != null) {
					for (Link lk : list1_2) {
						elist1.add(lk.getArg2());
					}
				}
				if (elist1.size() == 0) {
					elist1.add(e1);
				}
				List<Entity> elist2 = new ArrayList<>();
				if (list2_1 != null) {
					for (Link lk : list2_1) {
						elist2.add(lk.getArg1());
					}
				}
				if (list2_2 != null) {
					for (Link lk : list2_2) {
						elist2.add(lk.getArg2());
					}
				}
				if (elist2.size() == 0) {
					elist2.add(e2);
				}
				for (Entity ee1 : elist1) {
					for (Entity ee2 : elist2) {
						Pair pp = new Pair(ee1, ee2);
						if (!entityPairs.contains(pp))
							entityPairs.add(pp);
					}
				}
			}
			// 2. find corresponding links
			if (g1.getLinks() != null && g2.getLinks() != null)
				for (int i = 0; i < g1.getLinks().size(); i++) {
					Link lk1 = g1.getLinks().get(i);
					for (int j = 0; j < g2.getLinks().size(); j++) {
						Link lk2 = g2.getLinks().get(j);
						if (lk1.getType().equals(lk2.getType())) {
							Pair p1 = new Pair(lk1.getArg1(), lk2.getArg1());
							if (!entityPairs.contains(p1))
								continue;
							Pair p2 = new Pair(lk1.getArg2(), lk2.getArg2());
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
					if (lk11.getArg1() == lk21.getArg1()) {// first graph has a shared node
						String s = "" + i + "" + j + "11";
						if (lk12.getArg1() == lk22.getArg1()) {// the second graphs same
							hiLinks.add(s);
						}
					} else if (lk11.getArg1() == lk21.getArg2()) {
						String s = "" + i + "" + j + "12";
						if (lk12.getArg1() == lk22.getArg2()) {// the second graphs same
							hiLinks.add(s);
						}
					} else if (lk11.getArg2() == lk21.getArg1()) {// first graph has a shared node
						String s = "" + i + "" + j + "21";
						if (lk12.getArg2() == lk22.getArg1()) {// the second graphs same
							hiLinks.add(s);
						}
					} else if (lk11.getArg2() == lk21.getArg2()) {
						String s = "" + i + "" + j + "22";
						if (lk12.getArg2() == lk22.getArg2()) {// the second graphs same
							hiLinks.add(s);
						}
					}
				}
			}
			// find continuous graphs:
			List<Integer> usedLinks = new ArrayList<>();
			for (int i = 0; i < hiLinks.size(); i++) {
				String s = hiLinks.get(i);
				int ilk_1 = Integer.parseInt(s.substring(0, 1));
				int ilk_2 = Integer.parseInt(s.substring(1, 2));
				Integer k1 = new Integer(ilk_1);
				if (!usedLinks.contains(k1)) {
					usedLinks.add(k1);
				}
				Integer k2 = new Integer(ilk_2);
				if (!usedLinks.contains(k2)) {
					usedLinks.add(k2);
				}
				Link lk1 = (Link) linkPairs.get(ilk_1).o1;
				Link lk2 = (Link) linkPairs.get(ilk_2).o1;
				MatchGraph grf1 = null;
				MatchGraph grf2 = null;
				for (MatchGraph grf : graph) {
					if (grf.contains(lk1)) {
						grf1 = grf;
						break;
					}
				}
				for (MatchGraph grf : graph) {
					if (grf.contains(lk2)) {
						grf2 = grf;
						break;
					}
				}
				if (grf1 != null && grf2 != null) {
					if (grf1 != grf2) { // combine
						grf1.merge(grf2);
						graph.remove(grf2);
					}
				} else if (grf1 != null) {
					grf1.add(linkPairs.get(ilk_2));
				} else if (grf2 != null) {
					grf2.add(linkPairs.get(ilk_1));
				} else {
					List<Link> g_1 = new ArrayList<>();
					List<Link> g_2 = new ArrayList<>();
					g_1.add(lk1);
					g_1.add(lk2);
					Pair p1 = linkPairs.get(ilk_1);
					Pair p2 = linkPairs.get(ilk_2);
					g_2.add((Link) p1.o2);
					g_2.add((Link) p2.o2);
					List<Pair> lkp = new ArrayList<>();
					lkp.add(p1);
					lkp.add(p2);
					MatchGraph g = new MatchGraph(g_1, g_2, lkp);
					graph.add(g);
				}
			}
			List<Pair> usedLinkPairs = new ArrayList<>(linkPairs);
			Collections.sort(usedLinks);
			for (int i = usedLinks.size() - 1; i >= 0; i--) {
				Integer k = usedLinks.get(i);
				usedLinkPairs.remove(k.intValue());
			}
			// so far
			for (Pair p : usedLinkPairs) {
				if (!isMotionLink((Link) p.o1))
					continue;
				if (!isMotionLink((Link) p.o2))
					continue;
				List<Link> g_1 = new ArrayList<>();
				g_1.add((Link) p.o1);
				List<Link> g_2 = new ArrayList<>();
				g_2.add((Link) p.o2);
				List<Pair> lp = new ArrayList<>();
				lp.add(p);
				MatchGraph mg = new MatchGraph(g_1, g_2, lp);
				graph.add(mg);
			}
			if (graph.size() > 1)
				Collections.sort(graph, this);
			// finally, calculating score:
			int graphSize = 0;
			if (graph.size() > 0) {
				List<Entity> lst1 = g1.getMotionList();
				List<Entity> lst2 = g2.getMotionList();
				if (lst1.size() > 0 && lst2.size() > 0) {
					int id1 = lst1.get(0).getID();
					int id2 = lst2.get(0).getID();
					for (MatchGraph g : graph) {
						if (g.isTopMotionGraph(id1, id2)) {
							if (g.size() > graphSize)
								graphSize = g.size();
						}
					}
				}
			}
			//			score = entityPairs.size() + 2 * linkPairs.size() + 3 * graphSize;
			score = graphSize;
		}

		@Override
		public int compareTo(ComparePhrases o) {
			return o.score - score;
		}

		@Override
		public int compare(MatchGraph o1, MatchGraph o2) {
			return o2.g1.size() - o1.g1.size();
		}

	}

	static class MatchGraph {
		List<Link> g1, g2;
		List<Pair> linkPairs;

		MatchGraph(List<Link> _g1, List<Link> _g2, List<Pair> _linkPairs) {
			g1 = _g1;
			g2 = _g2;
			linkPairs = _linkPairs;
		}

		public int size() {
			return g1.size();
		}

		boolean contains(Link _lk) {
			return g1.contains(_lk);
		}

		void add(Pair p) {
			Link lk1 = (Link) p.o1;
			Link lk2 = (Link) p.o2;
			if (!g1.contains(lk1)) {
				g1.add(lk1);
			}
			if (!g2.contains(lk2)) {
				g2.add(lk2);
			}
			linkPairs.add(p);
		}

		void merge(MatchGraph m) {
			g1.addAll(m.g1);
			g2.addAll(m.g2);
			linkPairs.addAll(m.linkPairs);
		}

		boolean isTopMotionGraph(int id1, int id2) {
			int maxID_1 = ComparePhrases.motionMaxID(g1);
			int maxID_2 = ComparePhrases.motionMaxID(g2);
			if (id1 != maxID_1 || id2 != maxID_2)
				return false;
			return true;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Left:\n");
			for (Link lk : g1) {
				sb.append("\t" + lk.toString() + "\n");
			}
			sb.append("Right:\n");
			for (Link lk : g2) {
				sb.append("\t" + lk.toString() + "\n");
			}
			return sb.toString();
		}
	}
}
