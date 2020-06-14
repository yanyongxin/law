package sftrack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import common.Role;
import sfmotion.Case;
import sfmotion.ComplaintEntry;
import sfmotion.Entry;
import sfmotion.HearingEntry;
import sfmotion.MotionEntry;
import sfmotion.OppositionEntry;
import sfmotion.OrderEntry;
import sfmotion.ReplyEntry;

public class TrackMotion {
	// Organize motions in their life cycle
	// Motions include: motion, application, demurrer
	// life cycle:
	//	(1) notice of motion
	//		CA_SFC_464181	2017-05-24	NOTICE OF PLAINTIFFS UNOPPOSED MOTION TO SEAL FIRST AMENDED COMPLAINT (TRANSACTION ID # 100016241) FILED BY PLAINTIFF LEE, ANTHONY K HEARING SET FOR JUN-20-2017 AT 09:30 AM IN DEPT 302 (Fee:$60.00)
	//	(2) memorandum in support
	//		CA_SFC_464181	2017-05-24	MEMORANDUM IN SUPPORT OF PLAINTIFFS UNOPPOSED MOTION TO SEAL FIRST AMENDED COMPLAINT (TRANSACTION ID # 100016241) FILED BY PLAINTIFF LEE, ANTHONY K
	//		(2.1) other supports
	//	(3) oppositions to motions:
	//		OPPOSITION TO PLAINTIFFS MOTION ...
	//		(3.1) support of oppositions:
	//		CA_SFC_464181	2018-02-15	EXHIBITS A-L TO THE DECLARATION OF SPENCER HOSIE IN SUPPORT OF DEFENDANTS HOSIE RICE, LLP, SPENCER HOSIE, AND DIANE S. RICE'S OPPOSITION TO PLAINTIFF'S MOTION FOR A PRELIMINARY INJUNCTION (SEALED DOCUMENT) FILED BY DEFENDANT HOSIE, SPENCER AN INDIVIDUAL HOSIE RICE LLP RICE, DIANE
	//		CA_SFC_464181	2018-02-15	DECLARATION OF SPENCER HOSIE IN SUPPORT OF DEFENDANTS HOSIE RICE, LLP, SPENCER HOSIE, AND DIANE S. RICE'S OPPOSITION TO PLAINTIFF'S MOTION FOR A PRELIMINARY INJUNCTION (SEALED DOCUMENT) FILED BY DEFENDANT HOSIE, SPENCER AN INDIVIDUAL HOSIE RICE LLP RICE, DIANE
	//	(4)	Court reporting service
	//		CA_SFC_464181	2017-05-24	COURT REPORTING SERVICES LESS THAN 1 HOUR (TRANSACTION ID # 100016241) FILED BY PLAINTIFF LEE, ANTHONY K (Fee:$30.00)
	//	(5)	proof of electronic service
	//		CA_SFC_464181	2017-05-24	PROOF OF ELECTRONIC SERVICE (TRANSACTION ID # 100016241) FILED BY PLAINTIFF LEE, ANTHONY K
	//	(6)	Payment (date between motion and "Law and Motion")
	//		CA_SFC_464181	2017-06-01	Payment : MOTION; Amount : $60; Payment Type : ELECTRONIC ; Receipt Number : B5217601M004
	//	(7)	Law and Motion
	//		CA_SFC_464181	2017-06-20	LAW AND MOTION 302, PLAINTIFF ANTHONY LEE'S UNOPPOSED MOTION TO SEAL FIRST AMENDED COMPLAINT AND TO SEAL PREVIOUSLY FILED DOCUMENTS IS GRANTED. NO OPPOSITION FILED AND GOOD CAUSE SHOWN. THE COURT TO REVIEW PROPOSED ORDER SUBMITTED TO THE COURT. JUDGE: HAROLD KAHN, CLERK: M. GOODMAN, NOT REPORTED.
	//	(8)	Order affirming Law and Motion, same or next day 
	//		CA_SFC_464181	2017-06-21	ORDER GRANTING PLAINTIFF'S UNOPPOSED MOTION TO FILE FIRST AMENDED COMPLAINT UNDER SEAL AND TO SEAL PREVIOUSLY FILED DOCUMENTS

	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.out.println("args: infile path number");
			System.exit(-1);
		}
		//		if (args.length != 2) {
		//			System.out.println("args: infile outfile");
		//			System.exit(-1);
		//		}
		String infile = args[0];
		//		String outfile = args[1];
		BufferedReader br = new BufferedReader(new FileReader(infile));
		String line;
		String currentCaseID = "";
		Case cs = null;
		List<Case> cases = new ArrayList<>();
		Date last = null;
		while ((line = br.readLine()) != null) {
			String[] items = line.split("\\t");
			String caseID = items[0];
			if (!caseID.equals(currentCaseID)) {
				if (cs != null && last != null) {
					cs.lastDate = last;
				}
				cs = new Case(caseID);
				currentCaseID = caseID;
				cases.add(cs);
				ComplaintEntry c = new ComplaintEntry(items[1], items[2]);
				cs.addEntry(c);
				last = c.date;
			} else {
				Entry en = Entry.analyze(items[1], items[2]);
				cs.addEntry(en);
				if (en.date.after(last)) {
					last = en.date;
				}
			}
		}
		br.close();

		saveCS(args, cases);

		String tagfile = args[1] + "_tag_" + args[2] + ".txt";
		BufferedWriter wr = new BufferedWriter(new FileWriter(tagfile));
		for (Case c : cases) {
			//					c.sortEntries();
			wr.write(c.id + "\n");
			for (Entry e : c.entries) {
				wr.write("\t" + e.toTypeString() + "\n");
			}
		}
		wr.close();
		System.out.println("==========================================================================\n");
		System.out.println("Tag File:");
		System.out.println("\t" + tagfile);
		System.out.println("\nDone!");
	}

	static void saveCS(String[] args, List<Case> cslist) throws IOException {
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
		for (Case cs : cslist) {
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

}
