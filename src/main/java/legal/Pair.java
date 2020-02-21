package legal;
/**
 * Combine two objects into one.
 * 
 * @author Yongxin
 *
 */
public class Pair implements Comparable<Pair> {
	public Object o1;
	public Object o2;

	public Pair(Object _o1, Object _o2) {
		o1 = _o1;
		o2 = _o2;
	}

	public boolean equals(Object o) {
		if (o instanceof Pair) {
			Pair p = (Pair) o;
			return p.o2.equals(o2);
		}
		return o.equals(o2);
	}

	@Override
	// descending order
	public int compareTo(Pair p) {
		if (p.o1 instanceof Integer) {
			Integer i1 = (Integer) p.o1;
			Integer i2 = (Integer) this.o1;
			return i1 - i2;
		}
		if (p.o1 instanceof Double) {
			Double i1 = (Double) p.o1;
			Double i2 = (Double) this.o1;
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
