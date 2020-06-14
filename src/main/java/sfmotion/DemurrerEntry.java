package sfmotion;

public class DemurrerEntry {
	Entry entry;

	DemurrerEntry(Entry e) {
		entry = e;
		entry.setType(Entry.DEMURRER);
		parse();
	}

	public boolean parse() {
		return true;
	}
}
