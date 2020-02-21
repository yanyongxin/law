package sftrack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for a dictionary entry. Two possibilities: (1) a word , Example:
 * "counterclaim" (2) regular expression. In this case, a list of possible first
 * token text is given. For example for
 * "(the)?Smith\\s*Law\\*Firm(\\,?\\s*LLC)?", list="the smith" In other words,
 * the first word can either be "the" or "smith". Another example, for
 * "(US|U\\.S\\.)\\s*Judge", list="US U"
 * 
 * @author yanyongxin
 * 
 */
public class DictPhrase {
	private static final Logger log = LoggerFactory.getLogger(DictPhrase.class);

	String text; // ordinary String, or regular expression
	ERGraph graph;
	String synType = null; // syntactic type, "NP", "VP", etc.
	String tense; // if synType is "VP", then tense="present", "past", "ing"
	Pattern regex = null; // compiled regular expression if text=regular
							// expression
	String[] list; // a list of starting tokens if text is regular expression.

	@Override
	public boolean equals(Object o) {
		if (o.getClass() != this.getClass()) {
			return false;
		}
		DictPhrase dp = (DictPhrase) o;
		if (!text.equalsIgnoreCase(dp.getText())) {
			return false;
		}
		if (!graph.equals(dp.getGraph())) {
			return false;
		}
		return false;
	}

	public String getText() {
		return text;
	}

	public DictPhrase(String txt, String type, String ts, String ent, Ontology onto) throws Exception {
		text = txt;
		synType = type;
		tense = ts;
		Entity e = onto.getEntity(ent);
		if (e == null) {
			throw new LexEntityNotFoundException(ent);
		}
		graph = new ERGraph(e);
		// tklist = LexToken.tokenize(txt);
	}

	public DictPhrase(String txt, String type, String ts, String ent, String listUnder, Ontology onto) throws Exception {
		text = txt;
		regex = Pattern.compile(txt, Pattern.CASE_INSENSITIVE);
		synType = type;
		tense = ts;
		Entity e = onto.getEntity(ent);
		if (e == null) {
			throw new LexEntityNotFoundException(ent);
		}
		list = listUnder.split("\\s");
		graph = new ERGraph(e);
		// tklist = LexToken.tokenize(txt);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("text:" + text + "; ");
		sb.append("synType:" + synType + "; ");
		sb.append("entity: " + graph.getHead().toString());
		return sb.toString();
	}

	public Phrase matchOne(Phrase p) {
		if (text.equalsIgnoreCase(p.getText())) {
			Phrase ph = new Phrase(p, this.getSynType(), this.getGraph().cloneInstance(p.getBegToken()));
			return ph;
		}
		return null;
	}

	public Phrase matchFirstOne(Phrase p) {
		if (list != null && this.getGraph().getHead().isKindOf("OrgCo")) {
			for (String s : list) {
				if (s.equalsIgnoreCase(p.getText())) {
					Phrase ph = new Phrase(p, this.getSynType(), this.getGraph().cloneInstance(p.getBegToken()));
					return ph;
				}
			}
		}
		return null;
	}

	public Phrase match(Phrase p) {
		if (list != null) {
			for (String s : list) {
				if (s.equalsIgnoreCase(p.getText())) {
					int tkpos = p.getBegToken();
					List<LexToken> tks = p.getSentence();
					LexToken bk = tks.get(tkpos);
					int offset = bk.getStart();
					String sent = bk.parent;
					Matcher m = regex.matcher(sent);
					if (m.find(offset)) {
						int mstart = m.start();
						if (mstart != offset) {
							continue;
						}
						int mend = m.end();
						String result = m.group().trim();
						if (mend < sent.length()) {
							char c = sent.charAt(mend);
							if (c == 's' || c == 'S') {
								result = result + c;
								mend++;
							}
						}
						int end_tkpos = tks.size();
						for (int i = tkpos; i < tks.size(); i++) {
							if (tks.get(i).getStart() >= mend) {
								end_tkpos = i;
								break;
							}
						}
						Phrase ph = new Phrase(result, this.getSynType(), this.getGraph().cloneInstance(p.getBegToken()), tkpos, end_tkpos, tks);
						Entity eh = ph.getHead();
						if (eh.getName().equals("RuleNumber")) {
							// change (643:RuleNumber:INSTANCE:(500:RuleNumber)) to (643:7.1:INSTANCE:(500:RuleNumber))
							eh.name = result;
						}
						if (this.getSynType().equals("VP")) {
							ph.setTense(this.getTense());
						}
						return ph;
					}
				}
			}
		} else {
			Phrase ph = matchOne(p);
			if (ph == null) {
				return null;
			}
			if (this.getSynType().equals("VP")) {
				ph.setTense(this.getTense());
			}
			return ph;
			// List<Phrase> pl = matchAll(p);
			// if (pl != null && pl.size() > 0) {
			// Phrase ph;
			// if (pl.size() == 1) {
			// ph = new Phrase(p, this.getSynType(),
			// this.getGraph().cloneInstance());
			// } else {
			// ph = new Phrase(pl, this.getSynType(),
			// this.getGraph().cloneInstance());
			// }
			// if (this.getSynType().equals("VP")) {
			// ph.setTense(this.getTense());
			// }
			// return ph;
			// }
		}
		return null;
	}

	// public boolean matchFirst(Phrase ph) {
	// if (text.equalsIgnoreCase(ph.text)) {
	// return true;
	// }
	// if (tklist.get(0).text.equalsIgnoreCase(ph.text)) {
	// return true;
	// }
	// return false;
	// }
	//
	// public List<Phrase> matchAll(Phrase ph) {
	// List<Phrase> phlist = null;
	// if (matchFirst(ph)) {
	// phlist = new ArrayList<Phrase>();
	// phlist.add(ph);
	// } else {
	// return null;
	// }
	// Phrase p0 = ph;
	// for (int i = 1; i < tklist.size(); i++) {
	// Phrase p = Phrase.getNextPhrase(p0);
	// if (p == null) {
	// return null;
	// }
	// if (!tklist.get(i).text.equalsIgnoreCase(p.text)) {
	// return null;
	// }
	// phlist.add(p);
	// p0 = p;
	// }
	// return phlist;
	// }

	public String getSynType() {
		return synType;
	}

	public ERGraph getGraph() {
		return graph;
	}

	public String getTense() {
		return tense;
	}
}
