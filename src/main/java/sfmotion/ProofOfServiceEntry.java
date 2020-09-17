package sfmotion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import legal.TrackEntry;

public class ProofOfServiceEntry extends TrackEntry {
	//	Line 18935: CA_SFC_470419	2017-11-28	POS OF DEC. OF RON NEWT FILED BY PLAINTIFF NEWT, RON
	//	Line 19020: CA_SFC_470422	2019-02-15	PROOF OF SERVICE (TRANSACTION ID # 100063332) FILED BY DEFENDANT ZAWAIDEH, MAHER ANIS ZAWAIDEH, BAHA
	static final String pos = "^(PROOF OF (\\w+\\s+)*SERVICE|POS)\\s";
	static final Pattern pPos = Pattern.compile(pos, Pattern.CASE_INSENSITIVE);

	public ProofOfServiceEntry(String _sdate, String _text) {
		super(_sdate, _text, PROOFOFSERVICE);
	}

	public static ProofOfServiceEntry parse(String _sdate, String _text) {
		Matcher m = pPos.matcher(_text);
		if (m.find()) {
			ProofOfServiceEntry entry = new ProofOfServiceEntry(_sdate, _text);
			return entry;
		}
		return null;
	}
}
