package pnal.nal;

import javatools.administrative.D;
import pnal.nal.Stamp.BaseEntry;
import pnal.storage.FactStore;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.ListIterator;


//xch2.0
public class AlignmentSentence implements Cloneable, Serializable {
    /**
     * The content of a Sentence is a Term
     */
    public final SimilarityStatement similarityStatement;
    
    /**
     * The punctuation indicates the type of the Sentence:
     * Judgment '.', Question '?', Goal '!', or Quest '@'
     */
    public final char punctuation = '.';
    
    /**
     * The truth value of Judgment, or desire value of Goal     
     */
    public TruthValue truthValue;

    public Stamp stamp;

    public AlignmentSentence(int subj, int pred, TruthValue truthValue1, Stamp stamp1){
        similarityStatement = new SimilarityStatement(subj, pred);
        truthValue = truthValue1;
        stamp = stamp1;
    }

    //for evidence_type 2
    public AlignmentSentence(int subj, int pred, TruthValue truthValue1, int evidence_type){
        similarityStatement = new SimilarityStatement(subj, pred);
        truthValue = truthValue1;
        stamp = new Stamp(true);
        stamp.add_evidence(new TruthValue(), -1, -1, -1, truthValue1, evidence_type);
    }

    public AlignmentSentence(TruthValue x_truth, int subj, int pred, TruthValue truthValue1, boolean derived, int fact_id_1, int fact_id_2, int x2, boolean record_stamp_evidence){
        similarityStatement = new SimilarityStatement(subj, pred);
        truthValue = truthValue1;
        stamp = new Stamp(derived);
        if (record_stamp_evidence)
            stamp.add_evidence(x_truth, fact_id_1, fact_id_2, x2, truthValue1, 1);
    }

    public void add_evidence(TruthValue truthValue1, int evidence_type, boolean use_c_as_probability_value){
        if (use_c_as_probability_value)
            truthValue = TruthFunctions.paris_revision(truthValue, truthValue1);
        else
            truthValue = TruthFunctions.revision(truthValue, truthValue1);
        stamp.add_evidence(new TruthValue(), -1, -1, -1, truthValue1, evidence_type);
        return;
    }
    
    public void probabilistic_add_evidence(TruthValue x_truth, int fact_id_11, int fact_id_21, TruthValue truthValue1, int x2, boolean record_stamp_evidence, boolean add_evidence_remove_duplicate, boolean is_attr_align_bridge, boolean small_run, boolean use_c_as_probability_value){
        int evidence_type = 1; //evidence from align-bridge
        if (!add_evidence_remove_duplicate || !is_attr_align_bridge || small_run || use_c_as_probability_value){ //
            if (use_c_as_probability_value)
                truthValue = TruthFunctions.paris_revision(truthValue, truthValue1);
            else
                truthValue = TruthFunctions.probabilistic_revision(truthValue, truthValue1);
            if (record_stamp_evidence)
                stamp.add_evidence(x_truth, fact_id_11, fact_id_21, x2, truthValue1, evidence_type);
            return;
        }
        if (add_evidence_remove_duplicate){
            if (stamp.evidentialBase == null){
                stamp.evidentialBase = new LinkedList<BaseEntry>();
                stamp.add_evidence(x_truth, fact_id_11, fact_id_21, x2, truthValue1, evidence_type);
                return;
            }
            // 0,0  0,-1 -1,0 0,1 1,0 -1,-1 1,1 1,-1 
            ListIterator<BaseEntry> it = stamp.evidentialBase.listIterator();
            int flag1 = 0;
            int flag2 = 0;
            BaseEntry a = new BaseEntry();
            BaseEntry b = new BaseEntry();
            while (it.hasNext()) {
                a = it.next();
                if(fact_id_11 == a.fact_id_1){
                    if(truthValue1.getExpectation() > a.truthValue.getExpectation()){
                        truthValue = TruthFunctions.probabilistic_de_revision(truthValue, a.truthValue); 
                        it.remove();
                        flag1 = 1;
                        break;
                    }
                    flag1 = -1;
                } 
            }
            it = stamp.evidentialBase.listIterator();
            while (it.hasNext()) {
                b = it.next();
                if(x2 == b.x2 && fact_id_21 == b.fact_id_2){
                    if(truthValue1.getExpectation() > b.truthValue.getExpectation()){
                        truthValue = TruthFunctions.probabilistic_de_revision(truthValue, b.truthValue);
                        it.remove();
                        flag2 = 1;
                        break;
                    }
                    flag2 = -1;
                }
            }
            if(flag1 == 1 && flag2 == -1){
                stamp.add_evidence(a.x_truth, a.fact_id_1, a.fact_id_2, a.x2, a.truthValue, evidence_type);
                truthValue = TruthFunctions.probabilistic_revision(truthValue, a.truthValue);
                return;
            }
            if(flag1 == -1 && flag2 == 1){
                stamp.add_evidence(b.x_truth, b.fact_id_1, b.fact_id_2, b.x2, b.truthValue, evidence_type);
                truthValue = TruthFunctions.probabilistic_revision(truthValue, b.truthValue);
                return;
            }
            if(flag1 + flag2 >= 0){
                stamp.add_evidence(x_truth, fact_id_11, fact_id_21, x2, truthValue1, evidence_type);
                truthValue = TruthFunctions.probabilistic_revision(truthValue, truthValue1);
                return;
            }
        }
    }

    @Override
    public String toString(){
        return similarityStatement.toString() + truthValue.toString();
    }

    public String display(FactStore fs1, FactStore fs2){
        String str="";
        str += fs1.entity(similarityStatement.subjTermIndex) + fs2.entity(similarityStatement.predTermIndex);
        str += " " + truthValue.toString();
        return str;
    }

    @Override
    public AlignmentSentence clone() {
        return new AlignmentSentence(similarityStatement.subjTermIndex, similarityStatement.predTermIndex, truthValue.clone(), stamp.clone());
    }

    public AlignmentSentence swap_clone() {
        if(stamp == null)
            return new AlignmentSentence(similarityStatement.predTermIndex, similarityStatement.subjTermIndex, truthValue.clone(), null);
        return new AlignmentSentence(similarityStatement.predTermIndex, similarityStatement.subjTermIndex, truthValue.clone(), stamp.clone());
    }


   
}
