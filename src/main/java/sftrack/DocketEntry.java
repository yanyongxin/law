package sftrack;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sftrack.CaseData.LitiParty;

public class DocketEntry {
	private static final Logger log = LoggerFactory.getLogger(DocketEntry.class);
	// actions:
	static final String ACTION_GRANT = "Grant";
	static final String ACTION_DENY = "Deny";
	// Litigation Category:
	static final String EVENT_PROCEDURAL = "procedural";
	static final String EVENT_DISPOSITIVE = "dispositive";
	static final String EVENT_EVIDENTIARY = "evidentiary";
	static final String EVENT_MISCELLANEURS = "miscellaneous";
	// Party Roles:
	static final String ROLE_PLAINTIFF = "Plaintiff";
	static final String ROLE_DEFENDANT = "Defendant";
	static final String ROLE_COURT = "court";
	static final String ROLE_BOTH = "both";
	static final String ROLE_OTHER = "other";
	// Litigation Phase:
	static final String PHASE_PLEADINGS = "PLEADINGS";
	static final String PHASE_DISCOVERY = "DISCOVERY";
	static final String PHASE_CLAIMCONSTRUCTION = "CLAIM_CONSTRUCTION";
	static final String PHASE_SUMMARY_JUDGMENT = "SUMMARY_JUDGMENT"; // somehow I feel this is just a event, not a phase. 
	static final String PHASE_PRE_TRIAL = "PRE_TRIAL"; // pre-trial preparations, narrow definition. 
	static final String PHASE_TRIAL = "TRIAL";
	static final String PHASE_POST_TRIAL = "POST_TRIAL";
	static final String PHASE_APPEAL = "APPEAL";
	//	static final String PHASE_DONT_CARE = "NOT_MATTER"; // some events does not belong or mark any phrase
	static final String PHASE_TERMINATE = "TERMINATE"; // some events does not belong or mark any phrase

	// Docket Entry Canonical Types (DE_), Docket Entry subtypes (DES_), Docket Entry Expression Types (EXP_) 
	static final String DE_ACKNOWLEDGEMENT = "ACKNOWLEDGEMENT";
	static final String DES_ACKNOWLEDGEMENT_OF_RECEIPT = "ACKNOWLEDGEMENT_OF_RECEIPT";

	static final String DE_AFFIDAVIT = "AFFIDAVIT";
	static final String DES_AFFIDAVIT = "AFFIDAVIT";
	static final String DES_AFFIDAVIT_OF_SERVICE = "AFFIDAVIT_OF_SERVICE";

	static final String DE_VERDICT_SHEET = "VERDICT_SHEET";
	static final String DES_VERDICT_SHEET = "VERDICT_SHEET";

	static final String DE_VOIR_DIRE = "VOIR_DIRE";
	static final String DES_VOIR_DIRE = "VOIR_DIRE";

	static final String DE_INSTRUCTION = "INSTRUCTION";
	static final String DE_JURY_INSTRUCTION = "JURY_INSTRUCTION";
	static final String DES_JURY_INSTRUCTION = "JURY_INSTRUCTION";
	static final String DES_PROPOSED = "PROPOSED";

	static final String DE_APPENDIX = "APPENDIX";
	static final String DES_APPENDIX = "APPENDIX";

	static final String DE_ANSWER = "ANSWER";
	static final String DES_ANSWER = "ANSWER";

	static final String DE_BILL_OF_COSTS = "BILL_OF_COSTS";
	static final String DES_BILL_OF_COSTS = "BILL_OF_COSTS";

	static final String DE_BRIEF = "BRIEF";
	static final String DES_OPENING_BRIEF = "OPENING_BRIEF";
	static final String DES_ANSWERING_BRIEF = "ANSWER_BRIEF";
	static final String DES_REPLY_BRIEF = "REPLY_BRIEF";
	static final String DES_SURREPLY_BRIEF = "SURREPLY_BRIEF";

	static final String DE_CASE = "CASE";
	static final String DES_CASE_ASSIGN = "CASE_ASSIGN";
	static final String DES_CASE_REFER = "CASE_REFER";
	static final String DES_CASE_TRANSFER = "CASE_TRANSFER";
	static final String DES_CASE_TERMINATE = "CASE_TERMINATE";

	static final String DE_SET_OR_CHANGE_SCHEDULE = "SET_OR_CHANGE_SCHEDULE";
	static final String DES_SET_OR_CHANGE_SCHEDULE = "SET_OR_CHANGE_SCHEDULE";

	static final String DE_CHART = "CHART";
	static final String DES_CHART = "CHART";

	static final String DES_CLAIMCONSTRUCTION_PREHEARING_STATEMENT = "CLAIMCONSTRUCTION_PREHEARING_STATEMENT";

	static final String DE_COMPLAINT = "COMPLAINT";
	static final String DES_COMPLAINT = "COMPLAINT";
	static final String DES_INITIAL_COMPLAINT = "INITIAL_COMPLAINT";
	static final String DES_AMENDED_COMPLAINT = "AMENDED_COMPLAINT";
	static final String DES_COUNTERCLAIM = "COUNTERCLAIM";

	static final String DE_DECLARATION = "DECLARATION";
	static final String DES_DECLARATION = "DECLARATION";

	static final String DE_DISCLOSURE_STATEMENT = "DISCLOSURE_STATEMENT";
	static final String DES_DISCLOSURE_STATEMENT = "DISCLOSURE_STATEMENT";

	static final String DE_DOCUMENT = "DOCUMENT";

	static final String DE_EXHIBIT = "EXHIBIT";
	static final String DES_EXHIBIT = "EXHIBIT";

	static final String DE_FINDINGS_OF_FACT = "FINDINGS_OF_FACT ";
	static final String DES_FINDINGS_OF_FACT = "FINDINGS_OF_FACT ";

	static final String DE_JUDGMENT = "JUDGMENT";
	static final String DES_JUDGMENT = "JUDGMENT";

	static final String DE_LETTER = "LETTER";
	static final String DES_LETTER = "LETTER";

	static final String DE_MANDATE = "MANDATE";
	static final String DES_USCA_MANDATE = "USCA_MANDATE";

	static final String DE_MEETING = "MEETING";
	static final String DES_HEARING = "HEARING";
	static final String DES_CONFERENCE = "CONFERENCE";

	static final String DE_MEMORANDUM = "MEMORANDUM";
	static final String DES_MEMORANDUM = "MEMORANDUM";
	static final String DE_MEMORANDUM_OPINION = "MEMORANDUM_OPINION";
	static final String DES_MEMORANDUM_OPINION = "MEMORANDUM_OPINION";

	static final String DE_MINUTE_ENTRY = "MINUTE_ENTRY";
	static final String DES_TRIAL_HELD = "TRIAL_HELD";
	static final String DES_MARKMAN_HELD = "MARKMAN_HELD";

	static final String DE_MOTION = "MOTION";
	static final String DES_MOTION_TO_AMEND = "MOTION_TO_AMEND";
	static final String DES_MOTION_TO_COMPEL = "MOTION_TO_COMPEL";
	static final String DES_MOTION_TO_CONSOLIDATE = "MOTION_TO_CONSOLIDATE";
	static final String DES_MOTION_TO_DISMISS = "MOTION_TO_DISMISS";
	static final String DES_MOTION_TO_EXTEND_TIME = "MOTION_TO_EXTEND_TIME";
	static final String DES_MOTION_TO_STRIKE = "MOTION_TO_STRIKE";
	static final String DES_MOTION_TO_STAY = "MOTION_TO_STAY";
	static final String DES_MOTION_TO_QUASH = "MOTION_TO_QUASH";
	static final String DES_MOTION_TO_SEVER = "MOTION_TO_SEVER";
	static final String DES_MOTION_TO_TRANSFER = "MOTION_TO_TRANSFER";
	static final String DES_MOTION_TO_VACATE = "MOTION_TO_VACATE";
	static final String DES_MOTION_TO_PRECLUDE = "MOTION_TO_PRECLUDE";
	static final String DES_MOTION_TO_FILE = "MOTION_TO_FILE";

	static final String DES_MOTION_FOR_JUDGMENT = "MOTION_FOR_JUDGMENT";
	static final String DES_MOTION_FOR_JUDGMENT_MOL = "MOTION_FOR_JUDGMENT_MOL";
	static final String DES_MOTION_FOR_SUMMARY_JUDGMENT = "MOTION_FOR_SUMMARY_JUDGMENT";
	static final String DES_MOTION_FOR_FEES_COSTS = "MOTION_FOR_FEES_COSTS";
	static final String DES_MOTION_FOR_PROHACVICE = "MOTION_FOR_PROHACVICE";
	static final String DES_MOTION_FOR_PROTECTIVE_ORDER = "MOTION_FOR_PROTECTIVE_ORDER";// Protective Order, are there other orders needs Motion?

	static final String DE_NOTICE = "NOTICE";
	static final String DES_NOTICE = "NOTICE";
	static final String DE_NOTICE_OF_SERVICE = "NOTICE_OF_SERVICE";
	static final String DES_NOTICE_OF_APPEAL = "NOTICE_OF_APPEAL";
	static final String DES_LAWYER_APPEARANCE = "LAWYER_APPEARANCE";
	static final String DES_NOTICE_RELATED_CASE = "RELATED_CASE";
	static final String DES_REQUEST_FOR_PRODUCTION = "REQUEST_FOR_PRODUCTION";
	static final String DES_REQUEST_FOR_INSPECTION = "REQUEST_FOR_INSPECTION";
	static final String DES_REQUEST_FOR_ADMISSION = "REQUEST_FOR_ADMISSION";
	static final String DES_DISCLOSURE = "DISCLOSURE";
	static final String DES_INTERROGATORY = "INTERROGATORY";
	static final String DES_RESPONSE_TO_DISCOVERY = "RESPONSE_TO_DISCOVERY ";
	static final String DES_EXPERT_REPORT = "EXPERT_REPORT";
	static final String DES_DEPOSITION = "DEPOSITION";

	static final String DE_NOTICE_CONSENT_REFERRAL = "NOTICE_CONSENT_REFERRAL";
	static final String DES_NOTICE_CONSENT_REFERRAL = "NOTICE_CONSENT_REFERRAL";

	static final String DE_ORDER = "ORDER";
	static final String DES_SO_ORDER = "SO_ORDER";
	static final String DES_ORDER_SO_ORDER = "ORDER";
	static final String DES_SCHEDULING_ORDER = "SCHEDULING_ORDER";
	static final String DES_ORDER_GRANT_DENY = "ORDER_GRANT_DENY";
	static final String DES_ORDER_GRANT = "ORDER_GRANT";
	static final String DES_ORDER_DENY = "ORDER_DENY";
	static final String DES_SET_SCHEDULE = "SET_SCHEDULE";
	static final String DES_CASE_MANAGEMENT = "CASE_MANAGEMENT";
	static final String DES_ORDER_AMEND_ORDER = "ORDER_AMEND_ORDER";
	static final String DES_ORDER_TO_WITHDRAW = "ORDER_TO_WITHDRAW";
	static final String DES_ORDER_OF_DISMISSAL = "ORDER_OF_DISMISSAL";
	static final String DES_ORDER_OF_JUDGMENT = "ORDER_OF_JUDGMENT";
	static final String DES_USCA_ORDER = "USCA_ORDER";

	static final String DE_OPINION = "OPINION";
	static final String DES_OPINION = "OPINION";

	static final String DE_PROPOSED_ORDER = "PROPOSED_ORDER";

	static final String DE_REMARK = "REMARK";

	static final String DE_REPORT = "REPORT";
	static final String DES_REPORT_TO_COMMISSION_PATENT_TRADEMARK = "REPORT_TO_PTO";
	static final String DES_STATUS_REPORT = "STATUS_REPORT";

	static final String DE_REQUEST = "REQUEST";
	static final String DES_REQUEST = "REQUEST";

	static final String DE_RESPONSE = "RESPONSE";
	static final String DES_RESPONSE = "RESPONSE";
	static final String DES_OBJECTION = "OBJECTION";

	static final String DE_SCHEDULE = "SCHEDULE";
	static final String DES_SCHEDULE = "SCHEDULE";

	static final String DE_STATEMENT = "STATEMENT";

	static final String DE_STIPULATION = "STIPULATION";
	static final String DES_STIPULATION_EXTEND_TIME = "STIPULATION_EXTEND_TIME";
	static final String DES_STIPULATION_DISMISSAL = "STIPULATION_DISMISSAL";

	static final String DE_SUBPOENA = "SUBPOENA";
	static final String DES_SUBPOENA = "SUBPOENA";
	static final String DES_SUBPOENA_ISSUE = "SUBPOENA_ISSUE";
	static final String DES_SUBPOENA_RETURN_EXECUTED = "SUBPOENA_RETURN_EXECUTED";

	static final String DE_SUMMONS = "SUMMONS";
	static final String DES_SUMMONS_NO = "NO_SUMMONS_ISSUED";
	static final String DES_SUMMONS_ISSUED = "SUMMONS_ISSUED";
	static final String DES_SUMMONS_RETURN_EXECUTED = "SUMMONS_RETURN_EXECUTED";

	static final String DE_TRANSCRIPT = "TRANSCRIPT";
	static final String DES_TRANSCRIPT = "TRANSCRIPT";

	static final String DE_VERDICT = "VERDICT";
	static final String DES_VERDICT = "VERDICT";

	static final String DE_REDACTION_NOTICE = "REDACTION_NOTICE";
	static final String DES_REDACTION_NOTICE = "REDACTION_NOTICE";
	static final String DES_REDACTED = "REDACTED";

	static final String DES_WITHDRAW_ATTORNEY = "WITHDRAW_ATTORNEY";
	static final String DES_SUBSTITUTE_ATTORNEY = "SUBSTITUTE_ATTORNEY";

	static final String DES_SET_CONFERENCE = "SET_CONFERENCE";

	static final String DE_MISC = "MISCELLANEOUS";
	static final String DES_ATTORNEY_ADDED_FOR_ELECTRONIC_NOTICING = "ATTORNEY_ADDED_FOR_ELECTRONIC_NOTICING";
	static final String DES_PAYMENT = "PAYMENT";

	// Definitely Event:
	String eventCategory = EVENT_PROCEDURAL;// DISPOSITIVE EVIDENTIARY PROCEDURAL MISCELLANEURS
	String type = null; // canonical type, "claim",	String expression = null; // "Complaint", "motion", "notice of service", etc.
	String subType = null; // canonical subtype, "complaint", "counterclaim"
	Entity head = null; // head entity of this DE. it can be null on rare occasions.
	List<String> contents = null; // name::value pairs "jury demand::yes", structured content
	Date eventDate;
	// Definitely Docket Entry:
	int serial; // Serial in the order from data base. Including non-indexed DE
	int deIndex; // DE index as given in pacer
	String text;
	String processedText; // after removing brackets and other stuff, before feeding to tokenizer.
	CaseNumber csnumber;
	String lexID;
	List<LitiEvent> events;

	List<DockRef> frs; // refer in
	List<DockRef> drs; // refer out

	List<Entity> fromEntities = new ArrayList<Entity>(); // Some OrgCo
	String fromRole = ROLE_COURT; // PLAINTIFF, DEFENDANT, BOTH, COURT, OTHER. If nothing identifiable, default to court
	String litiPhase = null; // take PHASE_ values

	// Processing Intermediate:
	List<List<Analysis>> best;
	List<DocketEntry> dlist;
	CaseData casedata;
	Ontology onto;

	@SuppressWarnings("unchecked")
	public JSONObject toJSONObject() {
		JSONObject jo = new JSONObject();
		jo.put("type", type);
		jo.put("subType", subType);
		jo.put("litiPhase", litiPhase);
		jo.put("eventCat", eventCategory);
		jo.put("serial", serial);
		jo.put("index", deIndex);
		jo.put("text", text);
		jo.put("role", fromRole);
		jo.put("filedate", eventDate.toString());
		if (this.fromEntities != null && fromEntities.size() > 0) {
			JSONArray ja = new JSONArray();
			for (Entity e : fromEntities) {
				ja.add(e.getName());
			}
			jo.put("from", ja);
		}
		if (drs != null && drs.size() > 0) {
			JSONArray ja = new JSONArray();
			for (DockRef dr : drs) {
				JSONObject jdr = dr.toJSONObject();
				ja.add(jdr);
			}
			jo.put("ref_to", ja);
		}
		if (frs != null && frs.size() > 0) {
			JSONArray ja = new JSONArray();
			for (DockRef fr : frs) {
				JSONObject jfr = fr.toJSONObject();
				ja.add(jfr);
			}
			jo.put("ref_from", ja);
		}
		if (contents != null) {
			JSONArray ja = new JSONArray();
			for (String c : contents) {
				ja.add(c);
			}
			jo.put("contents", ja);
		}
		return jo;
	}

	public List<String> getInterpret() {
		List<String> ret = new ArrayList<String>();

		ret.add("type : " + type);
		ret.add("subType : " + subType);
		ret.add("category : " + eventCategory);
		if (litiPhase != null) {
			ret.add("litiPhase : " + litiPhase);
		}
		StringBuilder sb = new StringBuilder();
		sb.append("from : ");
		for (Entity e : fromEntities) {
			sb.append(e.getName() + " ");
		}
		ret.add(sb.toString());
		ret.add("fromRole : " + fromRole);
		if (contents != null && contents.size() > 0) {
			sb.setLength(0);
			sb.append("Contents : ");
			for (String s : contents) {
				sb.append("\n\t" + s);
			}
			ret.add(sb.toString());
		}
		sb.setLength(0);
		sb.append("refs : ");
		if (drs != null) {
			for (DockRef r : drs) {
				sb.append("source:" + r.sourcePrint() + ", ");
				sb.append("target:" + r.targetPrint() + ", ");
			}
		} else {
			sb.append("none");
		}
		ret.add(sb.toString());
		return ret;
	}

	public void setEventCategory(String s) {
		eventCategory = s;
	}

	private void addContent(String key, String value) {
		if (contents == null) {
			contents = new ArrayList<String>();
		}
		String s = key + "::" + value;
		if (!contents.contains(s)) {
			contents.add(s);
		}
	}

	public DocketEntry(Date filed, int num, String txt, List<DocketEntry> list, int ser, String cnumber) {
		eventDate = filed;
		deIndex = num;
		text = txt;
		dlist = list;
		serial = ser;
		//		casenumberfull = cnumber;
		csnumber = new CaseNumber(cnumber);
	}

	public DocketEntry(Date filed, int num, String txt, List<DocketEntry> list, int ser, String cnumber, String id, CaseData cd) {
		eventDate = filed;
		deIndex = num;
		text = txt;
		dlist = list;
		serial = ser;
		//		casenumberfull = cnumber;
		csnumber = new CaseNumber(cnumber);
		lexID = id;
		casedata = cd;
	}

	public void setOnto(Ontology ot) {
		onto = ot;
	}

	public int getDENumber() {
		return deIndex;
	}

	@Override
	public String toString() {
		return deIndex + "\t" + text;
	}

	// Currently best and bestNew co-exist. When bestNew is stablized, we'll delete best and replace it with bestNew and call it best.
	public void setBest(List<List<Analysis>> lla) {
		best = lla;
		geneUnderstanding();
	}

	public List<List<Analysis>> getBestNew() {
		return best;
	}

	public String getType() {
		return type;
	}

	public String getSubType() {
		return subType;
	}

	/**
	 * DE text often has errors, mostly caused by having an menu item as selection which automatically
	 * fill the initial part of the DE text, like "NOTICE OF SERVICE of ". The user may copy and paste in
	 * a paragraph that already has this part then duplication will result.
	 * 
	 * @param t
	 * @return
	 */
	public static String filterText(String t) {
		// of of => of
		t = t.replaceFirst("(?i:\\bof\\s*of\\b)", "of");
		// NOTICE OF SERVICE of NOTICE OF SERVICE => NOTICE OF SERVICE
		t = t.replaceFirst("(?i:NOTICE\\s*OF\\s*SERVICE\\s*of\\s*NOTICE\\s*OF\\s*SERVICE)", "NOTICE OF SERVICE");
		// Noticeof => Notice of
		t = t.replaceFirst("(?i:Noticeof)", "Notice of");
		// ANSWER to Amended Complaint ANSWER to 313 Amended Complaint
		// ANSWER to Amended Complaint ANSWER to => ANSWER to
		t = t.replaceFirst("(?i:ANSWER\\s*to\\s*Amended\\s*Complaint\\s*ANSWER\\s*to\\b)", "ANSWER to");
		return t;
	}

	public static DocketEntry DE(String line, List<DocketEntry> dlist, String casenumberfull, CaseData cd) {
		try {
			String[] split = line.split("\t");
			String lexCaseID = split[0];
			Date filed_on = Date.valueOf(split[1]);
			int number = Integer.parseInt(split[2]);
			String text = filterText(split[3]);
			if (split.length > 4) {
				log.info("DE error: " + line);
			}
			return new DocketEntry(filed_on, number, text, dlist, dlist.size(), casenumberfull, lexCaseID, cd);
		} catch (Exception e) {
			return null;
		}
	}

	private String findAgentRole(Entity e2) {
		if (e2 == null) {
			return null;
		}
		String ret = null;
		String agentName = e2.getName();
		LitiParty ltparty = casedata.getParty(agentName);
		if (ltparty != null) {
			ret = ltparty.getLitiRole();
			if (ret == null) {
				ret = DocketEntry.ROLE_OTHER;
			}
		} else {
			String role = casedata.getAttorneyForPartyRole(agentName);
			if (role != null) {
				return role;
			} else {
				role = casedata.getLawfirmRole(agentName);
				if (role != null) {
					return role;
				}
			}
		}
		return ret;
	}

	public static List<Phrase> getPhraseList(List<List<Analysis>> lla) {
		List<Phrase> plist = new ArrayList<Phrase>();
		for (List<Analysis> la : lla) {
			if (la.size() == 0) {
				continue;
			}
			int maxScore = la.get(0).score;
			for (int i = 0; i < la.size(); i++) {
				Analysis a = la.get(i);
				if (a.score == maxScore) {
					List<Phrase> pl = a.getPhraseList();
					plist.addAll(pl);
				}
			}
		}
		return plist;
	}

	private Analysis buildAnalysis(List<List<Analysis>> lla) {
		List<Phrase> plist = getPhraseList(lla);
		Analysis an = new Analysis(plist);
		return an;
	}

	private void findFrom(List<Phrase> plist) {
		for (int i = 0; i < plist.size(); i++) {
			Phrase pp = plist.get(i);
			ERGraph gg = pp.getGraph();
			List<Link> lks = gg.containLinkList("onBehalfOf", (Entity) null, (Entity) null);
			if (lks != null) {
				for (Link lk : lks) {
					Entity e2 = lk.getArg2();
					fillFromEntities(e2, gg);
				}
			}
			lks = gg.containLinkList("PRPRelation", "onBehalfOf", null);
			if (lks != null) {
				for (Link lk : lks) {
					Entity e2 = lk.getArg2();
					fillFromEntities(e2, gg);
				}
			}
			lks = gg.containLinkList("byAgent", (Entity) null, (Entity) null);
			if (lks != null) {
				for (Link lk : lks) {
					Entity e2 = lk.getArg2();
					fillFromEntities(e2, gg);
				}
			}
			lks = gg.containLinkList("PRPRelation", "byAgent", null);
			if (lks != null) {
				for (Link lk : lks) {
					Entity e2 = lk.getArg2();
					fillFromEntities(e2, gg);
				}
			}
		}
	}

	private void fillFromEntities(Entity e2, ERGraph gg) {
		if (e2 != null) {
			List<Link> lklist = null;
			if ((lklist = gg.containLinkList("hasMember", e2, null)) != null) {
				for (Link lk1 : lklist) {
					e2 = lk1.getArg2();
					fromEntities.add(e2);
				}
			} else {
				fromEntities.add(e2);
			}
			int pCount = 0;
			int dCount = 0;
			int oCount = 0;
			int cCount = 0;
			for (Entity e : fromEntities) {
				String role = findAgentRole(e);
				if (role == null)
					continue;
				if (role.equals(DocketEntry.ROLE_COURT)) {
					cCount++;
				} else if (role.equals(DocketEntry.ROLE_DEFENDANT)) {
					dCount++;
				} else if (role.equals(DocketEntry.ROLE_PLAINTIFF)) {
					pCount++;
				} else if (role.equals(DocketEntry.ROLE_OTHER)) {
					oCount++;
				}
			}
			if (pCount > 0 && dCount > 0) {
				fromRole = ROLE_BOTH;
			} else if (pCount > 0) {
				fromRole = ROLE_PLAINTIFF;
			} else if (dCount > 0) {
				fromRole = ROLE_DEFENDANT;
			} else if (cCount > 0) {
				fromRole = ROLE_COURT;
			} else if (oCount > 0) {
				fromRole = ROLE_OTHER;
			} else {
				fromRole = ROLE_COURT;
			}
		}

	}

	private void findFromOld(List<Phrase> plist) {
		for (int i = 0; i < plist.size(); i++) {
			Phrase pp = plist.get(i);
			ERGraph gg = pp.getGraph();
			Link lk;
			Entity e2 = null;
			if ((lk = gg.containLink("onBehalfOf", (Entity) null, (Entity) null)) != null) {
				e2 = lk.getArg2();
			} else {
				lk = gg.containLink("PRPRelation", "onBehalfOf", null);
				if (lk != null) {
					e2 = lk.getArg2();
				}
			}
			if (e2 == null) {
				if ((lk = gg.containLink("byAgent", (Entity) null, (Entity) null)) != null) {
					e2 = lk.getArg2();
				} else {
					lk = gg.containLink("PRPRelation", "byAgent", null);
					if (lk != null) {
						e2 = lk.getArg2();
					}
				}
			}
			if (e2 != null) {
				List<Link> lklist = null;
				if ((lklist = gg.containLinkList("hasMember", e2, null)) != null) {
					for (Link lk1 : lklist) {
						e2 = lk1.getArg2();
						fromEntities.add(e2);
					}
				} else {
					fromEntities.add(e2);
				}

				fromRole = findAgentRole(e2);
				if (fromRole == null) {
					fromRole = ROLE_COURT;
				}
			}
		}
	}

	/**
	 * assign values to :
	 * 		type
	 * 		subType
	 * 		eventCategory
	 * 		litiPhase
	 * 		contents
	 * 		etc.
	 * @param hd
	 * @param a
	 * @return
	 */
	private boolean handleHeadEntity(Entity hd, Analysis a) {
		if (hd.isKindOf(onto.getEntity("DocLegalMotion"))) {
			handleMotions(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalStipulation"))) {
			handleStipulations(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalComplaint"))) {
			handleComplaint(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalNoticeConsentReferralForm"))) {
			handleNoticeConsentReferralForm(a);
		} else if (hd.isKindOf(onto.getEntity("DocLegalReport"))) {
			handleReport(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalSubpoena"))) {
			handleSubpoena(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalSummons"))) {
			handleSummons(a);
		} else if (hd.isKindOf(onto.getEntity("DocLegalNotice"))) {
			handleNotice(a);
		} else if (hd.isInstanceOf(onto.getEntity("LegalCase"))) {
			handleCaseAssign(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalDisclosureStatement"))) {
			handleDisclosureStatement(a);
		} else if (hd.isKindOf(onto.getEntity("DocLegalStatement"))) {
			handleStatement(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalOrder"))) {
			handleOrder(a);
		} else if (hd.isKindOf(onto.getEntity("Schedule"))) {
			handleSchedule(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalMandate"))) {
			handleMandate(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalAffidavit"))) {
			handleAffidavit(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocVerdictSheet"))) {
			handleVerdictSheet(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocVoirDire"))) {// jury selection questionaire
			handleVoirDire(a);
		} else if (hd.isInstanceOf(onto.getEntity("Instruction"))) {// jury instruction
			handleInstruction(a);
		}
		//		else if (hd.isInstanceOf(onto.getEntity("Human"))) {
		//			handleHuman(a);
		//		} 
		else if (hd.isInstanceOf(onto.getEntity("DocLegalAnswer"))) {
			handleAnswer(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocAppendix"))) {
			handleAppendix(a);
		} else if (hd.isKindOf(onto.getEntity("DocLegalBrief"))) {
			handleBrief(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalMinuteEntry"))) {
			handleMinuteEntry(a);
		} else if (hd.isInstanceOf(onto.getEntity("Document")) || hd.isInstanceOf(onto.getEntity("Thing"))) {
			handleDocument(a);
		}
		//		else if (hd.isInstanceOf(onto.getEntity("Attorney"))) {
		//			handleAttorney(a);
		//		}
		else if (hd.isInstanceOf(onto.getEntity("DocLegalDeclaration"))) {
			handleDeclaration(a);
		} else if (hd.isInstanceOf(onto.getEntity("Judgment"))) {
			handleJudgment(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLetter"))) {
			handleLetter(a);
		} else if (hd.isInstanceOf(onto.getEntity("BillofCosts"))) {
			handleBillofCosts(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocRemark"))) {
			handleRemark(a);
		} else if (hd.isKindOf(onto.getEntity("Meeting"))) {
			handleMeeting(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalMemorandumOpinion"))) {
			handleMemorandumOpinion(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalMemorandum"))) {
			handleMemorandum(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocEntry"))) {
			handleDocEntry(a);
		} else if (hd.isKindOf(onto.getEntity("ResponseDocument"))) {
			handleResponse(a);
		} else if (hd.isInstanceOf(onto.getEntity("ProcRequest"))) {
			handleRequest(a);
		} else if (hd.isKindOf(onto.getEntity("ProcSetOrChangeSchedule"))) {
			handleChangeSchedule(a);
		} else if (hd.isInstanceOf(onto.getEntity("Opinion"))) {
			handleOpinion(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocExhibit"))) {
			handleExhibit(a);
		} else if (hd.isInstanceOf(onto.getEntity("FindingsOfFact"))) {
			handleFindingsOfFact(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocChart"))) {
			handleChart(a);
		} else if (hd.isInstanceOf(onto.getEntity("Transcript"))) {
			handleTranscript(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocVerdict"))) {
			handleVerdict(a);
		} else if (hd.isInstanceOf(onto.getEntity("ProcAcknowledge"))) {
			handleAcknowledgement(a);
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalAppeal")) || hd.isInstanceOf(onto.getEntity("ProcAppeal"))) {
			handleAppeal(a);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Based on parsing results (a list of phrases), make the best judgment on docket entry:
	 * type, subtype, contents, eventType, litigation phase, etc..
	 */
	void geneUnderstanding() {
		if (best == null || best.size() == 0) {
			return;
		}

		List<Phrase> plist = getPhraseList(best);
		// Extract head, usually in the first phrase:
		Phrase ph = plist.get(0);
		// VP need special handling because head is usually a verb. But for analysis purposes, I need a noun, the subject of the VP.
		if (ph.synType.equals("VP") && ph.subject != null) {
			Phrase subject = ph.subject;
			Entity eh = subject.getHead();
			ph.graph.setHead(eh);
		}
		Entity hd = ph.getHead();
		Analysis a = new Analysis(plist);

		// Extract source party, usually a byAgent Link,
		findFrom(plist);
		head = hd;
		boolean b = handleHeadEntity(hd, a);
		if (!b) {
			defaultHandler(hd, a);
		}
		if (type != null) {
			if (type.equalsIgnoreCase(DE_STIPULATION)) {
				fromRole = ROLE_BOTH;
			} else if (type.equals(DE_ORDER)) {
				fromRole = ROLE_COURT;
			}
		}
		if (fromRole.equals(ROLE_COURT) && fromEntities.size() > 0) {
			do {
				Entity e = fromEntities.get(0);
				if (e.isKindOf("Judge")) {
					break;
				} else if (e.isKindOf("USCA")) {
					break;
				} else {
					fromEntities.remove(0);
				}
			} while (fromEntities.size() > 0);
		}
	}

	/**
	 * When the DE is known to be a motion, this method handles it.
	 * 
	 * STIPULATION of Dismissal
	 * 
	 * @param a
	 */
	private void handleStipulations(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		// Extract head, usually in the first phrase:
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_STIPULATION;
		subType = null;
		List<Link> lks = g.containLinkList("ToInfinitive", hd, null);
		if (lks != null) {
			for (Link lk : lks) {
				if (lk.getArg2().isInstanceOf("ProcExtend")) {
					setSubType(DES_STIPULATION_EXTEND_TIME);
					break;
				}
			}
		}
		lks = g.containLinkList("for", hd, null);
		if (lks != null) {
			for (Link lk : lks) {
				if (lk.getArg2().isInstanceOf("ProcExtend")) {
					setSubType(DES_STIPULATION_EXTEND_TIME);
					break;
				}
			}
		}
		if (subType != null && subType.equals(DES_STIPULATION_EXTEND_TIME)) {
			//"stipulation to extend time to answer complaint to may 14, 2009"
			eventCategory = DocketEntry.EVENT_PROCEDURAL;
			//(669:ProcExtend) toPreposition (683:MonthDateValue)
			Link lk = g.containLink("toPreposition", "ProcExtend", "MonthDateValue");
			if (lk != null) {
				Entity e2 = lk.getArg2();
				//				(684:5) MonthValueOf (683:MonthDateValue)
				//				(685:14) DateValueOf (683:MonthDateValue)
				//				(686:2009) YearValueOf (683:MonthDateValue)
				Entity year = null;
				Entity month = null;
				Entity date = null;
				lk = g.containLink("YearValueOf", null, e2);
				if (lk != null) {
					year = lk.getArg1();
				}
				lk = g.containLink("MonthValueOf", null, e2);
				if (lk != null) {
					month = lk.getArg1();
				}
				lk = g.containLink("DateValueOf", null, e2);
				if (lk != null) {
					date = lk.getArg1();
				}
				if (year != null && month != null && date != null) {
					addContent("extend to", month.getName() + "-" + date.getName() + "-" + year.getName());
				}
			}
			//			(675:DocLegalComplaint) ugoerOfProc (673:ProcAnswer)
			//			(670:TimeRef) ToInfinitive (673:ProcAnswer)
			lks = g.containLinkList("ToInfinitive", null, "Process");
			if (lks != null) {
				for (Link ll : lks) {
					Entity e2 = ll.getArg2();
					if (e2.getName().equals("ProcExtend")) {
						continue;
					}
					Link lk1 = g.containLink("ugoerOfProc", null, e2);
					if (lk1 != null) {
						Entity e1 = lk1.getArg1();
						addContent("to " + e2.getName(), e1.getName());
					} else {
						addContent("to", e2.getName());
					}
				}
			}
		}
		lks = g.containLinkList("of", hd, null);
		if (lks != null) {
			for (Link lk : lks) {
				Entity e2 = lk.getArg2();
				if (g.containsKindOf(e2, "ProcDismiss")) {
					//				if (e2.isInstanceOf("ProcDismiss")) {
					eventCategory = DocketEntry.EVENT_DISPOSITIVE;
					subType = DES_STIPULATION_DISMISSAL;
					// find every OrgCo mentioned in the text
					List<String> dismissedCo = new ArrayList<String>();
					for (int i = 0; i < plist.size(); i++) {
						Phrase p = plist.get(i);
						ERGraph gx = p.getGraph();
						List<Entity> le = gx.findEntityByClass("OrgCo");
						if (le != null) {
							for (Entity ex : le) {
								String nm = ex.getName();
								if (dismissedCo.contains(nm)) {
									continue;
								}
								String role = casedata.getPartyRole(nm);
								if (role != null && role.equals(DocketEntry.ROLE_DEFENDANT)) {
									dismissedCo.add(nm);
								}
							}
						}
					}
					StringBuilder sb = new StringBuilder();
					for (String s : dismissedCo) {
						sb.append(s);
						sb.append(" ");
						addContent("dismiss", s);
					}
					LitiEvent le = new LitiEvent(LitiEvent.LE_STIPULATED_DISMISSAL, sb.toString().trim(), eventDate.toString(), serial, deIndex);
					casedata.addDismissal(le);

				} else {
					subType += ": " + lk.getArg2().getName();
				}
			}
		}
		Link lk = g.containLink("agentOfProc", hd, null);
		if (lk != null) {
			subType = ": " + lk.getArg2().getName();
		}

		for (int i = 1; i < plist.size(); i++) {
			Phrase p = plist.get(i);
			g = p.getGraph();
			//(728:DocLegalOrder) hasAttribute (727:StatusProposed)
			lk = g.containLink("hasAttribute", "DocLegalOrder", "StatusProposed");
			if (lk != null) {
				addContent("and", "Proposed Order");
			}
			lks = g.containLinkList("ToInfinitive", null, "Process");
			if (lks != null) {
				for (Link ll : lks) {
					Entity e2 = ll.getArg2();
					if (e2.getName().equals("ProcExtend")) {
						continue;
					}
					Link lk1 = g.containLink("ugoerOfProc", null, e2);
					if (lk1 != null) {
						Entity e1 = lk1.getArg1();
						addContent("to " + e2.getName(), e1.getName());
					} else {
						addContent("to", e2.getName());
					}
				}
			}
		}
	}

	/**
	 * When the DE is known to be a motion, this method handles it.
	 * 
	 * @param a
	 */
	private void handleMotions(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		// Extract head, usually in the first phrase:
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		// need to distinguish several kinds of motions:
		// Motion for leave to
		// Motion in limine
		// Motion for pro hac vice appearance
		// Motion for extension of time
		// Motion to strike
		// Motion to compel
		// Motion for Attorney Fees and Costs
		// Motion for New Trial
		// Motion for Judgment as a Matter of Law
		if (type == null) {
			type = DE_MOTION;
		}
		subType = null;
		if (hd.isInstanceOf("DocLegalMotionToStay")) {
			subType = DES_MOTION_TO_STAY;
		}
		// REDACTED VERSION of 242 MOTION
		Link lk = g.containLink("hasMeasure", hd, null);
		if (lk != null) {
			Entity e2 = lk.getArg2(); // version
			lk = g.containLink("hasAttribute", e2, "StatusRedacted");
			if (lk != null) {
				addContent("attribute", "Redacted");
			}
			return;
		}
		lk = g.containLink("ToInfinitive", hd, null);
		// to dismiss, strike, compel, to extend time:
		if (lk != null) {
			Entity e2 = lk.getArg2();
			if (e2.isInstanceOf("ProcDismiss")) {
				setSubType(DES_MOTION_TO_DISMISS);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("ProcStay")) {
				setSubType(DES_MOTION_TO_STAY);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("ProcExtend")) {
				setSubType(DES_MOTION_TO_EXTEND_TIME);
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else if (e2.isInstanceOf("ProcStrike")) {
				setSubType(DES_MOTION_TO_STRIKE);
				eventCategory = DocketEntry.EVENT_EVIDENTIARY;
			} else if (e2.isInstanceOf("ProcCompel")) {
				setSubType(DES_MOTION_TO_COMPEL);
				eventCategory = DocketEntry.EVENT_EVIDENTIARY;
			} else if (e2.isInstanceOf("ProcSever")) {
				setSubType(DES_MOTION_TO_SEVER);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("ProcTransfer")) {
				setSubType(DES_MOTION_TO_TRANSFER);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("ProcVacate")) {
				setSubType(DES_MOTION_TO_VACATE);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("ProcPreclude")) {
				setSubType(DES_MOTION_TO_PRECLUDE);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("ProcAmend")) {
				setSubType(DES_MOTION_TO_AMEND);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("ProcFileDoc")) {
				setSubType(DES_MOTION_TO_FILE);
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else {
				String translated = onto.translate(e2.getName());
				if (translated != null) {
					setSubType(DE_MOTION + "_TO_" + translated.toUpperCase());
				} else {
					setSubType(DE_MOTION + "_TO_" + e2.getName());
				}
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			}
		} //(646:DocLegalMotionForLeave) ToInfinitive (650:ProcFileDoc)
		lk = g.containLink("for", hd, null);
		// for leave, extension of time, pro hac vice appearance
		if (lk != null) {
			Entity e2 = lk.getArg2();
			if (e2.isInstanceOf("ProcExtend")) {
				setSubType(DES_MOTION_TO_EXTEND_TIME);
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else if (e2.isInstanceOf("ProcAppearance") || e2.isInstanceOf("ProcProHacViceAppearance")) {
				setSubType(DES_MOTION_FOR_PROHACVICE);
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else if (e2.isInstanceOf("FeeRef")) {
				setSubType(DES_MOTION_FOR_FEES_COSTS);
				eventCategory = DocketEntry.EVENT_MISCELLANEURS;
			} else if (e2.isInstanceOf("SummaryJudgment")) {
				setSubType(DES_MOTION_FOR_SUMMARY_JUDGMENT);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("JudgmentMOL")) {
				setSubType(DES_MOTION_FOR_JUDGMENT_MOL);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isInstanceOf("Judgment")) {
				setSubType(DES_MOTION_FOR_JUDGMENT);
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			} else if (e2.isKindOf("DocLegalOrder")) {// motion for protective order
				List<Entity> attrs = g.getModifierList(e2);
				if (attrs != null) {
					for (Entity et : attrs) {
						if (et.isKindOf("StatusScheduling")) {
							setSubType(DES_SCHEDULING_ORDER);
							eventCategory = DocketEntry.EVENT_PROCEDURAL;
						} else if (et.isKindOf("StatusProtective")) {
							setSubType(DES_MOTION_FOR_PROTECTIVE_ORDER);
							eventCategory = DocketEntry.EVENT_DISPOSITIVE;
						}
					}
				}
			} else {
				setSubType(DE_MOTION + "_FOR_" + e2.getName().toUpperCase());
			}
		}
		findAllReferences(plist);
	}

	private void handleComplaint(Analysis a) {
		// Case: 2000001681, 1:10-cv-00004-LPS
		// Initial complaint
		// Amended complaint
		// second amended complaint
		List<Phrase> plist = a.getPhraseList();
		eventCategory = DocketEntry.EVENT_DISPOSITIVE;
		type = DE_COMPLAINT;
		subType = DES_COMPLAINT;
		if (deIndex == 1) {
			subType = DES_INITIAL_COMPLAINT;
		}
		litiPhase = PHASE_PLEADINGS;
		ERGraph g = plist.get(0).getGraph();
		Entity hd = g.getHead();
		List<Entity> elist = g.getModifierList(hd);
		if (elist != null) {
			for (Entity e : elist) {
				if (e.isKindOf("StatusAmended")) {
					subType = DES_AMENDED_COMPLAINT;
				}
			}
		}

		for (Phrase ph : plist) {
			g = ph.getGraph();
			List<Entity> le = g.findEntityByExactName("ProcJuryDemand");
			if (le != null && le.size() > 0) {
				addContent("with", "Jury Demand");
			}
			Link lk = g.containLink("against", (Entity) null, null);
			Entity e2 = null;
			if (lk != null) {
				e2 = lk.getArg2();
			} else {//	(775:against) PRPRelation (776:OrgCoKyoceraCorporation) 
				lk = g.containLink("PRPRelation", "against", null);
				if (lk != null) {
					e2 = lk.getArg2();
				}
			}
			if (e2 != null) {
				List<Link> lklist = null;
				if ((lklist = g.containLinkList("hasMember", e2, null)) != null) {
					for (Link lk1 : lklist) {
						e2 = lk1.getArg2();
						addContent("against", e2.getName());
					}
				} else {
					addContent("against", e2.getName());
				}
			}
		}
	}

	private void handleResponse(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		eventCategory = DocketEntry.EVENT_DISPOSITIVE;

		ERGraph g = ph.getGraph();
		type = DE_RESPONSE;
		subType = DES_RESPONSE;
		if (hd.getName().equals("DocLegalObjection")) {
			subType = DES_OBJECTION;
		}
		findAllReferences(plist);
	}

	private void handleMemorandum(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		eventCategory = DocketEntry.EVENT_DISPOSITIVE;

		ERGraph g = ph.getGraph();
		type = DE_MEMORANDUM;
		subType = DES_MEMORANDUM;
		List<Entity> le = g.getModifierList(hd);
		if (le != null) {
			for (Entity e : le) {
				if (e.isInstanceOf("StatusPretrial")) {
					litiPhase = DocketEntry.PHASE_PRE_TRIAL;
				}
			}
		}
		findAllReferences(plist);
	}

	private void handleNoticeConsentReferralForm(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_NOTICE_CONSENT_REFERRAL;
		subType = DE_NOTICE_CONSENT_REFERRAL;
		eventCategory = DocketEntry.EVENT_MISCELLANEURS;
		Link lk = g.containLink("re", hd, null);
		if (lk != null) {
			Entity e2 = lk.getArg2();
			Link lk1 = g.containLink("define", null, e2);
			if (lk1 != null) {
				addContent("regarding", lk1.getArg1().getName() + " " + e2.getName());
			} else {
				addContent("regarding", e2.getName());
			}
		}
	}

	private void handleReport(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		eventCategory = DocketEntry.EVENT_MISCELLANEURS;

		ERGraph g = ph.getGraph();
		type = DE_REPORT;
		if (hd.isInstanceOf("DocStatusReport")) {
			subType = DES_STATUS_REPORT;
		}
		Link lk = g.containLink("toPreposition", hd, null);
		if (lk != null) {
			// CommissionerPatentTrademark
			Entity e2 = lk.getArg2();
			subType = "Report to " + e2.getName();
			if (e2.getName().equalsIgnoreCase("CommissionerPatentTrademark")) {
				subType = DES_REPORT_TO_COMMISSION_PATENT_TRADEMARK;
				List<String> patents = new ArrayList<String>();
				for (Phrase p : plist) {
					// (674:NumberClass) hasValue (675:5,365,450)
					g = p.getGraph();
					lk = g.containLink("hasValue", null, "NumberValue");
					if (lk != null) {
						Entity ep = lk.getArg2();
						List<Link> lklist = null;
						if ((lklist = g.containLinkList("hasMember", ep, null)) != null) {
							for (Link lk1 : lklist) {
								Entity em = lk1.getArg2();
								//								addContent("Patent", em.getName());
								patents.add(em.getName());
							}
						} else {
							//							addContent("Patent", ep.getName());
							patents.add(ep.getName());
						}
					} else {
						List<Entity> le = g.findEntityByClass("NumberValue");
						if (le != null) {
							for (Entity ep : le) {
								List<Link> lklist = null;
								if ((lklist = g.containLinkList("hasMember", ep, null)) != null) {
									for (Link lk1 : lklist) {
										Entity em = lk1.getArg2();
										String name = em.getName();
										if (Character.isDigit(name.charAt(0)) && (name.length() >= 7 && name.length() <= 9)) {
											//											addContent("Patent", name);
											patents.add(name);
										}
									}
								} else {
									String name = ep.getName();
									if (Character.isDigit(name.charAt(0)) && (name.length() >= 7 && name.length() <= 9)) {
										//										addContent("Patent", name);
										patents.add(name);
									}
								}
							}
						}
					}
				}
				for (String p : patents) {
					addContent("Patent", p);
				}
				casedata.addPatents(patents);
			}
		}
	}

	private void handleSubpoena(Analysis a) {
		// Summons Issued as to
		// SUMMONS Returned Executed
		eventCategory = DocketEntry.EVENT_PROCEDURAL;
		type = DE_SUBPOENA;
		List<Phrase> plist = a.getPhraseList();
		Phrase phhead = plist.get(0);
		Entity hd = phhead.getHead();
		ERGraph g = phhead.getGraph();
		//(755:ProcIssueDoc:INSTANCE:(61:ProcIssueDoc))
		List<Entity> le = g.findEntityByExactName("ProcIssueDoc");
		if (le != null && le.size() > 0) {
			subType = DES_SUBPOENA_ISSUE;
		}
		Link lk = g.containLink("as_to", hd, null);
		if (lk != null) {
			Entity e1 = lk.getArg2();
			Link lk2 = g.containLink("on", null, "DateValue");
			if (lk2 != null) {
				Entity e2 = lk2.getArg2();
				addContent(e1.getName(), e2.getName());
			}
		} else {
			Link lk1 = g.containLink("hasAttribute", hd, "StatusEmptySet");
			if (lk1 == null) {
				subType = DES_SUBPOENA_RETURN_EXECUTED;
			}
		}
		if (subType != null) {
			for (int i = 1; i < plist.size(); i++) {
				Phrase ph = plist.get(i);
				g = ph.getGraph();
				//	(787:OrgCoQualcommInc) on (791:3/23/2009) ==> topLink
				Link lk2 = g.containLink("on", "OrgCo", "DateValue");
				if (lk2 != null) {
					Entity e1 = lk2.getArg1();
					Entity e2 = lk2.getArg2();
					addContent(e1.getName(), "on " + e2.getName());
				}
			}
		}
	}

	private void handleSummons(Analysis a) {
		// Summons Issued as to
		// SUMMONS Returned Executed
		eventCategory = DocketEntry.EVENT_PROCEDURAL;
		type = DE_SUMMONS;
		//		litiPhase = DocketEntry.PHASE_PLEADINGS;
		List<Phrase> plist = a.getPhraseList();
		Phrase phhead = plist.get(0);
		Entity hd = phhead.getHead();
		ERGraph g = phhead.getGraph();
		//(755:ProcIssueDoc:INSTANCE:(61:ProcIssueDoc))
		List<Entity> le = g.findEntityByExactName("ProcIssueDoc");
		if (le != null && le.size() > 0) {
			subType = DES_SUMMONS_ISSUED;
		}
		Link lk = g.containLink("as_to", hd, null);
		if (lk != null) {
			Entity e1 = lk.getArg2();
			Link lk2 = g.containLink("on", null, "DateValue");
			if (lk2 != null) {
				Entity e2 = lk2.getArg2();
				addContent(e1.getName(), e2.getName());
			}
		} else {
			Link lk1 = g.containLink("hasAttribute", hd, "StatusEmptySet");
			if (lk1 == null) {
				subType = DES_SUMMONS_RETURN_EXECUTED;
			} else {
				subType = DES_SUMMONS_NO;
				eventCategory = DocketEntry.EVENT_MISCELLANEURS;
			}
		}
		if (subType != null && !subType.equals(DES_SUMMONS_NO)) {
			for (int i = 1; i < plist.size(); i++) {
				Phrase ph = plist.get(i);
				g = ph.getGraph();
				//	(787:OrgCoQualcommInc) on (791:3/23/2009) ==> topLink
				Link lk2 = g.containLink("on", "OrgCo", "DateValue");
				if (lk2 != null) {
					Entity e1 = lk2.getArg1();
					Entity e2 = lk2.getArg2();
					addContent(e1.getName(), "on " + e2.getName());
				}
			}
		}
	}

	private void findOneDefine(ERGraph g, Entity e, String attribute, String attributeTag, List<Entity> list) {
		List<Link> llls = g.containLinkList(attribute, null, e);
		if (llls != null) {
			for (Link lll : llls) {
				Entity e1 = lll.getArg1();
				List<Link> lks = g.containLinkList("hasMember", e1, null);
				if (lks != null) {
					for (Link lk : lks) {
						Entity e2 = lk.getArg2();
						addContent(attributeTag, e2.getName());
						list.add(e2);
					}
				} else {
					addContent(attributeTag, e1.getName());
					list.add(e1);
				}
			}
		}
	}

	private void findOneAttribute(ERGraph g, Entity e, String attribute, String attributeTag, List<Entity> list) {
		List<Link> llls = g.containLinkList(attribute, e, null);
		if (llls != null) {
			for (Link lll : llls) {
				Entity e1 = lll.getArg2();
				List<Link> lks = g.containLinkList("hasMember", e1, null);
				if (lks != null) {
					for (Link lk : lks) {
						Entity e2 = lk.getArg2();
						addContent(attributeTag, e2.getName());
						list.add(e2);
					}
				} else {
					addContent(attributeTag, e1.getName());
					list.add(e1);
				}
			}
		}
	}

	private List<Entity> processOneEntity(ERGraph g, Entity e) {
		List<Entity> nextlist = new ArrayList<Entity>();
		findOneAttribute(g, e, "hasAttribute", "Attribute", nextlist);
		findOneAttribute(g, e, "hasMeasure", "Measure", nextlist);
		findOneAttribute(g, e, "hasOwner", "Owner", nextlist);
		findOneAttribute(g, e, "hasValue", "Value", nextlist);
		findOneDefine(g, e, "define", "Attribute", nextlist);
		return nextlist;
	}

	private void findTargets(ERGraph g, Entity e) {
		List<Link> llls = g.containLinkList("toPreposition", e, null);
		if (llls != null) {
			for (Link lll : llls) {
				Entity e4 = lll.getArg2();
				List<Link> lls = g.containLinkList("hasMember", e4, null);
				if (lls != null) {
					for (Link llk : lls) {
						Entity e3 = llk.getArg2();
						addContent("to", e3.getName());
					}
				} else {
					addContent("to", e4.getName());
				}
			}
		}
	}

	private void findAllAttributesList(ERGraph g, List<Entity> elist) {
		while (elist.size() > 0) {
			List<Entity> nextlist = new ArrayList<Entity>();
			for (Entity e : elist) {
				List<Entity> dlist = processOneEntity(g, e);
				if (dlist != null) {
					nextlist.addAll(dlist);
				}
			}
			elist = nextlist;
		}
	}

	private void processEntity(ERGraph g, Entity e) {
		List<Entity> elist = new ArrayList<Entity>();
		elist.add(e);
		findAllAttributesList(g, elist);
		findTargets(g, e);
	}

	private boolean lookForEntityInParse(List<Phrase> plist, String entityName, String tagName) {
		for (Phrase ph : plist) {
			ERGraph g = ph.getGraph();
			List<Entity> le = g.findEntityByClass(entityName);
			if (le != null) {
				if (entityName.equals("ProcRequest")) {
					distinguishRequest(g, le.get(0));
				} else {
					setSubType(tagName);
					for (Entity e : le) {
						processEntity(g, e);
					}
				}
				return true;
			}
		}
		return false;
	}

	private boolean RelaxedSearch(List<Phrase> plist) {
		if (lookForEntityInParse(plist, "DocLegalResponse", DES_RESPONSE_TO_DISCOVERY))
			return true;
		if (lookForEntityInParse(plist, "DocLegalInterrogatory", DES_INTERROGATORY))
			return true;
		if (lookForEntityInParse(plist, "ProcRequest", DES_REQUEST_FOR_PRODUCTION))
			return true;
		if (lookForEntityInParse(plist, "DocLegalObjection", DES_RESPONSE_TO_DISCOVERY))
			return true;
		if (lookForEntityInParse(plist, "DocLegalDisclosure", DES_DISCLOSURE))
			return true;
		if (lookForEntityInParse(plist, "DocLegalDisclosureStatement", DES_DISCLOSURE))
			return true;
		if (lookForEntityInParse(plist, "DocLegalSubpoena", DES_SUBPOENA))
			return true;
		if (lookForEntityInParse(plist, "DocLegalExpertReport", DES_EXPERT_REPORT))
			return true;
		if (lookForEntityInParse(plist, "Deposition", DES_DEPOSITION))
			return true;
		return false;
	}

	private void distinguishRequest(ERGraph g, Entity e2) {
		Link ll = g.containLink("for", e2, null);
		if (ll != null) {
			Entity e3 = ll.getArg2();
			if (e3.getName().equals("ProcProduction")) {
				setSubType(DES_REQUEST_FOR_PRODUCTION);
			} else if (e3.getName().equals("ProcAdmission")) {
				setSubType(DES_REQUEST_FOR_ADMISSION);
			} else if (e3.getName().equals("ProcInspection")) {
				setSubType(DES_REQUEST_FOR_INSPECTION);
			} else {
				setSubType(DES_REQUEST_FOR_PRODUCTION);
			}
			processEntity(g, e2);
			//			List<Link> llls = g.containLinkList("hasAttribute", e2, null);
			//			if (llls != null) {
			//				for (Link lll : llls) {
			//					Entity e4 = lll.getArg2();
			//					addContent("Request Attribute", e4.getName());
			//				}
			//			}
		}
	}

	private void setSubType(String tp) {
		if (subType == null) {
			subType = tp;
		}
	}

	private void handleNotice(Analysis a) {
		// NOTICE of SUGGESTION OF BANKRUPTCY
		// Notice of voluntary dismissal
		// NOTICE OF SERVICE
		// NOTICE of Withdrawal of Counsel
		// NOTICE of Change of Firm Affiliation
		// NOTICE of Change of Firm Name
		// NOTICE to Take Deposition
		// NOTICE of related case
		// NOTICE of Issuance of Subpoenas
		// NOTICE OF SERVICE of Expedia Inc.'s 1st Set of Interrogatories and
		// Expedia Inc.'s Requests for Production
		// NOTICE OF SERVICE of SUPPLEMENTAL INITIAL DISCLOSURES
		// NOTICE OF APPEAL
		// NOTICE of Docketing Record on Appeal
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_NOTICE;
		subType = null;
		Link lk = g.containLink("of", hd, null);
		if (lk != null) {
			Entity cause = lk.getArg2();
			String name = cause.getName();
			if (name.equalsIgnoreCase("ProcDismiss")) {
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
				Link lk1 = g.containLink("as_to", hd, "OrgCo");
				if (lk1 != null) {
					String org = lk1.getArg2().getName();
					boolean bVoluntary = false;
					Link lk2 = g.containLink("hasAttribute", cause, null);
					if (lk2 != null) {//NOTICE of Voluntary Dismissal by NetDeposit LLC as to Wausau Financial Systems, Inc. (Moore, David) (Entered: 04/20/2011)
						String attr = lk2.getArg2().getName();
						if (attr.equals("StatusVoluntary")) {
							bVoluntary = true;
						}
					}
					if (!bVoluntary) {
						LitiEvent le = new LitiEvent(LitiEvent.LE_PARTY_TERMINATE, org, eventDate.toString(), serial, deIndex);
						casedata.addDismissal(le);
					} else {
						LitiEvent le = new LitiEvent(LitiEvent.LE_VOLUNTARY_DISMISSAL, org, eventDate.toString(), serial, deIndex);
						casedata.addDismissal(le);
					}
				}
			} else if (name.equalsIgnoreCase("DocLegalAppeal") || name.equalsIgnoreCase("ProcAppeal")) {
				subType = DES_NOTICE_OF_APPEAL;
				lk = g.containLink("toPreposition", hd, null);
				if (lk != null) {
					Entity e2 = lk.getArg2();
					addContent("To", e2.getName());
				}
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
				this.litiPhase = DocketEntry.PHASE_APPEAL;
				drs = new ArrayList<DockRef>();
				findAllReferences(plist);
			} else if (name.equalsIgnoreCase("ProcIssueDoc")) {
				lk = g.containLink("of", cause, "DocLegalSubpoena");
				if (lk != null) {
					subType = DES_SUBPOENA_ISSUE;
					litiPhase = DocketEntry.PHASE_DISCOVERY;
				}
				eventCategory = DocketEntry.EVENT_EVIDENTIARY;
			} else if (name.equalsIgnoreCase("DocLegalSubpoena")) {
				subType = DES_SUBPOENA_ISSUE;
				eventCategory = DocketEntry.EVENT_EVIDENTIARY;
				litiPhase = DocketEntry.PHASE_DISCOVERY;
			} else if (name.equalsIgnoreCase("ProcServeDoc")) {
				type = DE_NOTICE_OF_SERVICE;
				lk = g.containLink("of", cause, null);
				if (lk != null) {
					eventCategory = DocketEntry.EVENT_EVIDENTIARY;
					// first look for the right things:
					// initial disclosure
					// request for production of documents
					// set of document requests
					// set of interrogatories
					// subpoena
					// response to request for production
					// response to set of interrogatories
					//
					Entity e1 = lk.getArg2();
					if (e1.isKindOf("OrgCo")) {
						// Notice of Service of OrgCo interrogatories ...
						// parsing somehow break sentence into two "Notice of Service of OrgCo" and "interrogatories ..."

					}
					List<Entity> le1 = new ArrayList<Entity>();
					List<Link> lks1 = g.containLinkList("hasMember", e1, null);
					if (lks1 != null) {
						for (Link lk1 : lks1) {
							le1.add(lk1.getArg2());
						}
					} else {
						le1.add(e1);
					}
					for (Entity e2 : le1) {
						String e1name = e2.getName();
						if (e1name.equals("ProcRequest")) {
							// 1. Request for Production:
							distinguishRequest(g, e2);
							litiPhase = DocketEntry.PHASE_DISCOVERY;
						} else if (e1name.equals("DocLegalInterrogatory")) {
							setSubType(DES_INTERROGATORY);
							litiPhase = DocketEntry.PHASE_DISCOVERY;
							processEntity(g, e1);
						} else if (e1name.equals("DocLegalDisclosure") || e1name.equals("DocLegalDisclosureStatement")) {
							setSubType(DES_DISCLOSURE);
							litiPhase = DocketEntry.PHASE_DISCOVERY;
							processEntity(g, e1);
						} else if (e1name.equals("DocLegalSubpoena")) {
							setSubType(DES_SUBPOENA);
							litiPhase = DocketEntry.PHASE_DISCOVERY;
							processEntity(g, e1);
						} else if (e1name.equals("DocLegalResponse") || e1name.equals("DocLegalObjection")) {
							setSubType(DES_RESPONSE_TO_DISCOVERY);
							litiPhase = DocketEntry.PHASE_DISCOVERY;
							processEntity(g, e1);
						} else if (e1name.equals("DocLegalExpertReport")) {
							setSubType(DES_EXPERT_REPORT);
							litiPhase = DocketEntry.PHASE_DISCOVERY;
							processEntity(g, e1);
						} else if (e1name.equals("DocLegalReport")) {
							List<Entity> le2 = g.getModifierList(e1);
							if (le2 != null) {
								for (Entity em : le2) {
									if (em.isInstanceOf("Expert")) {
										setSubType(DES_EXPERT_REPORT);
									}
								}
							}
							litiPhase = DocketEntry.PHASE_DISCOVERY;
							processEntity(g, e1);
						} else if (e1name.equals("Deposition")) {
							setSubType(DES_DEPOSITION);
							litiPhase = DocketEntry.PHASE_DISCOVERY;
							processEntity(g, e1);
						}
					}
					if (subType == null) {
						if (RelaxedSearch(plist)) {
							litiPhase = DocketEntry.PHASE_DISCOVERY;
						}
					}
				} else {
					if (RelaxedSearch(plist)) {
						litiPhase = DocketEntry.PHASE_DISCOVERY;
						eventCategory = DocketEntry.EVENT_EVIDENTIARY;
					}
				}
			} else if (name.equalsIgnoreCase("ProcAppearance")) {
				setSubType(DES_LAWYER_APPEARANCE);
				lk = g.containLink("onBehalfOf", null, "OrgCo");
				if (lk != null) {
					Entity e2 = lk.getArg2();
					List<Link> lklist = null;
					if ((lklist = g.containLinkList("hasMember", e2, null)) != null) {
						for (Link lk1 : lklist) {
							e2 = lk1.getArg2();
							addContent("onBehalfOf", e2.getName());
						}
					} else {
						addContent("onBehalfOf", e2.getName());
					}
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else if (name.equalsIgnoreCase("DocEntry")) {
				lk = g.containLink("of", cause, "ProcAppearance");
				if (lk != null) {
					setSubType(DES_LAWYER_APPEARANCE);
				}
				lk = g.containLink("onBehalfOf", null, "OrgCo");
				if (lk != null) {
					Entity e2 = lk.getArg2();
					List<Link> lklist = null;
					if ((lklist = g.containLinkList("hasMember", e2, null)) != null) {
						for (Link lk1 : lklist) {
							e2 = lk1.getArg2();
							addContent("onBehalfOf", e2.getName());
						}
					} else {
						addContent("onBehalfOf", e2.getName());
					}
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else if (name.equalsIgnoreCase("LegalCase")) {
				//				(660:LegalCase) hasAttribute (659:StatusRelated)
				//				(657:DocLegalNotice) of (660:LegalCase) ==> topLink
				lk = g.containLink("hasAttribute", cause, "StatusRelated");
				if (lk != null) {
					setSubType(DES_NOTICE_RELATED_CASE);
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else if (name.equalsIgnoreCase("Thing")) {
				Entity eeof = null;
				Link lkof = g.containLink("of", cause, null);
				if (lkof != null) {
					eeof = lkof.getArg2();
				}
				List<Link> lks = g.containLinkList("hasMember", cause, null);
				if (lks != null) {
					for (Link lk1 : lks) {
						Entity ee = lk1.getArg2();
						if (ee.getName().equals("ProcWithdraw")) {
							if (eeof != null && eeof.equals("Counsel")) {
								subType += " " + DES_WITHDRAW_ATTORNEY;
							} else {
								subType += "WITHDRAW";
							}
						} else if (ee.getName().equals("ProcSubstitute")) {
							if (eeof != null && eeof.equals("Counsel")) {
								subType += " " + DES_SUBSTITUTE_ATTORNEY;
							} else {
								subType = "SUBSTITUTION";
							}
						}
					}
				}
				lk = g.containLink("hasAttribute", cause, "StatusRelated");
				if (lk != null) {
					setSubType(DES_NOTICE_RELATED_CASE);
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else {
				Entity e2 = lk.getArg2();
				subType = "Notice " + onto.translate(e2.getName());
			}

		} else if ((lk = g.containLink("ToInfinitive", hd, "ProcTake")) != null) {
			Entity e2 = lk.getArg2();
			lk = g.containLink("ugoerOfProc", "Deposition", e2);
			if (lk != null) {
				setSubType(DES_DEPOSITION);
			}
			eventCategory = DocketEntry.EVENT_EVIDENTIARY;
			litiPhase = DocketEntry.PHASE_DISCOVERY;
		}
		// Need to dig other phrases: (do that later)
		if (subType == null) {
			List<Entity> modifiers = g.getModifierList(hd);
			if (modifiers != null) {
				for (Entity e : modifiers) {
					if (e.isKindOf("ProcRedact")) {
						type = DE_REDACTION_NOTICE;
						subType = DES_REDACTION_NOTICE;
						eventCategory = DocketEntry.EVENT_MISCELLANEURS;
						return;
					}
				}
			}
			List<Phrase> pplist = new ArrayList<Phrase>(plist);
			while (pplist.size() > 1) {
				pplist.remove(0);
				hd = pplist.get(0).getHead();
				Analysis aa = new Analysis(pplist);
				boolean b = handleHeadEntity(hd, aa);
				if (subType != null) {
					break;
				}
			}
			if (!type.equals(DE_NOTICE_OF_SERVICE)) {
				type = DE_NOTICE;// restore type
			}
		}
		findAllReferences(plist);
	}

	private void handleCaseAssign(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		type = DE_CASE;
		ERGraph g = ph.getGraph();
		Link lk = g.containLink("ugoerOfProc", hd, null);
		if (lk != null) {
			Entity e = lk.getArg2();
			if (e.getName().equals("ProcAssign")) {
				lk = g.containLink("toPreposition", hd, null);
				if (lk != null) {
					String judge = lk.getArg2().getName();
					addContent("Assigned to", judge);
					LitiEvent le = new LitiEvent(LitiEvent.LE_JUDGE_ASSIGN, judge, eventDate.toString(), serial, deIndex);
					casedata.addJudgeAssign(le);
				}
				setSubType(DES_CASE_ASSIGN);
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else if (e.getName().equals("ProcClose")) {
				subType = DES_CASE_TERMINATE;
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
				litiPhase = PHASE_TERMINATE;
				LitiEvent le = new LitiEvent(LitiEvent.LE_TERMINATE, null, eventDate.toString(), serial, deIndex);
				casedata.addTerminate(le);
			} else if (e.getName().equals("ProcRefer")) {
				subType = DES_CASE_REFER;
				lk = g.containLink("toPreposition", hd, null);
				if (lk != null) {
					String judge = lk.getArg2().getName();
					addContent("Referred to", judge);
					LitiEvent le = new LitiEvent(LitiEvent.LE_JUDGE_REFER, judge, eventDate.toString(), serial, deIndex);
					casedata.addJudgeAssign(le);
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else if (e.getName().equals("ProcTransfer")) {
				subType = DES_CASE_TRANSFER;
				lk = g.containLink("toPreposition", hd, null);
				if (lk != null) {
					String court = lk.getArg2().getName();
					addContent("Case Transferred to", court);
					LitiEvent le = new LitiEvent(LitiEvent.LE_CASE_TRANSFER, court, eventDate.toString(), serial, deIndex);
					casedata.addLitiEvents(le);
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
			} else {
				;
			}
		}
	}

	private void handleDisclosureStatement(Analysis a) {
		List<Phrase> plist = a.getPhraseList();

		eventCategory = DocketEntry.EVENT_EVIDENTIARY;
		type = DE_DISCLOSURE_STATEMENT;

		for (Phrase ph : plist) {
			ERGraph g = ph.getGraph();
			Link lk = g.containLink("hasValue", "LegalRule", null);
			if (lk != null) { //(671:LegalRule) hasValue (672:7.1)
				Entity e2 = lk.getArg2();
				subType = "Rule " + e2.getName();
				break;
			}
		}
	}

	private void handleStatement(Analysis a) {
		List<Phrase> plist = a.getPhraseList();

		eventCategory = DocketEntry.EVENT_EVIDENTIARY;
		type = DE_STATEMENT;
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		if (hd.isInstanceOf("JointClaimConstructionAndPrehearingStatement")) {
			subType = DES_CLAIMCONSTRUCTION_PREHEARING_STATEMENT;
			litiPhase = DocketEntry.PHASE_CLAIMCONSTRUCTION;
		}
		findAllReferences(plist);
	}

	static Pattern ptSoOrdered = Pattern.compile("so(\\s*|-)order", Pattern.CASE_INSENSITIVE);

	public void addRef(DockRef dr) {
		if (drs == null) {
			drs = new ArrayList<DockRef>();
		}
		drs.add(dr);
	}

	public void addForwardRef(DockRef dr) {
		if (frs == null) {
			frs = new ArrayList<DockRef>();
		}
		frs.add(dr);
	}

	public List<DockRef> getDockRefs() {
		return drs;
	}

	private void addDockRef(String tp, Entity hd, int referred) {
		DockRef dr = new DockRef();
		dr.setSourceEntity(type);
		dr.setSubType(tp);
		dr.setTargetEntity(hd.getName());
		dr.setSourceNumbers(serial, this.deIndex);
		dr.setTargetIndex(referred);
		drs.add(dr);
	}

	private boolean inDockRefList(Entity e1, int referred) {
		if (drs == null) {
			return false;
		}
		for (DockRef dr : drs) {
			if (dr.targetIndex == referred) {
				return true;
			}
		}
		return false;
	}

	private void findOrderActionRefs(ERGraph g, Entity e1, String action) {
		if (drs == null) {
			drs = new ArrayList<DockRef>();
		}
		findOrderActionRefs(g, action, "hasSerial", e1, null);
		findOrderActionRefs(g, action, "hasAttribute", e1, "NumberValue");
	}

	private void findAllReferences(List<Phrase> plist) {
		for (Phrase p : plist) {
			ERGraph g = p.getGraph();
			findReferences(g);
		}
		findForwardRefs(plist);
	}

	private void findForwardRefs(List<Phrase> plist) {
		Entity hd = null;
		ERGraph gg = null;
		List<Link> kks = null;
		for (Phrase p : plist) {
			ERGraph g = p.getGraph();
			if (hd == null) {//(728:DocLegalMotion) hasAttribute (725:StatusFollowing)
				Link lk = g.containLink("hasAttribute", null, "StatusFollowing");
				if (lk != null) {
					hd = lk.getArg1();
					gg = g;
					kks = gg.containLinkList("ugoerOfProc", hd, null);
				}
			} else {
				// (733:DocketEntryIndex) hasValue (740:302)
				// (733:DocketEntryIndex) define (740:302)
				Link lk = g.containLink("hasValue", "DocketEntryIndex", "NumberValue");
				if (lk != null) {
					Entity e1 = lk.getArg2();
					List<Link> lks = g.containLinkList("hasMember", e1, null);//(740:302) hasMember (737:302)
					if (lks != null) {
						for (Link ll : lks) {
							Entity e2 = ll.getArg2();
							String num = e2.getName();
							int referred = Integer.parseInt(num);
							if (num != null && !inDockRefList(hd, referred)) {
								if (kks != null) {
									for (Link kk : kks) {
										Entity ek = kk.getArg2();
										String action = onto.translate(ek.getName());
										if (ek.isKindOf("ProcDeny")) {
											action = "Deny";
										} else if (ek.isKindOf("ProcGrant")) {
											action = "Grant";
										}
										addDockRef(action, hd, referred);
										addContent(action, referred + " " + hd.getName());
									}
								} else {
									addDockRef(subType, hd, referred);
									//									addContent("referred", referred + " " + hd.getName());
								}
							}
						}
					} else {
						String num = e1.getName();
						int referred = Integer.parseInt(num);
						if (num != null && !inDockRefList(hd, referred)) {
							if (kks != null) {
								for (Link kk : kks) {
									Entity ek = kk.getArg2();
									String action = onto.translate(ek.getName());
									if (ek.isKindOf("ProcDeny")) {
										action = "Deny";
									} else if (ek.isKindOf("ProcGrant")) {
										action = "Grant";
									}
									addDockRef(action, hd, referred);
									addContent(action, referred + " " + hd.getName());
								}
							} else {
								addDockRef(subType, hd, referred);
								//								addContent("referred", referred + " " + hd.getName());
							}
							//							addDockRef(subType, hd, referred);
							//							addContent("referred", referred + " " + hd.getName());
						}
					}
				}
			}
		}
	}

	private void findReferences(ERGraph g) {
		if (drs == null) {
			drs = new ArrayList<DockRef>();
		}
		findReferences(g, "hasSerial", null, null);
		findReferences(g, "hasAttribute", null, "NumberValue");
		findReferences(g, "PRPRelation", "re", "NumberValue");
	}

	private void findReferences(ERGraph g, String linkName, String e1name, String e2name) {
		List<Link> lks = g.containLinkList(linkName, e1name, e2name);
		if (lks != null) {
			for (Link ll : lks) {
				Entity e1 = ll.getArg1();
				Entity numE = ll.getArg2();
				if (e1.isKindOf("Document") || e1.isKindOf("Process") || e1.isInstanceOf("re")) {
					String num = numE.getName();
					Link lk = g.containLink("in", numE, null);
					if (lk != null) {
						Entity e2 = lk.getArg2();
						if (!ERGraph.caseNumberCompatible(e2.getName(), csnumber.raw)) {
							continue;
							// DED order text can contain motion of other cases.
							// See case 2000007339 (1:11-cv-00313-SLR), DE 315
						}
					} else {
						// DED order text can contain motion of other cases in arrays
						// See case 2000007339 (1:11-cv-00313-SLR), order between DE332 DE333
						// re(318 in 1:11-cv-00313-SLR, 110 in 1:12-cv-00140-SLR, 86 in 1:12-cv-00141-SLR) 
						Link lk2 = g.containLink("hasMember", numE, null);
						if (lk2 != null) {
							num = null;
							List<Entity> le = g.findCasenumbers(csnumber);
							if (le == null) {
								continue;
							}
							Entity e0 = le.get(0);
							lk = g.containLink("in", "NumberValue", e0);
							if (lk != null) {
								Entity e2 = lk.getArg1();
								lk2 = g.containLink("hasMember", numE, e2);
								if (lk2 != null) {
									num = e2.getName();
								}
							}
						}
					}
					try {
						int referred = Integer.parseInt(num);
						if (num != null && !inDockRefList(e1, referred)) {
							addDockRef(subType, e1, referred);
							//							addContent("referred", referred + " " + e1.getName());
						}
					} catch (Exception ex) {
						;
					}
				}
			}
		}
	}

	private void findOrderActionRefs(ERGraph g, String action, String linkName, Entity e1, String e2name) {
		Link ll = g.containLink(linkName, e1, e2name);
		if (ll == null) {
			return;
		}
		Entity numE = ll.getArg2();
		String num = numE.getName();
		Link lk = g.containLink("in", numE, null);
		if (lk != null) {
			Entity e2 = lk.getArg2();
			if (!ERGraph.caseNumberCompatible(e2.getName(), csnumber.raw)) {
				return;
				// DED order text can contain motion of other cases.
				// See case 2000007339 (1:11-cv-00313-SLR), DE 315
			}
		} else {
			// DED order text can contain motion of other cases in arrays
			// See case 2000007339 (1:11-cv-00313-SLR), order between DE332 DE333
			// re(318 in 1:11-cv-00313-SLR, 110 in 1:12-cv-00140-SLR, 86 in 1:12-cv-00141-SLR) 
			Link lk2 = g.containLink("hasMember", numE, null);
			if (lk2 != null) {
				num = null;
				List<Entity> le = g.findCasenumbers(csnumber);
				if (le == null) {
					return;
				}
				Entity e0 = le.get(0);
				lk = g.containLink("in", "NumberValue", e0);
				if (lk != null) {
					Entity e2 = lk.getArg1();
					lk2 = g.containLink("hasMember", numE, e2);
					if (lk2 != null) {
						num = e2.getName();
					}
				}
			}
		}
		try {
			int referred = Integer.parseInt(num);
			if (num != null && !inDockRefList(e1, referred)) {
				addDockRef(action, e1, referred);
				addContent(action, num + " " + e1.getName());
			}
		} catch (Exception ex) {
			;
		}
	}

	private void handleOrder(Analysis a) {
		// SO ORDERED
		// propose order
		// order that
		// it is ordered
		// SCHEDULING ORDER
		// ORDER REFERRING CASE to Clerk
		// ORDER appointing Special Master
		// CASE REGULATION ORDER
		// ORDER REGARDING THE FEES AND EXPENSES OF THE SPECIAL MASTER
		// PROPOSED PROTECTIVE ORDER
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		ERGraph g = ph.getGraph();
		type = DE_ORDER;
		String txt = ph.getText();
		Matcher m = ptSoOrdered.matcher(txt);
		if (m.find()) {
			subType = DES_SO_ORDER;
			eventCategory = DocketEntry.EVENT_PROCEDURAL;
			// it also take the default panel type "Procedural"
		}
		List<String> actionlist = new ArrayList<String>();
		List<Entity> elist = g.getModifierList(hd);
		if (elist != null) {
			for (Entity e2 : elist) {
				if (e2.getName().equals("StatusProposed")) {
					type = DE_PROPOSED_ORDER;
					subType = type;
					findAllReferences(plist);
					return;
				} else if (e2.getName().equals("StatusScheduling")) {
					subType = DES_SCHEDULING_ORDER;
					eventCategory = DocketEntry.EVENT_PROCEDURAL;
					// we don't return here. We'll add content to the scheduling order below.
				} else if (e2.getName().equals("StatusRedacted")) {
					addContent("Order Attribute", "StatusRedacted");
				} else {
					String attr = e2.getName();
					addContent("Order Attribute", attr);
					if (attr.equals("Management")) {
						subType = DES_CASE_MANAGEMENT;
					} else if (attr.equals("USCA")) {
						subType = DES_USCA_ORDER;
						eventCategory = DocketEntry.EVENT_DISPOSITIVE;
						litiPhase = DocketEntry.PHASE_APPEAL;
					}
				}
			}
		}
		//		(713:DocLegalOrder) ugoerOfProc (704:ProcAmend)
		//		(703:DocLegalOrder) agentOfProc (704:ProcAmend) ==> topLink

		Link lk = g.containLink("agentOfProc", hd, null);
		if (lk != null) {
			Entity e2 = lk.getArg2();
			if (e2.isKindOf("ProcAmend")) {
				Link lk1 = g.containLink("ugoerOfProc", "DocLegalOrder", e2);
				if (lk1 != null) {
					subType = DES_ORDER_AMEND_ORDER;
				}
			} else if (e2.isKindOf("ProcWithdraw")) {
				subType = DES_ORDER_TO_WITHDRAW;
				List<Link> lks = g.containLinkList("ugoerOfProc", null, e2);
				if (lks != null) {
					for (Link ll : lks) {
						Entity e1 = ll.getArg1();
						if (e1.isKindOf("DocLegalMotion")) {
							findOrderActionRefs(g, e1, "Withdraw");
							// should really be determined by the motions eventCategory that this order granted or denied.
							eventCategory = DocketEntry.EVENT_DISPOSITIVE;
						}
					}
				}
			}
		}
		lk = g.containLink("of", hd, "ProcDismiss");
		if (lk != null) {
			subType = DES_ORDER_OF_DISMISSAL;
		}
		lk = g.containLink("for", hd, "Judgment");
		if (lk != null) {
			subType = DES_ORDER_OF_JUDGMENT;
			LitiEvent le = new LitiEvent(LitiEvent.LE_JUDGMENT, null, eventDate.toString(), serial, deIndex);
			casedata.addJudgment(le);
		}
		for (int i = 0; i < plist.size(); i++) {
			ph = plist.get(i);
			g = ph.getGraph();
			Link lkt = g.containLink("ugoerOfProc", null, "ProcTerminate");
			if (lkt != null) {
				eventCategory = DocketEntry.EVENT_DISPOSITIVE;
				Entity e = lkt.getArg1();
				if (e.isKindOf("OrgCo")) {
					List<Entity> eelist = g.getMembers(e);
					StringBuilder sb = new StringBuilder();
					if (eelist != null) {
						for (Entity ee : eelist) {
							sb.append(ee.getName());
							sb.append(" ");
						}
					} else {
						sb.append(e.getName());
					}
					LitiEvent le = new LitiEvent(LitiEvent.LE_PARTY_TERMINATE, sb.toString().trim(), eventDate.toString(), serial, deIndex);
					casedata.addDismissal(le);
				} else if (e.isInstanceOf("LegalCase")) {
					addContent("Case", "Terminate");
					LitiEvent le = new LitiEvent(LitiEvent.LE_TERMINATE, subType, eventDate.toString(), serial, deIndex);
					casedata.addTerminate(le);
				} else if (e.isKindOf("Judgment")) {
					LitiEvent le = new LitiEvent(LitiEvent.LE_JUDGMENT, subType, eventDate.toString(), serial, deIndex);
					casedata.addJudgment(le);
				}
			}
			lkt = g.containLink("ugoerOfProc", "LegalCase", "ProcStay");
			if (lkt != null) {
				LitiEvent le = new LitiEvent(LitiEvent.LE_STAY, subType, eventDate.toString(), serial, deIndex);
				casedata.addTerminate(le);
			}
			lkt = g.containLink("ugoerOfProc", "LegalCase", "ProcClose");
			if (lkt != null) {
				LitiEvent le = new LitiEvent(LitiEvent.LE_TERMINATE, subType, eventDate.toString(), serial, deIndex);
				casedata.addTerminate(le);
			}
			if (g.containLink("ugoerOfProc", "Schedule", "ProcSetOrChangeSchedule") != null) {
				// Here contents are added to scheduling orders.
				// "Set brief schedule:" indicate from here on, it is the
				// content, not the references, so end reference processing:
				// This section can be greatly expanded.
				if (subType == null) {
					subType = DES_SET_OR_CHANGE_SCHEDULE;
				} else {
					addContent("including", DES_SET_OR_CHANGE_SCHEDULE);
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
				break;
			}
			if (g.containLink("ugoerOfProc", "TimeRef", "ProcSetOrChangeSchedule") != null) {
				// Here contents are added to scheduling orders.
				// "Set brief schedule:" indicate from here on, it is the
				// content, not the references, so end reference processing:
				// This section can be greatly expanded.
				if (subType == null) {
					subType = DES_SET_OR_CHANGE_SCHEDULE;
				} else {
					addContent("including", DES_SET_OR_CHANGE_SCHEDULE);
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
				break;
			}
			lkt = g.containLink("ugoerOfProc", "Meeting", "ProcSetOrChangeSchedule");
			if (lkt != null) {
				// "Set Rule 16(b) Conference" indicate from here on, it is the
				// content, not the references, so end reference processing:
				Entity e1 = lkt.getArg1();
				if (subType == null) {
					subType = DES_SET_CONFERENCE;
				} else {
					addContent("including", DES_SET_CONFERENCE);
				}
				List<Link> lks = g.containLinkList("define", null, e1);
				if (lks != null) {
					for (Link ll : lks) {
						String ctype = ll.getArg1().getName();
						Link lll = g.containLink("hasValue", ll.getArg1(), null);
						if (lll != null) {
							ctype += " " + lll.getArg2().getName();
						}
						addContent("conference type", ctype);
					}
				}
				eventCategory = DocketEntry.EVENT_PROCEDURAL;
				break;
			}
			List<Link> lks = g.containLinkList("ugoerOfProc", (Entity) null, null);
			if (lks != null) {
				for (Link ll : lks) {
					String action = null;
					Entity e1 = ll.getArg1();
					Entity e2 = ll.getArg2();
					String process = e2.getName();
					if (process.equals("ProcGrant")) {
						action = ACTION_GRANT;
					} else if (process.equals("ProcDeny")) {
						action = ACTION_DENY;
					} else if (process.equals("ProcFind")) {
						Link llk = g.containLink("as", e2, "Moot");
						if (llk != null) {
							action = ACTION_DENY;
						}
					}
					if (action != null) {
						//						setSubType(DES_ORDER_GRANT_DENY);
						findOrderActionRefs(g, e1, action);
						// should really be determined by the motions eventCategory that this order granted or denied.
						eventCategory = DocketEntry.EVENT_DISPOSITIVE;
						actionlist.add(action);
					}
				}
			}
			lks = g.containLinkList("re", "ProcDisposition", null);
			if (lks != null) {
				for (Link ll : lks) {
					String action = null;
					// notice e1 and e2 position is reverse
					Entity e2 = ll.getArg1();
					Entity e1 = ll.getArg2();
					String process = e2.getName();
					if (process.equals("ProcGrant")) {
						action = ACTION_GRANT;
					} else if (process.equals("ProcDeny")) {
						action = ACTION_DENY;
					} else if (process.equals("ProcFind")) {
						Link llk = g.containLink("as", e2, "Moot");
						if (llk != null) {
							action = ACTION_DENY; // "Moot"
						}
					}
					if (action != null) {
						//						setSubType(DES_ORDER_GRANT_DENY);
						findOrderActionRefs(g, e1, action);
						// should really be determined by the motions eventCategory that this order granted or denied.
						eventCategory = DocketEntry.EVENT_DISPOSITIVE;
						actionlist.add(action);
					}
				}
			}
			findReferences(g);
		}
		findForwardRefs(plist);
		if (actionlist.size() > 0) {
			int gcount = 0;
			int dcount = 0;
			for (String ac : actionlist) {
				if (ac.equals(ACTION_GRANT)) {
					gcount++;
				} else {
					dcount++;
				}
			}
			if (gcount > 0 && dcount > 0) {
				subType = DES_ORDER_GRANT_DENY;
			} else if (gcount > 0) {
				subType = DES_ORDER_GRANT;
			} else if (dcount > 0) {
				subType = DES_ORDER_DENY;
			}
		}
	}

	private void handleSchedule(Analysis a) {
		// SO ORDERED
		// propose order
		// order that
		// it is ordered
		// SCHEDULING ORDER
		// ORDER REFERRING CASE to Clerk
		// ORDER appointing Special Master
		// CASE REGULATION ORDER
		// ORDER REGARDING THE FEES AND EXPENSES OF THE SPECIAL MASTER
		// PROPOSED PROTECTIVE ORDER
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		ERGraph g = ph.getGraph();
		type = DE_SCHEDULE;
		subType = DES_SCHEDULE;
		eventCategory = DocketEntry.EVENT_PROCEDURAL;
		List<Entity> elist = g.getModifierList(hd);
		if (elist != null) {
			for (Entity e2 : elist) {
				if (e2.getName().equals("StatusProposed")) {
					addContent("status:", "Proposed");
				} else if (e2.getName().equals("StatusRedacted")) {
					addContent("attribute", "Redacted");
				}
			}
		}
	}

	/**
	 * Mandate is a circuit court decision. It can declare district court decision as "Vacated and Remanded", that is, invalid.
	 * Example, case 79781 DE 532, it declared a preliminary injunction as "Vacated and Remanded".
	 * 
	 * @param a
	 */
	private void handleMandate(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		ERGraph g = ph.getGraph();
		type = DE_MANDATE;
		subType = DES_USCA_MANDATE;
		eventCategory = DocketEntry.EVENT_DISPOSITIVE;
		litiPhase = DocketEntry.PHASE_APPEAL;
		// StringBuilder sb = new StringBuilder();
		drs = new ArrayList<DockRef>();
		for (int i = 0; i < plist.size(); i++) {
			ph = plist.get(i);
			g = ph.getGraph();
			findReferences(g, "hasSerial", null, null);
			findReferences(g, "hasAttribute", null, "NumberValue");
		}
		fromEntities.clear();
		Entity usca = new Entity("USCA", onto);
		fromEntities.add(usca);
		fromRole = ROLE_COURT;
		List<LexToken> sent = ph.getSentence();
		LexToken lasttok = sent.get(sent.size() - 1);
		int pos = lasttok.getEnd();
		String[] split = this.processedText.substring(0, pos).split("(?i:usca\\s*decision\\:?)");
		if (split.length > 1) {
			for (int i = 1; i < split.length; i++) {
				addContent("USCA Decision", split[i].trim());
			}
		}
	}

	private void handleAffidavit(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_AFFIDAVIT;
		subType = DES_AFFIDAVIT;
		Link lk = g.containLink("of", hd, null);
		if (lk != null) {
			Entity e2 = lk.getArg2();
			if (e2.getName().equals("ProcServeDoc")) {
				subType = DES_AFFIDAVIT_OF_SERVICE;
			}
		}
		lk = g.containLink("for", hd, null);
		if (lk != null) {
			addContent("for", lk.getArg2().getName());
		}
		findAllReferences(plist);
	}

	private void handleVerdictSheet(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_VERDICT_SHEET;
		subType = DES_VERDICT_SHEET;
		litiPhase = DocketEntry.PHASE_PRE_TRIAL;
		eventCategory = DocketEntry.EVENT_PROCEDURAL;
	}

	private void handleVoirDire(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_VOIR_DIRE;
		subType = DES_VOIR_DIRE;
		litiPhase = DocketEntry.PHASE_PRE_TRIAL;
		eventCategory = DocketEntry.EVENT_PROCEDURAL;
	}

	private void handleInstruction(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		List<Entity> le = g.getModifierList(hd);
		if (le != null) {
			for (Entity e : le) {
				if (e.isInstanceOf("HumanSetJury")) {
					type = DE_JURY_INSTRUCTION;
					setSubType(DES_JURY_INSTRUCTION);
				} else if (e.isInstanceOf("StatusProposed")) {
					subType = DES_PROPOSED;
				}
			}
		}
		litiPhase = DocketEntry.PHASE_PRE_TRIAL;
		eventCategory = DocketEntry.EVENT_PROCEDURAL;
	}

	private void handleAppendix(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_APPENDIX;
		subType = DES_APPENDIX;
		Link lk = g.containLink("of", hd, null);
		if (lk != null) {
			addContent("for", lk.getArg2().getName());
		}
		lk = g.containLink("for", hd, null);
		if (lk != null) {
			Entity e = lk.getArg2();
			addContent("for", e.getName());
			List<Entity> le = g.getModifierList(e);
			if (le != null) {
				for (Entity ee : le) {
					if (ee.isKindOf("LitiPhase")) {
						if (ee.isKindOf("ClaimConstruction")) {
							litiPhase = DocketEntry.PHASE_CLAIMCONSTRUCTION;
						} else if (ee.isKindOf("ProcTrial")) {
							litiPhase = DocketEntry.PHASE_TRIAL;
						}
					}
				}
			}
		}
		findAllReferences(plist);
	}

	private void handleHuman(Analysis a) {
		// electronic noticing
		type = "electronic noticing";
		eventCategory = DocketEntry.EVENT_MISCELLANEURS;
		subType = type;
	}

	private void handleAnswer(Analysis a) {
		// ANSWER to 37 Answer to Complaint, Counterclaim / Plaintiff Walker
		// Digital, LLC's Answer to Defendant eBay, Inc.'s Counterclaims by
		// Walker Digital LLC.
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		eventCategory = DocketEntry.EVENT_DISPOSITIVE;

		ERGraph g = ph.getGraph();
		type = DE_ANSWER;
		subType = DES_ANSWER;
		findAllReferences(plist);
		Link lk0 = g.containLink("toPreposition", hd, null);
		Entity e2 = null;
		boolean foundCounterClaim = false;
		List<Link> lks0 = g.containLinkList("hasAttribute", hd, null);
		if (lks0 != null) {
			for (Link lk : lks0) {
				Entity e3 = lk.getArg2();
				addContent("sourceAttribute", e3.getName());
			}
		}
		if (lk0 != null) {
			e2 = lk0.getArg2();
			List<Link> lks = g.containLinkList("hasAttribute", e2, null);
			if (lks != null) {
				for (Link lk : lks) {
					Entity e3 = lk.getArg2();
					if (e3.isKindOf("NumberValue")) {
						List<Link> lks1 = g.containLinkList("hasMember", e2, null);
						List<Entity> elist = new ArrayList<Entity>();
						if (lks1 != null) {
							for (Link lk1 : lks1) {
								Entity ee = lk1.getArg2();
								if (ee.getName().equals("DocLegalCounterclaim")) {
									addContent("with", ee.getName());
									foundCounterClaim = true;
								} else {
									elist.add(ee);
								}
							}
						} else {
							elist.add(e2);
						}
						if (elist.size() > 0) {
							subType += " to";
							for (Entity e : elist) {
								subType += " " + e.getName();
							}
						}
					} else {
						addContent("targetAttribute", e3.getName());
					}
				}
			}
		}
		if (e2 != null) {
			String claimName = e2.getName();
			if (claimName.equals("DocLegalComplaint") && !foundCounterClaim) {
				for (Phrase p : plist) {
					g = p.getGraph();
					List<Entity> le = g.findEntityByExactName("DocLegalCounterclaim");
					if (le != null && le.size() > 0) {
						addContent("with", le.get(0).getName());
					}
				}
			} else {
				List<Link> lks = g.containLinkList("hasAttribute", e2, null);
				if (lks != null) {
					for (Link ll : lks) {
						Entity e3 = ll.getArg2();
						if (!e3.isKindOf("NumberValue")) {
							addContent("targetAttribute", e3.getName());
						}
					}
				}
				lks = g.containLinkList("hasOwner", e2, null);
				if (lks != null) {
					for (Link ll : lks) {
						Entity e3 = ll.getArg2();
						List<Link> lkks = g.containLinkList("hasMember", e3, null);
						if (lkks != null) {
							for (Link lkk : lkks) {
								addContent("from", lkk.getArg2().getName());
							}
						} else {
							addContent("from", e3.getName());
						}
					}
				}
			}
		}
	}

	private void handleBrief(Analysis a) {
		// OpeningBrief, AnsweringBrief, ReplyBrief
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		// the following assignement is tentative. It really depends on the category of the Motion
		// if the Motion is evidentiary, for instance "Protective Order", then it is EVIDENTIARY.
		// this assignment just allows this program can be used to analyze isolated DE.
		// in actual applications, this should check back to the Motion to find out.
		eventCategory = DocketEntry.EVENT_DISPOSITIVE;
		type = DE_BRIEF;
		if (hd.isInstanceOf(onto.getEntity("DocLegalSurreplyBrief"))) {
			subType = DES_SURREPLY_BRIEF;
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalOpeningBrief"))) {
			subType = DES_OPENING_BRIEF;
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalReplyBrief"))) {
			subType = DES_REPLY_BRIEF;
		} else if (hd.isInstanceOf(onto.getEntity("DocLegalAnsweringBrief"))) {
			subType = DES_ANSWERING_BRIEF;
		}
		ERGraph g = ph.getGraph();
		List<Entity> le = g.getModifierList(hd);
		if (le != null) {
			for (Entity e : le) {
				addContent("Modifier", e.getName());
				if (e.isInstanceOf("ClaimConstruction") || e.isInstanceOf("Markman")) {
					eventCategory = DocketEntry.EVENT_EVIDENTIARY;
					litiPhase = DocketEntry.PHASE_CLAIMCONSTRUCTION;
				}
				if (e.isInstanceOf("StatusOpening")) {
					subType = DES_OPENING_BRIEF;
				}
			}
		}
		//		Link lk = g.containLink("hasMeasure", hd, null);
		//		if (lk != null) {
		//			Entity e2 = lk.getArg2();
		//			lk = g.containLink("hasAttribute", e2, "StatusRedacted");
		//			if (lk != null) {
		//				addContent("Attribute", "Redacted");
		//			}
		//		}
		findAllReferences(plist);
	}

	private void handleMinuteEntry(Analysis a) {
		// to do: link minute entry to previous mentions of meeting. Scheduling order for instance.
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_MINUTE_ENTRY;

		for (Phrase p : plist) {
			//			(728:ProcTrial) ugoerOfProc (729:ProcHoldMeeting) ==> topLink
			g = p.getGraph();
			Link lk = g.containLink("ugoerOfProc", null, "ProcHoldMeeting");
			if (lk == null) {//	(649:JuryTrial) define (651:ProcDeliberation)
				lk = g.containLink("define", "ProcTrial", "ProcDeliberation");
			}
			if (lk == null) {//	(649:JuryTrial) define (651:ProcDeliberation)
				lk = g.containLink("ugoerOfProc", null, "ProcComplete");
			}
			if (lk != null) {
				hd = lk.getArg1();
				if (hd.isKindOf("ProcTrial")) {
					subType = DES_TRIAL_HELD;
					eventCategory = DocketEntry.EVENT_DISPOSITIVE;
					litiPhase = DocketEntry.PHASE_TRIAL;
					String eventDate = this.eventDate.toString();
					Link ll = g.containLink("on", null, "DateValue");
					if (ll != null) {
						eventDate = ll.getArg2().getName();
					}
					addContent("On", eventDate);
					LitiEvent te = new LitiEvent(LitiEvent.LE_TRIAL, null, eventDate, serial, this.deIndex);
					casedata.addTrialEvent(te);
					if (hd.isInstanceOf("JuryTrial")) {
						addContent("Attribute", "Jury");
						te.setInfo(LitiEvent.SB_TRIAL_JURY);
					} else {
						processEntity(g, hd);
						for (String c : contents) {
							String[] split = c.split("::");
							if (split.length > 1) {
								String s = split[1].trim();
								if (s.equalsIgnoreCase("bench")) {
									te.setInfo(LitiEvent.SB_TRIAL_BENCH);
									break;
								} else if (s.equalsIgnoreCase("jury")) {
									te.setInfo(LitiEvent.SB_TRIAL_JURY);
									break;
								}
							}
						}
					}
				} else {
					List<Entity> le = g.getModifierList(hd);
					if (le != null) {
						for (Entity e : le) {
							if (e.isKindOf("Markman")) {
								subType = DES_MARKMAN_HELD;
								eventCategory = DocketEntry.EVENT_EVIDENTIARY;
								litiPhase = DocketEntry.PHASE_CLAIMCONSTRUCTION;
								//							(674:9/7/2011:INSTANCE:(340:DateValue))
								//							(671:Hearing) hasAttribute (670:Markman)
								//							(671:Hearing) on (674:9/7/2011)
								Link ll = g.containLink("on", null, "DateValue");
								if (ll != null) {
									String eventDate = ll.getArg2().getName();
									addContent("On", eventDate);
									LitiEvent me = new LitiEvent(LitiEvent.LE_MARKMAN, null, eventDate, serial, this.deIndex);
									casedata.addMarkman(me);
								} else {
									String eventDate = this.eventDate.toString();
									LitiEvent me = new LitiEvent(LitiEvent.LE_MARKMAN, null, eventDate, serial, this.deIndex);
									casedata.addMarkman(me);
								}
							}
						}
					}
				}
			}
			//			else {
			//				//	(649:JuryTrial) define (651:ProcDeliberation)
			//				lk = g.containLink("define", "ProcTrial", "ProcDeliberation");
			//				if (lk != null) {
			//					subType = DES_TRIAL_HELD;
			//					eventCategory = DocketEntry.EVENT_DISPOSITIVE;
			//					litiPhase = DocketEntry.PHASE_TRIAL;
			//					hd = lk.getArg1();
			//					if (hd.isInstanceOf("JuryTrial")) {
			//						addContent("Attribute", "Jury");
			//					} else {
			//						processEntity(g, hd);
			//					}
			//				}
			//			}
		}
		findAllReferences(plist);
	}

	private boolean checkAddElectronicNoticing(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		for (Phrase p : plist) {
			ERGraph g = p.getGraph();
			List<Entity> elec = g.findEntityByExactName("ProcElectronicNoticing");
			if (elec != null && elec.size() > 0) {
				return true;
			}
		}
		return false;
	}

	private void handleDocumentProper(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		// Extract head, usually in the first phrase:
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_DOCUMENT;
		//		(659:VersionRef) hasAttribute (658:StatusRedacted)
		//		(667:Document) hasMeasure (659:VersionRef) ==> topLink
		Link lk = g.containLink("hasMeasure", hd, "VersionRef");
		if (lk != null) {
			Entity e2 = lk.getArg2();
			Link lk1 = g.containLink("hasAttribute", e2, "StatusRedacted");
			if (lk1 != null) {
				subType = DES_REDACTED;
			}
		} else {
			List<Link> lks = g.containLinkList("hasAttribute", hd, null);
			if (lks != null) {
				for (Link ll : lks) {
					addContent("attribute", ll.getArg2().getName());
				}
			}
		}
		findAllReferences(plist);
	}

	private void handleDocument(Analysis a) {
		// STIPULATION and Order Regarding Amended Complaint
		List<Phrase> plist = a.getPhraseList();
		// Extract head, usually in the first phrase:
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		List<Link> lks = g.containLinkList("hasMember", hd, null);
		if (lks != null) {
			for (Link lk : lks) {
				Entity e1 = lk.getArg2();
				if (e1.isInstanceOf("Document")) {
					handleDocumentProper(a);
				} else if (e1.isInstanceOf("Thing")) {// ignore
					continue;
				} else {
					if (handleHeadEntity(e1, a)) {
						continue;
					} else {
						defaultHandler(e1, a);
					}
				}
			}
		} else {
			handleDocumentProper(a);
		}
	}

	private void defaultHandler(Entity hd, Analysis a) {
		if (checkAddElectronicNoticing(a)) {
			type = DE_MISC;
			subType = DES_ATTORNEY_ADDED_FOR_ELECTRONIC_NOTICING;
			return;
		}
		List<Phrase> plist = new ArrayList<Phrase>(a.getPhraseList());
		while (plist.size() > 1) {
			plist.remove(0);
			Analysis aa = new Analysis(plist);
			hd = plist.get(0).getHead();
			if (handleHeadEntity(hd, aa)) {
				return;
			}
		}
	}

	private void handleAttorney(Analysis a) {
		handleHuman(a);
	}

	private void handleDeclaration(Analysis a) {
		// DECLARATION re 258 MOTION for Leave
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		eventCategory = DocketEntry.EVENT_EVIDENTIARY;

		ERGraph g = ph.getGraph();
		type = DE_DECLARATION;
		subType = type;
		findAllReferences(plist);
		Link lk = g.containLink("hasMeasure", hd, null);
		if (lk != null) {
			Entity e2 = lk.getArg2();
			lk = g.containLink("hasAttribute", e2, "StatusRedacted");
			if (lk != null) {
				addContent("attribute", "Redacted");
			}
		}
	}

	private void handleJudgment(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		type = DE_JUDGMENT;
		subType = DES_JUDGMENT;

		eventCategory = DocketEntry.EVENT_DISPOSITIVE;
		//		litiPhase = DocketEntry.PHASE_POST_TRIAL;
		List<String> favor = new ArrayList<String>();
		for (Phrase p : plist) {
			ERGraph g = p.getGraph();
			Entity hd = g.getHead();
			if (hd.isKindOf("Judgment")) {
				List<Entity> attributes = g.getModifierList(hd);
				if (attributes != null) {
					for (Entity e : attributes) {
						subType += " " + e.getName();
					}
				}
				Link lk = g.containLink("inFavorOf", hd, null);
				if (lk != null) {
					Entity org = lk.getArg2();
					List<Link> lks = g.containLinkList("hasMember", org, null);
					if (lks != null) {
						for (Link lk1 : lks) {
							Entity e2 = lk1.getArg2();
							addContent("inFavorOf", e2.getName());
							favor.add(e2.getName());
						}
					} else {
						addContent("inFavorOf", org.getName());
						favor.add(org.getName());
					}
				}
			} else {
				Link lk = g.containLink("PRPRelation", "inFavorOf", null);
				if (lk != null) {
					Entity org = lk.getArg2();
					List<Link> lks = g.containLinkList("hasMember", org, null);
					if (lks != null) {
						for (Link lk1 : lks) {
							Entity e2 = lk1.getArg2();
							addContent("inFavorOf", e2.getName());
							favor.add(e2.getName());
						}
					} else {
						addContent("inFavorOf", org.getName());
						favor.add(org.getName());
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		if (favor.size() > 0) {
			sb.append("In favor of: ");
			for (String f : favor) {
				sb.append(f);
				sb.append(" ");
			}
		}
		LitiEvent le = new LitiEvent(LitiEvent.LE_JUDGMENT, sb.toString().trim(), eventDate.toString(), serial, deIndex);
		casedata.addJudgment(le);
	}

	private void handleLetter(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_LETTER;
		subType = DES_LETTER;
		fromEntities.clear();
		for (Phrase p : plist) {
			g = p.getGraph();
			Link lk = g.containLink("from", hd, null);
			Entity e2 = null;
			if (lk != null) {
				e2 = lk.getArg2();
				Link lk1 = null;
				// if more than one, any one is sufficient to determine party, they are from same party.
				// handle it later if necessary.
				if ((lk1 = g.containLink("hasMember", e2, null)) != null) {
					e2 = lk1.getArg2();
				}
			} else {
				lk = g.containLink("PRPRelation", "from", null);
				if (lk != null) {
					e2 = lk.getArg2();
				}
			}
			if (e2 != null) {
				this.fromEntities.add(e2);
				fromRole = findAgentRole(e2);
			}
			//			lk = g.containLink("re", hd, null);
			//			if (lk != null) {
			//				String[] split = p.text.split("(?i:\\b(re|regarding)\\b)");
			//				if (split.length > 1) {
			//					for (int i = 1; i < split.length; i++) {
			//						addContent("regarding", split[i].replaceFirst("(?i:\\b(re|regarding)\\b)", ""));
			//					}
			//				}
			//			} else {
			//				lk = g.containLink("PRPRelation", "re", null);
			//				if (lk != null) {
			//					addContent("regarding", p.text.replaceAll("(?i:\\b(re|regarding)\\b)", ""));
			//				}
			//			}

		}
		List<LexToken> sent = ph.getSentence();
		LexToken lasttok = sent.get(sent.size() - 1);
		int pos = lasttok.getEnd();
		String[] split = this.processedText.substring(0, pos).split("(?i:\\b(re|regarding)\\b)");
		if (split.length > 1) {
			for (int i = 1; i < split.length; i++) {
				addContent("regarding", split[i].replaceFirst("(?i:\\b(re|regarding)\\b)", ""));
			}
		}

		// may also be PLAINTIF, definitely not COURT (default value)
		if (fromRole == null || fromRole.equals(ROLE_COURT)) {
			fromRole = DocketEntry.ROLE_PLAINTIFF;
		}
		findAllReferences(plist);
	}

	/**
	 * Remarks are really court orders. Most are scheduling orders. I'll get back to this issue when I
	 * take on scheduling orders seriously.
	 * 
	 * @param a
	 */
	private void handleRemark(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_REMARK;
		subType = type;
		fromRole = ROLE_COURT;
		eventCategory = DocketEntry.EVENT_PROCEDURAL;
	}

	private void handleMeeting(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_MEETING;
		subType = type;
		fromRole = ROLE_COURT;
		Link lk = g.containLink("ugoerOfProc", hd, "ProcSetOrChangeSchedule");
		if (lk != null) {
			subType = DE_SET_OR_CHANGE_SCHEDULE;
		}
	}

	private void handleChangeSchedule(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_SET_OR_CHANGE_SCHEDULE;
		subType = type;
		fromRole = ROLE_COURT;
	}

	private void handleMemorandumOpinion(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		eventCategory = DocketEntry.EVENT_DISPOSITIVE;

		ERGraph g = ph.getGraph();
		type = DE_MEMORANDUM_OPINION;
		subType = type;
		fromRole = ROLE_COURT;
	}

	private void handleDocEntry(Analysis a) {
		// CORRECTING ENTRY
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		ERGraph gr = new ERGraph(hd);
		eventCategory = DocketEntry.EVENT_MISCELLANEURS;

		ERGraph g = ph.getGraph();
		type = "CORRECTING ENTRY";
		subType = type;
	}

	private void handleOpinion(Analysis a) {
		eventCategory = DocketEntry.EVENT_DISPOSITIVE;

		type = DE_OPINION;
		subType = DES_OPINION;
	}

	private void handleExhibit(Analysis a) {
		List<Phrase> plist = a.getPhraseList();

		type = DE_EXHIBIT;
		subType = DES_EXHIBIT;
		findAllReferences(plist);
	}

	private void handleFindingsOfFact(Analysis a) {
		List<Phrase> plist = a.getPhraseList();

		type = DE_FINDINGS_OF_FACT;
		subType = DES_FINDINGS_OF_FACT;
		findAllReferences(plist);
	}

	private void handleRequest(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		eventCategory = DocketEntry.EVENT_MISCELLANEURS;

		ERGraph g = ph.getGraph();
		type = DE_REQUEST;
		subType = DES_REQUEST;
		Link lk = g.containLink("for", hd, null);
		if (lk != null) {
			addContent("for", lk.getArg2().getName());
		}
	}

	private void handleVerdict(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();
		ERGraph g = ph.getGraph();

		eventCategory = DocketEntry.EVENT_DISPOSITIVE;
		litiPhase = DocketEntry.PHASE_TRIAL;

		type = DE_VERDICT;
		subType = DES_VERDICT;
		processEntity(g, hd);
	}

	private void handleChart(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_CHART;
		subType = DES_CHART;
		litiPhase = DocketEntry.PHASE_CLAIMCONSTRUCTION;
		eventCategory = DocketEntry.EVENT_EVIDENTIARY;
		Link lk = g.containLink("define", null, hd);
		if (lk != null) {
			addContent("type", lk.getArg1().getName());
		}
	}

	private void handleBillofCosts(Analysis a) {
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_BILL_OF_COSTS;
		subType = DES_BILL_OF_COSTS;
		eventCategory = DocketEntry.EVENT_MISCELLANEURS;
	}

	private void handleTranscript(Analysis a) {
		// to do: link transcript to previous mentions of meeting, minute entry and scheduling order for instance.
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_TRANSCRIPT;
		subType = DES_TRANSCRIPT;
		eventCategory = DocketEntry.EVENT_MISCELLANEURS;
		Link lk = g.containLink("of", hd, null);
		if (lk != null) {
			Entity e2 = lk.getArg2();
			addContent("of", e2.getName());
			if (e2.isKindOf("ProcTrial") || e2.isKindOf("TrialDay")) {
				litiPhase = DocketEntry.PHASE_POST_TRIAL;
				subType = DES_TRIAL_HELD;
				String eventDate = getFirstOnDate(plist);
				if (eventDate != null) {
					addContent("On", eventDate);
					LitiEvent te = new LitiEvent(LitiEvent.LE_TRIAL, null, eventDate, serial, this.deIndex);
					casedata.addTrialEvent(te);
					if (e2.isInstanceOf("JuryTrial")) {
						addContent("Attribute", "Jury");
						te.setInfo(LitiEvent.SB_TRIAL_JURY);
					} else {
						processEntity(g, e2);
						for (String c : contents) {
							String[] split = c.split("::");
							if (split.length > 1) {
								String s = split[1].trim();
								if (s.equalsIgnoreCase("bench")) {
									te.setInfo(LitiEvent.SB_TRIAL_BENCH);
									break;
								} else if (s.equalsIgnoreCase("jury")) {
									te.setInfo(LitiEvent.SB_TRIAL_JURY);
									break;
								}
							}
						}
					}
				}
			} else {
				List<Entity> le = g.getModifierList(e2);
				if (le != null) {
					for (Entity e : le) {
						if (e.isKindOf("Markman") || e.isKindOf("ClaimConstruction")) {
							subType = DES_MARKMAN_HELD;
							eventCategory = DocketEntry.EVENT_EVIDENTIARY;
							//							litiPhase = DocketEntry.PHASE_CLAIMCONSTRUCTION;
							//						Link ll = g.containLink("on", null, "DateValue");
							//						if (ll != null) {
							//							Entity ee = ll.getArg2();
							//							String eventDate = getEntityTime(g, ee);
							//							if (eventDate != null) {
							//								addContent("On", eventDate);
							//								MarkmanEvent me = new MarkmanEvent(eventDate, serial, this.deIndex);
							//								casedata.addMarkman(me);
							//							}
							//						}
							String eventDate = getFirstOnDate(plist);
							if (eventDate != null) {
								addContent("On", eventDate);
								LitiEvent me = new LitiEvent(LitiEvent.LE_MARKMAN, null, eventDate, serial, this.deIndex);
								casedata.addMarkman(me);
							}
						}
					}
				}
			}
		}
	}

	private String getEntityTime(ERGraph g, Entity ee) {
		if (ee.isInstanceOf("MonthDateValue")) {
			Link kkYear = g.containLink("YearValueOf", null, ee);
			Link kkMonth = g.containLink("MonthValueOf", null, ee);
			Link kkDate = g.containLink("DateValueOf", null, ee);
			StringBuilder sb = new StringBuilder();
			if (kkMonth != null) {
				sb.append(kkMonth.getArg1().getName());
			} else {
				sb.append("00");
			}
			sb.append("/");
			if (kkDate != null) {
				sb.append(kkDate.getArg1().getName());
			} else {
				sb.append("00");
			}
			sb.append("/");
			if (kkYear != null) {
				sb.append(kkYear.getArg1().getName());
			} else {
				sb.append("0000");
			}
			return sb.toString();
		} else {
			List<Link> lks = g.containLinkList("hasMember", ee, null);
			if (lks != null) {
				for (Link lk : lks) {
					Entity e = lk.getArg2();
					String etime = getEntityTime(g, e);
					if (etime != null) {
						return etime;
					}
				}
			}
			return ee.getName();
		}
	}

	private String getFirstOnDate(ERGraph g) {
		Link ll = g.containLink("on", null, "DateValue");
		if (ll != null) {
			Entity ee = ll.getArg2();
			return getEntityTime(g, ee);
		}
		return null;
	}

	private String getFirstOnDate(List<Phrase> plist) {
		for (Phrase p : plist) {
			ERGraph g = p.getGraph();
			String stime = getFirstOnDate(g);
			if (stime != null) {
				return stime;
			}
		}
		return null;
	}

	private void handleAcknowledgement(Analysis a) {
		// to do: link transcript to previous mentions of meeting, minute entry and scheduling order for instance.
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_ACKNOWLEDGEMENT;
		subType = null;
		Link lk = g.containLink("of", hd, null);
		if (lk != null) {
			Entity e = lk.getArg2();
			if (e.getName().equals("ProcReceipt")) {
				setSubType(DES_ACKNOWLEDGEMENT_OF_RECEIPT);
			} else {
				setSubType(DE_ACKNOWLEDGEMENT + " of " + e.getName());
			}
		}
	}

	private void handleAppeal(Analysis a) {
		// to do: link transcript to previous mentions of meeting, minute entry and scheduling order for instance.
		List<Phrase> plist = a.getPhraseList();
		Phrase ph = plist.get(0);
		Entity hd = ph.getHead();

		ERGraph g = ph.getGraph();
		type = DE_MISC;
		litiPhase = DocketEntry.PHASE_APPEAL;
		for (Phrase p : plist) {
			g = p.getGraph();
			List<Entity> le = g.findEntityByClass("Payment");
			if (le != null) {
				type = DE_MISC;
				subType = DES_PAYMENT;
				break;
			}
		}
	}

	static class DockRef {
		// reference to a docket entry
		String type; // "OrderMotion", "PartyOutcome",
		String subType; // "Grant", "Deny"
		String sourceEntity;
		String targetEntity;
		int sourceIndex = 0; // DE index;
		int sourceSerial = -1; // DE serial;
		int targetIndex = 0;
		int targetSerial = -1;

		@SuppressWarnings("unchecked")
		public JSONObject toJSONObject() {
			JSONObject jo = new JSONObject();
			jo.put("type", type);
			jo.put("subType", subType);
			jo.put("sourceEntity", sourceEntity);
			jo.put("targetEntity", targetEntity);
			jo.put("sourceIndex", sourceIndex);
			jo.put("sourceSerial", sourceSerial);
			jo.put("targetIndex", targetIndex);
			jo.put("targetSerial", targetSerial);
			return jo;
		}

		public static DockRef buildFromJSON(JSONObject jo) {
			String tp = (String) jo.get("type");
			String stp = (String) jo.get("subType");
			String se = (String) jo.get("sourceEntity");
			String te = (String) jo.get("targetEntity");
			Long ss = (Long) jo.get("sourceSerial");
			Long si = (Long) jo.get("sourceIndex");
			Long ts = (Long) jo.get("targetSerial");
			Long ti = (Long) jo.get("targetIndex");

			DockRef dr = new DockRef(tp, stp, se, te, Integer.valueOf(si.toString()), Integer.valueOf(ss.toString()), Integer.valueOf(ti.toString()),
					Integer.valueOf(ts.toString()));
			return dr;
		}

		public DockRef(String tp, String stp, String se, String te, int srcID, int srcSer, int tgID, int tgSer) {
			type = tp;
			subType = stp;
			sourceEntity = se;
			targetEntity = te;
			sourceIndex = srcID;
			sourceSerial = srcSer;
			targetIndex = tgID;
			targetSerial = tgSer;
		}

		public DockRef() {

		}

		public void setType(String tp) {
			type = tp;
		}

		public void setSubType(String stp) {
			subType = stp;
		}

		public void setSourceEntity(String en) {
			sourceEntity = en;
		}

		public void setTargetEntity(String en) {
			targetEntity = en;
		}

		public void setSourceNumbers(int ser, int id) {
			sourceSerial = ser;
			sourceIndex = id;
		}

		public void setTargetNumbers(int ser, int id) {
			targetSerial = ser;
			targetIndex = id;
		}

		public void setTargetIndex(int id) {
			targetIndex = id;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			return sb.toString();
		}

		public String sourcePrint() {
			// information about source:
			StringBuilder sb = new StringBuilder();
			// if (subType != null) {
			// sb.append(subType);
			// }
			if (sourceEntity != null) {
				sb.append(sourceEntity);
			}
			sb.append("(");
			if (sourceSerial >= 0) {
				sb.append(sourceSerial);
			}
			sb.append(",");
			if (sourceIndex > 0) {
				sb.append(sourceIndex);
			}
			sb.append(")");
			return sb.toString();
		}

		public String targetPrint() {
			// information about target:
			StringBuilder sb = new StringBuilder();
			// if (subType != null) {
			// sb.append(subType);
			// }
			if (targetEntity != null) {
				sb.append(targetEntity);
			}
			sb.append("(");
			if (targetSerial >= 0) {
				sb.append(targetSerial);
			}
			sb.append(",");
			if (targetIndex > 0) {
				sb.append(targetIndex);
			}
			sb.append(")");
			return sb.toString();
		}
	}

	static class PartyOutcome {
		String type; // "dismiss"
		String subType; // "voluntary", "terminate"
		int serial;
		int index;
		String doc;
		String partyEntity; // Ontological Name
		String partyName;

		public JSONObject toJSONObject() {
			JSONObject jo = new JSONObject();
			jo.put("type", type);
			jo.put("subType", subType);
			jo.put("serial", serial);
			jo.put("index", index);
			jo.put("Entity", partyEntity);
			jo.put("name", partyName);
			return jo;
		}

		public static PartyOutcome buildFromJSON(JSONObject jo) {
			String type_ = (String) jo.get("type");
			String subtype_ = (String) jo.get("subType");
			Long serial_ = (Long) jo.get("serial");
			Long index_ = (Long) jo.get("index");
			String entity_ = (String) jo.get("Entity");
			String name_ = (String) jo.get("name");
			PartyOutcome po = new PartyOutcome(type_);
			po.setSubType(subtype_);
			po.setSerial(Integer.valueOf(serial_.toString()));
			po.setIndex(Integer.valueOf(index_.toString()));
			po.setPartyEntity(entity_);
			po.setPartyName(name_);
			return po;
		}

		public PartyOutcome(String tp) {
			type = tp;
		}

		public void setSubType(String stp) {
			subType = stp;
		}

		public void setSerial(int ser) {
			serial = ser;
		}

		public void setIndex(int id) {
			index = id;
		}

		public void setDoc(String dc) {
			doc = dc;
		}

		public void setPartyEntity(String e) {
			partyEntity = e;
		}

		public void setPartyName(String ne) {
			partyName = ne;
		}

		public String sourcePrint() {
			StringBuilder sb = new StringBuilder();
			sb.append(type);
			if (subType != null) {
				sb.append(" " + subType);
			}
			if (partyName != null) {
				sb.append(" " + partyName);
			}
			return sb.toString();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("(" + serial + ", " + index + ")");
			sb.append(type);
			if (subType != null) {
				sb.append(" " + subType);
			}
			if (partyName != null) {
				sb.append(" " + partyName);
			}
			return sb.toString();
		}
	}
}
