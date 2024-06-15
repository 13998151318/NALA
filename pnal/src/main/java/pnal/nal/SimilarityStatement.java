package pnal.nal;

import javatools.administrative.D;
import java.io.Serializable;

//xch2.0
public class SimilarityStatement implements Cloneable, Serializable {
    //final int subjTermFactStore;
    public final int subjTermIndex;
    //final int predTermFactStore;
    public final int predTermIndex;

    public SimilarityStatement(final int subj, final int pred) {
        this.subjTermIndex = subj;
        this.predTermIndex = pred;
    }

    @Override
    public String toString(){
        return "" + subjTermIndex + " <-> " + predTermIndex + " . ";
    }

    @Override
    public SimilarityStatement clone() {
        return new SimilarityStatement(subjTermIndex, predTermIndex);
    }
}
