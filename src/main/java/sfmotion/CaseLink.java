package sfmotion;

import legal.CaseEntity;

public class CaseLink {
	CaseEntity e1;
	CaseEntity e2;
	String link;// AGENT, OWNER, TRUSTEE

	public CaseLink(CaseEntity _e1, CaseEntity _e2, String _l) {
		if (_e1 == null || _e2 == null) {
			System.out.print(" debug ");
		}
		e1 = _e1;
		e2 = _e2;
		link = _l;
	}

	public boolean equals(Object o) {
		if (!(o instanceof CaseLink))
			return false;
		CaseLink lk = (CaseLink) o;
		return e1.equals(lk.e1) && e2.equals(lk.e2) && link.equalsIgnoreCase(lk.link);
	}

	public boolean sameArgs(CaseLink lk) {
		return e1.equals(lk.e1) && e2.equals(lk.e2);
	}

	public void relink() {
		e1 = e1.getLeaf();
		e2 = e2.getLeaf();
	}

	public String toString() {
		return link + " : {" + e1.name + "<==>" + e2.name + "}";
	}
}
