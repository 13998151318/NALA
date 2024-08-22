package nala;

import java.util.ArrayList;
import java.util.HashMap;

import javatools.administrative.Announce;
import javatools.administrative.D;
import javatools.datatypes.Pair;
import nala.nal.TruthFunctions;
import nala.nal.TruthValue;
import nala.storage.FactStore;

/** This class is part of the PARIS ontology matching project at INRIA Saclay/France.
 * 
 * It is licensed under a Creative Commons Attribution Non-Commercial License
 * by the author Fabian M. Suchanek (http://suchanek.name). For all further information,
 * see http://webdam.inria.fr/paris
 *
 * This class stores the result of one pass by one thread (i.e., the result of the map operation, hence the name).
 * It can be reduced with another instance of the same object to get the merged results.
 * Note that this does not store computed entity alignments: they are stored in an EqualityStore which
 * is shared between threads, because the concurrent writes are not a problem in this case. */


public class MapperOutput {
	/** Relation alignments */
  HashArrayNeighborhood[] neighborhoods;
  /** Relation normalizers */
  RelationNormalizer relationNormalizer;
  FactStore fs;

  HashMap<Integer, ArrayList<Pair<TruthValue, TruthValue>>> positive_evidence_of_path3_r1;
  HashMap<Integer, HashMap<Integer, TruthValue>> path3_r1_to_r2;
  
  public MapperOutput(MapperOutput other) {
  	this(other.fs);
  	reduceWith(other);
  }
  
  public MapperOutput(FactStore fs) {
  	this.fs = fs;
  	this.neighborhoods = new HashArrayNeighborhood[fs.maxJoinRelationCode()];
  	if (fs.setting.optimizeNoJoins && fs.getJoinLengthLimit() == 1)
  		this.relationNormalizer = new ArrayRelationNormalizer(fs);
  	else
  		this.relationNormalizer = new HashRelationNormalizer(fs);
    this.positive_evidence_of_path3_r1 = new HashMap<>();
    this.path3_r1_to_r2 = new HashMap<Integer, HashMap<Integer, TruthValue>>();
  }

  public void compute_path3(int r1, TruthValue negative_evidence_of_r1, double missing_triple_initial_confidence){
    ArrayList<Pair<TruthValue, TruthValue>> positive_evidence_of_path3_r1_r2;
    Pair<TruthValue, TruthValue> evidence;
    TruthValue evidence_of_r1_r2 = new TruthValue();
    TruthValue negative = new TruthValue(negative_evidence_of_r1);
    TruthValue tr[], temp; 
    TruthValue p[]; 
    tr = new TruthValue[25];
    p = new TruthValue[6];
    for (int r2:positive_evidence_of_path3_r1.keySet()){
        positive_evidence_of_path3_r1_r2 = positive_evidence_of_path3_r1.get(r2);
        negative = new TruthValue(negative_evidence_of_r1);
        //D.p("negative_evidence_of_r1:",negative_evidence_of_r1);
        for (int k=0; k < positive_evidence_of_path3_r1_r2.size(); k++){
            evidence = positive_evidence_of_path3_r1_r2.get(k);
            tr[1] = new TruthValue(1,1);    //(∗, x1, y1) → r1 (12)
            tr[2] = new TruthValue(evidence.first);    //x1 ↔ x2 (13)
            tr[3] = TruthFunctions.analogy(tr[1], tr[2]);    //(∗, x2, y1) → r1 (14)
            tr[4] = new TruthValue(evidence.second);    //y1 ↔ y2 (15)
            tr[5] = TruthFunctions.analogy(tr[3], tr[4]);    //(∗, x2, y2) → r1 (16)
            tr[6] = new TruthValue(0,missing_triple_initial_confidence);    //(∗, x2, y2) → r2 (17)
            p[1] = TruthFunctions.induction(tr[6], tr[5]); //r1 → r2 (18)  //caution (tr[6], tr[5])
            //D.p("negative p[1]:",p[1]);
            negative = TruthFunctions.de_revision(negative, p[1]);
            //D.p("negative:",negative);
        }
        //D.p("after de_revision negative:",negative);
        for (int k=0; k < positive_evidence_of_path3_r1_r2.size(); k++){
            evidence = positive_evidence_of_path3_r1_r2.get(k);
            tr[1] = new TruthValue(1,1);    //(∗, x1, y1) → r1 (12)
            tr[2] = new TruthValue(evidence.first);    //x1 ↔ x2 (13)
            tr[3] = TruthFunctions.analogy(tr[1], tr[2]);    //(∗, x2, y1) → r1 (14)
            tr[4] = new TruthValue(evidence.second);    //y1 ↔ y2 (15)
            tr[5] = TruthFunctions.analogy(tr[3], tr[4]);    //(∗, x2, y2) → r1 (16)
            tr[6] = new TruthValue(1,1);    //(∗, x2, y2) → r2 (17)
            p[1] = TruthFunctions.induction(tr[6], tr[5]); //r1 → r2 (18)  //caution (tr[6], tr[5])
            negative = TruthFunctions.revision(new TruthValue(negative), p[1]);
            //D.p("positive p[1]:",p[1]);
            //D.p("negative:",negative);
        }
        //D.p("after revision negative:",negative);
        if (path3_r1_to_r2.get(r1) == null) {
            path3_r1_to_r2.put(r1, new HashMap<Integer, TruthValue>());
            HashMap<Integer, TruthValue> r1_to = path3_r1_to_r2.get(r1);
            if (r1_to.get(r2) == null) {
                r1_to.put(r2, new TruthValue(negative));
            }
            else{
                r1_to.put(r2, TruthFunctions.revision(r1_to.get(r2), negative));
            }
        }
    }
    this.positive_evidence_of_path3_r1 = new HashMap<>();
  }
  
  /** Reduce this result with another result */
  public void reduceWith(MapperOutput mo) {
  	for (int i = 0; i < fs.maxJoinRelationCode(); i++) {
  		Neighborhood other = mo.neighborhoods[i];
  		if (other == null)
  			continue;
  		if (neighborhoods[i] == null) {
  			neighborhoods[i] = new HashArrayNeighborhood(other.fs, other.run, true, other.maxDepth);
  		}
			neighborhoods[i].reduceWith(mo.neighborhoods[i]);
  	}
  	relationNormalizer.reduceWith(mo.relationNormalizer);

    for(int i:mo.path3_r1_to_r2.keySet()){
        for(int j:mo.path3_r1_to_r2.get(i).keySet()){
            if (path3_r1_to_r2.get(i) == null) {
                path3_r1_to_r2.put(i, new HashMap<Integer, TruthValue>());
            }
            HashMap<Integer, TruthValue> r1_to = path3_r1_to_r2.get(i);
            if (r1_to.get(j) == null) {
                r1_to.put(j, new TruthValue());
            }
            r1_to.put(j, TruthFunctions.revision(r1_to.get(j), mo.path3_r1_to_r2.get(i).get(j)));
            
        }
    }
  }
  
  public void printNeighborhoodsForFactStore(FactStore fs) {
  	
  }
  
  public void scaleDown(int n) {
  	for (int i = 0; i < fs.maxJoinRelationCode(); i++) {
  		if (this.neighborhoods[i] == null)
  			continue;
  		this.neighborhoods[i].scaleDown(n);
  	}
  	relationNormalizer.scaleDown(n);
  }
  public void print(FactStore other) {
  	for (int i = 0; i < fs.maxJoinRelationCode(); i++) {
  		if (neighborhoods[i] == null) continue;
  		JoinRelation r = fs.joinRelationByCode(i);
  		Announce.message("== normalizer for", r, ": ", relationNormalizer.getNormalizer(r), "==");
  		Announce.message("== neighborhood for", r, "==");
  		neighborhoods[i].print(new JoinRelation(other));
  	}
  }
}
