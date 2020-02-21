package sftrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A list of phrases in sequential order. They are the parsed segments of original text.
 * For a completely parsed sentence, there is only one phrase, covering the entire text.
 * If not, then original text are parsed into several segments. A non-overlapping sequence is an Analysis.
 * 
 * @author yanyongxin
 * 
 */
public class Analysis {
	/**
	 * One Case Demo. Yongxin Yan 2013.10.22.
	 */
	List<Phrase> plist;
	int totalTokens = 0; // total number of tokens in the collection
	int totalLength = 0; // total number of characters in the collection
	int score = -1000;

	// in terms tokens:
	int begToken;
	int endToken;

	int startPos;
	int endPos;

	public Analysis(List<Phrase> pl) {
		plist = pl;
		for (Phrase p : pl) {
			totalTokens += p.getEndToken() - p.getBegToken();
		}
		begToken = pl.get(0).getBegToken();
		endToken = pl.get(pl.size() - 1).getBegToken();
		score = calcScore();
	}

	public List<Phrase> getPhraseList() {
		return plist;
	}

	public int getPhraseCount() {
		return plist.size();
	}

	public int getBegToken() {
		return begToken;
	}

	public int getEndToken() {
		return endToken;
	}

	public int getStartPos() {
		return startPos;
	}

	public int getEndPos() {
		return endPos;
	}

	public int getTotalTokens() {
		return totalTokens;
	}

	public int getScore() {
		return score;
	}

	public int calcScore() {
		int d = 0;
		for (int i = 0; i < plist.size(); i++) {
			Phrase p = plist.get(i);
			// NP reward:
			if (p.getSynType().equals("NP")) {
				d += 2;
			} else if (p.getSynType().equals("PRP") || p.getSynType().equals("PARTICLE")) {
				d -= 4;
			}
			// need to have a link:
			ERGraph g = p.getGraph();
			if (g != null && g.links != null && g.links.size() > 0) {
				d++;
			}
			// length score:
			int nTokens = p.getEndToken() - p.getBegToken();
			d += nTokens - 1;
			int nEntities = g.entities.size();
			// less entities gives higher score. This ensures "opening brief"(as one concept) wins over "opening" (modify) "brief" (as two concepts) 
			int def = nTokens - nEntities;
			d += def;
			// break penalty:
			if (i > 0) {
				int gap = p.getBegToken() - plist.get(i - 1).getEndToken();
				if (gap > 0) {
					d -= 2;
				} else {
					// this is to make sure that "stipulation to extend time to respond to complaint"
					// wins over "stipulation to extend" ;; "time to respond to complaint"
					// In fact, that's what happened when I removed this penalty.
					d -= 4;
				}
			}
		}
		d -= plist.size() - 1;
		return d;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(" + score + "," + plist.size() + ") ");
		for (Phrase p : plist) {
			sb.append(p + ";; ");
		}
		return sb.toString();
	}

	public String getText() {
		StringBuilder sb = new StringBuilder();
		for (Phrase p : plist) {
			sb.append(p + ";; ");
		}
		return sb.toString();
	}

	private boolean addToCombineList(List<List<Phrase>> rll) {
		List<Phrase> pl = getPhraseList();
		if (pl.size() != rll.size()) {
			return false;
		}
		for (int i = 0; i < pl.size(); i++) {
			Phrase p = pl.get(i);
			List<Phrase> pi = rll.get(i);
			if (p.sameRange(pi.get(0))) {
				if (!pi.contains(p)) {
					pi.add(p);
				}
			} else {
				return false;
			}
		}
		return true;
	}

	public static List<List<List<Phrase>>> combine(List<Analysis> al) {
		List<List<List<Phrase>>> ret = new ArrayList<List<List<Phrase>>>();
		// each list under ret is a combination of phrases having the same phrase lengths
		List<List<Phrase>> first = new ArrayList<List<Phrase>>();
		// initialize:
		Analysis a0 = al.get(0);
		List<Phrase> lp = a0.getPhraseList();
		for (int i = 0; i < lp.size(); i++) {
			List<Phrase> cl = new ArrayList<Phrase>();
			cl.add(lp.get(i));
			first.add(cl);
		}
		ret.add(first);
		for (Analysis a : al) {
			boolean added = false;
			for (List<List<Phrase>> rll : ret) {
				if (a.addToCombineList(rll)) {
					added = true;
					break;
				}
			}
			if (!added) {
				List<List<Phrase>> qll = new ArrayList<List<Phrase>>();
				ret.add(qll);
				a.addToCombineList(qll);
			}
		}
		return ret;
	}

	/**
	 * Build a list of phrase starting positions. These are positions having to phrases longer than 1 tokens.
	 * Example: 0 2 3 4 5 7 8 9 10 11 14
	 * 
	 * @param rpmap
	 * @return
	 */
	static List<Integer> buildKeyList(Map<Integer, List<Phrase>> rpmap) {
		List<Integer> klist = new ArrayList<Integer>();
		for (Integer key : rpmap.keySet()) {
			if (!klist.contains(key)) {
				klist.add(key);
			}
		}
		Collections.sort(klist);
		return klist;
	}

	/**
	 * Generate a list of integers.
	 * list[i] gives the index on the input list of integer to pick the next token position.
	 * 
	 * if the input klist = [0, 2, 3, 4, 5, 7, 8, 9, 10, 11, 14]
	 * then return is: ret[ ] = [1, 1, 1, 2, 3, 4, 5, 5, 6, 7, 8, 9, 10, 10, 10]
	 * 
	 * Here ret[0]=1 means, at token index 0, the next token index that have a starting parsed phrase can be found
	 * on klist[1]=2.
	 * Here ret[6]=5 means, at token index 6, the next token index that have a starting parsed phrase can be found
	 * on klist[5]=7.
	 * Here ret[12]=10 means, at token index 12, the next token index that have a starting parsed phrase can be found
	 * on klist[10]=14.
	 * 
	 * @param klist
	 * @return
	 */
	static int[] endList(List<Integer> klist) {
		int max = klist.get(klist.size() - 1);
		int[] ret = new int[max + 1];
		int index = 1;
		int next = klist.get(index);
		for (int i = 0; i <= max; i++) {
			ret[i] = index;
			if (i >= next && i < max) {
				index++;
				next = klist.get(index);
			}
		}
		return ret;
	}

	/**
	 * Dynamic programming for selection TOP_N best sequences.
	 * 
	 * @param rpmap
	 * @param keylist
	 * @return
	 *         Analysis is a sequence of non-overlapping parsed segments (Phrase). The last phrase ends at or before a given position
	 *         List<Analysis> is all analysis of same score end at or before same position.
	 *         List<List<Analysis>> is all analysis of all scores end at or before a same position
	 *         List<List<List<Analysis>>> is all analysis of all scores ends at all positions
	 */
	static List<List<List<Analysis>>> findBest(Map<Integer, List<Phrase>> rpmap, List<Integer> keylist, int topN) {
		int[] nextlist = endList(keylist);

		List<List<List<Analysis>>> alist = new ArrayList<List<List<Analysis>>>();
		for (int i = 0; i < keylist.size(); i++) {
			alist.add(new ArrayList<List<Analysis>>());
		}
		for (int i = 0; i < keylist.size() - 1; i++) {// for all starting token indexes
			Integer pos = keylist.get(i);
			List<Phrase> pl = rpmap.get(pos);
			List<List<Analysis>> alli = alist.get(i);
			int j = i;
			while (alli.size() == 0 && j > 0) {
				j--;
				alli = alist.get(j);
			}
			for (Phrase p : pl) { // for all phrases starting at this index, long or short.
				int end = p.getEndToken();
				int next = nextlist[end];
				List<List<Analysis>> allp = alist.get(next);
				addBig(p, alli, allp, topN);
			}
		}
		return alist;
	}

	static List<List<List<Analysis>>> findBestJr(Map<Integer, List<Phrase>> rpmap, List<Integer> keylist, int[] nextlist, int topN) {

		List<List<List<Analysis>>> alist = new ArrayList<List<List<Analysis>>>();
		for (int i = 0; i < keylist.size(); i++) {
			alist.add(new ArrayList<List<Analysis>>());
		}
		for (int i = 0; i < keylist.size() - 1; i++) {// for all starting token indexes
			Integer pos = keylist.get(i);
			List<Phrase> pl = rpmap.get(pos);
			List<List<Analysis>> alli = alist.get(i);
			int j = i;
			while (alli.size() == 0 && j > 0) {
				j--;
				alli = alist.get(j);
			}
			for (Phrase p : pl) { // for all phrases starting at this index, long or short.
				int end = p.getEndToken();
				int next = nextlist[end];
				List<List<Analysis>> allp = alist.get(next);
				addBig(p, alli, allp, topN);
			}
		}
		return alist;
	}

	private static Integer[] findSegments(boolean[] brk, List<Integer> bks) {
		for (int i = 0; i < brk.length; i++) {
			if (brk[i]) {
				bks.add(i);
			}
		}
		return bks.toArray(new Integer[0]);
	}

	/**
	 * Dynamic programming for selection TOP_N best sequences.
	 * 
	 * @param rpmap
	 * @param keylist
	 * @return
	 *         Analysis is a sequence of non-overlapping parsed segments (Phrase). The last phrase ends at or before a given position
	 *         List<Analysis> is a list of no less than topN Analysis for a segment of a text. For tokens [beg, end],
	 *         with decreasing score. List[0] has the highest score, score[1] <= score[0].
	 *         List<List<Analysis>> is a list for all segments. Segments may have gaps between them.
	 */
	static List<List<Analysis>> findBestNew(Map<Integer, List<Phrase>> rpmap, List<Integer> keylist, int topN, List<Integer> segments) {
		// Step 1. Build a boolean array mark positions along the text token chain that there is no phrase span across it.
		// 		positions marked true are such "break points".
		boolean brk[] = null;
		for (List<Phrase> phlist : rpmap.values()) {
			if (brk == null) {
				Phrase ph = phlist.get(0);
				List<LexToken> tks = ph.getSentence();
				brk = new boolean[tks.size() + 1];
				for (int i = 0; i < brk.length; i++) {
					brk[i] = true;
				}
			}
			for (Phrase ph : phlist) {
				int beg = ph.getBegToken();
				int end = ph.getEndToken();
				for (int i = beg + 1; i < end; i++) {
					brk[i] = false;
				}
			}
		}
		// between each neighboring breaking points, find the list of Analysis

		int[] nextlist = endList(keylist);
		findSegments(brk, segments);

		List<List<Analysis>> lla = new ArrayList<List<Analysis>>();
		for (int i = 0; i < segments.size() - 1; i++) {
			List<Analysis> la = findSegmentBest(segments.get(i), segments.get(i + 1), rpmap, keylist, nextlist, topN);
			lla.add(la);
		}

		return lla;
	}

	static List<Analysis> findSegmentBest(int start, int stop, Map<Integer, List<Phrase>> rpmap, List<Integer> keylist, int[] nextlist, int topN) {
		List<List<List<Analysis>>> alist = new ArrayList<List<List<Analysis>>>();
		for (int i = 0; i < keylist.size(); i++) {
			int pos = keylist.get(i);
			if (pos < start) {
				continue;
			}
			alist.add(new ArrayList<List<Analysis>>());
			if (pos > stop) {
				break;
			}
		}
		List<Analysis> ret = new ArrayList<Analysis>();
		int first = 0;
		for (int i = 0; i < keylist.size() - 1; i++) {// for all starting token indexes
			Integer pos = keylist.get(i);
			if (pos < start) {
				continue;
			}
			if (pos == start) {
				first = i;
			}
			if (pos >= stop) {
				break;
			}
			List<Phrase> pl = rpmap.get(pos);
			List<List<Analysis>> alli = null;
			try {
				alli = alist.get(i - first);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			int j = i - first;
			while (alli.size() == 0 && j > 0) {
				j--;
				alli = alist.get(j);
			}
			for (Phrase p : pl) { // for all phrases starting at this index, long or short.
				int end = p.getEndToken();
				if (end > stop) {
					continue;
				}
				int next = nextlist[end];
				try {
					List<List<Analysis>> allp = alist.get(next - first);
					addBig(p, alli, allp, topN);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		// by now, alist contains our stuff. We'll get what we need into la:
		// the last one is the longest one:
		int k = alist.size() - 1;
		List<List<Analysis>> lla = alist.get(k);
		while (lla.size() == 0) {
			k--;
			if (k <= 0) {
				break;
			}
			lla = alist.get(k);
		}
		for (List<Analysis> la : lla) {
			ret.addAll(la);
			if (ret.size() >= topN) {
				break;
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param p
	 *            phrase begin at start position, end at end position
	 * @param alli
	 *            analysis at or before start position, that has one or more analysis
	 * @param allp
	 *            the first existing analysis at or beyond end position
	 */
	static void addBig(Phrase p, List<List<Analysis>> alli, List<List<Analysis>> allp, int topN) {
		if (alli.size() == 0) {
			List<Phrase> pl = new ArrayList<Phrase>();
			pl.add(p);
			Analysis an = new Analysis(pl);
			int score_1 = an.getScore();
			boolean added = false;
			// Compare with higher scores first (at lower index in List allp),
			// gradually move to lower scores (higher indexes)
			for (int pos = 0; pos < allp.size(); pos++) {
				List<Analysis> al = allp.get(pos);
				Analysis a = al.get(0);
				int score_2 = a.getScore();
				// If an existing Analysis at current position is lower than our current score,then
				// new Analysis is inserted at current position, the existing Analysis is moved up one index.
				if (score_2 < score_1) {
					List<Analysis> aln = new ArrayList<Analysis>();
					aln.add(an);
					allp.add(pos, aln);
					added = true;
					break;
				} else if (score_2 == score_1) {// equal, add new phrase to current list.
					al.add(an);
					added = true;
					break;
				}
			}
			if (!added) {
				if (allp.size() < topN) {
					List<Analysis> aln = new ArrayList<Analysis>();
					aln.add(an);
					allp.add(aln);
					added = true;
				}
			}
			if (allp.size() > topN) {
				allp.remove(topN);
			}
		} else {
			for (int k = 0; k < alli.size(); k++) {
				List<Analysis> aol = alli.get(k);
				for (Analysis ao : aol) {
					List<Phrase> pl = new ArrayList<Phrase>();
					pl.addAll(ao.getPhraseList());
					pl.add(p);
					Analysis an = new Analysis(pl);
					int score_1 = an.getScore();
					boolean added = false;
					for (int pos = 0; pos < allp.size(); pos++) {
						List<Analysis> al = allp.get(pos);
						Analysis a = al.get(0);
						int score_2 = a.getScore();
						if (score_2 < score_1) {
							List<Analysis> aln = new ArrayList<Analysis>();
							aln.add(an);
							allp.add(pos, aln);
							added = true;
							break;
						} else if (score_2 == score_1) {
							al.add(an);
							added = true;
							break;
						}
					}
					if (!added) {
						if (allp.size() < topN) {
							List<Analysis> aln = new ArrayList<Analysis>();
							aln.add(an);
							allp.add(aln);
							added = true;
						}
					}
					if (allp.size() > topN) {
						allp.remove(topN);
					}
				}
			}
		}
	}
}
