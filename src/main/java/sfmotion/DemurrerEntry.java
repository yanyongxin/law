package sfmotion;

public class DemurrerEntry {
	TrackEntry entry;

	DemurrerEntry(TrackEntry e) {
		entry = e;
		entry.setType(TrackEntry.DEMURRER);
		parse();
	}

	public boolean parse() {
		return true;
	}
}
