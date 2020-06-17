package track;

public class SimpleAnalysis {
	int score;
	int count;
	String text;

	public SimpleAnalysis(int sc, int cnt, String txt) {
		score = sc;
		count = cnt;
		text = txt;
	}

	public SimpleAnalysis(String s) {
		String[] split = s.split("\\t");
		score = Integer.parseInt(split[0]);
		count = Integer.parseInt(split[1]);
		text = split[2];
	}

	@Override
	public String toString() {
		return score + "\t" + count + "\t" + text;
	}

	public int diffScore(SimpleAnalysis an) {
		return score - an.score;
	}

	public int diffAmbiguity(SimpleAnalysis an) {
		return count - an.count;
	}

	public int getScore() {
		return score;
	}

	public int getAmbiguity() {
		return count;
	}
}
