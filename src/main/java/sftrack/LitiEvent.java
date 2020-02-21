package sftrack;

import org.json.simple.JSONObject;

public class LitiEvent {
	// Litigation Event Template.
	static final String LE_TRIAL = "TRIAL";
	static final String LE_TERMINATE = "TERMINATE";
	static final String LE_STAY = "STAY";
	static final String LE_JUDGMENT = "JUDGMENT";
	static final String LE_ORDER_ON_MOTION = "ORDER_ON_MOTION";
	static final String LE_MARKMAN = "MARKMAN";
	static final String LE_STIPULATED_DISMISSAL = "STIPULATED_DISMISSAL";
	static final String LE_VOLUNTARY_DISMISSAL = "VOLUNTARY_DISMISSAL";
	static final String LE_JUDGE_ASSIGN = "JUDGE_ASSIGN";
	static final String LE_JUDGE_REFER = "JUDGE_REFER";
	static final String LE_PARTY_TERMINATE = "PARTY_TERMINATE";
	static final String LE_CASE_TRANSFER = "CASE_TRANSFER";
	static final String SB_TRIAL_BENCH = "bench";
	static final String SB_TRIAL_JURY = "jury";
	String type;
	String info;
	String date; // yyyy-mm-dd
	int serial;
	int index;

	// change mm/dd/yyyy to yyyy-mm-dd
	static String normalizeDate(String d) {
		String[] split = d.split("/|-");// handle other possibilities later.
		if (split.length != 3) {// don't know what to do with it.
			return d;
		}
		if (split[0].length() >= 4) {
			return d;
		} else if (split[2].length() == 4) {
			return split[2] + "-" + split[0] + "-" + split[1];
		} else if (split[2].length() == 2) {
			return "20" + split[2] + "-" + split[0] + "-" + split[1];
		}
		return d;
	}

	public LitiEvent(String type_, String info_, String date_, int serial_, int index_) {
		type = type_;
		info = info_;
		date = normalizeDate(date_);
		serial = serial_;
		index = index_;
	}

	public void setInfo(String info_) {
		info = info_;
	}

	public LitiEvent(JSONObject jo) {
		type = (String) jo.get("type");
		info = (String) jo.get("info");
		date = (String) jo.get("date");
		serial = Integer.parseInt((String) jo.get("serial"));
		index = Integer.parseInt((String) jo.get("index"));
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {
		JSONObject jo = new JSONObject();
		jo.put("date", date);
		jo.put("type", type);
		jo.put("info", info);
		jo.put("serial", Integer.toString(serial));
		jo.put("index", Integer.toString(index));
		return jo;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append(" ");
		if (info != null) {
			sb.append(info);
			sb.append(" ");
		}
		sb.append(date);
		sb.append("(");
		sb.append(serial);
		sb.append(",");
		sb.append(index);
		sb.append(")  ");
		return sb.toString();
	}
}
