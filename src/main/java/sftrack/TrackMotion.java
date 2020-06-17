package sftrack;

import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

import common.Role;
import legal.EntitiesAndCaseDockets;
import legal.LegalCase;
import legal.TrackEntry;
import legal.TrackEntry.DePhrase;
import legal.TrackEntry.Section;
import sfmotion.ComplaintEntry;
import sfmotion.HearingEntry;
import sfmotion.MotionEntry;
import sfmotion.OppositionEntry;
import sfmotion.OrderEntry;
import sfmotion.ReplyEntry;
import sfmotion.SFMotionEntry;
import sftrack.LegaLanguage.Srunner;
import utils.Pair;

public class TrackMotion {
	static LegaLanguage legalang;
	static final int TOP_N = 3;
	static final String rulesFile = "sftrack/docketParse.drl";
	static final String triplesFile = "src/main/resources/sftrack/triples.txt";
	static final String lexiconFile = "src/main/resources/sftrack/lexicon.txt";
	static String[] entityResources = { "C:\\data\\191023\\dockets\\judgeparty/ca_sfc_party.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_judge.txt",
			"C:\\data\\191023\\dockets\\judgeparty/ca_sfc_attorney.txt",
			"C:\\data\\191023\\dockets/motions_3.txt" };

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

	private static void ruleEngineParse(TrackEntry e, String id) {
		if (e.text.startsWith("Payment")) {
			return;
		}
		System.out.println(id + "\t" + e.getDate() + "\t" + e.text);
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

	static TrackCase convertOneLegalCaseToTrackCase(LegalCase cs) {
		TrackCase tc = new TrackCase(cs.getID());
		tc.casetype = cs.casetype;
		tc.caseSubtype = cs.caseSubtype;
		tc.names = cs.names;
		tc.namep = cs.namep;
		tc.entries = cs.entries;
		tc.daily = cs.daily;
		tc.complaint = cs.complaint;
		tc.gel = cs.gel;
		tc.glk = cs.glk;
		tc.lastDate = cs.lastDate;
		tc.judges = cs.judges;
		//		Map<String, List<SFMotionEntry>> transactions = new TreeMap<>();
		//		public List<SFMotionEntry> motionEntries = new ArrayList<>();
		//		List<PartyCluster> clusters = new ArrayList<>();
		return tc;
	}

	static List<TrackCase> convertToTrackCases(List<LegalCase> lcs) {
		List<TrackCase> tcs = new ArrayList<>();
		for (LegalCase cs : lcs) {
			TrackCase tc = convertOneLegalCaseToTrackCase(cs);
			tcs.add(tc);
		}
		return tcs;
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("args: path number");
			System.exit(-1);
		}
		String path = args[0];
		String number = args[1];
		int nMotionTracked = 0;
		int nMotionUntracked = 0;
		int nAppTracked = 0;
		int nAppUntracked = 0;
		int nDemTracked = 0;
		int nDemUntracked = 0;
		int nMotionInLimine = 0;
		int nUnHearing = 0;
		int nUnOrder = 0;
		int nUnOppos = 0;
		int nUnReply = 0;
		int nOppositions = 0;
		String trackedMotionFile = path + "m_" + number + ".txt";
		String trackedApplicationFile = path + "a_" + number + ".txt";
		String trackedDemurrerFile = path + "d_" + number + ".txt";
		String untrackedMotionFile = path + "mu_" + number + ".txt";
		String untrackedApplicationFile = path + "au_" + number + ".txt";
		String untrackedDemurrerFile = path + "du_" + number + ".txt";
		String untrackedHearingFile = path + "h_" + number + ".txt";
		String untrackedOrderFile = path + "or_" + number + ".txt";
		String untrackedOppositionFile = path + "op_" + number + ".txt";
		String untrackedReplyFile = path + "re_" + number + ".txt";
		String limineFile = path + "lmn_" + number + ".txt";
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(trackedMotionFile));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(untrackedMotionFile));
		BufferedWriter wr3 = new BufferedWriter(new FileWriter(untrackedHearingFile));
		BufferedWriter wr4 = new BufferedWriter(new FileWriter(untrackedOrderFile));
		BufferedWriter wr5 = new BufferedWriter(new FileWriter(untrackedOppositionFile));
		BufferedWriter wr6 = new BufferedWriter(new FileWriter(untrackedReplyFile));
		BufferedWriter wr7 = new BufferedWriter(new FileWriter(trackedApplicationFile));
		BufferedWriter wr8 = new BufferedWriter(new FileWriter(trackedDemurrerFile));
		BufferedWriter wr9 = new BufferedWriter(new FileWriter(untrackedApplicationFile));
		BufferedWriter wr10 = new BufferedWriter(new FileWriter(untrackedDemurrerFile));
		BufferedWriter wr11 = new BufferedWriter(new FileWriter(limineFile));
		int countGrouped = 0;
		int duplicates = 0;
		legalang = TrackMotion.initializeRuleEngine();
		EntitiesAndCaseDockets etcd = new EntitiesAndCaseDockets(entityResources);
		List<TrackCase> tcs = convertToTrackCases(etcd.cases);
		for (TrackCase cs : tcs) {
			System.out.println("\n================ " + cs.getID() + " ==================\n");
			TrackEntry e0 = cs.entries.get(0); // first entry is always Complaint in San Francisco
			ComplaintEntry c = new ComplaintEntry(e0.sdate, e0.text);
			cs.addEntry(c);
			for (int i = 1; i < cs.entries.size(); i++) {
				TrackEntry e = cs.entries.get(i);
				ruleEngineParse(e, cs.getID());
				SFMotionEntry en = SFMotionEntry.analyze(e.sdate, e.text);
				cs.addEntry(en);
			}
			cs.generateLists();
			cs.findTransactions();
			//			countGrouped += cs.groupTransactions();
			//			duplicates += cs.removeDuplicateMotions1();
			cs.trackMotionSequences();
			List<MotionEntry> mslist = cs.getMotionList();
			List<HearingEntry> hrlist = cs.getHearingEntries();
			List<OrderEntry> orlist = cs.getOrderEntries();
			for (MotionEntry ms : mslist) {
				if (ms.isTracked() || (ms.hearingDate != null && ms.hearingDate.after(cs.lastDate))) {// hearing date in the future
					if (ms.subtype == MotionEntry.TYPE_APPLIC) {
						nAppTracked++;
						wr7.write(cs.id + "\n" + ms.toString() + "\n\n");
					} else if (ms.subtype == MotionEntry.TYPE_DEMURR) {
						nDemTracked++;
						wr8.write(cs.id + "\n" + ms.toString() + "\n\n");
					} else {
						nMotionTracked++;
						wr1.write(cs.id + "\n" + ms.toString() + "\n\n");
					}
				} else {
					//					if (ms.isMotionInLimine()) {
					//						nMotionInLimine++;
					//					}
					if (ms.subtype == MotionEntry.TYPE_APPLIC) {
						nAppUntracked++;
						wr9.write(cs.id + "\n" + ms.toString() + "\n\n");
					} else if (ms.subtype == MotionEntry.TYPE_DEMURR) {
						nDemUntracked++;
						wr10.write(cs.id + "\n" + ms.toString() + "\n\n");
					} else {
						nMotionUntracked++;
						wr2.write(cs.id + "\n" + ms.toString() + "\n\n");
					}
				}
			}
			for (HearingEntry hr : hrlist) {
				wr3.write(cs.id + "\n" + hr.toString() + "\n\n");
				nUnHearing++;
			}
			for (OrderEntry or : orlist) {
				wr4.write(cs.id + "\n" + or.toString() + "\n\n");
				nUnOrder++;
			}
			if (cs.mlnlists != null || cs.oplists != null) {
				wr11.write(cs.id + "\n");
				if (cs.mlnlists != null) {
					for (Role key : cs.mlnlists.keySet()) {
						wr11.write("Motions In Limine from " + key + ":\n");
						List<MotionEntry> list = cs.mlnlists.get(key);
						for (MotionEntry me : list) {
							nMotionInLimine++;
							wr11.write(me.toString() + "\n");
						}
					}
				}
				if (cs.oplists != null) {
					for (Role key : cs.oplists.keySet()) {
						wr11.write("Oppositions from " + key + ":\n");
						List<OppositionEntry> list = cs.oplists.get(key);
						for (OppositionEntry me : list) {
							nOppositions++;
							wr11.write(me.toString() + "\n");
						}
					}
				}
			}
			for (OppositionEntry hr : cs.oppositions) {
				wr5.write(cs.id + "\n" + hr.toString() + "\n\n");
				nUnOppos++;
			}
			for (ReplyEntry or : cs.replies) {
				wr6.write(cs.id + "\n" + or.toString() + "\n\n");
				nUnReply++;
			}
		}
		wr1.write("Motion Tracked: " + nMotionTracked);
		wr1.close();
		wr7.write("Application Tracked: " + nAppTracked);
		wr7.close();
		wr8.write("Demurrer Tracked: " + nDemTracked);
		wr8.close();
		wr11.write("Motion In Limine: " + nMotionInLimine);
		wr11.close();
		wr2.write("Motion Not Tracked: " + nMotionUntracked);
		wr2.close();
		wr9.write("Application Not Tracked: " + nAppUntracked);
		wr9.close();
		wr10.write("Demurrer Not Tracked: " + nDemUntracked);
		wr10.close();
		wr3.write("Hearing Not Used: " + nUnHearing);
		wr3.close();
		wr4.write("Order Not Used: " + nUnOrder);
		wr4.close();
		wr5.write("Oppositions Not Used: " + nUnOppos);
		wr5.close();
		wr6.write("Reply Not Used: " + nUnReply);
		wr6.close();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println("==========================================================================\n" + dtf.format(now));
		System.out.println("Track Files:");
		System.out.println("\t" + args[0]);
		System.out.println("\t" + trackedMotionFile);
		System.out.println("\t" + trackedApplicationFile);
		System.out.println("\t" + trackedDemurrerFile);
		System.out.println("\t" + untrackedMotionFile);
		System.out.println("\t" + untrackedApplicationFile);
		System.out.println("\t" + untrackedDemurrerFile);
		System.out.println("\t" + untrackedHearingFile);
		System.out.println("\t" + untrackedOrderFile);
		System.out.println("\t" + untrackedOppositionFile);
		System.out.println("\t" + untrackedReplyFile);
		System.out.println("CountGrouped: " + countGrouped + ", duplicates: " + duplicates);
		System.out.println("Motion Tracked: " + nMotionTracked);
		System.out.println("Application Tracked: " + nAppTracked);
		System.out.println("Demurrer Tracked: " + nDemTracked);
		System.out.println("Motion Not Tracked: " + nMotionUntracked);
		System.out.println("Application Not Tracked: " + nAppUntracked);
		System.out.println("Demurrer Not Tracked: " + nDemUntracked);
		System.out.println("Motion In Limine: " + nMotionInLimine);
		System.out.println("Hearing Not Used: " + nUnHearing);
		System.out.println("Order Not Used: " + nUnOrder);
		System.out.println("Oppositions Not Used: " + nUnOppos);
		System.out.println("Reply Not Used: " + nUnReply);

		String tagfile = args[1] + "_tag_" + args[2] + ".txt";
		BufferedWriter wr = new BufferedWriter(new FileWriter(tagfile));
		for (TrackCase c : tcs) {
			wr.write(c.id + "\n");
			for (SFMotionEntry e : c.motionEntries) {
				wr.write("\t" + e.toTypeString() + "\n");
			}
		}
		wr.close();
		System.out.println("==========================================================================\n");
		System.out.println("Tag File:");
		System.out.println("\t" + tagfile);
		System.out.println("\nDone!");
	}

	static void saveCS(String[] args, List<TrackCase> cslist) throws IOException {
		int nMotionTracked = 0;
		int nMotionUntracked = 0;
		int nAppTracked = 0;
		int nAppUntracked = 0;
		int nDemTracked = 0;
		int nDemUntracked = 0;
		int nMotionInLimine = 0;
		int nUnHearing = 0;
		int nUnOrder = 0;
		int nUnOppos = 0;
		int nUnReply = 0;
		int nOppositions = 0;
		String path = args[1];
		String number = args[2];
		String trackedMotionFile = path + "m_" + number + ".txt";
		String trackedApplicationFile = path + "a_" + number + ".txt";
		String trackedDemurrerFile = path + "d_" + number + ".txt";
		String untrackedMotionFile = path + "mu_" + number + ".txt";
		String untrackedApplicationFile = path + "au_" + number + ".txt";
		String untrackedDemurrerFile = path + "du_" + number + ".txt";
		String untrackedHearingFile = path + "h_" + number + ".txt";
		String untrackedOrderFile = path + "or_" + number + ".txt";
		String untrackedOppositionFile = path + "op_" + number + ".txt";
		String untrackedReplyFile = path + "re_" + number + ".txt";
		String limineFile = path + "lmn_" + number + ".txt";
		BufferedWriter wr1 = new BufferedWriter(new FileWriter(trackedMotionFile));
		BufferedWriter wr2 = new BufferedWriter(new FileWriter(untrackedMotionFile));
		BufferedWriter wr3 = new BufferedWriter(new FileWriter(untrackedHearingFile));
		BufferedWriter wr4 = new BufferedWriter(new FileWriter(untrackedOrderFile));
		BufferedWriter wr5 = new BufferedWriter(new FileWriter(untrackedOppositionFile));
		BufferedWriter wr6 = new BufferedWriter(new FileWriter(untrackedReplyFile));
		BufferedWriter wr7 = new BufferedWriter(new FileWriter(trackedApplicationFile));
		BufferedWriter wr8 = new BufferedWriter(new FileWriter(trackedDemurrerFile));
		BufferedWriter wr9 = new BufferedWriter(new FileWriter(untrackedApplicationFile));
		BufferedWriter wr10 = new BufferedWriter(new FileWriter(untrackedDemurrerFile));
		BufferedWriter wr11 = new BufferedWriter(new FileWriter(limineFile));
		int countGrouped = 0;
		int duplicates = 0;
		for (TrackCase cs : cslist) {
			cs.generateLists();
			cs.findTransactions();
			//			countGrouped += cs.groupTransactions();
			//			duplicates += cs.removeDuplicateMotions1();
			cs.trackMotionSequences();
			List<MotionEntry> mslist = cs.getMotionList();
			List<HearingEntry> hrlist = cs.getHearingEntries();
			List<OrderEntry> orlist = cs.getOrderEntries();
			for (MotionEntry ms : mslist) {
				if (ms.isTracked() || (ms.hearingDate != null && ms.hearingDate.after(cs.lastDate))) {// hearing date in the future
					if (ms.subtype == MotionEntry.TYPE_APPLIC) {
						nAppTracked++;
						wr7.write(cs.id + "\n" + ms.toString() + "\n\n");
					} else if (ms.subtype == MotionEntry.TYPE_DEMURR) {
						nDemTracked++;
						wr8.write(cs.id + "\n" + ms.toString() + "\n\n");
					} else {
						nMotionTracked++;
						wr1.write(cs.id + "\n" + ms.toString() + "\n\n");
					}
				} else {
					//					if (ms.isMotionInLimine()) {
					//						nMotionInLimine++;
					//					}
					if (ms.subtype == MotionEntry.TYPE_APPLIC) {
						nAppUntracked++;
						wr9.write(cs.id + "\n" + ms.toString() + "\n\n");
					} else if (ms.subtype == MotionEntry.TYPE_DEMURR) {
						nDemUntracked++;
						wr10.write(cs.id + "\n" + ms.toString() + "\n\n");
					} else {
						nMotionUntracked++;
						wr2.write(cs.id + "\n" + ms.toString() + "\n\n");
					}
				}
			}
			for (HearingEntry hr : hrlist) {
				wr3.write(cs.id + "\n" + hr.toString() + "\n\n");
				nUnHearing++;
			}
			for (OrderEntry or : orlist) {
				wr4.write(cs.id + "\n" + or.toString() + "\n\n");
				nUnOrder++;
			}
			if (cs.mlnlists != null || cs.oplists != null) {
				wr11.write(cs.id + "\n");
				if (cs.mlnlists != null) {
					for (Role key : cs.mlnlists.keySet()) {
						wr11.write("Motions In Limine from " + key + ":\n");
						List<MotionEntry> list = cs.mlnlists.get(key);
						for (MotionEntry me : list) {
							nMotionInLimine++;
							wr11.write(me.toString() + "\n");
						}
					}
				}
				if (cs.oplists != null) {
					for (Role key : cs.oplists.keySet()) {
						wr11.write("Oppositions from " + key + ":\n");
						List<OppositionEntry> list = cs.oplists.get(key);
						for (OppositionEntry me : list) {
							nOppositions++;
							wr11.write(me.toString() + "\n");
						}
					}
				}
			}
			for (OppositionEntry hr : cs.oppositions) {
				wr5.write(cs.id + "\n" + hr.toString() + "\n\n");
				nUnOppos++;
			}
			for (ReplyEntry or : cs.replies) {
				wr6.write(cs.id + "\n" + or.toString() + "\n\n");
				nUnReply++;
			}
		}
		wr1.write("Motion Tracked: " + nMotionTracked);
		wr1.close();
		wr7.write("Application Tracked: " + nAppTracked);
		wr7.close();
		wr8.write("Demurrer Tracked: " + nDemTracked);
		wr8.close();
		wr11.write("Motion In Limine: " + nMotionInLimine);
		wr11.close();
		wr2.write("Motion Not Tracked: " + nMotionUntracked);
		wr2.close();
		wr9.write("Application Not Tracked: " + nAppUntracked);
		wr9.close();
		wr10.write("Demurrer Not Tracked: " + nDemUntracked);
		wr10.close();
		wr3.write("Hearing Not Used: " + nUnHearing);
		wr3.close();
		wr4.write("Order Not Used: " + nUnOrder);
		wr4.close();
		wr5.write("Oppositions Not Used: " + nUnOppos);
		wr5.close();
		wr6.write("Reply Not Used: " + nUnReply);
		wr6.close();

		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println("==========================================================================\n" + dtf.format(now));
		System.out.println("Track Files:");
		System.out.println("\t" + args[0]);
		System.out.println("\t" + trackedMotionFile);
		System.out.println("\t" + trackedApplicationFile);
		System.out.println("\t" + trackedDemurrerFile);
		System.out.println("\t" + untrackedMotionFile);
		System.out.println("\t" + untrackedApplicationFile);
		System.out.println("\t" + untrackedDemurrerFile);
		System.out.println("\t" + untrackedHearingFile);
		System.out.println("\t" + untrackedOrderFile);
		System.out.println("\t" + untrackedOppositionFile);
		System.out.println("\t" + untrackedReplyFile);
		System.out.println("CountGrouped: " + countGrouped + ", duplicates: " + duplicates);
		System.out.println("Motion Tracked: " + nMotionTracked);
		System.out.println("Application Tracked: " + nAppTracked);
		System.out.println("Demurrer Tracked: " + nDemTracked);
		System.out.println("Motion Not Tracked: " + nMotionUntracked);
		System.out.println("Application Not Tracked: " + nAppUntracked);
		System.out.println("Demurrer Not Tracked: " + nDemUntracked);
		System.out.println("Motion In Limine: " + nMotionInLimine);
		System.out.println("Hearing Not Used: " + nUnHearing);
		System.out.println("Order Not Used: " + nUnOrder);
		System.out.println("Oppositions Not Used: " + nUnOppos);
		System.out.println("Reply Not Used: " + nUnReply);
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
						Entity e3 = new Entity(ph.getText(), legalang.getEntity("IndividualParty"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
						ph.setGraph(e3);
					} else if (party.type == legal.Party.TYPE_DOESROESMOES) {
						Entity e3 = new Entity(ph.getText(), legalang.getEntity("GenericParty"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
						ph.setGraph(e3);
					} else {
						Entity e3 = new Entity(ph.getText(), legalang.getEntity("OrgCoParty"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
						ph.setGraph(e3);
					}
				} else if (dp.entity instanceof legal.EntitiesAndCaseDockets.Attorney) {
					//					legal.ExtractEntities.Attorney attorney = (legal.ExtractEntities.Attorney)p.entity;
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Attorney"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof legal.EntitiesAndCaseDockets.Judge) {
					//					legal.ExtractEntities.Judge judge = (legal.ExtractEntities.Judge)p.entity;
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Judge"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof legal.EntitiesAndCaseDockets.Clerk) {
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Clerk"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof legal.EntitiesAndCaseDockets.Reporter) {
					Entity e3 = new Entity(ph.getText(), legalang.getEntity("Reporter"), Entity.TYPE_INSTANCE, legalang, ph.getBegToken());
					ph.setGraph(e3);
				} else if (dp.entity instanceof legal.EntitiesAndCaseDockets.SFCaseNumber) {
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

}
