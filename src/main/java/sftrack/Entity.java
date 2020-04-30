package sftrack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Entity implements Cloneable {
	static final int TYPE_INSTANCE = 1;
	static final int TYPE_CLASS = 2;
	static final int TYPE_SET = 3; // multiple instances

	// To generate unique ID for generated entities.
	private static final AtomicInteger globalIDSource = new AtomicInteger(1);
	String name;
	int id; // Unique ID
	int position = -1; // token position on the entire sentence string. -1 means not initialized. 
	int entityType = TYPE_INSTANCE;
	Entity theClass; // if this is an instance, or an homogeneous set, then the
						// class of this instance. If this is a class, then null.
	Ontology onto; // the global unique ontology, saved for convenient access

	public void setPosition(int pos) {
		position = pos;
	}

	public int getPosition() {
		return position;
	}

	public static void resetSerial() {
		globalIDSource.set(1);
	}

	// private static ThreadLocal<Ontology> ontoRef = new
	// ThreadLocal<Ontology>();
	// public static void setOntology(Ontology onto) {
	// ontoRef.set(onto);
	// }

	public boolean isClass() {
		return (entityType == TYPE_CLASS);
	}

	public String pprint(String indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent + "(" + id + ":" + name + ":");
		switch (entityType) {
		case TYPE_INSTANCE:
			sb.append("INSTANCE");
			break;
		case TYPE_CLASS:
			sb.append("CLASS");
			break;
		case TYPE_SET:
			sb.append("SET");
			break;
		}
		if (theClass == null) {
			sb.append(":null)");
		} else {
			sb.append(":" + theClass.toString() + ")");
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "(" + id + ":" + name + ")";
	}

	public Entity() {
		id = globalIDSource.getAndIncrement();
	}

	/**
	 * Create an instance entity for a given class
	 * 
	 * @param clsName
	 */
	public Entity(String clsName, Ontology ot) {
		name = clsName;
		id = globalIDSource.getAndIncrement();
		if (!clsName.equals("EmptyClass")) {
			// Ontology onto = ontoRef.get();
			onto = ot;
			theClass = onto.getEntity(clsName);
		}
	}

	public Entity(String name, Entity cls, int type, Ontology ot, int pos) {
		this.name = name;
		id = globalIDSource.getAndIncrement();
		theClass = cls;
		entityType = type;
		onto = ot;
		position = pos;
	}

	public int getEntityType() {
		return entityType;
	}

	public void setEntityType(int type) {
		entityType = type;
	}

	public Entity getTheClass() {
		return theClass;
	}

	public void setTheClass(Entity e) {
		theClass = e;
	}

	public int getID() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String _n) {
		name = _n;
	}

	// this has to be pre-built. The following implementation is too wasteful,
	// when Ontology is big.
	public boolean isKindOf(Entity cls) {
		if (this.name.equals("EmptyClass")) {
			return false;
		}
		if (this.equals(cls)) {
			return true;
		}
		if (this.name != null && this.name.equals(cls.getName())) {
			return true;
		}
		if (theClass != null && theClass.equals(cls)) {
			return true;
		}
		if (theClass != null && cls.getTheClass() != null && theClass.equals(cls.getTheClass())) {
			return true;
		}
		if (onto.isKindOf(this.name, cls.getName())) {
			return true;
		}
		Entity clsEntity = cls.getTheClass();
		if (clsEntity != null) {
			String clsTheClassName = clsEntity.getName();
			if (!clsTheClassName.equals(cls.getName())) {
				if (onto.isKindOf(this.name, clsTheClassName)) {
					return true;
				}
			}
		}
		if (theClass != null && !theClass.getName().equals(this.name)) {
			if (onto.isKindOf(theClass.getName(), cls.getName())) {
				return true;
			}
			clsEntity = cls.getTheClass();
			if (clsEntity != null) {
				String clsTheClassName = clsEntity.getName();
				if (!clsTheClassName.equals(cls.getName())) {
					if (onto.isKindOf(theClass.getName(), clsTheClassName)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// this has to be pre-built. The following implementation is too wasteful,
	// when Ontology is big.
	public boolean isKindOf_Old(Entity cls) {
		if (this.equals(cls)) {
			return true;
		}
		if (this.name != null && this.name.equals(cls.getName())) {
			return true;
		}
		if (theClass != null && theClass.equals(cls)) {
			return true;
		}
		if (theClass != null && cls.getTheClass() != null && theClass.equals(cls.getTheClass())) {
			return true;
		}
		// Ontology onto = ontoRef.get();
		for (Link lk : onto.relations) {
			Entity e1 = lk.getArg1();
			if (this.equals(e1) || (theClass != null && theClass.equals(e1))) {
				String lkname = lk.getType();
				if (lkname.equals("instanceOf") || lkname.equals("subclassOf")) {
					Entity e2 = lk.getArg2();
					if (e2.isKindOf_Old(cls)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean isKindOf(String clsname) {
		if (this.name.equals("EmptyClass")) {
			return false;
		}
		// Ontology onto = ontoRef.get();
		if (onto.isKindOf(this.name, clsname)) {
			return true;
		}
		if (theClass != null && !theClass.getName().equals(this.name)) {
			if (onto.isKindOf(theClass.getName(), clsname)) {
				return true;
			}
		}
		return false;
	}

	public boolean isKindOf_Old(String clsname) {
		// Ontology onto = ontoRef.get();
		Entity cls = onto.getEntity(clsname);
		if (cls == null) {
			return false;
		}
		return isKindOf_Old(cls);
	}

	public boolean isInstanceOf(Entity cls) {
		if (cls.getEntityType() == Entity.TYPE_CLASS && this.getEntityType() == Entity.TYPE_INSTANCE) {
			if (theClass != null && theClass.equals(cls)) {
				return true;
			}
			// should trace parent class
		}
		return false;
	}

	public boolean isInstanceOf(String clsName) {
		// Ontology onto = ontoRef.get();
		Entity cls = onto.getEntity(clsName);
		if (cls == null) {
			return false;
		}
		if (cls.getEntityType() == Entity.TYPE_CLASS && this.getEntityType() == Entity.TYPE_INSTANCE) {
			if (theClass != null && theClass.equals(cls)) {
				return true;
			}
			// should trace parent class
		}
		return false;
	}

	public void reID() {
		id = globalIDSource.getAndIncrement();
	}

	public int distance(Entity e) {
		if (this.equals(e)) {
			return 0;
		}
		if (this.theClass != null && this.theClass.equals(e.theClass)) {
			return 1;
		}
		// Ontology onto = ontoRef.get();
		Entity ancestor = onto.getCommonAncestor(this.theClass, e.theClass);
		if (ancestor == null) {
			return -1;
		}
		int d1 = this.getClassDistance(ancestor);
		int d2 = e.getClassDistance(ancestor);
		if (d1 > d2) {
			return d1;
		}
		return d2;
	}

	public String formName() {
		String formed;
		// Ontology onto = ontoRef.get();
		if (this.theClass == null) {
			formed = this.getName();
		} else if (onto.isClass(this.theClass)) {
			formed = this.getName() + "_" + getID();
		} else {

			formed = this.getName();
		}
		return "\"" + formed + "\"";
	}

	/**
	 * 
	 * @param e
	 *            an ancestor class of e1
	 * @return
	 */
	public int getClassDistance(Entity e) {
		if (this.equals(e)) {
			return 0;
		}
		if (this.isInstanceOf(e)) {
			return 1;
		}
		// this is not finding shortest distance.
		// to find the shortest distance, instead of return the first find
		// distance, find all distances, return the shortest.
		// Ontology onto = ontoRef.get();
		for (Link lk : onto.relations) {
			Entity e1 = lk.getArg1();
			if (this.equals(e1) || (theClass != null && theClass.equals(e1))) {
				String lkname = lk.getType();
				if (lkname.equals("instanceOf") || lkname.equals("subclassOf")) {
					Entity e2 = lk.getArg2();
					if (e2.equals(e2)) {
						return 1;
					}
					int d = e2.getClassDistance(e);
					if (d >= 0) {
						return d + 1;
					}
				}
			}
		}
		return -1; // not connected
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass() != this.getClass()) {
			return false;
		}
		Entity e = (Entity) o;
		return id == e.getID();
	}

	public boolean equivalent(Entity e) {
		if (id == e.getID()) {
			return true;
		}
		if (name != null && name.equalsIgnoreCase(e.getName())) {
			return true;
		}
		return false;
	}

	/**
	 * After removing an entity of the name rem, what is left.
	 * 
	 * @param rem
	 * @return remaining entity.
	 */

	public Entity remove(String rem) {
		// specific to the abbreviation :
		if (name.equalsIgnoreCase(rem)) {
			return null;
		}
		if (isClass()) {
			return this;
		}
		// all links this entity is engaged in
		// Ontology onto = ontoRef.get();
		List<Link> lklist = onto.linkmap.get(theClass.getName());
		if (lklist == null) {
			return this;
		}
		List<Link> lknewlist = new ArrayList<Link>();
		boolean changes = false;
		for (Link lk : lklist) {
			String lkname = lk.getType();
			if (lkname.equals("instanceOf") || lkname.equals("subclassOf")) {
				Entity e2 = lk.getArg2();
				// if removing the parent of this entity:
				if (rem.equalsIgnoreCase(e2.getName())) {
					changes = true;
				} else {
					lknewlist.add(lk);
				}
			} else {
				lknewlist.add(lk);
			}
		}
		if (!changes) {
			return this;
		} else {
			if (lknewlist.size() == 1) {
				Link lk = lknewlist.get(0);
				Entity cl = lk.getArg2();
				Entity enew = new Entity(cl.getName(), cl, this.entityType, onto, cl.getPosition());
				return enew;
			} else {
				// create a new, temporary class entity as parent class:
				Entity tempClass = new Entity("tempClass" + this.id, null, TYPE_CLASS, onto, -1);
				Entity enew = new Entity(tempClass.getName(), tempClass, this.entityType, onto, this.position);
				onto.linkmap.put(tempClass.getName(), lknewlist);
				// Ontology.linkmap.put(theClass.getName(), lknewlist);
				return enew;
			}
		}
	}

	/**
	 * cloned entity has a different ID, so e != e.clone()
	 */
	@Override
	public Entity clone() {
		return new Entity(name, this.theClass, this.entityType, onto, this.position);
	}

	/**
	 * For tracking in Entity links. 
	 * Difference between Bond and Link:
	 * 	Link : Entity_1 linkType Entity_2
	 * 	Bond : (self) linkType Entity_2
	 * 	Bond is used inside Entity to keep track of Links.
	 * For Links that are only meaningful to one particular Entity, Bond is best.
	 * For example, DBA, AKA, FKA, ESA (erronously sued as), the other Entity have no general relevance.
	 * 
	 * @author yanyo
	 *
	 */
	static class Bond {
		String type;
		Entity other;

		public Bond(String _type, Entity _other) {
			type = _type;
			other = _other;
		}
	}
}
