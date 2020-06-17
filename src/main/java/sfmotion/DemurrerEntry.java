package sfmotion;

public class DemurrerEntry {
	SFMotionEntry entry;

	DemurrerEntry(SFMotionEntry e) {
		entry = e;
		entry.setType(SFMotionEntry.DEMURRER);
		parse();
	}

	public boolean parse() {
		return true;
	}
}
