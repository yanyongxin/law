package sftrack;

public class PhraseCounter {
	int phraseCount = 0;

	public int getPhraseCount() {
		return phraseCount;
	}

	public int incrementPhraseCount() {
		phraseCount++;
		return phraseCount;
	}

	public void resetPhraseCount() {
		phraseCount = 0;
	}
}
