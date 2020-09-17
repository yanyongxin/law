package legal;

import java.util.ArrayList;
import java.util.List;

public class CaseEntity implements Comparable<CaseEntity> {
	public String name;
	public String nameNormalized;
	List<String> roles = new ArrayList<>(); // defendant, plaintiff, etc.
	public String type; // INDIVIDUAL, ENTITY, UNKNOWN
	int min = 1; // minimum number of entities
	int count;
	int entityCount = 1;
	String surname;
	String givenname;
	String midname;
	public List<CaseEntity> children; // when one entity line consists of multiple entities.

	public CaseEntity(String _name, String _role, String _type, int _count) {
		if (_role != null)
			roles.add(_role);
		init(_name, roles, _type, _count, 1);
	}

	public CaseEntity getLeaf() {
		if (children != null) {
			return children.get(0).getLeaf();
		} else {
			return this;
		}
	}

	public CaseEntity(String _name, String _role, String _type, int _count, int _entityCount) {
		if (_role != null)
			roles.add(_role);
		init(_name, roles, _type, _count, _entityCount);
	}

	public CaseEntity(String _name, List<String> _roles, String _type, int _count) {
		init(_name, _roles, _type, _count, 1);
	}

	void init(String _name, List<String> _roles, String _type, int _count, int _entityCount) {
		name = _name.replaceAll("\\,+", ",");
		if (_roles != null && _roles != roles) {
			roles.addAll(_roles);
		}
		type = _type;
		count = _count;
		entityCount = _entityCount;
		nameNormalized = getNormalizedName(name);
		if (nameNormalized.startsWith(" ")) {
			System.out.print(" debug ");
		}
		parseName();
	}

	static String getNormalizedName(String _s) {
		return _s.replaceAll("\\p{Punct}", " ").replaceAll("\\s+", " ").trim();
	}

	private void parseName() {
		if (surname != null)
			return;
		if (type.equalsIgnoreCase("INDIVIDUAL")) {
			if (parseIndividualName())
				return;
		}
		if (type.equalsIgnoreCase("ENTITY")) {
			return;
		}
		if (parseIndividualName()) {
			type = "INDIVIDUAL";
		}
	}

	void reparseName() {
		surname = null;
		givenname = null;
		midname = null;
		parseName();
	}

	boolean parseIndividualName() {
		int idx = name.indexOf(',');
		if (idx >= 0) {
			String n1 = name.substring(0, idx).trim();
			String[] n1split = n1.split("\\s+");
			if (n1split.length > 1) {
				return false;
			}
			surname = n1;
			String rest = name.substring(idx + 1).trim();
			String[] split = rest.split("\\s+");
			givenname = split[0].trim();
			if (split.length > 1) {
				midname = split[1].replaceAll("\\W+", "");
			}
			return true;
		} else {
			String[] split = this.nameNormalized.split("\\s+");
			if (split.length == 3 && split[1].length() == 1) {
				surname = split[2];
				midname = split[1];
				givenname = split[0];
				return true;
			}
		}
		return false;
	}

	CaseEntity decomposeHead(CaseEntity e) {
		if (nameNormalized.startsWith(e.nameNormalized)) {
			String left = nameNormalized.substring(e.nameNormalized.length());
			if (left.length() > 4) {
				if (Character.isLetterOrDigit(left.charAt(0))) {// in the middle of a word
					return null;
				}
				left = left.trim();
				String[] tks = e.nameNormalized.split("\\s+");
				String lastTk = tks[tks.length - 1];
				int idx = name.indexOf(lastTk);
				int start = idx + lastTk.length() + 1;
				String name2 = name.substring(start);
				CaseEntity e2 = new CaseEntity(name2, this.roles, this.type, this.count);
				return e2;
			}
		}
		return null;
	}

	void addRoles(CaseEntity e) {
		for (String r : e.roles) {
			if (!roles.contains(r)) {
				roles.add(r);
			}
		}
	}

	void setMin(int _min) {
		min = _min;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(nameNormalized + "\n");
		if (surname != null) {
			sb.append("\t(" + surname + ", " + givenname);
			if (midname != null) {
				sb.append(" " + midname);
			}
			sb.append(")\n");
		}
		if (roles.size() > 0) {
			sb.append("\t\tRole: ");
			for (String r : roles) {
				sb.append(r + ", ");
			}
			sb.append("\n");
		}
		sb.append("\t\tType: " + type + "\n");
		sb.append("\t\tCount: " + count + "\n");
		if (entityCount > 1) {
			sb.append("\t\tentityCount: " + entityCount + "\n");
		}
		return sb.toString();
	}

	@Override
	public int compareTo(CaseEntity o) {
		return nameNormalized.compareToIgnoreCase(o.nameNormalized);
	}

	public boolean equals(Object o) {
		if (!(o instanceof CaseEntity)) {
			return false;
		}
		CaseEntity e = (CaseEntity) o;
		if (surname != null && e.surname != null) {
			if (!surname.equalsIgnoreCase(e.surname)) {
				return false;
			}
			if (!givenname.equalsIgnoreCase(e.givenname)) {
				return false;
			}
			if (midname != null && e.midname != null) {
				if (midname.length() == 1 || e.midname.length() == 1) {
					if (midname.charAt(0) != e.midname.charAt(0)) {
						return false;
					}
				} else {
					if (!midname.equalsIgnoreCase(e.midname)) {
						return false;
					}
				}
			}
			return true;
		}
		return nameNormalized.equalsIgnoreCase(e.nameNormalized);
	}

	public void combine(CaseEntity e) {
		addRoles(e);
		parseName();
		count += e.count;
	}
}
