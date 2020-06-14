package utils;

public class Triple implements Comparable<Triple> {
	public Object o1;
	public Object o2;
	public Object o3;

	public Triple(Object _o1, Object _o2, Object _o3) {
		o1 = _o1;
		o2 = _o2;
		o3 = _o3;
	}

	@Override
	// descending order
	public int compareTo(Triple p) {
		if (p.o3 instanceof Integer) {
			Integer i1 = (Integer) p.o3;
			Integer i2 = (Integer) this.o3;
			return i1 - i2;
		}
		if (p.o3 instanceof Double) {
			Double i1 = (Double) p.o3;
			Double i2 = (Double) this.o3;
			double d = i1 - i2;
			if (d > 0.0)
				return 1;
			if (d == 0.0)
				return 0;
			if (d < 0.0)
				return -1;
		}
		return 0;
	}
}