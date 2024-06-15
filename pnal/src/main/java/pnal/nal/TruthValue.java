package pnal.nal;

import javatools.administrative.D;
import pnal.Setting;

import java.beans.Transient;
import java.io.Serializable;

//xch2.0
public class TruthValue implements Cloneable, Serializable {
    /**
     * character that marks the two ends of a truth value
     */
    private static final char DELIMITER = '%';
    /**
     * character that separates the factors in a truth value
     */
    private static final char SEPARATOR = ';';
    /**
     * frequency factor of the truth value
     */
    private float frequency;
    /**
     * confidence factor of the truth value
     */
    private double confidence;

    /** determines the internal precision used for TruthValue calculations.
     *  a value of 0.01 gives 100 truth value states between 0 and 1.0.
     *  other values may be used, for example, 0.02 for 50, 0.10 for 10, etc.
     *  Change at your own risk, but can't be changed at runtime
     */
    public static float TRUTH_EPSILON;

    /* ---------- logical parameters ---------- */
    /** Evidential Horizon, the amount of future evidence to be considered.
     * Must be &gt;=1.0, usually 1 .. 2, not changeable at runtime as evidence measurement would change
     */
    public static float HORIZON = 1;

    public TruthValue() {
        this(0,0);
    }

    public static void set_TRUTH_EPSILON(float TRUTH_EPSILON1) {
        TRUTH_EPSILON = TRUTH_EPSILON1;
    }

    /**
     * Constructor
     *
     * @param f frequency value
     * @param c confidence value
     * @param isAnalytic is the truth value an analytic one?
     * @param TRUTH_EPSILON parameters of the reasoner
     */
    public TruthValue(final float f, final double c) {
        setFrequency(f);                
        setConfidence(c);      
    }

    public TruthValue(final double f, final double c) {
        setFrequency((float)f);                
        setConfidence(c);      
    }

    /**
     * Constructor with a TruthValue to clone
     *
     * @param v truth value to be cloned
     */
    public TruthValue(final TruthValue v) {
        frequency = v.getFrequency();
        confidence = v.getConfidence();
    }

    public static TruthValue truthValueFromEvidenceAmount(double w_positive, double w) {
        return new TruthValue((float)(w_positive / w), (double)(w / (w + HORIZON)));
    }

    public static TruthValue truthValue_of_attribute_literal(double sim, double attribute_value_emb_sim_confidence) {
        /**
        if (sim<0.8){
            return new TruthValue((float)(sim), sim-0.1);
        }
        else if(sim<0.85){
            return new TruthValue((float)(sim), sim-0.05);
        }
        else{
            return new TruthValue((float)(sim), sim);
        }
        */
        return new TruthValue((float)(sim), sim);
    }

    public static TruthValue truthValue_of_entity_embedding(double sim, double entity_emb_sim_confidence, boolean best, boolean use_c_as_probability_value) {
        if (use_c_as_probability_value)
            return new TruthValue(1, sim);
        else
            return new TruthValue(sim, entity_emb_sim_confidence);
        //return new TruthValue((float)(sim), sim);
        //if(!best)
        //    return new TruthValue(sim, entity_emb_sim_confidence-0.15);
    }

    /**
     * returns the frequency value
     *
     * @return frequency value
     */
    public float getFrequency() {
        return frequency;
    }

    /**
     * returns the confidence value
     *
     * @return confidence value
     */
    public double getConfidence() {
        return confidence;
    }

    public TruthValue setFrequency(final float f) {
        if(f < 0){
            this.frequency = 0;
            return this;
        }
        if(f > 1){
            this.frequency = 1;
            return this;
        }
        this.frequency = f;
        return this;
    }

    public TruthValue setFrequency(final double f) {
        if(f < 0){
            this.frequency = 0;
            return this;
        }
        if(f > 1){
            this.frequency = 1;
            return this;
        }
        this.frequency = (float)f;
        return this;
    }
    
    public TruthValue setConfidence(final double c) {
        if(c < 0){
            this.confidence = 0;
            return this;
        }
        if(c > 1){
            this.confidence = 1;
            return this;
        }
        //double max_confidence = 1.0 - TRUTH_EPSILON;
        //this.confidence = (c < max_confidence) ? c : max_confidence;
        this.confidence = c;
        return this;
    }

    public TruthValue swapClone() {
        return new TruthValue(this.confidence, this.frequency);
    }

    public TruthValue increseFrequency() {
        setFrequency(1 - (1 - this.frequency) * (1 - this.frequency));
        return this;
    }

    public TruthValue increse_frequency_linearly(Setting setting, boolean attributive) {
        double percent = 0;
        if (setting.use_c_as_probability_value){
            if (getConfidence()>=0.25){
                if(attributive)
                    percent = setting.increse_attribute_frequency;
                else
                    percent = setting.increse_relation_frequency;
            }
            else if (getConfidence()>=0.2){
                if(attributive)
                    percent = setting.increse_attribute_frequency/2;
                else
                    percent = setting.increse_relation_frequency/2;
            }
        }
        else{
            if (getConfidence()>=0.8){
                if (getFrequency()>=0.25){
                    if(attributive)
                        percent = setting.increse_attribute_frequency;
                    else
                        percent = setting.increse_relation_frequency;
                }
                else if (getFrequency()>=0.2){
                    if(attributive)
                        percent = setting.increse_attribute_frequency/2;
                    else
                        percent = setting.increse_relation_frequency/2;
                }
            }
        }
        if (setting.use_c_as_probability_value)
            setConfidence(this.confidence + (1 - this.confidence) * (percent));
        else
            setFrequency(this.frequency + (1 - this.frequency) * (percent));
        return this;
    }

    public TruthValue increseConfidencelinear() {
        setConfidence(this.confidence + (1 - this.confidence) * (1f/2f));
        return this;
    }

    public TruthValue divideFrequency(double div){
        setFrequency(this.frequency / (float)div);
        return this;
    }

    public TruthValue divideConfidence(double div){
        setConfidence(this.confidence / div);
        return this;
    }

    public TruthValue increse() {
        setFrequency(1 - (1 - this.frequency) * (1 - this.frequency));
        setConfidence(1 - (1 - this.confidence) * (1 - this.confidence));
        return this;
    }
    
    public TruthValue mulConfidence(final float mul) {
        final double max_confidence = 1.0 - this.TRUTH_EPSILON;
        final double c = this.confidence * mul;
        this.confidence = (c < max_confidence) ? c : max_confidence;
        return this;
    }

    /**
     * Calculate the expectation value of the truth value
     *
     * @return expectation value
     */
    public double getExpectation_0() {
        return (confidence * (frequency - 0.5f) + 0.5f);
    }

    public double getExpectation() {
        return (confidence * (frequency));
    }

    /**
     * Calculate the absolute difference of the expectation value and that of a
     * given truth value
     *
     * @param t given value
     * @return absolute difference
     */
    public double getExpDifAbs(final TruthValue t) {
        return Math.abs(getExpectation() - t.getExpectation());
    }

    /**
     * Check if the truth value is negative
     *
     * @return True if the frequency is less than 1/2
     */
    public boolean isNegative() {
        return getFrequency() < 0.5;
    }

    public static boolean isEqual(final double a, final double b, final double epsilon) {
        final double d = Math.abs(a - b);
        return (d < epsilon);
    }
    
    /**
     * Compare two truth values
     *
     * @param that other TruthValue
     * @return Whether the two are equivalent
     */
    @Override
    public boolean equals(final Object that) { 
        if (that instanceof TruthValue) {
            final TruthValue t = (TruthValue)that;
            return
                isEqual(getFrequency(), t.getFrequency(), this.TRUTH_EPSILON) &&
                isEqual(getConfidence(), t.getConfidence(), this.TRUTH_EPSILON);
        }
        return false;
    }

    /**
     * The hash code of a TruthValue
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return ((int)(0xFFFF * this.frequency) << 16) | (int)(0xFFFF * this.confidence);
    }

    @Override
    public TruthValue clone() {
        return new TruthValue(frequency, confidence);
    }

    public static long hundredths(final float d) {
        return (long) ((d * 100f + 0.5f));
    }
     
    public static final CharSequence n2(final float x) {         
        if ((x < 0) || (x > 1.0f))
            throw new IllegalStateException("Invalid value for Texts.n2");
        
        final int hundredths = (int)hundredths(x);
        switch (hundredths) {
            //some common values
            case 100: return "1.00";
            case 99: return "0.99";
            case 90: return "0.90";
            case 0: return "0.00";
        }
                    
        if (hundredths > 9) {
            final int tens = hundredths/10;
            return new String(new char[] {
                '0', '.', (char)('0' + tens), (char)('0' + hundredths%10)
            });
        }
        else {
            return new String(new char[] {
                '0', '.', '0', (char)('0' + hundredths)
            });
        }            
    }

    public static CharSequence n2(final double p) {
        return n2((float)p);
    }

    /**
     * A simplified String representation of a TruthValue
     */
    public StringBuilder appendString(final StringBuilder sb, final boolean external) {        
        sb.ensureCapacity(11);
        return sb
            .append(DELIMITER)
            .append(n2(frequency))
            .append(SEPARATOR)
            .append(n2(confidence))
            .append(DELIMITER);        
    }

    public CharSequence name() {
        final StringBuilder sb =  new StringBuilder();
        return appendString(sb, false);
    }

    /** output representation */
    public CharSequence toStringExternal() {
        final StringBuilder sb =  new StringBuilder();
        return appendString(sb, true);
    }
    /**
     * Returns a String representation of a TruthValue, as used internally by the system
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return name().toString();
    }

    public TruthValue set(final float frequency, final double confidence) {
        setFrequency(frequency);
        setConfidence(confidence);
        return this;
    }

    public static void main(String[] args) throws Exception {
        TruthValue t = new TruthValue((float)0.90, 0.4);
        D.p(t.toString());
    }
}
