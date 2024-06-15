package pnal;

import java.io.IOException;

import pnal.nal.TruthFunctions;
import pnal.nal.TruthValue;
import pnal.storage.FactStore;

/** This class is part of the PARIS ontology matching project at INRIA Saclay/France.
 * 
 * It is licensed under a Creative Commons Attribution Non-Commercial License
 * by the author Fabian M. Suchanek (http://suchanek.name). For all further information,
 * see http://webdam.inria.fr/paris
 *
 * This class is a SubThingStore<JoinRelation> with specialized methods to store the relation alignment
 */

public abstract class SubRelationStore extends SubThingStore<JoinRelation> {  
    public SubRelationStore(FactStore fs1, FactStore fs2) {
        super(fs1, fs2);
    }

    /** Load the alignments from the output of a mapping step */
    public void loadMapperOutput(MapperOutput output, boolean use_path_3) throws IOException {
        if (use_path_3){
            for (int i = 0; i < fs1.maxJoinRelationCode(); i++) {
                if (output.path3_r1_to_r2.get(i) == null)
                    continue;
                JoinRelation r1 = fs1.joinRelationByCode(i);
                for(int j:output.path3_r1_to_r2.get(i).keySet()){
                    setValueReverse(r1, fs2.joinRelationByCode(j), output.path3_r1_to_r2.get(i).get(j), false);
                }
            }
        }
        else{
            clear();
            for (int i = 0; i < fs1.maxJoinRelationCode(); i++) {
                if (output.neighborhoods[i] == null)
                    continue;
                JoinRelation r1 = fs1.joinRelationByCode(i);
                HashArrayNeighborhood n = output.neighborhoods[i];
                double normalizer = output.relationNormalizer.getNormalizer(r1);
                if (normalizer == 0) continue; // r1 has occurrences but no alignment candidates
                populate(r1, normalizer, new JoinRelation(fs2), n, false);
            }
        }
        
    }
    public void set_entityMatchers_label(){
        JoinRelation r1 = fs1.joinRelationByCode(fs1.getOrAddRelation("EntityMatchers:label"));
        JoinRelation r1r = fs1.joinRelationByCode(FactStore.inverse(fs1.getOrAddRelation("EntityMatchers:label")));
        JoinRelation r2 = fs2.joinRelationByCode(fs2.getOrAddRelation("EntityMatchers:label"));
        JoinRelation r2r = fs2.joinRelationByCode(FactStore.inverse(fs2.getOrAddRelation("EntityMatchers:label")));
        setTruthValueHighLevel(r1, r2, new TruthValue(1,1));
        setTruthValueHighLevel(r1r, r2r, new TruthValue(1,1));
    }  

    public String toTsv(SubPair<JoinRelation> p) {
        return p.sub.toString()+"\t"+p.supr.toString()+"\t"+p.truthValue.toString()+"\n";
    }
    public void setValueReverse(JoinRelation r1, JoinRelation r2, TruthValue val, boolean opposite) {
        // a possible reason for this assert to fail is when you have duplicate facts in your fact store
        assert(val.getFrequency() >= 0 && val.getFrequency() <= 1.01);
        if (!opposite) { 
            setTruthValueHighLevel(r1, r2, val);
        } else {
            //xch:此分支作用不明
            //double oval = getValueRaw(r1, r2.reversed());
            TruthValue oval = getTruthValue(r1, r2.reversed());
            setTruthValueHighLevel(r1, r2.reversed(), TruthFunctions.revision(val, oval));
        }
        //Announce.message("Setting " + r1 + " as subrel of " + r2 + " with score " + val);
    }
    
    protected void populate(JoinRelation r1, double normalizer, JoinRelation r2, HashArrayNeighborhood n, boolean opposite) {
        if (fs1.setting.interestingnessThreshold && !n.worthTrying())
            return;
        TruthValue val = TruthValue.truthValueFromEvidenceAmount(n.score, normalizer);
        if (r2.length() > 0) {
            setValueReverse(r1, r2, val, opposite);
        }
        for (Integer relation : n.children.keySet()) {
            JoinRelation nr2 = new JoinRelation(r2);
            nr2.push(relation);
            assert(nr2.length() == r2.length() + 1);
            populate(r1, normalizer, nr2, n.children.get(relation), opposite);
        }
    }

    public abstract TruthValue getTruthValueCode(int sub, int supr);
    public abstract void clear();
    
}
