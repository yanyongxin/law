package sftrack;

import java.util.List;

/**
 * Link is immutable, once created, it cannot be changed. This makes it possible
 * for different graphs to refer to the same links. Otherwise, if one graph
 * changes it, all other graph get inadvertently changed.
 * 
 * Therefore there no such methods as setArg1(Entity e) etc.
 * 
 * @author yanyongxin
 * 
 */
public class Link implements Cloneable {
	static final String LINK_InstanceOf = "instanceOf"; // (domain: instance)
														// (range: class)
	static final String LINK_SubclassOf = "subclassOf"; // (domain: class)
														// (range: class)
	static final String LINK_MemberOf = "memberOf"; // (domain: entity) (range:
													// set)
	static final String LINK_ProcHasAgent = "procHasAgent"; // (domain:
															// procInstance)
															// (range: entity)
	static final String LINK_AgentOfProc = "agentOfProc"; // (domain: Entity)
															// (range:
															// ProcInstance)
	static final String LINK_ProcHasUgoer = "procHasUgoer"; // (domain:
															// procInstance)
															// (range: entity)
	static final String LINK_UgoerOfProc = "ugoerOfProc"; // (domain: Entity)
															// (range:
															// ProcInstance)
	static final String LINK_HasDomain = "hasDomain"; // (range: Domain)(domain:
														// Proc, Entity)
	static final String LINK_HasValue = "hasValue"; // (range: Domain)(domain:
													// Proc, Entity)
	static final String LINK_DomainOf = "domainOf"; // (domain: Domain)(range:
													// Proc, Entity)
	static final String LINK_FromOrigin = "fromOrigin"; // Prep: From
	static final String LINK_With = "with"; // Prep: with
	static final String LINK_Against = "against"; // prep: against
	static final String LINK_ByCreator = "byCreator"; // prep: by (creator)
	static final String LINK_ByNear = "byNear"; // prep: by (near)
	static final String LINK_InDomain = "in"; // prep: in
	static final String LINK_For = "for"; // prep: for. We separate various
											// meanings later.
	static final String LINK_ToProc = "toProc"; // prep: to (process
												// (infinitive)). Example: I
												// used a stick to get rid of
												// it.
	static final String LINK_ToTarget = "toTarget"; // prep: to (target).
													// Example: I went to New
													// York.
	static final String LINK_At = "At"; // prep: at
	static final String LINK_HasSetQuantifier = "hasSetQuantifier"; //
	static final String LINK_Define = "define"; // in the NP-NP combination,
												// first NP defines second NP
	static final String LINK_HasMeasure = "hasMeasure"; // five apples ==>
														// Set(apple)
														// link(LINK_HasMeasure)
														// (unit: count,
														// value=5)
	String type;
	Entity e1;
	Entity e2;

	public String pprint(String indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(indent + e1.toString() + " " + type + " " + e2.toString());
		return sb.toString();
	}

	@Override
	public String toString() {
		return pprint("");
	}

	public Link(String tp, Entity et1, Entity et2) {
		type = tp;
		e1 = et1;
		e2 = et2;
	}

	public boolean debug() {
		return true;
	}

	public boolean debug1() {
		return true;
	}

	public String getType() {
		return type;
	}

	public static Entity getClassEntities(String s, Ontology onto) throws Exception {
		Entity ent = onto.getEntity(s);
		if (ent == null) {
			throw new LexEntityNotFoundException(s);
			// ent = new Entity(s, null, Entity.TYPE_CLASS);
			// Ontology.namedEntities.put(s, ent);
		}
		return ent;
	}

	public Link(String n, String s1, String s2, Ontology onto) throws Exception {
		type = n;
		e1 = getClassEntities(s1, onto);
		e2 = getClassEntities(s2, onto);
	}

	public Entity getArg1() {
		return e1;
	}

	public Entity getArg2() {
		return e2;
	}

	public boolean isArgument(Entity e, int pos) {
		if (pos == 1) {
			return e.equals(e1);
		}
		if (pos == 2) {
			return e.equals(e2);
		}
		return false;
	}

	public boolean isArg1(Entity e) {
		return e.equals(e1);
	}

	public boolean isArg2(Entity e) {
		return e.equals(e2);
	}

	/**
	 * See if this link matches specification. If any argument is null, it means
	 * this argument does not matter. The method matchKind() allows matching any
	 * ancestors.
	 * 
	 * @param lkname
	 * @param e1
	 * @param e2
	 * @return
	 */
	public boolean match(String lkname, Entity d1, Entity d2) {
		if (lkname != null) {
			if (!type.equals(lkname)) {
				return false;
			}
		}
		if (d1 != null) {
			if (d1.theClass == null) {
				if (!e1.isKindOf(d1)) {
					return false;
				}
			} else if (!d1.equals(e1)) {
				return false;
			}
		}
		if (d2 != null) {
			if (d2.theClass == null) {
				if (!e2.isKindOf(d2)) {
					return false;
				}
			} else if (!d2.equals(e2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Match not lkname. lkname cannot be null. argument requirement just like
	 * match(). If any argument is null, it means this argument does not matter.
	 * 
	 * @param lkname
	 * @param d1
	 * @param d2
	 * @return true if this Link have a name different from lkname, but fulfill
	 *         d1 d2 requirements
	 */
	public boolean matchNot(String lkname, Entity d1, Entity d2) {
		if (lkname != null) {
			if (type.equals(lkname)) {
				return false;
			}
		}
		if (d1 != null) {
			if (!d1.equals(e1)) {
				return false;
			}
		}
		if (d2 != null) {
			if (!d2.equals(e2)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Match not lkname. lkname cannot be null. argument requirement just like
	 * match(). If any argument is null, it means this argument does not matter.
	 * 
	 * @param lkname
	 * @param d1
	 * @param d2
	 * @return true if this Link have a name different from lkname, but fulfill
	 *         d1 d2 requirements
	 */
	public boolean matchExcept(List<String> lks, List<Integer> pos, Entity d) {
		// if this matches any input link, return false, else, return true
		for (int i = 0; i < lks.size(); i++) {
			String lkname = lks.get(i);
			if (type.equals(lkname)) {
				int p = pos.get(i);
				if (p == 1) {
					if (d.equals(e1)) {
						return false;
					}
				} else if (p == 2) {
					if (d.equals(e2)) {
						return false;
					}
				} else {
					if (d.equals(e1) && d.equals(e2)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Similar to matchExcept(). Except one difference:
	 * If this link is not related to the given entity, it returns false.
	 * 
	 * @param lkname
	 * @param d1
	 * @param d2
	 * @return true if this Link have a name different from lkname, but fulfill
	 *         d1 d2 requirements
	 */
	public boolean matchRelatedExcept(List<String> lks, List<Integer> pos, Entity d) {
		if (!d.equals(e1) && (!d.equals(e2))) {
			return false;
		}
		// if this matches any input link, return false, else, return true
		for (int i = 0; i < lks.size(); i++) {
			String lkname = lks.get(i);
			if (type.equals(lkname)) {
				int p = pos.get(i);
				if (p == 1) {
					if (d.equals(e1)) {
						return false;
					}
				} else if (p == 2) {
					if (d.equals(e2)) {
						return false;
					}
				} else {
					if (d.equals(e1) && d.equals(e2)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * see if this isKindOf lkname Link. That is, if lkname is a parent Link,
	 * match also succeeds. The method match() require exact match.
	 * 
	 * @param lkname
	 * @param d1
	 * @param d2
	 * @return
	 */
	public boolean matchKind(String lkname, Entity d1, Entity d2) {
		Ontology onto;
		if (d1 != null) {
			onto = d1.onto;
		} else if (d2 != null) {
			onto = d2.onto;
		} else {
			return false;
		}
		if (lkname != null) {
			Entity cls = onto.getEntity(lkname);
			if (cls == null) {
				return false;
			}
			Entity clsthis = onto.getEntity(type);
			if (clsthis == null) {
				return false;
			}
			if (!clsthis.isKindOf(cls)) {
				return false;
			}
		}
		if (d1 != null) {
			if (d1.getEntityType() == Entity.TYPE_CLASS) {
				if (!e1.isKindOf(d1)) {
					return false;
				}
			} else {
				if (!d1.equals(e1)) {
					return false;
				}
			}
		}
		if (d2 != null) {
			if (d2.getEntityType() == Entity.TYPE_CLASS) {
				if (!e2.isKindOf(d2)) {
					return false;
				}
			} else {
				if (!d2.equals(e2)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * see if this isKindOf lkname Link. That is, if lkname is a parent Link,
	 * match also succeeds. The method match() require exact match.
	 * 
	 * @param lkname
	 * @param d1
	 * @param d2
	 * @return
	 */
	public boolean matchKind(Ontology onto, String lkname, Entity d1, Entity d2) {
		if (lkname != null) {
			if (!type.equals(lkname)) {
				Entity cls = onto.getEntity(lkname);
				if (cls == null) {
					return false;
				}
				Entity clsthis = onto.getEntity(type);
				if (clsthis == null) {
					return false;
				}
				if (!clsthis.isKindOf(cls)) {
					return false;
				}
			}
		}
		if (d1 != null) {
			if (d1.getEntityType() == Entity.TYPE_CLASS) {
				if (!e1.isKindOf(d1)) {
					return false;
				}
			} else {
				if (!d1.equals(e1)) {
					return false;
				}
			}
		}
		if (d2 != null) {
			if (d2.getEntityType() == Entity.TYPE_CLASS) {
				if (!e2.isKindOf(d2)) {
					return false;
				}
			} else {
				if (!d2.equals(e2)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean isArg(Entity e) {
		if (isArg1(e)) {
			return true;
		}
		if (isArg2(e)) {
			return true;
		}
		return false;
	}

	public Link replaceEntity(Entity eold, Entity enew) {
		// Link is immutable
		Entity ee1 = null;
		Entity ee2 = null;
		if (isArg1(eold)) {
			ee1 = enew;
			ee2 = e2;
		} else if (isArg2(eold)) {
			ee1 = e1;
			ee2 = enew;
		}
		if (ee1 != null) {
			Link cl = new Link(this.type, ee1, ee2);
			return cl;
		}
		return null;
	}

	@Override
	public Link clone() {
		Link cl = new Link(this.type, e1.clone(), e2.clone());
		return cl;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o.getClass() != this.getClass()) {
			return false;
		}
		Link r = (Link) o;
		boolean b1 = type.equals(r.getType());
		boolean b2 = e1.equals(r.getArg1());
		boolean b3 = e2.equals(r.getArg2());
		return (b1 && b2 && b3);
	}

	public Link formLinkInstance(Entity et1, Entity et2) {
		if (e1.getEntityType() != Entity.TYPE_CLASS && e2.getEntityType() != Entity.TYPE_CLASS) {
			return this;
		}
		if (et1.getEntityType() != Entity.TYPE_INSTANCE || et2.getEntityType() != Entity.TYPE_INSTANCE) {
			return null;
		}
		if (et1.isKindOf(e1) && et2.isKindOf(e2)) {
			return new Link(this.getType(), et1, et2);
		} else if (et2.isKindOf(e1) && et1.isKindOf(e2)) {
			return new Link(this.getType(), et2, et1);
		}
		return null;
	}
}
