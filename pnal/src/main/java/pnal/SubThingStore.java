package pnal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javatools.administrative.Announce;
import javatools.datatypes.Triple;
import pnal.nal.TruthValue;
import pnal.storage.FactStore;


/** This class is part of the PARIS ontology matching project at INRIA Saclay/France.
 *
 * It is licensed under a Creative Commons Attribution Non-Commercial License
 * by the author Fabian M. Suchanek (http://suchanek.name). For all further information,
 * see http://webdam.inria.fr/paris
 *
 * This class stores an alignment between relations or instances.
 * */



public abstract class SubThingStore<T> {

    /** Represents a pair of a sub-entity and a super-entity with a score */
    public static class SubPair<T> implements Comparable<SubPair<T>> {

        long id;

        /** Holds the first object*/
        public T sub;

        /** Holds the second object*/
        public T supr;

        /** Holds the score*/
        public TruthValue truthValue;

        /** Constructs a pair*/
        public SubPair(T s1, T s2, TruthValue truthValue2) {
            sub = s1;
            supr = s2;
            truthValue = truthValue2;
        }

        /** Returns this as a triple*/
        public Triple<T, T, TruthValue> toTriple() {
            return (new Triple<T, T, TruthValue>(sub, supr, truthValue));
        }

        @Override
        public String toString() {
            return sub + "/" + supr + "/" + truthValue.toString() + "/" + id;
        }

        @Override
        public int compareTo(SubPair<T> arg0) {
            double v = this.truthValue.getConfidence() - arg0.truthValue.getConfidence();
            return (v == 0. ? 0 : (v > 0. ? 1 : -1));
        }
        
        
    }

    /** The log writer */
    protected Writer tsvWriter;
    
    protected FactStore fs1;
    protected FactStore fs2;  
    
    public SubThingStore(FactStore fs1, FactStore fs2) {
        this.fs1 = fs1;
        this.fs2 = fs2;
    }
    
    /** Returns the probability that s1 is a sub-thing of s2*/
    public abstract TruthValue getTruthValue(T sub, T supr);
    
    /** Low-level set */
    protected abstract void setTruthValue(T sub, T supr, TruthValue val);
    
    /** Returns all sub-super-pairs */
    public abstract Iterable<SubPair<T>> all();
    
    /** Shut down*/
    public void close() {
        // store.close();
        if (tsvWriter != null) try {
            tsvWriter.close();
        } catch (IOException e) {
        }
    }
    
    /** Sets the writer
     * @throws IOException */
    public void setTSVfile(File file) throws IOException {
        if (tsvWriter != null) try {
            tsvWriter.close();
        } catch (IOException e) {
        }
        tsvWriter = new FileWriter(file);
    }
    
    public abstract String toTsv(SubPair<T> p);
        
    /** Sets the probability that s1 is a sub-thing of s2*/
    public void setTruthValueHighLevel(T sub, T supr, TruthValue truthValue) {
        //xch与paris-main不同之处20
        if (truthValue.getFrequency() < Config.THETA) return;
        setTruthValue(sub, supr, truthValue);
        if (tsvWriter != null) try {
            SubPair<T> p = new SubPair<T>(sub, supr, truthValue);
            tsvWriter.write(toTsv(p));
        } catch (IOException e1) {
            Announce.warning("TSV Writer failed", e1.getMessage());
            tsvWriter = null;
        }
    }

    /** Dump to a file */
    public void dump(File file) throws IOException {
        if (tsvWriter != null) try {
            tsvWriter.close();
        } catch (IOException e) {
        }
        Announce.doing("Saving TSV");
        BufferedWriter w = new BufferedWriter(new FileWriter(file));

        int num = 0;
        for (SubPair<T> pair : all()) {
            num++;
        }
        w.write("total num: " + num + "\n");
        num = 0;
        for (SubPair<T> pair : all()) {
            if (pair.truthValue.getConfidence() > 0) {
                num++;
                w.write(toTsv(pair));
                if(num>30000)
                    break;    			
            }
        }
        w.close();
        Announce.done();
    }

    /** Returns 10 sample pairs*/
    public List<Triple<T, T, TruthValue>> sample() {
        ArrayList<Triple<T, T, TruthValue>> result = new ArrayList<Triple<T, T, TruthValue>>();
        Iterable<SubPair<T>> pairs = all();
        int limit = 10;
        for (SubPair<T> pair : pairs) {
            result.add(pair.toTriple());
            limit--;
            if (limit <= 0)
                break;
        }
        return result;
    }

}