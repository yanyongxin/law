package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjectionEntry extends Entry {
	static final String objection = "^OBJECTION";
	static final Pattern pobjection = Pattern.compile(objection, Pattern.CASE_INSENSITIVE);

	public ObjectionEntry(String _sdate, String _text) {
		super(_sdate, _text, OBJECTION);
	}

	public static ObjectionEntry parse(String _sdate, String _text) {
		Matcher m = pobjection.matcher(_text);
		if (m.find()) {
			ObjectionEntry entry = new ObjectionEntry(_sdate, _text);
			return entry;
		}
		return null;
	}
}
