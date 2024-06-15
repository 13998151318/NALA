package pnal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import pnal.SubThingStore.SubPair;
import pnal.storage.FactStore;

/** This class is part of the PARIS ontology matching project at INRIA Saclay/France.
 * 
 * It is licensed under a Creative Commons Attribution Non-Commercial License
 * by the author Fabian M. Suchanek (http://suchanek.name). For all further information,
 * see http://webdam.inria.fr/paris
 *
 * This class stores pairs of entities with a score. It is used for the entity alignment.
 * The first entity will live in one fact store, and the second entity will live in another fact store.
 * If a TSV file is set, every assignment will be printed into that file (in addition to storing it in the data base).
 * This class keeps all alignments and enforces takeMaxMax later by a simple heuristic of maximum weighted bipartite matching.
 * This class is thread-safe and one instance is shared between all threads in findEqualsOf. */

public class EqualityStoreMultiple {
	
  /** only remember at most that many candidates per entity */
  public static int maxMatches = 5;
 
	FactStore fs1;
	FactStore fs2;
	protected SubPair<Integer> subIndex[][];

	@SuppressWarnings("unchecked")
	public EqualityStoreMultiple(FactStore fs1, FactStore fs2) {
		this.fs1 = fs1;
		this.fs2 = fs2;
		subIndex = new SubPair[fs1.numEntities() + fs1.numClasses() + 1][];
	}

	@SuppressWarnings("unchecked")
	public void set(int sub, Map<Integer, Double> equalities) {
		
	}
	
	/** Approximate weighted bipartite maximum matching to ensure that each entity is
	 * mapped to at most one other entity */
	public EqualityStore takeMaxMaxClever() throws IOException {
		EqualityStore eq = null;
		
		return eq;
	}
}
