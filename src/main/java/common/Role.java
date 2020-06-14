package common;

public class Role {
	public static final String NAME_PLAINTIFF = "PLAINTIFF";
	public static final String NAME_DEFENDANT = "DEFENDANT";
	public static final String NAME_CROSS_DEFENDANT = "CROSS DEFENDANT";
	public static final String NAME_CROSS_COMPLAINANT = "CROSS COMPLAINANT";
	public static final String NAME_PETITIONER = "PETITIONER";
	public static final String NAME_RESPONDENT = "RESPONDENT";
	public static final String NAME_INTERVENOR = "INTERVENOR";

	static final String[] roles = new String[] { NAME_PLAINTIFF, NAME_DEFENDANT, NAME_CROSS_DEFENDANT, NAME_CROSS_COMPLAINANT, NAME_PETITIONER, NAME_RESPONDENT, NAME_INTERVENOR };

	public static final Role PLAINTIFF = new Role("PLAINTIFF");
	public static final Role DEFENDANT = new Role("DEFENDANT");
	public static final Role CROSS_DEFENDANT = new Role("CROSS DEFENDANT");
	public static final Role CROSS_COMPLAINANT = new Role("CROSS COMPLAINANT");
	public static final Role PETITIONER = new Role("PETITIONER");
	public static final Role RESPONDENT = new Role("RESPONDENT");
	public static final Role INTERVENOR = new Role("INTERVENOR");

	String name;

	public Role(String _name) {
		for (String role : roles) {
			if (role.equals(_name)) {
				name = _name;
				return;
			}
		}
		throw new RuntimeException("Non-standard Role: " + _name);
	}

	public boolean equals(Object o) {
		if (o instanceof Role) {
			Role r = (Role) o;
			return nameEquals(name, r.name);
		}
		if (o instanceof String) {
			return nameEquals(name, (String) o);
		}
		return false;
	}

	private boolean nameEquals(String n1, String n2) {
		if (_nameEquals(n1, n2))
			return true;
		if (_nameEquals(n2, n1))
			return true;
		return false;
	}

	private boolean _nameEquals(String n1, String n2) {
		if (n1.equals(n2)) {
			return true;
		}
		if (n1.equals(NAME_PLAINTIFF) && n2.equals(NAME_CROSS_DEFENDANT))
			return true;
		if (n1.equals(NAME_DEFENDANT) && n2.equals(NAME_CROSS_COMPLAINANT))
			return true;
		return false;
	}

	public String toString() {
		return name;
	}
}
