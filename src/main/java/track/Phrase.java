package track;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import core.LegaLanguage.Srunner;

public class Phrase implements Cloneable {
	private static final Logger log = LoggerFactory.getLogger(Phrase.class);
	// static HashMap<Integer, Phrase> phraseMap = new HashMap<Integer,
	// Phrase>();
	// static HashMap<Integer, Phrase> mainPhraseMap = new HashMap<Integer,
	// Phrase>();
	// static HashMap<Integer, Phrase> sidePhraseMap = new HashMap<Integer,
	// Phrase>();
	static final int TOP_N = 3;

	String text;
	ERGraph graph;
	String synType = null;
	String tense = null; // only when synType = "VP"
	int begToken = 0;
	int endToken = 1;
	List<Phrase> subphrases;
	List<LexToken> sentence;
	boolean composite = false;
	Phrase subject = null;

	public Phrase getLastPhrase() {
		if (subphrases == null || subphrases.size() == 0) {
			return this;
		}
		// if (graph.members != null && graph.members.size() > 1) {
		// return this;
		// }
		if (subphrases.size() == 1)
			return this;
		return subphrases.get(subphrases.size() - 1).getLastPhrase();
	}

	public Entity getLastHead() {
		return getLastPhrase().getHead();
	}

	// public Phrase defineSet(Phrase ph) {
	// List<Phrase> subs = ph.getSubphrases();
	// Phrase ph0 = subs.get(0);
	// Phrase ph1 = subs.get(1);
	// ERGraph g0 = ph0.getGraph();
	// ERGraph g1 = ph1.getGraph();
	// if (g1.containLinkKind("define", e1, e2))
	// ;
	// }

	public boolean lastPhraseIs(String s) {
		Phrase ph = getLastPhrase();
		boolean b = ph.getHead().isKindOf(s);
		return b;
	}

	public boolean headLast() {
		if (graph.isSet()) {
			Phrase last = subphrases.get(subphrases.size() - 1);
			return last.headLast();
			//return getLastPhrase().headLast();
			// for (Phrase ph : subphrases) {
			// if (ph.getHead() != ph.getLastPhrase().getHead()) {
			// return false;
			// }
			// }
			// return true;
		} else {
			Link lk = graph.getTopLink();
			if (lk != null) {
				if (lk.getType().equals("hasName")) {
					return true;// union structure
				}
			}
			return (getHead() == getLastPhrase().getHead());
		}
	}

	public String pprint(String indent) {
		return pprint(indent, true);
	}

	public String pprint(String indent, boolean iterate) {
		if (indent == null) {
			indent = "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(indent + "(" + begToken + ", " + endToken + ") " + synType + " \"" + text + "\"\n");
		if (synType != null && synType.equals("VP")) {
			sb.append(indent + "tense: " + tense + ", " + "Subj:" + subject);
			if (subject != null) {
				sb.append(", " + subject.getHead() + "\n");
			} else {
				sb.append("\n");
			}
		}
		sb.append(indent + "Graph: \n");
		if (graph != null) {
			sb.append(graph.pprint(indent + "\t"));
		} else {
			sb.append(indent + "\tnull\n");
		}
		if (subphrases != null && iterate) {
			sb.append(indent + "subphrases:\n");
			String indentJr = indent + "\t";
			for (Phrase p : subphrases) {
				sb.append(p.pprint(indentJr));
			}
		}
		return sb.toString();
	}

	public String getSynType() {
		return synType;
	}

	public void setSynType(String s) {
		synType = s;
	}

	public String getTense() {
		return tense;
	}

	public void setTense(String t) {
		tense = t;
	}

	public int getBegToken() {
		return begToken;
	}

	public int getEndToken() {
		return endToken;
	}

	public String getText() {
		return text;
	}

	public List<Phrase> getSubphrases() {
		return this.subphrases;
	}

	public void setGraph(ERGraph e) {
		graph = e;
	}

	public void setGraph(Entity e) {
		graph = new ERGraph(e);
	}

	public ERGraph getGraph() {
		return graph;
	}

	public int getEntityCount() {
		if (graph == null) {
			return 0;
		}
		return graph.getEntityCount();
	}

	public Entity getHead() {
		if (graph == null) {
			return null;
		}
		return graph.getHead();
	}

	public List<LexToken> getSentence() {
		return sentence;
	}

	public boolean isComposite() {
		return composite;
	}

	public void setComposite(boolean b) {
		composite = b;
	}

	/**
	 * Constructor for dictionary phrase.
	 * 
	 * @param token
	 *            : string form, like "filed"
	 * @param root
	 *            : stem form, like "file"
	 * @param phraseType
	 *            : like "VP"
	 */
	// public Phrase(String token, String phraseType, String entName) {
	// text = token;
	// synType = phraseType;
	// graph = new ERGraph(new Entity(entName));
	// }

	/**
	 * Two phrases combine to form a new phrase.
	 * 
	 * @param p1
	 *            Left phrase
	 * @param p2
	 *            Right phrase
	 * @param lk
	 *            Link that join them.
	 * @param headNew
	 *            1: left phrase is the new head, 2: right phrase is the new
	 *            head.
	 */
	public Phrase(Phrase p1, Phrase p2, Link lk, int headNew) {
		sentence = p1.sentence;
		subphrases = new ArrayList<Phrase>();
		if (lk == null && p1.isComposite()) {
			subphrases.addAll(p1.subphrases);
			subphrases.add(p2);
		} else {
			subphrases.add(p1);
			subphrases.add(p2);
		}
		graph = ERGraph.combine(p1.getGraph(), p2.getGraph(), lk, headNew);
		if (graph == null) {
			log.info("Graph==null");
		}
		// text = p1.getText() + " " + p2.getText();
		text = joinPhrases(p1, p2, null, null);
		if (headNew == 1) {
			synType = p1.getSynType();
			tense = p1.getTense();
			subject = p1.getSubject();
		} else if (headNew == 2) {
			synType = p2.getSynType();
			tense = p2.getTense();
			subject = p2.getSubject();
		} else {
			synType = p1.getSynType();
			tense = p1.getTense();
			subject = p1.getSubject();
		}

		begToken = p1.getBegToken();
		endToken = p2.getEndToken();
	}

	/**
	 * Combine two phrases while removing one of the entities in the resulting
	 * graph. used only in rule "abbreviation with dot"
	 * 
	 * @param p1
	 * @param p2
	 * @param remove
	 * @param headNew
	 */
	public Phrase(Phrase p1, Phrase p2, String remove, int headNew) {
		sentence = p1.sentence;
		subphrases = new ArrayList<Phrase>();
		subphrases.add(p1);
		subphrases.add(p2);
		if (headNew == 1) {
			graph = p1.getGraph().duplicate();
		} else {
			graph = p2.getGraph().duplicate();
		}
		graph.remove(remove);
		text = joinPhrases(p1, p2, null, null);
		if (headNew == 1) {
			synType = p1.getSynType();
			tense = p1.getTense();
			subject = p1.getSubject();
		} else if (headNew == 2) {
			synType = p2.getSynType();
			tense = p2.getTense();
			subject = p2.getSubject();
		}
		begToken = p1.getBegToken();
		endToken = p2.getEndToken();
	}

	/**
	 * This is just add a layer to indicate this phrase is one entity
	 * to make Phrase.headLast() behave correctly when "5 days" become a combined object.
	 * In general, objects combine to form one object, this is needed. 
	 * rule "NumberValue_TimeRef" use this constructor.
	 * @param ph
	 */
	public Phrase(Phrase ph) {
		sentence = ph.sentence;
		subphrases = new ArrayList<Phrase>();
		subphrases.add(ph);
		graph = ph.getGraph();
		text = ph.getText();
		synType = ph.getSynType();
		tense = ph.getTense();
		subject = ph.getSubject();
		begToken = ph.getBegToken();
		endToken = ph.getEndToken();
	}

	/**
	 * One phrase matched to one template phrase
	 * 
	 * @param p1
	 * @param phraseType
	 * @param g
	 */
	public Phrase(Phrase p1, String phraseType, ERGraph g) {
		sentence = p1.sentence;
		graph = g;
		text = p1.getText();
		synType = phraseType;
		if (phraseType.equals("VP")) {
			tense = p1.getTense();
			subject = p1.getSubject();
		}
		begToken = p1.getBegToken();
		endToken = p1.getEndToken();
	}

	@Override
	public Phrase clone() {
		Phrase ph = new Phrase(this, this.synType, this.graph);
		ph.composite = this.composite;
		if (subphrases != null) {
			ph.subphrases = new ArrayList<Phrase>(subphrases);
		}
		return ph;
	}

	/**
	 * Three phrases matched to one template phrase
	 * 
	 * @param p1
	 * @param p2
	 * @param phraseType
	 * @param g
	 */
	public Phrase(Phrase p1, Phrase p2, Phrase p3, String phraseType, ERGraph g) {
		sentence = p1.sentence;
		subphrases = new ArrayList<Phrase>();
		if (g.isSet()) {
			ERGraph g1 = p1.getGraph();
			if (g1.isSet()) {
				List<Phrase> ls = p1.getSubphrases();
				if (ls != null) {
					subphrases.addAll(ls);
				} else {
					subphrases.add(p1);
				}
			} else {
				subphrases.add(p1);
			}
			if (p2 != null) {
				ERGraph g2 = p2.getGraph();
				if (g2.isSet()) {
					List<Phrase> ls = p2.getSubphrases();
					if (ls != null) {
						subphrases.addAll(ls);
					} else {
						subphrases.add(p2);
					}
				} else {
					subphrases.add(p2);
				}
			}
			ERGraph g2 = p2.getGraph();
			if (g2.isSet()) {
				List<Phrase> ls = p3.getSubphrases();
				if (ls != null) {
					subphrases.addAll(ls);
				} else {
					subphrases.add(p3);
				}
			} else {
				subphrases.add(p3);
			}
		} else {
			subphrases.add(p1);
			if (p2 != null) {
				subphrases.add(p2);
			}
			subphrases.add(p3);
		}
		graph = g;
		// text = p1.getText() + " " + p2.getText() + " " + p3.getText();
		if (p2 == null) {
			text = joinPhrases(p1, p3, null, null);
		} else {
			text = joinPhrases(p1, p2, p3, null);
		}
		synType = phraseType;
		if (phraseType.equals("VP")) {
			tense = p1.getTense();
			subject = p1.getSubject();
		}
		begToken = p1.getBegToken();
		endToken = p3.getEndToken();
	}

	public Phrase(List<Phrase> pl, String phraseType, ERGraph g) {
		sentence = pl.get(0).sentence;
		subphrases = pl;
		graph = g;
		text = joinPhrases(pl);
		synType = phraseType;
		begToken = pl.get(0).getBegToken();
		endToken = pl.get(pl.size() - 1).getEndToken();
	}

	/**
	 * Four phrases matched to one template phrase
	 * 
	 * @param p1
	 * @param p2
	 * @param phraseType
	 * @param g
	 */
	public Phrase(Phrase p1, Phrase p2, Phrase p3, Phrase p4, String phraseType, ERGraph g) {
		sentence = p1.sentence;
		subphrases = new ArrayList<Phrase>();
		subphrases.add(p1);
		subphrases.add(p2);
		subphrases.add(p3);
		subphrases.add(p4);
		graph = g;
		// text = p1.getText() + " " + p2.getText() + " " + p3.getText();
		text = joinPhrases(p1, p2, p3, p4);
		synType = phraseType;
		begToken = p1.getBegToken();
		endToken = p4.getEndToken();
	}

	public LegaLanguage getOnto() {
		return graph.onto;
	}

	/**
	 * Two phrases matched to one template phrase
	 * 
	 * @param p1
	 * @param p2
	 * @param phraseType
	 * @param g
	 */
	public Phrase(Phrase p1, Phrase p2, String phraseType, ERGraph g) {
		sentence = p1.sentence;
		subphrases = new ArrayList<Phrase>();
		subphrases.add(p1);
		subphrases.add(p2);
		graph = g;
		// text = p1.getText() + " " + p2.getText();
		text = joinPhrases(p1, p2, null, null);
		synType = phraseType;
		begToken = p1.getBegToken();
		endToken = p2.getEndToken();
	}

	public static String joinPhrases(Phrase p1, Phrase p2, Phrase p3, Phrase p4) {
		StringBuilder sb = new StringBuilder(p1.getText());
		if (p2 == null) {
			return sb.toString();
		}
		int startIndex = p1.getBegToken();
		int startPos = p1.sentence.get(startIndex).getStart();

		int index2 = p2.getBegToken();
		int pos2 = p1.sentence.get(index2).getStart();
		int offset2 = pos2 - startPos;
		while (sb.length() < offset2) {
			sb.append(" ");
		}
		sb.append(p2.getText());
		if (p3 == null) {
			return sb.toString();
		}

		int index3 = p3.getBegToken();
		int pos3 = p1.sentence.get(index3).getStart();
		int offset3 = pos3 - startPos;
		while (sb.length() < offset3) {
			sb.append(" ");
		}
		sb.append(p3.getText());
		if (p4 == null) {
			return sb.toString();
		}

		int index4 = p4.getBegToken();
		int pos4 = p1.sentence.get(index4).getStart();
		int offset4 = pos4 - startPos;
		while (sb.length() < offset4) {
			sb.append(" ");
		}
		sb.append(p4.getText());
		return sb.toString();

	}

	public static String joinPhrases(List<Phrase> pl) {
		Phrase p0 = pl.get(0);
		StringBuilder sb = new StringBuilder(p0.getText());
		int startIndex = p0.getBegToken();
		int startPos = p0.sentence.get(startIndex).getStart();
		for (int i = 1; i < pl.size(); i++) {
			Phrase p = pl.get(i);
			int index2 = p.getBegToken();
			int pos2 = p.sentence.get(index2).getStart();
			int offset2 = pos2 - startPos;
			while (sb.length() < offset2) {
				sb.append(" ");
			}
			sb.append(p.getText());
		}
		return sb.toString();
	}

	public Phrase createOffsetPhrase(int offset) {
		Phrase ph = new Phrase(this, this.synType, this.graph);
		ph.begToken += offset;
		ph.endToken += offset;
		return ph;
	}

	/**
	 * Two phrases combine to form a new phrase.
	 * 
	 * @param p1
	 *            Left phrase
	 * @param p0
	 *            middle phrase
	 * @param p2
	 *            Right phrase
	 * @param lk
	 *            Link that join them.
	 * @param headNew
	 *            1: left phrase is the new head, 2: right phrase is the new
	 *            head.
	 */
	public Phrase(Phrase p1, Phrase p0, Phrase p2, Link lk, int headNew) {
		sentence = p1.sentence;
		subphrases = new ArrayList<Phrase>();
		if (lk == null && p1.isComposite()) {
			subphrases.addAll(p1.subphrases);
			subphrases.add(p0);
			subphrases.add(p2);
		} else {
			subphrases.add(p1);
			subphrases.add(p0);
			subphrases.add(p2);
		}
		graph = ERGraph.combine(p1.getGraph(), p2.getGraph(), lk, headNew);
		// text = p1.getText() + " " + p0.getText() + " " + p2.getText();
		text = joinPhrases(p1, p0, p2, null);
		if (headNew == 1) {
			synType = p1.getSynType();
			tense = p1.getTense();
			subject = p1.getSubject();
		} else if (headNew == 2) {
			synType = p2.getSynType();
			tense = p2.getTense();
			subject = p2.getSubject();
		}
		begToken = p1.getBegToken();
		endToken = p2.getEndToken();
	}

	/**
	 * @param args
	 */
	// public Phrase(String token, int beg, int end, String phraseType) {
	// text = token;
	// begToken = beg;
	// endToken = end;
	// synType = phraseType;
	// }

	/**
	 * @param args
	 */
	public Phrase(String token, int beg, int end, List<LexToken> tks) {
		text = token;
		begToken = beg;
		endToken = end;
		synType = null;
		sentence = tks;
	}

	public Phrase(String token, String type, ERGraph g, int beg, int end, List<LexToken> tks) {
		text = token;
		begToken = beg;
		endToken = end;
		synType = type;
		sentence = tks;
		graph = g;
	}

	/**
	 * require synType and Entity type and linkType agreement
	 * 
	 * @param p1
	 * @param p2
	 * @param p3
	 */
	public List<Phrase> lastPhrases(Phrase p1, Phrase p2, Phrase p3) {
		List<Phrase> plist = null;
		int endbound = 0;
		if (composite) {
			Phrase headPhrase = this.subphrases.get(0);
			plist = headPhrase.getPhraseList();
			endbound = headPhrase.getEndToken();
		} else {
			plist = getPhraseList();
			endbound = this.endToken;
		}
		if (plist == null) {
			return null;
		}
		// find the last match:
		List<Phrase> pl3 = new ArrayList<Phrase>();
		for (Phrase p : plist) {
			if (p.endToken != endbound) {
				continue;
			}
			if (p.synType.equals(p3.synType)) {
				pl3.add(p);
			}
		}
		if (pl3.size() == 0) {
			return null;
		}
		// find match for p2:
		List<List<Phrase>> pll2 = new ArrayList<List<Phrase>>();
		for (Phrase pp3 : pl3) {
			for (Phrase p : plist) {
				if (p.endToken != pp3.begToken) {
					continue;
				}
				if (p == null || p.synType == null || p2 == null || p2.synType == null) {
					log.info("Error checking lastPhrase");
				}
				if (p.synType.equals(p2.synType)) {
					List<Phrase> pl = new ArrayList<Phrase>();
					pl.add(p);
					pl.add(pp3);
					pll2.add(pl);
				}
			}
		}
		if (pll2.size() == 0) {
			return null;
		}
		List<List<Phrase>> pll1 = new ArrayList<List<Phrase>>();
		for (List<Phrase> pl : pll2) {
			Phrase pp2 = pl.get(0);
			for (Phrase p : plist) {
				if (p.endToken != pp2.begToken) {
					continue;
				}
				if (p.synType.equals(p1.synType)) {
					List<Phrase> pl1 = new ArrayList<Phrase>(pl);
					pl1.add(0, p);
					pll1.add(pl1);
				}
			}
		}
		if (pll1.size() == 0) {
			return null;
		}
		List<Phrase> selected = null;
		int dmax = 1000;
		for (List<Phrase> pl : pll1) {
			int dtotal = 0;
			int d1 = pl.get(0).distance(p1);
			int d2 = pl.get(1).distance(p2);
			int d3 = pl.get(2).distance(p3);
			if (d1 < 0 || d2 < 0 || d3 < 0) {
				continue;
			}
			dtotal = d1 + d2 + d3;
			if (dtotal < dmax) {
				dmax = dtotal;
				selected = pl;
			}
		}
		return selected;
	}

	/**
	 * require synType and Entity type and linkType agreement
	 * 
	 * @param p1
	 * @param p2
	 * @param p3
	 */
	public List<Phrase> lastPhrases1(Phrase p1) {
		List<Phrase> plist = null;
		int endbound = 0;
		if (composite) {
			Phrase headPhrase = this.subphrases.get(0);
			plist = headPhrase.getPhraseList();
			endbound = headPhrase.getEndToken();
		} else {
			plist = getPhraseList();
			endbound = this.endToken;
		}
		if (plist == null) {
			return null;
		}
		// find the last match:
		List<Phrase> pl3 = new ArrayList<Phrase>();
		for (Phrase p : plist) {
			if (p.endToken != endbound) {
				continue;
			}
			if (p == null || p.synType == null || p1 == null || p1.synType == null) {
				continue;
			}
			if (p.synType.equals(p1.synType)) {
				pl3.add(p);
			}
		}
		if (pl3.size() == 0) {
			return null;
		}
		Phrase selected = null;
		int dmax = 1000;
		for (Phrase p : pl3) {
			int dtotal = p.distance(p1);
			if (dtotal < 0) {
				continue;
			}
			if (dtotal < dmax) {
				dmax = dtotal;
				selected = p;
			}
		}
		if (selected != null) {
			List<Phrase> ret = new ArrayList<Phrase>();
			ret.add(selected);
			return ret;
		}
		return null;
	}

	public int distance(Phrase p) {
		int d = 0;
		if (getText().equalsIgnoreCase(p.getSynType())) {
			return d;
		}
		d++;
		Entity phead = p.getHead();
		Entity myhead = getHead();
		int dd = myhead.distance(phead);
		if (dd < 0) {
			return dd;
		}
		return d + dd;
	}

	/**
	 * 
	 * @return A list of all phrases at all levels, not sorted.
	 */
	public List<Phrase> getPhraseList() {
		List<Phrase> ret = new ArrayList<Phrase>();
		ret.add(this);
		if (subphrases != null) {
			for (Phrase p : subphrases) {
				List<Phrase> pl = p.getPhraseList();
				ret.addAll(pl);
			}
			return ret;
		}
		return ret;
	}

	// public boolean match(Phrase p) {
	// if (synType == null || p.getSynType() == null) {
	// return false;
	// }
	// if (!synType.equals(p.getSynType())) {
	// return false;
	// }
	// if (!entity.equals(p.entity)) {
	// return false;
	// }
	// return true;
	// }

	@Override
	public String toString() {
		return text;
	}

	public void addSubphrase(Phrase sub) {
		this.subphrases.add(sub);
	}

	public List<Phrase> joinParse(int joinPoint, List<Phrase> pl) {
		// Krunner krun = Krunner.getRunner();
		LegaLanguage onto = null;
		for (Phrase p : pl) {
			Entity eh = p.getHead();
			if (eh.onto != null) {
				onto = eh.onto;
				break;
			}
		}
		if (onto == null) {
			return null;
		}
		Srunner srun = null;
		try {
			srun = onto.createSrunner(false);
		} catch (Exception ex) {
			log.info("");
		}
		List<LexToken> tokens = new ArrayList<LexToken>();
		for (int i = begToken; i < joinPoint; i++) {
			tokens.add(this.sentence.get(i));
		}
		int nextStart = pl.get(0).getBegToken();
		int nextEnd = pl.get(pl.size() - 1).getEndToken();// pl.get(pl.size() -1).endToken
		for (int i = nextStart; i < nextEnd; i++) {
			tokens.add(this.sentence.get(i));
		}
		List<Phrase> phlist = new ArrayList<Phrase>();
		for (int i = 0; i < tokens.size(); i++) {
			LexToken tk = tokens.get(i);
			Phrase ph = new Phrase(tk.text.toLowerCase(), i, i + 1, tokens);
			phlist.add(ph);
		}
		srun.insertList(phlist);
		srun.execute();
		phlist = srun.oneBestPhrase(nextEnd);
		srun.dispose();
		return phlist;
	}

	/**
	 * Test to see if this phrase can be successfully can be joined to a another phrase
	 * The reason this is needed is because: Suppose we see a pattern
	 * 
	 * p0 + p1 + "and" + p2
	 * 
	 * we want to see if "and" joins p1 and p2. For this to be true, both "p0 + p1" and "p0 + p2" must be valid phrases.
	 * So we need to test "p0 + p2". However, all rules in rule engine only test for adjacent phrases. This function
	 * build a token list with p0 tokens and p2 tokens. So the rules in rule engine now applies to them (because p2 tokens
	 * now immediately follow p0 tokens) and create a new Knowledge Session just to test this.
	 * 
	 * It is an expensive operation. If it get tested too often it'll significantly slow down the process.
	 * 
	 * @param joinPoint
	 * @param p1
	 *            the joining partner. Usually not immediately following this phrase
	 * @return the successfully joined phrase.
	 */
	public List<Phrase> joinParse(Phrase p1) {
		LegaLanguage onto = null;

		Entity eh = getHead();
		if (eh.onto != null) {
			onto = eh.onto;
		} else {
			eh = p1.getHead();
			if (eh.onto != null) {
				onto = eh.onto;
			}
		}
		if (onto == null) {
			return null;
		}
		Srunner srun = null;
		try {
			srun = onto.createSrunner(false);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		List<LexToken> tokens = new ArrayList<LexToken>();
		for (int i = begToken; i < endToken; i++) {
			tokens.add(this.sentence.get(i));
		}
		int nextStart = p1.getBegToken();
		int nextEnd = p1.getEndToken();
		for (int i = nextStart; i < nextEnd; i++) {
			tokens.add(this.sentence.get(i));
		}
		List<Phrase> phlist = new ArrayList<Phrase>();
		for (int i = 0; i < tokens.size(); i++) {
			LexToken tk = tokens.get(i);
			Phrase ph = new Phrase(tk.text.toLowerCase(), i, i + 1, tokens);
			phlist.add(ph);
		}
		srun.insertList(phlist);
		srun.execute();
		phlist = srun.oneBestPhrase(nextEnd);
		srun.dispose();
		if (phlist.size() == 0) {
			return null;
		}
		Phrase ph = phlist.get(0);
		if (ph.begToken != begToken || ph.endToken != endToken) {
			return null;
		}
		return phlist;
	}

	/**
	 * Determine whether head has an attribute instance of a given kind.
	 * Note, we use isInstanceOf() , not the more general isKindOf() to save time.
	 * 
	 * @param attr
	 * @return
	 */
	public boolean hasAttribute(String attr) {
		Entity hd = getHead();
		List<Entity> le = graph.getModifierList(hd);
		if (le != null) {
			for (Entity e : le) {
				if (e.isInstanceOf(attr)) {
					return true;
				}
			}
		}
		le = graph.getMembers(hd);
		if (le == null)
			return false;
		for (Entity e : le) {
			List<Entity> le1 = graph.getModifierList(e);
			if (le1 != null) {
				for (Entity e1 : le1) {
					if (e1.isInstanceOf(attr)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isSubString(Phrase ph) {
		String t1 = text.toLowerCase();
		String t2 = ph.getText().toLowerCase();
		if (t1.contains(t2)) {
			return true;
		}
		if (t2.contains(t1)) {
			return true;
		}
		return false;
	}

	public boolean similar(Phrase ph) {
		if (!synType.equals(ph.getSynType())) {
			return false;
		}
		int myDepth = getDepth();
		int phDepth = ph.getDepth();
		int diffDepth = myDepth - phDepth;
		if (diffDepth > 3) {//NOTICE OF MOTION AND PLAINTIFFS MOTION FOR JUDGMENT ON THE PLEADINGS 
			// the second phrase can be long, but the first cannot be too long
			//			if (diffDepth < -1 || diffDepth > 1) {
			return false;
		}
		ERGraph g1 = getGraph();
		ERGraph g2 = ph.getGraph();
		Entity h1 = g1.getHead();
		Entity h2 = g2.getHead();
		if (h1.isKindOf("Document") && h2.isKindOf("Document"))
			return true;
		if (h1.isKindOf("IntelAgent") && h2.isKindOf("IntelAgent"))
			return true;
		if (h1.isKindOf("SerialValue") && h2.isKindOf("SerialValue"))
			return true;
		if (h1.isKindOf("StatusProc") && h2.isKindOf("StatusProc"))
			return true;
		Entity cls1 = h1.getTheClass();
		if (h2.isKindOf(cls1)) {
			return true;
		} else {
			List<Link> list1 = h1.onto.findLinks("subclassOf", cls1, null);
			for (Link lk : list1) {
				cls1 = lk.getArg2();
				if (h2.isKindOf(cls1))
					return true;
			}
		}
		Entity cls2 = h2.getTheClass();
		if (h1.isKindOf(cls2)) {
			return true;
		} else {
			List<Link> list2 = h1.onto.findLinks("subclassOf", cls2, null);
			for (Link lk : list2) {
				cls2 = lk.getArg2();
				if (h1.isKindOf(cls2))
					return true;
			}
		}
		//		Link lk1 = g1.getTopLink();
		//		Link lk2 = g2.getTopLink();
		//		if (lk1 != null && lk2 == null)
		//			return false;
		//		if (lk2 != null && lk1 == null)
		//			return false;
		//		if (lk1 != null && lk2 != null) {
		//			return lk1.getType().equals(lk2.getType());
		//		}
		return false;
	}

	public int getDepth() {
		if (subphrases == null || subphrases.size() == 0) {
			return 1;
		}
		int maxDepth = 0;
		for (Phrase ph : subphrases) {
			int depth = ph.getDepth();
			if (depth > maxDepth) {
				maxDepth = depth;
			}
		}
		return maxDepth + 1;
	}

	/**
	 * 
	 * @param p0
	 *            separator, like ";", ","
	 * @param p
	 *            a phrase with head as a member
	 * @bLeftToRight
	 * 		true: phrase order: this, p0, p
	 * 		false:phrase order: p, p0, this    
	 * @return
	 */
	public Phrase addToSet(Phrase p0, Phrase p, String setName, boolean bLeftToRight) {
		ERGraph gp = p.getGraph();
		ERGraph gt = this.getGraph();
		Entity hd = gt.getHead();
		LegaLanguage onto = hd.onto;
		String setType = "AND";
		if (p0 != null) {
			if (p0.text.equals("/") || p0.text.equalsIgnoreCase("or")) {
				setType = "OR";
			} else {
				if (p0.text.equals("-")) {
					setType = "RANGE";
				}
			}
		}
		ERGraph gn;
		Entity cls = onto.getEntity(setName);
		if (this.isSet() || gp.isSet()) {
			gn = gt.duplicate();
			gn.mergeSet(gp);
		} else {
			// create a pseudo entity as head:
			Entity en;
			if (cls != null) {
				if (bLeftToRight) {
					en = new Entity(setName, cls, hd.getEntityType(), onto, this.begToken);
				} else {
					en = new Entity(setName, cls, hd.getEntityType(), onto, p.getBegToken());
				}
			} else {
				en = hd.clone();
			}
			gn = new ERGraph(en);
			gn.merge(gt);
			gn.merge(gp);
			Entity cls_t = gt.getHead().getTheClass();
			Entity cls_p = gp.getHead().getTheClass();
			if (cls_t != null && cls_p != null) {
				if (cls_t.isKindOf("LitigationParty") && cls_p.isKindOf("LitigationParty") && !cls_t.equals(cls_p)) {
					en.setTheClass(onto.getEntity("LitigationParty"));
				}
			}
			// Prevent two identical entities to combine. This happens when "LG Electronics and LG" of  "LG Electronics and LG Mobile" is seen.
			Link r1 = new Link("hasMember", en, gt.getHead());
			Link r2 = new Link("hasMember", en, gp.getHead());
			en.addClass(cls_t);
			en.addClass(cls_p);
			en.setEntityType(Entity.TYPE_SET);
			gn.addLink(r1);
			gn.addLink(r2);
		}

		gn.setSetType(setType);
		gn.setEntityName(setName);
		Phrase ph;
		if (p0 == null) {
			if (bLeftToRight) {
				ph = new Phrase(this, p, this.synType, gn);
			} else {
				ph = new Phrase(p, this, this.synType, gn);
			}
		} else {
			if (bLeftToRight) {
				ph = new Phrase(this, p0, p, this.synType, gn);
			} else {
				ph = new Phrase(p, p0, this, this.synType, gn);
			}
		}
		ph.setTense(this.tense);
		return ph;
	}

	/**
	 * 
	 * @param p0
	 *            separator, like ";", ","
	 * @param p
	 *            a phrase with head as a member
	 * @return
	 */
	public Phrase addToSetRightToLeft(Phrase p0, Phrase p, String setName) {
		ERGraph gp = p.getGraph();
		ERGraph gt = this.getGraph();
		Entity hd = gt.getHead();
		LegaLanguage onto = hd.onto;

		String setType = "AND";
		if (p0.text.equals("/") || p0.text.equalsIgnoreCase("or")) {
			setType = "OR";
		} else {
			if (p0.text.equals("-")) {
				setType = "RANGE";
			}
		}
		ERGraph gn;
		Entity cls = onto.getEntity(setName);
		if (cls == null) {
			cls = hd.theClass;
		}
		if (this.isSet() && cls != null && hd.isInstanceOf(cls)) {
			gn = gt.duplicate();
			gn.mergeSet(gp);
		} else if (gp.isSet() && cls != null && gp.getHead().isInstanceOf(cls)) {
			gn = gp.duplicate();
			gn.mergeSet(gt);
		} else {
			// create a pseudo entity as head:
			Entity en;
			if (cls != null) {
				en = new Entity(setName, cls, hd.getEntityType(), hd.onto, p.getBegToken());
			} else {
				en = hd.clone();
			}
			gn = new ERGraph(en);

			gn.merge(gt);
			gn.merge(gp);
			Link r1 = new Link("hasMember", en, gt.getHead());
			Link r2 = new Link("hasMember", en, gp.getHead());
			gn.addLink(r1);
			gn.addLink(r2);
		}
		gn.setSetType(setType);
		Phrase ph = new Phrase(p, p0, this, this.synType, gn);
		return ph;
	}

	public boolean topLinkMatch(Phrase p) {
		if (isSet()) {
			Phrase ph = subphrases.get(subphrases.size() - 1);
			return ph.topLinkMatch(p);
		} else {
			return graph.topLinkMatch(p.getGraph());
		}
	}

	public boolean lengthInRange(Phrase p, int range) {
		int myLength = endToken - begToken;
		int hisLength = p.getEndToken() - p.getBegToken();
		int delta = Math.abs(hisLength - myLength);
		if (delta <= range) {
			return true;
		}
		return false;
	}

	public void transferLinks(String linkType, Entity efrom, Entity eto) {
		graph.transferLinks(linkType, efrom, eto);
	}

	// public Phrase addToSetOld(Phrase p0, Phrase p, String setName) {
	// ERGraph gp = p.getGraph();
	// ERGraph g0 = this.getGraph();
	// Entity hd = this.getHead();
	//
	// String setType = "AND";
	// if (p0.text.equals("/") || p0.text.equalsIgnoreCase("or")) {
	// setType = "OR";
	// } else {
	// if (p0.text.equals("-")) {
	// setType = "RANGE";
	// }
	// }
	// // create a pseudo entity as head:
	// Entity en;
	// Entity cls = Ontology.getEntity(setName);
	// if (cls != null) {
	// en = new Entity(setName, cls, hd.getEntityType());
	// } else {
	// en = hd.clone();
	// }
	// ERGraph gn = new ERGraph(en);
	//
	// if (gp.members != null) {// is a set
	// if (!gp.getSetType().equals(setType)) {
	// return null;
	// }
	// if (g0.members != null) {
	// if (!g0.getSetType().equals(setType)) {
	// return null;
	// }
	// gn.addmembers(g0.members);
	// gn.addmembers(gp.members);
	// } else {
	// gn.addmembers(g0);
	// gn.addmembers(gp.members);
	// }
	// } else if (g0.members != null) {
	// if (!g0.getSetType().equals(setType)) {
	// return null;
	// }
	// gn.addmembers(g0.members);
	// gn.addmembers(gp);
	// } else {
	// gn.addmembers(g0);
	// gn.addmembers(gp);
	// }
	// gn.setSetType(setType);
	//
	// Phrase ph = new Phrase(this, p0, p, this.synType, gn);
	// return ph;
	// }

	// public Phrase addToSet(Phrase p, String setName) {
	// ERGraph gp = p.getGraph();
	// ERGraph g0 = this.getGraph();
	// Entity hd = this.getHead();
	//
	// String setType = "AND";
	// // create a pseudo entity as head:
	// Entity en = new Entity(setName, Ontology.getEntity(setName),
	// hd.getEntityType());
	// ERGraph gn = new ERGraph(en);
	//
	// if (gp.members != null) {// is a set
	// if (!gp.getSetType().equals(setType)) {
	// return null;
	// }
	// if (g0.members != null) {
	// if (!g0.getSetType().equals(setType)) {
	// return null;
	// }
	// gn.addmembers(g0.members);
	// gn.addmembers(gp.members);
	// } else {
	// gn.addmembers(g0);
	// gn.addmembers(gp.members);
	// }
	// } else if (g0.members != null) {
	// if (!g0.getSetType().equals(setType)) {
	// return null;
	// }
	// gn.addmembers(g0.members);
	// gn.addmembers(gp);
	// } else {
	// gn.addmembers(g0);
	// gn.addmembers(gp);
	// }
	// gn.setSetType(setType);
	//
	// Phrase ph = new Phrase(this, null, p, this.synType, gn);
	// return ph;
	// }

	public boolean equivalent(Phrase p) {
		if (synType == null || !synType.equals(p.getSynType())) {
			return false;
		}
		if (synType.equals("VP")) {
			try {
				if (!tense.equals(p.getTense())) {
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// String txt = p.getText();
		// if (txt.startsWith("motion") && txt.endsWith("LLC")) {
		// log.info("");
		// }
		return graph.equivalent(p.getGraph());
	}

	public boolean isSet() {
		if (graph == null) {
			return false;
		}
		return graph.isSet();
	}

	/**
	 * For
	 * "motion to extend time for American Airlines, Inc. to Respond to complaint filed by Walker Digital LLC"
	 * There is ambiguity, "filed by Walker Digital LLC" can attach to "motion"
	 * or "complaint". For docket entries, they are always to the head of entire
	 * sentences, "motion" in this case. This method transfers attachments from
	 * "complaint" to "motion".
	 * 
	 * @return
	 */
	public boolean transferFiledBy() {
		Entity hd = getHead();
		boolean b = false;
		List<Link> lks = graph.getLinks();
		for (int i = 0; i < lks.size(); i++) {
			Link lk = lks.get(i);
			if (lk.getType().equals("ugoerOfProc") && lk.getArg2().isInstanceOf("ProcFileDoc") && (lk.getArg1() != hd)) {
				Link nlk = new Link("ugoerOfProc", hd, lk.getArg2());
				lks.remove(i);
				lks.add(i, nlk);
				b = true;
			} else if (lk.getType().equals("byAgent") && (lk.getArg1() != hd) && lk.getArg1().isKindOf("Document")) {
				Link nlk = new Link("byAgent", hd, lk.getArg2());
				lks.remove(i);
				lks.add(i, nlk);
				b = true;
			}
		}
		return b;
	}

	public boolean sameRange(Phrase p) {
		return (p.getBegToken() == begToken && p.getEndToken() == endToken);
	}

	public Phrase getSubject() {
		return subject;
	}

	public void setSubject(Phrase p) {
		subject = p;
	}

	public int diffTokenCount(Phrase p) {
		int diff = p.tokenLength() - this.tokenLength();
		if (diff < 0) {
			diff = -diff;
		}
		return diff;
	}

	public int tokenLength() {
		return endToken - begToken;
	}

	public int leafPhraseCount() {
		if (subphrases == null || subphrases.size() == 0) {
			return 1;
		}
		int count = 0;
		for (Phrase p : subphrases) {
			count += p.leafPhraseCount();
		}
		return count;
	}
}
