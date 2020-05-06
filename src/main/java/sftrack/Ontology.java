package sftrack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.drools.core.WorkingMemory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;

import sftrack.CaseData.LitiEntity;
import sftrack.CaseData.LitiParty;

/**
 * This is a helper class.
 * 
 * @author yanyongxin
 * 
 */
public class Ontology {
	//	private static final Logger log = LoggerFactory.getLogger(Ontology.class);
	// static Ontology worldOnto;

	protected final Map<String, Entity> namedEntities = new HashMap<String, Entity>();
	public final List<Link> relations = Collections.synchronizedList(new ArrayList<Link>());
	public final Map<String, List<Link>> linkmap = new HashMap<String, List<Link>>();
	public final Map<String, List<String>> parentMap = new HashMap<String, List<String>>();
	List<DictPhrase> dictPhrases = new ArrayList<DictPhrase>();
	List<DictPhrase> caseDictPhrases = new ArrayList<DictPhrase>();
	Map<String, String> transmap = new HashMap<String, String>();
	HashSet<String> notNameSet = new HashSet<String>();
	KieBase kbase;
	CaseData caseData = null;
	static String ontofile;
	static String dictfile;

	static Phrase EmptyPhrase;

	private void initNotNameSet() {
		notNameSet.add("motion");
		notNameSet.add("for");
	}

	static {
		Entity e = new Entity("EmptyClass", null, Entity.TYPE_CLASS, null, -1);
		ERGraph g = new ERGraph(e);
		LexToken tk = new LexToken(" EMPTY ", "EMPTY", 1, 5, LexToken.LEX_EMPTY);
		List<LexToken> tks = new ArrayList<LexToken>();
		tks.add(tk);
		EmptyPhrase = new Phrase("EMPTY", "EMPTY", g, 0, 1, tks);
	}

	private void ReInitTransmap() {
		transmap.put("ProcFileDoc", "File Document");
		transmap.put("ProcDismiss", "Dismissal");
		transmap.put("DocLegalAnswer", "Answer");
		transmap.put("DocLegalResponse", "Response");
		transmap.put("DocLegalMotion", "Motion");
		transmap.put("DocLegalNotice", "Notice");
		transmap.put("DocLegalOrder", "Order");
		transmap.put("DocLegalStipulation", "Stipulation");
		transmap.put("StatusVoluntary", "Voluntary");
	}

	/**
	 * Translate from ontological concept name back to plain term.
	 * 
	 * @param entityName
	 * @return plain term
	 */
	public String translate(String entityName) {
		return transmap.get(entityName);
	}

	public void loadGlobalDictionary(String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		JSONParser parser = new JSONParser();

		while ((line = br.readLine()) != null) {
			try {
				line = line.trim();
				if (line.length() == 0) {
					continue;
				}
				if (line.charAt(0) != '{') {
					continue;
				}
				JSONObject jo = (JSONObject) parser.parse(line);
				if (jo == null) {
					continue;
				}
				String text = (String) jo.get("text");
				//				if (text.equals("by")) {
				//					System.out.println();
				//				}
				String type = (String) jo.get("type");
				String entity = (String) jo.get("entity");
				String tense = null;
				if (type.equals("VP")) {
					tense = (String) jo.get("tense");
				}
				String listUnder = (String) jo.get("list");
				DictPhrase dp = null;
				if (listUnder == null) {
					dp = new DictPhrase(text, type, tense, entity, this);
					transmap.put(entity, text);
				} else {
					dp = new DictPhrase(text, type, tense, entity, listUnder, this);
				}
				dictPhrases.add(dp);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		br.close();
	}

	/**
	 * 1. an attorney name string s; 2. new Litigant(s, "Attorney"); 3. a Lk =
	 * new Link(), add to relations; 4. add to linkmap; 5. create a new
	 * DictPhrase(), add to caseDictPhrases 6. assert in ksession, Lk, 7. assert
	 * in ksession, DictPhrase
	 * 
	 * @param attname
	 * @throws Exception
	 */
	public void createNewPerson(String person_name, String type, Ontology onto, WorkingMemory session) throws Exception {
		// new Litigant(s, "Attorney"):
		String[] segments = person_name.toLowerCase().split("\\s+");
		for (String s : segments) {
			if (notNameSet.contains(s)) {
				return;
			}
		}
		try {
			Litigant lt = new Litigant(person_name, type);
			// new relation, add to linkmap;
			List<Litigant> alist = new ArrayList<Litigant>();
			alist.add(lt);
			List<Link> links = onto.loadCaseOntologyOld(alist);
			for (Link lk : links) {
				session.insert(lk);
			}
			// create a new DictPhrase(), add to caseDictPhrases
			DictPhrase dp = new DictPhrase(lt.regex, "NP", null, lt.entityName, lt.listUnder, this);
			if (!caseDictPhrases.contains(dp)) {
				caseDictPhrases.add(dp);
				session.insert(dp);
			} else {
				//				log.info("Already in dictionary: " + dp);
			}
		} catch (Exception ex) {
			;
		}
	}

	/**
	 * 1. an attorney name string s; 2. new Litigant(s, "Attorney"); 3. a Lk =
	 * new Link(), add to relations; 4. add to linkmap; 5. create a new
	 * DictPhrase(), add to caseDictPhrases 6. assert in ksession, Lk, 7. assert
	 * in ksession, DictPhrase
	 * 
	 * @param attname
	 * @throws Exception
	 */
	public void createNewParty(String partyName, Ontology onto, WorkingMemory session, Entity oldParty) throws Exception {
		try {
			LitiParty party = new LitiParty(partyName);
			List<LitiParty> partyList = caseData.parties;
			List<String> partyNames = CaseData.getPartyNames(partyList);
			party.createRegex(partyNames);
			if (oldParty != null) {
				String role = caseData.getPartyRole(oldParty.getName());
				party.setLitiRole(role);
			}
			caseData.parties.add(party);
			List<LitiParty> pts = new ArrayList<LitiParty>();
			pts.add(party);
			List<Link> links = onto.loadCaseOntology(pts);
			for (Link lk : links) {
				session.insert(lk);
			}
			try {
				DictPhrase dp = new DictPhrase(party.getRegex(), "NP", null, party.getEntityName(), party.getListUnder(), this);
				caseDictPhrases.add(dp);
				transmap.put(party.getEntityName(), party.getNameText());
				session.insert(dp);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			// new relation, add to linkmap;
			// create a new DictPhrase(), add to caseDictPhrases
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void loadCaseDictionaryOld(List<Litigant> dlist) throws Exception {
		for (Litigant lt : dlist) {
			try {
				DictPhrase dp = new DictPhrase(lt.regex, "NP", null, lt.entityName, lt.listUnder, this);
				caseDictPhrases.add(dp);
				transmap.put(lt.entityName, lt.nameText);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void loadCaseDictionary(List<? extends LitiEntity> dlist) throws Exception {
		for (LitiEntity lt : dlist) {
			try {
				DictPhrase dp = new DictPhrase(lt.getRegex(), "NP", null, lt.getEntityName(), lt.getListUnder(), this);
				caseDictPhrases.add(dp);
				transmap.put(lt.getEntityName(), lt.getNameText());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void insertSession(Srunner srun) {
		srun.insertList(dictPhrases);
		srun.insertList(caseDictPhrases);
		srun.insertList(relations);
	}

	protected Ontology() {

	}

	public static void LoadWorld(String otfile, String dcfile) {
		ontofile = otfile;
		dictfile = dcfile;
		// worldOnto = new Ontology(ontofile, dictfile);
	}

	public static Ontology createOntology(KieBase kbs) throws IOException {
		// Ontology ot = worldOnto.clone();
		Ontology ot = new Ontology(ontofile, dictfile);
		ot.setKnowledgeBase(kbs);
		return ot;
	}

	public void setKnowledgeBase(KieBase kbs) {
		kbase = kbs;
	}

	public Ontology(String ontofile, String dictfile) throws IOException {
		// List<Link> links = new ArrayList<Link>();
		BufferedReader br = new BufferedReader(new FileReader(ontofile));

		String line = "";
		try {
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0)
					continue;
				if (line.charAt(0) == ';') { // skip
					continue;
				}
				String[] split = line.split("\\s+");
				if (split.length == 3) {
					Entity e1 = getEntity(split[0]);
					if (e1 == null) {
						e1 = new Entity(split[0], null, Entity.TYPE_CLASS, this, -1);
						addEntity(split[0], e1);
					}
					Entity e2 = getEntity(split[2]);
					if (e2 == null) {
						e2 = new Entity(split[2], null, Entity.TYPE_CLASS, this, -1);
						addEntity(split[2], e2);
					}

					Link lk = new Link(split[1], e1, e2);
					relations.add(lk);
					addToRelationMap(lk);
				}
			}
		} catch (Exception ex) {
			System.out.println(line);
		}
		br.close();
		createParentMap(relations, parentMap);
		ReInitTransmap();
		initNotNameSet();
		loadGlobalDictionary(dictfile);
	}

	@Override
	public Ontology clone() {
		Ontology onto = new Ontology();
		onto.namedEntities.putAll(namedEntities);
		onto.relations.addAll(relations);
		onto.linkmap.putAll(linkmap);
		onto.parentMap.putAll(parentMap);
		onto.caseDictPhrases.addAll(caseDictPhrases);
		onto.dictPhrases.addAll(dictPhrases);
		onto.transmap.putAll(transmap);
		return onto;
	}

	public void setCaseData(CaseData cd) {
		caseData = cd;
	}

	/**
	 * Adding child/parent relations into a child/parent map
	 * 
	 * @param re
	 *            new repository of relations, may contain child/parent
	 *            relations.
	 * @param pmap
	 *            child/parent map to add to
	 */
	public static void createParentMap(List<Link> re, Map<String, List<String>> pmap) {
		for (Link lk : re) {
			String lkname = lk.getType();
			if (lkname.equals("instanceOf") || lkname.equals("subclassOf")) {
				Entity e1 = lk.getArg1();
				Entity e2 = lk.getArg2();
				String s1 = e1.getName();
				String s2 = e2.getName();
				List<String> parents = pmap.get(s1);
				if (parents == null) {
					parents = new ArrayList<String>();
					pmap.put(s1, parents);
				}
				parents.add(s2);
			}
		}
	}

	public boolean isKindOf(String s1, String s2) {
		if (s1 == null || s2 == null) {
			return false;
		}
		if (s1.equals("EmptyClass")) {
			return false;
		}
		if (s1.equals(s2)) {
			return true;
		}
		List<String> parents = parentMap.get(s1);
		if (parents != null) {
			for (String p : parents) {
				if (isKindOf(p, s2)) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Link> loadCaseOntology(List<? extends LitiEntity> olist) {
		List<Link> links = new ArrayList<Link>();
		if (olist != null) {
			for (LitiEntity lt : olist) {
				String[] lines = lt.getOntoText().split("\\n");
				for (String line : lines) {
					String[] split = line.split("\\s+");
					if (split.length == 3) {
						Entity e1 = getEntity(split[0]);
						if (e1 == null) {
							e1 = new Entity(split[0], null, Entity.TYPE_CLASS, this, -1);
							addEntity(split[0], e1);
						}
						Entity e2 = getEntity(split[2]);
						if (e2 == null) {
							e2 = new Entity(split[2], null, Entity.TYPE_CLASS, this, -1);
							addEntity(split[2], e2);
						}
						Link lk = new Link(split[1], e1, e2);
						links.add(lk);
						addToRelationMap(lk);
					}
				}
			}
		}
		relations.addAll(links);
		createParentMap(links, parentMap);
		return links;
	}

	public List<Link> loadCaseOntologyOld(List<Litigant> olist) {
		List<Link> links = new ArrayList<Link>();
		if (olist != null) {
			for (Litigant lt : olist) {
				String[] lines = lt.ontoText.split("\\n");
				for (String line : lines) {
					String[] split = line.split("\\s+");
					if (split.length == 3) {
						Entity e1 = getEntity(split[0]);
						if (e1 == null) {
							e1 = new Entity(split[0], null, Entity.TYPE_CLASS, this, -1);
							addEntity(split[0], e1);
						}
						Entity e2 = getEntity(split[2]);
						if (e2 == null) {
							e2 = new Entity(split[2], null, Entity.TYPE_CLASS, this, -1);
							addEntity(split[2], e2);
						}
						Link lk = new Link(split[1], e1, e2);
						links.add(lk);
						addToRelationMap(lk);
					}
				}
			}
		}
		relations.addAll(links);
		createParentMap(links, parentMap);
		return links;
	}

	// public static List<Link> loadOntology(List<String> olist) {
	// if (olist != null) {
	// for (String line : olist) {
	// String[] split = line.split("\\s+");
	// if (split.length == 3) {
	// Entity e1 = Ontology.getEntity(split[0]);
	// if (e1 == null) {
	// e1 = new Entity(split[0], null, Entity.TYPE_CLASS);
	// Ontology.addEntity(split[0], e1);
	// }
	// Entity e2 = Ontology.getEntity(split[2]);
	// if (e2 == null) {
	// e2 = new Entity(split[2], null, Entity.TYPE_CLASS);
	// Ontology.addEntity(split[2], e2);
	// }
	//
	// Link lk = new Link(split[1], e1, e2);
	// relations.add(lk);
	// addToRelationMap(lk);
	// }
	// }
	// }
	//
	// return relations;
	// }

	public Entity getCommonAncestor(Entity e1, Entity e2) {
		for (Entity e : namedEntities.values()) {
			if (e.isKindOf("AttributeClass")) {
				continue;
			}
			if (e1.isKindOf(e)) {
				if (e2.isKindOf(e)) {
					return e;
				}
			}
		}
		return null;
	}

	public void buildRelationMap() {
		for (Link lk : relations) {
			addToRelationMap(lk);
		}
	}

	public void addToRelationMap(Link lk) {
		Entity e1 = lk.getArg1();
		List<Link> li = linkmap.get(e1.getName());
		if (li == null) {
			li = new ArrayList<Link>();
			linkmap.put(e1.getName(), li);
		}
		li.add(lk);
	}

	public boolean linkExists(String lkname, Entity e1, Entity e2) {
		for (Link lk : relations) {
			if (lk.type.equals(lkname)) {
				if (e1.isKindOf(lk.getArg1()) && e2.isKindOf(lk.getArg2())) {
					return true;
				}
			}
		}
		return false;
	}

	public List<Link> findLinks(String lkname, Entity e1, Entity e2) {
		List<Link> list = new ArrayList<>();
		if (e1 != null && e2 != null) {
			for (Link lk : relations) {
				if (lk.type.equals(lkname)) {
					if (e1.equals(lk.getArg1()) && e2.equals(lk.getArg2())) {
						list.add(lk);
					}
				}
			}
		} else if (e1 != null && e2 == null) {
			for (Link lk : relations) {
				if (lk.type.equals(lkname)) {
					if (e1.equals(lk.getArg1())) {
						list.add(lk);
					}
				}
			}
		} else if (e1 == null && e2 != null) {
			for (Link lk : relations) {
				if (lk.type.equals(lkname)) {
					if (e2.equals(lk.getArg2())) {
						list.add(lk);
					}
				}
			}
		} else {
			for (Link lk : relations) {
				if (lk.type.equals(lkname)) {
					list.add(lk);
				}
			}
		}
		return list;
	}

	/**
	 * The entities in Ontology knowledge base are: (1) classes, they are either
	 * a top-level or subclass (2) attributes
	 * 
	 * @param e
	 * @return
	 */
	public boolean isClass(Entity e) {
		for (Link lk : relations) {
			if (e.getName().equals(lk.getArg1().getName())) {
				String lkname = lk.getType();
				if (lkname.equals("subclassOf")) {
					return true;
				}
			} else if (e.getName().equals(lk.getArg2().getName())) {
				return true;
			}
		}
		return false;
	}

	public boolean isKindOf_Old(String clsname1, String clsname2) {
		if (clsname1.equals(clsname2)) {
			return true;
		}
		Entity e1 = getEntity(clsname1);
		Entity e2 = getEntity(clsname2);
		if (e1 == null || e2 == null) {
			return false;
		}
		return e1.isKindOf_Old(e2);
	}

	//	public synchronized Entity getEntity(String name) {
	public Entity getEntity(String name) {
		return namedEntities.get(name);
	}

	public void addEntity(String key, Entity e) {
		namedEntities.put(key, e);
	}

	public static KieBuilder loadRules(String rulesfile) throws Exception {
		KieServices ks = KieServices.Factory.get();
		KieBuilder kbuilder = ks.newKieBuilder(new File("/Users/yanyongxin/thousand/src/main/rules"));
		kbuilder.buildAll();
		return kbuilder;
	}

	public Srunner createSrunner(boolean b) {
		LtUtil lu = new LtUtil();
		lu.setPrint(b);
		Srunner srun = new Srunner(kbase, lu, this);
		insertSession(srun);
		srun.initGlobal();
		return srun;
	}

	public static class Srunner {
		KieSession ksession = null;
		LtUtil ut;
		LexFlag lexflag = new LexFlag(true);

		// KnowledgeRuntimeLogger logger;

		public Srunner(KieBase kbase, LtUtil lu, Ontology ot) {
			ksession = kbase.newKieSession();
			ut = lu;
			ksession.setGlobal("ut", ut);
			ksession.setGlobal("onto", ot);
		}

		public void insertList(List<? extends Object> olist) {
			// insert objects in reverse order
			int osize = olist.size() - 1;
			for (int i = osize; i >= 0; i--) {
				Object o = olist.get(i);
				try {
					ksession.insert(o);
					// ut.print(o.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public void execute() {
			PhraseCounter pc = new PhraseCounter();
			ksession.insert(pc);
			ksession.fireAllRules();
		}

		public void dispose() {
			// logger.close();
			ksession.dispose();
			ksession = null;
		}

		public void reset() {
			lexflag.setFlag(true);
			ksession.insert(lexflag);
			ksession.fireAllRules();
			lexflag.setFlag(false);
			ksession.insert(EmptyPhrase);
		}

		public Map<Integer, List<Phrase>> findAllPhrases() {
			Map<Integer, List<Phrase>> rpmap = new TreeMap<Integer, List<Phrase>>();
			QueryResults results = ksession.getQueryResults("all phrases");
			for (QueryResultsRow row : results) {
				Phrase ph = (Phrase) row.get("ph");
				List<Phrase> pl = rpmap.get(ph.getBegToken());
				if (pl == null) {
					pl = new ArrayList<Phrase>();
					rpmap.put(ph.getBegToken(), pl);
				}
				pl.add(ph);
			}
			return rpmap;
		}

		public List<Phrase> oneBestPhrase(int end) {
			List<Phrase> phlist = new ArrayList<Phrase>();
			QueryResults results = ksession.getQueryResults("One Phrase longest sentence");
			// log.info("we have " + results.size() +
			// " One Phrase longest sentence");
			if (results.size() > 0) {
				for (QueryResultsRow row : results) {
					Phrase ph = (Phrase) row.get("ph");
					ph.endToken = end;
					phlist.add(ph);
				}
			}
			return phlist;
		}

		// It's handy in rules engine to have an empty phrase:
		public void initGlobal() {
			ksession.insert(EmptyPhrase);

		}
	}
}
