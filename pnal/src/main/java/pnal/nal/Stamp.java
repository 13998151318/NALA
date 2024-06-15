package pnal.nal;

import javatools.administrative.D;
import java.beans.Transient;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.ListIterator;

//xch2.0
public class Stamp implements Cloneable, Serializable {

    /**
     * Whether the truth value is derived from a definition
     */
    private boolean derived = false;

    //xch2.0
    public LinkedList<BaseEntry> evidentialBase;

    static int max_evidence;

    public static class BaseEntry implements Serializable {

        //fact associated with the subj entity in the fs1 which participated in reasoning this piece of evidence
        public int fact_id_1;
        //fact associated with the pred entity in the fs2
        public int fact_id_2;
        public int x2;
        //this piece of evidence (SimilarityStatement)'s frequency confidence //expectation
        //public float frequency;
        //public double confidence;
        public TruthValue truthValue;
        public TruthValue x_truth;

        /** 1 for align-bridge 
         *  2 for entity name/description based embedding similarity */
        public int evidence_type;

        //SimilarityStatement subject entity(or literal)'s id in fs1
        //public int subj_id;
        //SimilarityStatement predicate entity's id in fs2
        //public int pred_id;

        public BaseEntry(TruthValue x_truth1, int fact_id_11, int fact_id_21, int x21, TruthValue truthValue1, int evidence_type1){
            fact_id_1 = fact_id_11;
            fact_id_2 = fact_id_21;
            x2 = x21;
            truthValue = new TruthValue(truthValue1);
            x_truth = new TruthValue(x_truth1);
            evidence_type = evidence_type1;
        }

        public BaseEntry(){

        }
    }

    public Stamp(final boolean derived) {
        setDerived(derived);
    }

    public static void set_max_evidence(int max_evidence1){
        max_evidence = max_evidence1;
    }

    public static int get_max_evidence(){
        return max_evidence;
    }

    //public BaseEntry get_BaseEntry(int id){
    //    return evidentialBase.;
    //}
    public ListIterator<BaseEntry> get_evidential_Iterator(){
        return evidentialBase.listIterator();
    }
    


    /**
     * @return is it a derived truth value?
     */
    public boolean getDerived() {
        return derived;
    }

    /**
     * Set it to derived truth
     */
    public void setDerived() {
        derived = true;
    }

    @Override
    public Stamp clone() {
        return new Stamp(getDerived());
    }
    
    public Stamp setDerived(final boolean a) {
        derived = a;
        return this;
    }

    public void add_evidence(TruthValue x_truth, int fact_id_11, int fact_id_21, int x2, TruthValue truthValue1, int evidence_type){
        if (evidentialBase == null){
            evidentialBase = new LinkedList<BaseEntry>();
        }
        ListIterator<BaseEntry> it = evidentialBase.listIterator();
        boolean inserted = false;
        while (it.hasNext()) {
            BaseEntry a = it.next();
            if(truthValue1.getExpectation() > a.truthValue.getExpectation()){  //xch2.1  getExpectation有待深化
                it.previous();
                it.add(new BaseEntry(x_truth, fact_id_11, fact_id_21, x2, truthValue1, evidence_type));
                inserted = true;
                break;
            }
        }
        if (!inserted && evidentialBase.size() < max_evidence){
            evidentialBase.addLast(new BaseEntry(x_truth, fact_id_11, fact_id_21, x2, truthValue1, evidence_type));
            return;
        }
        if(evidentialBase.size() > max_evidence){
            evidentialBase.removeLast();
        }
    }

}
