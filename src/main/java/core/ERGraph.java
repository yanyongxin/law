package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entity-Relation Graph.
 * 
 * @author yanyongxin
 * 
 */
public class ERGraph implements Cloneable {
	private static final Logger log = LoggerFactory.getLogger(ERGraph.class);
	List<Entity> entities;
	List<Link> links;
	Entity headEntity;
	Link topLink;
	// List<ERGraph> members = null;
	String setType = null; // AND, OR, RANGE (5 - 9, March through August), MIXED
	LegaLanguage onto = null; // saved here for convenient access
	static Pattern ptCaseNum = Pattern.compile("-(\\d{2,5})-", Pattern.CASE_INSENSITIVE);

	@Override
	public String toString() {
		return pprint("");
	}

	public void setEntityName(String _name) {
		if (headEntity != null) {
			headEntity.setName(_name);
		} else if (entities.size() > 1) {
			Entity e = entities.get(0);
			e.setName(_name);
		}
	}

	public List<Entity> getMembers(Entity e) {
		List<Link> lks = this.containLinkList("hasMember", e, null);
		if (lks != null) {
			List<Entity> le = new ArrayList<Entity>();
			for (Link lk : lks) {
				le.add(lk.getArg2());
			}
			return le;
		}
		return null;
	}

	public String pprint(String indent) {
		StringBuilder sb = new StringBuilder();
		for (Entity e : entities) {
			if (e.equals(headEntity)) {
				sb.append(e.pprint(indent) + " ==> Head\n");
			} else {
				sb.append(e.pprint(indent) + "\n");
			}
		}
		if (links != null) {
			for (Link lk : links) {
				if (lk.equals(topLink)) {
					sb.append(lk.pprint(indent) + " ==> topLink\n");
				} else {
					sb.append(lk.pprint(indent) + "\n");
				}
			}
		}
		return sb.toString();
	}

	/**
	 * determine if two ERGraphs are equivalent
	 * 
	 * @param g
	 * @return
	 */
	public boolean equivalent(ERGraph g) {
		List<Entity> ge = g.getEntities();
		if (entities.size() != ge.size()) {
			return false;
		}
		List<Link> lks = g.getLinks();
		if ((links == null && lks != null) || (links != null && lks == null)) {
			return false;
		}
		if (links != null && lks != null) {
			if (links.size() != lks.size()) {
				return false;
			}
		}
		// if(entities.size() > 6){
		// return match(g);
		// }
		List<Entity> le1 = new ArrayList<Entity>(entities);
		List<Entity> le2 = new ArrayList<Entity>(ge);
		for (Entity e : entities) {
			boolean rme = le2.remove(e);
			if (rme) {
				le1.remove(e);
			}
		}
		if (le1.size() != le2.size()) {
			return false;
		}
		List<List<Integer>> ll = getCorrespondenceCandidates(le1, le2);
		if (links == null && lks == null && le1.size() == 0) {
			return true;
		}
		List<Link> lk1 = new ArrayList<Link>();
		List<Link> lk2 = new ArrayList<Link>();
		if (links != null) {
			lk1.addAll(links);
			lk2.addAll(lks);
			for (Link lk : links) {
				boolean rmk = lk2.remove(lk);
				if (rmk) {
					lk1.remove(lk);
				}
			}
		}
		if (le1.size() == 0 && le2.size() == 0 && lk1.size() == 0 && lk2.size() == 0) {
			return true;
		}

		for (List<Integer> l : ll) {
			if (matchCorrespondence(l, le1, le2, lk1, lk2)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchCorrespondence(List<Integer> l, List<Entity> le1, List<Entity> le2, List<Link> lks1, List<Link> lks20) {
		List<Link> lks2 = new ArrayList<Link>(lks20);
		for (Link lk1 : lks1) {
			Entity e1 = lk1.getArg1();
			int idx1 = le1.indexOf(e1);
			Entity e2 = lk1.getArg2();
			int idx2 = le1.indexOf(e2);
			boolean found = false;
			for (int i = 0; i < lks2.size(); i++) {
				Link lk2 = lks2.get(i);
				if (lk2.getType().equals(lk1.getType())) {
					if (idx1 >= 0) {
						e1 = le2.get(idx1);
					}
					if (idx2 >= 0) {
						e2 = le2.get(idx2);
					}
					if (e1 == lk2.getArg1() && e2 == lk2.getArg2()) {
						lks2.remove(i);
						found = true;
						break;
					}
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @param le1
	 *            List of entities of ERGraph 1
	 * @param le2
	 *            List of entities of ERGraph 2
	 * @param l
	 * @param ll
	 *            All possible combinations of correspondence. like ( 1,2,3,4)
	 *            (1,3,4,2), ... If le1 have 10 entities of the same name, le2
	 *            have 10 entities of the same name, then there are 10! viable
	 *            combinations. This is a place the program can explode. As it
	 *            turns out to be true.
	 */
	private static void allCombinations(List<Entity> le1, List<Entity> le2, List<Integer> l, List<List<Integer>> ll) {
		if (le1.size() == 0) {
			return;
		}
		// compare every Entity from le1, with every Entity of le2
		Entity e1 = le1.get(l.size());
		for (int i = 0; i < le2.size(); i++) {
			// check if it is already used:
			if (l.contains(i)) {
				continue;
			}
			Entity e2 = le2.get(i);
			if (e2.equivalent(e1)) {
				//				if (e2.getName().equals(e1.getName())) {
				List<Integer> next = new ArrayList<Integer>(l);
				next.add(i);
				if (next.size() == le2.size()) {
					ll.add(next);
				} else {
					allCombinations(le1, le2, next, ll);
				}
			}
		}
	}

	public static List<List<Integer>> getCorrespondenceCandidates(List<Entity> le1, List<Entity> le2) {
		List<List<Integer>> ll = new ArrayList<List<Integer>>();

		allCombinations(le1, le2, new ArrayList<Integer>(), ll);
		return ll;
	}

	/**
	 * Involve multiple steps: (1) Identify the type, usually the head entity is
	 * enough (2) identify other attributes and relations. (3) there are AND, OR
	 * conditions // There also required and optional Entities and relations. //
	 * Some are valid only two relations are valid simultaneously
	 * 
	 * @param t
	 * @return
	 */
	public ERGraph extractTemplate(ERGraph t) {
		ERGraph tg = t.clone();
		ERGraph g = this.clone();

		if (t.headEntity != null) {
			if (this.headEntity == null) {
				return null;
			}

		}
		List<Link> tlink = tg.getLinks();
		List<Link> slink = g.getLinks();

		for (Link lk : tlink) {
			if (lk.getType().equals("mapTo")) {
				continue;
			}
			for (int i = 0; i < slink.size(); i++) {

			}
		}
		return null;
	}

	/**
	 * Generate a graphviz graph.
	 * 
	 * 
	 * @return
	 */
	public String toGraph() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph G {\n");
		if (links != null) {
			for (Link lk : links) {
				sb.append("\t" + lk.getArg1().formName() + "->" + lk.getArg2().formName() + "[label=\"" + lk.getType() + "\"]" + ";\n");
			}
		} else {
			for (Entity e : this.entities) {
				sb.append("\t" + e.formName() + ";\n");
			}
		}
		sb.append("}\n");
		return sb.toString();
	}

	public ERGraph() {
	}

	public ERGraph(Entity e) {
		addEntity(e);
		headEntity = e;
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass() != this.getClass()) {
			return false;
		}
		ERGraph g = (ERGraph) o;
		if (!headEntity.equivalent(g.getHead())) {
			return false;
		}
		return true;
	}

	// public void addmembers(List<ERGraph> memberlist) {
	// if (memberlist == null || memberlist.size() == 0) {
	// return;
	// }
	// if (members == null) {
	// members = new ArrayList<ERGraph>();
	// }
	// members.addAll(memberlist);
	// }
	//
	// public void addmembers(ERGraph g) {
	// if (g == null) {
	// return;
	// }
	// if (members == null) {
	// members = new ArrayList<ERGraph>();
	// }
	// members.add(g);
	// }

	/**
	 * Like clone(), but with classes instantiated to entities. For instantiate
	 * graph instances from graph templates
	 * 
	 * @return
	 */
	public ERGraph instantiate() {
		ERGraph eg = clone();
		eg.reInstantiate();
		return eg;
	}

	public void reInstantiate() {
		for (Entity e : entities) {
			if (e.getEntityType() == Entity.TYPE_CLASS) {
				e.setEntityType(Entity.TYPE_INSTANCE);
				e.setTheClass(e);
			}
		}
	}

	public void addEntity(Entity e) {
		if (entities == null) {
			entities = new ArrayList<Entity>();
		}
		if (!entities.contains(e)) {
			entities.add(e);
		}
		if (onto == null) {
			onto = e.onto;
		}
	}

	public void addLink(Link lk) {
		Entity e = lk.getArg1();
		addEntity(e);
		e = lk.getArg2();
		addEntity(e);
		if (links == null) {
			links = new ArrayList<Link>();
		}
		if (!links.contains(lk)) {
			links.add(lk);
		}
	}

	public List<Link> getLinks() {
		return links;
	}

	public List<Entity> getEntities() {
		return entities;
	}

	public List<Entity> findEntityByContainingName(String nm) {
		List<Entity> le = new ArrayList<Entity>();
		for (Entity e : entities) {
			if (e.name.toLowerCase().contains(nm.toLowerCase()) || nm.toLowerCase().contains(e.name.toLowerCase())) {
				le.add(e);
			}
		}
		if (le.size() > 0) {
			return le;
		}
		return null;
	}

	public int getEntityCount() {
		return entities.size();
	}

	public List<Entity> findCasenumbers(CaseNumber cn) {
		List<Entity> le = new ArrayList<Entity>();
		for (Entity e : entities) {
			if (cn.equals(e.getName())) {
				le.add(e);
			}
		}
		if (le.size() > 0) {
			return le;
		}
		return null;
	}

	public List<Entity> findEntityByExactName(String nm) {
		List<Entity> le = new ArrayList<Entity>();
		for (Entity e : entities) {
			if (e.name.equals(nm)) {
				le.add(e);
			}
		}
		if (le.size() > 0) {
			return le;
		}
		return null;
	}

	public List<Entity> findEntityByClass(String cls) {
		List<Entity> le = new ArrayList<Entity>();
		for (Entity e : entities) {
			if (e.isKindOf(cls)) {
				le.add(e);
			}
		}
		if (le.size() > 0) {
			return le;
		}
		return null;
	}

	public static boolean caseNumberCompatible(String c1, String c2) {
		Matcher m1 = ptCaseNum.matcher(c1);
		int ic1 = 1;
		if (m1.find()) {
			String g = m1.group(1);
			ic1 = Integer.parseInt(g);
		}
		Matcher m2 = ptCaseNum.matcher(c2);
		int ic2 = 2;
		if (m2.find()) {
			String g = m2.group(1);
			ic2 = Integer.parseInt(g);
		}
		return ic1 == ic2;
		//		return (c1.contains(c2.toLowerCase()) || c2.toLowerCase().contains(c1));
	}

	public void setHead(Entity e) {
		headEntity = e;
	}

	public Entity getHead() {
		return headEntity;
	}

	public LegaLanguage getOnto() {
		return onto;
	}

	public Link getTopLink() {
		return topLink;
	}

	public void setTopLink(Link lk) {
		topLink = lk;
	}

	public void transferLinks(String linkType, Entity efrom, Entity eto) {
		for (int i = 0; i < links.size(); i++) {
			Link lk = links.get(i);
			if (onto.isKindOf(lk.getType(), linkType)) {
				if (lk.getArg1() == efrom && lk.getArg2() != eto) {
					// if (Ontology.linkExists(lk.getType(), eto, lk.getArg2()))
					// {
					Link nlk = new Link(lk.getType(), eto, lk.getArg2());
					links.remove(i);
					links.add(i, nlk);
					// }
				} else if (lk.getArg2() == efrom && lk.getArg1() != eto) {
					// if (Ontology.linkExists(lk.getType(), lk.getArg1(), eto))
					// {
					Link nlk = new Link(lk.getType(), lk.getArg1(), eto);
					links.remove(i);
					links.add(i, nlk);
					// }
				}
			}
		}
	}

	public boolean topLinkMatch(ERGraph g) {
		Link lk = g.getTopLink();
		if (topLink == null && lk == null) {
			return true;
		}
		if (topLink == null && lk != null) {
			return false;
		}
		if (topLink != null && lk == null) {
			return false;
		}
		if (!topLink.getType().equals(lk.getType())) {
			return false;
		}
		if (topLink.getArg1() == headEntity && lk.getArg1() == g.getHead()) {
			return true;
		}
		if (topLink.getArg2() == headEntity && lk.getArg2() == g.getHead()) {
			return true;
		}

		return false;
	}

	/**
	 * Not only create a new graph, but also new entities and links. Use
	 * duplicate() if you do not wish to create new entities and links.
	 */
	@Override
	public ERGraph clone() {
		ERGraph eg = new ERGraph();
		for (Entity e : entities) {
			Entity ce = e.clone();
			eg.addEntity(ce);
			if (e.equals(headEntity)) {
				eg.setHead(ce);
			}
		}
		List<Entity> le = eg.getEntities();
		if (links != null) {
			for (Link lk : links) {
				Entity e1 = lk.getArg1();
				int idx = entities.indexOf(e1);
				try {
					e1 = le.get(idx);
				} catch (Exception eex) {
					eex.printStackTrace();
				}
				Entity e2 = lk.getArg2();
				idx = entities.indexOf(e2);
				e2 = le.get(idx);
				Link llk = new Link(lk.getType(), e1, e2);
				eg.addLink(llk);
				if (lk.equals(topLink)) {
					eg.setTopLink(llk);
				}
			}
		}
		return eg;
	}

	/**
	 * 
	 * @param lkname
	 * @param e1
	 * @param e2
	 * @return first Link that matches criteria
	 */
	public Link containLink(String lkname, Entity e1, Entity e2) {
		if (links == null) {
			return null;
		}
		for (Link lk : links) {
			if (lk.match(lkname, e1, e2)) {
				return lk;
			}
		}
		return null;
	}

	/**
	 * 
	 * @param lkname
	 * @param e1
	 * @param e2
	 * @return first Link that matches criteria
	 */
	public List<Link> containLinkList(String lkname, Entity e1, Entity e2) {
		if (links == null) {
			return null;
		}
		List<Link> lklist = new ArrayList<Link>();
		for (Link lk : links) {
			if (lk.match(lkname, e1, e2)) {
				lklist.add(lk);
			}
		}
		if (lklist.size() == 0) {
			return null;
		}
		return lklist;
	}

	/**
	 * 
	 * @param lkname
	 * @param e1
	 * @param e2
	 * @return first Link that matches criteria
	 */
	public List<Link> containLinkKindList(String lkname, Entity e1, Entity e2) {
		if (links == null) {
			return null;
		}
		List<Link> lklist = new ArrayList<Link>();
		for (Link lk : links) {
			if (lk.matchKind(onto, lkname, e1, e2)) {
				lklist.add(lk);
			}
		}
		if (lklist.size() == 0) {
			return null;
		}
		return lklist;
	}

	/**
	 * 
	 * @param lkname
	 * @param e1
	 * @param e2
	 * @return
	 */
	public List<Entity> getModifierList(Entity e1) {
		List<Entity> elist = new ArrayList<Entity>();
		List<Link> lks = containLinkList("hasAttribute", e1, null);
		if (lks != null) {
			for (Link lk : lks) {
				Entity e2 = lk.getArg2();
				elist.add(e2);
			}
		}
		lks = containLinkList("hasOwner", e1, null);
		if (lks != null) {
			for (Link lk : lks) {
				Entity e2 = lk.getArg2();
				elist.add(e2);
			}
		}
		// note the order change between e1 and e2:
		lks = containLinkList("define", null, e1);
		if (lks != null) {
			for (Link lk : lks) {
				Entity e2 = lk.getArg1();
				elist.add(e2);
			}
		}
		if (elist.size() > 0) {
			return elist;
		} else {
			return null;
		}
	}

	public Link containLink(String lkname, Object o1, Object o2) {
		if (links == null) {
			return null;
		}
		for (Link lk : links) {
			Entity e1 = null;
			Entity e2 = null;
			if (o1 != null) {
				if (o1.getClass().equals(Entity.class)) {
					e1 = (Entity) o1;
				} else if (o1.getClass().equals(String.class)) {
					String type1 = (String) o1;
					e1 = onto.getEntity(type1);
					if (e1 == null) { // should throw exception
						return null;
					}
				}
			}
			if (o2 != null) {
				if (o2.getClass().equals(Entity.class)) {
					e2 = (Entity) o2;
				} else if (o2.getClass().equals(String.class)) {
					String type2 = (String) o2;
					e2 = onto.getEntity(type2);
					if (e2 == null) { // should throw exception
						return null;
					}
				}
			}
			if (lk.match(lkname, e1, e2)) {
				return lk;
			}
		}
		return null;
	}

	public Link containLink(String lkname, String type1, String type2) {
		if (links == null) {
			return null;
		}
		for (Link lk : links) {
			if (!lk.type.equals(lkname)) {
				continue;
			}
			Entity e1 = null;
			if (type1 != null) {
				e1 = lk.getArg1();
				if (!e1.isKindOf(type1)) {
					continue;
				}
			}
			Entity e2 = null;
			if (type2 != null) {
				e2 = lk.getArg2();
				if (!e2.isKindOf(type2)) {
					continue;
				}
			}
			return lk;
		}
		return null;
	}

	public List<Link> containLinkList(String lkname, String type1, String type2) {
		if (links == null) {
			return null;
		}
		List<Link> ret = new ArrayList<Link>();
		for (Link lk : links) {
			if (!lk.type.equals(lkname)) {
				continue;
			}
			Entity e1 = null;
			if (type1 != null) {
				e1 = lk.getArg1();
				if (!e1.isKindOf(type1)) {
					continue;
				}
			}
			Entity e2 = null;
			if (type2 != null) {
				e2 = lk.getArg2();
				if (!e2.isKindOf(type2)) {
					continue;
				}
			}
			ret.add(lk);
		}
		if (ret.size() > 0) {
			return ret;
		}
		return null;
	}

	/**
	 * 
	 * @param lkname
	 * @param e1
	 * @param e2
	 * @return true if a Link exists that's not lkname, but matches e1 and e2
	 *         requirements.
	 */
	public boolean containLinkNot(String lkname, Entity e1, Entity e2) {
		if (links == null) {
			return false;
		}
		for (Link lk : links) {
			// if (lk.matchNot(lkname, e1, e2)) {
			// return true;
			// }
			if (!lk.match(lkname, e1, e2)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return true if given entity in involved in any relation other than given exceptions.
	 * These exceptions are given by Link type, and the position of given entity.
	 * Matches except Three links, given their name, and entity position.
	 * 
	 * Example:
	 * p1: Phrase(synType=="NP", !graph.containLinkExcept(head, "define", 2, "hasAttribute", 1, "hasMember", 1) )
	 * means:
	 * p1.head is not involved in links except
	 * Link("define", something, head)
	 * Link("hasAttribute", head, something)
	 * Link("hasMember", head, something)
	 * 
	 * @param e
	 *            Entity of concern
	 * @param lk1
	 *            String
	 * @param n1
	 *            1 or 2
	 * @param lk2
	 *            String
	 * @param n2
	 *            1 or 2
	 * @param lk3
	 *            String
	 * @param n3
	 *            1 or 2
	 * @return
	 */
	public boolean containLinkExcept(Entity e, String lk1, int n1, String lk2, int n2, String lk3, int n3) {
		if (links == null) {
			return false;
		}
		List<Integer> pos = new ArrayList<Integer>();
		List<String> lks = new ArrayList<String>();

		lks.add(lk1);
		pos.add(n1);
		if (lk2 != null) {
			lks.add(lk2);
			pos.add(n2);
			if (lk3 != null) {
				lks.add(lk3);
				pos.add(n3);
			}
		}
		for (Link lk : links) {
			if (lk.matchExcept(lks, pos, e)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Find if there are links other than the in the given list.
	 * This allows unlimited number of links to be excluded.
	 * 
	 * Example:
	 * p1: Phrase(synType=="NP", !graph.containLinkExcept(head, "define hasAttribute hasMember", "2 1 1") )
	 * means:
	 * p1.head is not involved in links except
	 * Link("define", something, head)
	 * Link("hasAttribute", head, something)
	 * Link("hasMember", head, something)
	 * 
	 * @param e
	 * @param linktypes
	 *            : list of link types to be excepted, separated by spaces
	 * @param positions
	 *            : list of positions for the above links, separated by spaces. They should match in number.
	 * @return
	 * @throws Exception
	 */
	public boolean containLinkExcept(Entity e, String linktypes, String positions) throws Exception {
		if (links == null) {
			return false;
		}
		List<Integer> pos = new ArrayList<Integer>();
		List<String> lks = new ArrayList<String>();

		String[] splitlk = linktypes.split("\\s+");
		String[] splitps = positions.split("\\s+");
		if (splitlk.length != splitps.length) {
			throw new Exception("Number of arguments not equal.");
		}
		for (int i = 0; i < splitlk.length; i++) {
			lks.add(splitlk[i]);
			Integer ps = Integer.parseInt(splitps[i]);
			pos.add(ps);
		}
		for (Link lk : links) {
			if (lk.matchExcept(lks, pos, e)) {
				return true;
			}
		}
		return false;
	}

	public boolean containRelatedLinkExcept(Entity e, String linktypes, String positions) throws Exception {
		if (links == null) {
			return false;
		}
		List<Integer> pos = new ArrayList<Integer>();
		List<String> lks = new ArrayList<String>();

		String[] splitlk = linktypes.split("\\s+");
		String[] splitps = positions.split("\\s+");
		if (splitlk.length != splitps.length) {
			throw new Exception("Number of arguments not equal.");
		}
		for (int i = 0; i < splitlk.length; i++) {
			lks.add(splitlk[i]);
			Integer ps = Integer.parseInt(splitps[i]);
			pos.add(ps);
		}
		for (Link lk : links) {
			if (lk.matchRelatedExcept(lks, pos, e)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Allow matching of ancestor Links.
	 * 
	 * @param lkname
	 * @param e1
	 * @param e2
	 * @return
	 */
	public boolean containLinkKind(String lkname, Entity e1, Entity e2) {
		if (links == null) {
			return false;
		}
		for (Link lk : links) {
			if (lk.matchKind(lkname, e1, e2)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsKindOf(Entity e, String cls) {
		if (e.isKindOf(cls)) {
			return true;
		}
		List<Link> lks = this.containLinkList("hasMember", e, null);
		if (lks != null) {
			for (Link lk : lks) {
				Entity e2 = lk.getArg2();
				if (e2.isKindOf(cls)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 
	 * @param pos:	the begin offset relative to the entire sentence string.
	 *  
	 * @return
	 */
	public ERGraph cloneInstance(int pos) {
		ERGraph eg = new ERGraph();
		for (Entity e : entities) {
			Entity ce = e.clone();
			if (ce.getEntityType() == Entity.TYPE_CLASS) {
				ce.setTheClass(e);
				ce.setEntityType(Entity.TYPE_INSTANCE);
				ce.setPosition(pos);
			}
			eg.addEntity(ce);
			if (e.equals(headEntity)) {
				eg.setHead(ce);
			}
		}
		List<Entity> le = eg.getEntities();
		if (links != null) {
			for (Link lk : links) {
				Entity e1 = lk.getArg1();
				int idx = entities.indexOf(e1);
				e1 = le.get(idx);
				Entity e2 = lk.getArg2();
				idx = entities.indexOf(e2);
				e2 = le.get(idx);
				Link llk = new Link(lk.getType(), e1, e2);
				eg.addLink(llk);
				if (lk.equals(topLink)) {
					eg.setTopLink(llk);
				}
			}
		}
		return eg;
	}

	/**
	 * create a new graph, with new lists of entities and links. Copy the entity
	 * and link references into the newly created lists, but do not create new
	 * entities or links.
	 * 
	 * If you want create new entities and links, use clone().
	 * 
	 * @return
	 */
	public ERGraph duplicate() {
		ERGraph eg = new ERGraph();
		for (Entity e : entities) {
			eg.addEntity(e);
		}
		if (links != null) {
			for (Link lk : links) {
				eg.addLink(lk);
			}
		}
		eg.setType = setType;
		eg.setHead(headEntity);
		eg.setTopLink(topLink);
		return eg;
	}

	public void merge(ERGraph eg) {
		List<Entity> le = eg.getEntities();
		for (Entity e : le) {
			addEntity(e);
		}
		List<Link> lk = eg.getLinks();
		if (lk != null) {
			for (Link l : lk) {
				addLink(l);
			}
		}
	}

	/**
	 * Both of following are true:
	 * 
	 * 1. one of the heads is a set
	 * 2. both heads merge into a set
	 * 
	 * @param eg
	 */
	public void mergeSet(ERGraph eg) {
		Entity hdRight = eg.getHead();
		Entity hdLeft = this.headEntity;
		Link lk;
		Entity clsLeft = hdLeft.getTheClass();
		Entity clsRight = hdRight.getTheClass();
		merge(eg);
		if (this.isSet()) {
			Entity thisClone = hdLeft.clone();
			this.addEntity(thisClone);
			// this is to protect hdleft, not to get inadverdently changed, because it is used in other places.
			replaceLinks(hdLeft, thisClone);
			this.remove(hdLeft);
			headEntity = thisClone;
			if (eg.isSet()) {
				replaceLinks(hdRight, thisClone);
				for (Entity c : hdRight.classes) {
					thisClone.addClass(c);
				}
				this.remove(hdRight);
			} else {
				lk = new Link("hasMember", thisClone, hdRight);
				thisClone.addClass(hdRight.theClass);
				this.addLink(lk);
			}
		} else {
			if (eg.isSet()) {
				Entity thatClone = hdRight.clone();
				this.addEntity(thatClone);
				replaceLinks(hdRight, thatClone);
				this.remove(hdRight);
				lk = new Link("hasMember", thatClone, hdLeft);
				this.addLink(lk);
				thatClone.addClass(hdLeft.theClass);
				this.headEntity = thatClone;
			}
			// the case both are not set is not considered here.
			// it should be considered in the calling function
		}
		if (clsLeft.isKindOf("LitigationParty") && clsRight.isKindOf("LitigationParty") && !clsLeft.equals(clsRight)) {
			this.getHead().setTheClass(onto.getEntity("LitigationParty"));
		}
	}

	public static ERGraph combine(ERGraph g1, ERGraph g2, Link lk, int headNew) {
		ERGraph eg = g1.duplicate();
		eg.merge(g2);
		Entity h1 = g1.getHead();
		Entity h2 = g2.getHead();
		if (headNew == 1) {
			eg.setHead(h1);
		} else if (headNew == 2) {
			eg.setHead(h2);
		}
		if (lk != null) {
			Link linkNew = lk.formLinkInstance(h1, h2);
			if (linkNew == null) {
				return null;
			}
			eg.addLink(linkNew);
			Link tlink = eg.getTopLink();
			if (tlink != null) {
				if ("ugoerOfProc".equals(tlink.getType())) {
					return eg;
				}
			}
			eg.setTopLink(linkNew);
		} else {
			if (headNew == 1) {
				eg.setTopLink(g1.topLink);
			} else if (headNew == 2) {
				eg.setTopLink(g2.topLink);
			}
		}
		return eg;
	}

	public void removeRelatedLinks(Entity e) {
		List<Link> lk = getLinks();
		if (lk != null) {
			for (int i = lk.size() - 1; i >= 0; i--) {
				Link l = lk.get(i);
				if (l.isArg(e)) {
					lk.remove(i);
				}
			}
		}
	}

	// Link is immutable. So old links are replaced with new links, not modified.
	// This is necessary because old links are used in other places,
	// modification will have unintended side effects.
	public void replaceLinks(Entity eold, Entity enew) {
		List<Link> lks = getLinks();
		if (lks != null) {
			for (int i = 0; i < lks.size(); i++) {
				Link l = lks.get(i);
				Link replacelink = l.replaceEntity(eold, enew);
				if (replacelink != null) {
					lks.remove(i);
					lks.add(i, replacelink);
				}
			}
		}
	}

	/**
	 * Removing one entity is not as simple as removing one entity from entity
	 * list. Because the specification may be given as a parent class. We need
	 * to: (1) removing all the links involving this entity (2) if this entity
	 * is one of the set entity, then there may be surviving member entities
	 * taking place of its old position. All links involving this entity should
	 * replace this entity with substitute entity.
	 * 
	 * @param rem
	 */
	public void remove(String rem) {
		List<Entity> le = getEntities();
		for (int i = le.size() - 1; i >= 0; i--) {
			Entity e = le.get(i);
			Entity enew = e.remove(rem);
			if (enew == null) {// removed
				le.remove(i);
				removeRelatedLinks(e); // remove all the links involving e as an
										// argument
			} else if (enew.equals(e)) {
				continue;
			} else {
				le.remove(i);
				le.add(i, enew);
				replaceLinks(e, enew);
				if (headEntity.equals(e)) {
					headEntity = enew;
				}
			}
		}
	}

	public void remove(Entity e) {
		List<Entity> le = getEntities();
		removeRelatedLinks(e); // remove all the links involving e as an
								// argument
		le.remove(e);
		if (headEntity == e) {
			headEntity = le.get(0);
		}
	}

	public boolean isSet() {
		return containLink("hasMember", headEntity, null) != null;
	}

	public boolean isSet(Entity e) {
		return containLink("hasMember", e, null) != null;
	}

	public String getSetType() {
		return setType;
	}

	public void setSetType(String s) {
		setType = s;
	}

	public List<Entity> getMotionList() {
		List<Entity> elist = new ArrayList<>();
		for (Link lk : links) {
			Entity e = lk.e1;
			if (e.isSet())
				continue;
			if (e.isKindOf("DocLegalMotion") || e.isKindOf("DocLegalDemurrer") || e.isKindOf("DocApplication")) {
				if (!elist.contains(e)) {
					// added in order of descending id:
					boolean b = false;
					for (int i = 0; i < elist.size(); i++) {
						Entity ee = elist.get(i);
						if (ee.id < e.id) {
							elist.add(i, e);
							b = true;
							break;
						}
					}
					if (!b) {
						elist.add(e);
					}
				}
			}
		}
		return elist;
	}

	public int entityDistance(Entity e) {
		// the distance for the headEntity. If no headEntity, then topLink,
		// if non if those, from largest entity number, because my current scheme id from right to left. 
		// Left most entity usually have largest id. Except for Party, Attorney, Judge, etc, they are id'ed first.
		return 0;
	}

	/**
	 * determine if two ERGraphs are equivalent
	 * 
	 * @param g
	 * @return
	 */
	public boolean match(ERGraph g) {
		List<Entity> ce1; // Entities in g1 that have unique correspondence in g2
		List<Entity> ce2; // Entities in g2 that have unique correspondence in g1
		// ce1[i] corresponds to ce2[i]

		List<Link> ck1; // Links in g1 that have unique correspondence in g2
		List<Link> ck2; // Links in g2 that have unique correspondence in g1
		// ck1[i] corresponds to ck2[i]

		List<Entity> me1; // Entities in g1 that have not established unique correspondence in g2
		List<Entity> me2; // Entities in g2 that have not established unique correspondence in g1

		List<Link> mk1; // Links in g1 that have not established unique correspondence in g2
		List<Link> mk2; // Links in g2 that have not established unique correspondence in g1

		Map<Entity, List<Link>> entityMatch11 = new HashMap<Entity, List<Link>>(); // Entity in g1 as arg1 of a link
		Map<Entity, List<Link>> entityMatch12 = new HashMap<Entity, List<Link>>(); // Entity in g1 as arg2 of a link
		Map<Entity, List<Link>> entityMatch21 = new HashMap<Entity, List<Link>>(); // Entity in g2 as arg1 of a link
		Map<Entity, List<Link>> entityMatch22 = new HashMap<Entity, List<Link>>(); // Entity in g2 as arg2 of a link

		return false;
	}

	public int getPrpCount() {
		List<Link> ll = this.containLinkKindList("PRPRelation", (Entity) null, null);
		if (ll == null) {
			return 0;
		}
		return ll.size();
	}
}
